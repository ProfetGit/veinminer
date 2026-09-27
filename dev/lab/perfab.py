#!/usr/bin/env python3
"""perfab.py BASELINE.zip [runs]: A/B of every tick of the big chain (dev/lab/probe/perf_ticks.txt), baseline and current
interleaved so load from other processes hits both alike. Prints per-tick medians and the chain's total."""
import re, statistics, subprocess, sys, tempfile, shutil
from pathlib import Path
sys.path.insert(0, str(Path.home() / ".claude/skills/polish/scripts"))
import perf  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
probe = ROOT / "dev/lab/probe/perf_ticks.txt"
zipb = Path(sys.argv[1]).resolve()
runs = int(sys.argv[2]) if len(sys.argv) > 2 else 3
res = {"base": [], "cur": []}
with tempfile.TemporaryDirectory(prefix="perfab-") as tmp:
    sh = perf.shadow(ROOT, zipb, Path(tmp))
    for i in range(runs):
        for tag, root in (("base", sh), ("cur", ROOT)):
            work = Path(tempfile.mkdtemp(prefix=f"perfab-{tag}-"))
            r = subprocess.run(["bash", str(root / "dev/test/run.sh"), "26.3", str(work), "explore", str(probe)], cwd=ROOT,
                               env={**__import__("os").environ, "SKIP_BUILD": "1"}, capture_output=True, text=True)
            shutil.rmtree(work, ignore_errors=True)
            m = re.search(r"last 62 tick ms:(.*)", r.stdout)
            res[tag].append([float(v) for v in m[1].split()])
            print(tag, i + 1, f"sum {sum(res[tag][-1]):.1f} ms", flush=True)
for tag in res:
    med = [statistics.median(col) for col in zip(*res[tag])]
    print(tag, "per-tick median:", " ".join(f"{v:.1f}" for v in med))
    print(tag, f"chain total (median of per-tick medians summed): {sum(med):.1f} ms, worst tick {max(med):.1f} ms")
