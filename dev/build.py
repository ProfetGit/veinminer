#!/usr/bin/env python3
"""Validate the pack and build dist/Veinminer-<version>.zip (deterministic)."""
import json
import re
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PACK = ROOT / "pack"
DIST = ROOT / "dist"
NAMESPACE = "veinminer"


def version() -> str:
    load = (PACK / "data/veinminer/function/load.mcfunction").read_text()
    m = re.search(r'storage veinminer:meta version set value "([^"]+)"', load)
    if not m:
        sys.exit("version string not found in load.mcfunction")
    return m.group(1)


def check_json(errors: list[str]) -> None:
    for f in PACK.rglob("*"):
        if f.suffix in (".json", ".mcmeta"):
            try:
                json.loads(f.read_text())
            except json.JSONDecodeError as e:
                errors.append(f"{f.relative_to(ROOT)}: {e}")


def resource_roots() -> list[Path]:
    roots = [PACK]
    meta = json.loads((PACK / "pack.mcmeta").read_text())
    for entry in meta.get("overlays", {}).get("entries", []):
        roots.append(PACK / entry["directory"])
    return roots


def check_refs(errors: list[str]) -> None:
    roots = resource_roots()

    def exists(kind: str, rid: str, ext: str) -> bool:
        ns, path = rid.split(":", 1) if ":" in rid else ("minecraft", rid)
        return any((r / "data" / ns / kind / f"{path}{ext}").exists() for r in roots)

    ref = re.compile(r"\bfunction (#?)([a-z0-9_.-]+:[a-z0-9_./-]+)")
    pred = re.compile(r"\bpredicate ([a-z0-9_.-]+:[a-z0-9_./-]+)")
    tag = re.compile(r"#(veinminer:[a-z0-9_./-]+)")
    for root in roots:
        for f in (root / "data").rglob("*.mcfunction"):
            text = f.read_text()
            rel = f.relative_to(ROOT)
            for n, line in enumerate(text.splitlines(), 1):
                if line.lstrip().startswith("#"):
                    continue
                for is_tag, rid in ref.findall(line):
                    if not rid.startswith(NAMESPACE + ":"):
                        continue
                    kind = "tags/function" if is_tag else "function"
                    ext = ".json" if is_tag else ".mcfunction"
                    if not exists(kind, rid, ext):
                        errors.append(f"{rel}:{n}: missing function {'#' if is_tag else ''}{rid}")
                for rid in pred.findall(line):
                    if not exists("predicate", rid, ".json"):
                        errors.append(f"{rel}:{n}: missing predicate {rid}")
                for rid in tag.findall(line):
                    if "$(" in line and rid.endswith("/"):
                        continue
                    if not (exists("tags/block", rid, ".json") or exists("tags/item", rid, ".json")
                            or exists("tags/function", rid, ".json")):
                        errors.append(f"{rel}:{n}: missing tag #{rid}")
                if line.startswith("$") and "$(" not in line:
                    errors.append(f"{rel}:{n}: macro line without $(...) variable")
                if "$(" in line and not line.startswith("$"):
                    errors.append(f"{rel}:{n}: $(...) used outside a macro line")


def build(ver: str) -> Path:
    DIST.mkdir(exist_ok=True)
    out = DIST / f"Veinminer-{ver}.zip"
    for old in DIST.glob("Veinminer-*.zip"):
        old.unlink()
    files = sorted(p for p in PACK.rglob("*") if p.is_file())
    files.append(ROOT / "LICENSE")
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        for f in files:
            arc = f.name if f.parent == ROOT else f.relative_to(PACK).as_posix()
            info = zipfile.ZipInfo(arc, date_time=(2026, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o644 << 16
            z.writestr(info, f.read_bytes())
    return out


def main() -> None:
    errors: list[str] = []
    check_json(errors)
    check_refs(errors)
    if errors:
        print("\n".join(errors))
        sys.exit(f"{len(errors)} problem(s)")
    ver = version()
    major, minor, patch = (int(p) for p in ver.split("."))
    vid = major * 10000 + minor * 100 + patch
    if f"storage veinminer:meta version_id set value {vid}\n" not in (PACK / "data/veinminer/function/load.mcfunction").read_text():
        sys.exit(f"load.mcfunction: expected version_id {vid} for {ver}")
    for rel, needle in (("pack/pack.mcmeta", f"v{ver}"), ("pack/data/veinminer/function/settings.mcfunction", f"v{ver}"),
                        ("pack/data/veinminer/function/uninstall.mcfunction", f"Veinminer-{ver}.zip"),
                        ("CHANGELOG.md", f"## {ver}")):
        if needle not in (ROOT / rel).read_text():
            sys.exit(f"{rel}: expected '{needle}' (version mismatch)")
    out = build(ver)
    nfunc = sum(1 for _ in PACK.rglob("*.mcfunction"))
    print(f"OK  {nfunc} functions, refs resolved -> {out.relative_to(ROOT)} ({out.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
