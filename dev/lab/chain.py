#!/usr/bin/env python3
"""Per-member analysis of a Veinminer lab take: every stand-in and every loot ghost aligned on its own start, so the
chain's parts can be judged against each other (lablib's group curves mix members at different phases).

  python3 dev/lab/chain.py TAKE_DIR [TAKE_DIR ...] [--plot out.png]

Writes chain.txt (and chain.png) into each take folder; with several takes, one overlaid plot (--plot).
Stand-ins: scale y / xz against ticks since first drawn. Ghosts: height and vertical speed against ticks since launch
(first frame drawn above 2 % scale), a least-squares parabola, and the hand-off to the real item (model bottom of the
ghost's last frame vs the real item's drawn bottom, from <take>.items.tsv when the probe logged it).
"""
import argparse
import math
import sys
from pathlib import Path

import numpy as np

TPF = 3
FLIGHT = 12  # ticks from launch to contact (dev/gen.py)
BLOCK_ITEMS = ("_ore", "ancient_debris", "_block")


def load(d: Path):
    name = d.name
    disp, kinds, ents, items, sounds = {}, {}, {}, {}, []
    for line in (d / f"{name}.displays.tsv").read_text().splitlines()[1:]:
        c = line.split("\t")
        m = np.array([float(v) for v in c[4:16]]).reshape(3, 4)
        disp.setdefault(int(c[3]), []).append((int(c[0]), m))
    for line in (d / f"{name}.kinds.tsv").read_text().splitlines()[1:]:
        c = line.split("\t") + ["", ""]
        kinds[int(c[0])] = (c[1], c[2])
    e = d / f"{name}.entities.tsv"
    if e.exists():
        for line in e.read_text().splitlines()[1:]:
            c = line.split("\t")
            ents.setdefault(int(c[3]), []).append((int(c[0]), c[4], float(c[5]), float(c[6]), float(c[7])))
    it = d / f"{name}.items.tsv"
    if it.exists():
        for line in it.read_text().splitlines()[1:]:
            c = line.split("\t")
            items.setdefault(int(c[3]), {})[int(c[0])] = (float(c[5]), float(c[7]), float(c[9]))  # y, lift, spin
    s = d / f"{name}.sounds.tsv"
    for line in s.read_text().splitlines()[2:]:
        c = line.split("\t")
        sounds.append({"frame": int(c[0]), "id": c[1].split(":", 1)[-1], "vol": float(c[4]), "pitch": float(c[5])})
    return disp, kinds, ents, items, sounds


def scales(m):
    return np.linalg.norm(m[:, :3], axis=0)  # x, y, z column norms


def analyse(d: Path) -> dict:
    disp, kinds, ents, items, sounds = load(d)
    brk = next((s["frame"] for s in sounds if s["id"].endswith(".break") and s["vol"] > 0.9), None)
    pops = [s for s in sounds if s["id"] == "entity.chicken.egg"]
    rings = [s for s in sounds if s["id"].endswith(".break") and abs(s["vol"] - 0.8) < 0.01]
    rep = {"take": d.name, "break": brk, "standins": [], "ghosts": []}
    for i, rows in sorted(disp.items()):
        kind, label = kinds.get(i, ("?", ""))
        rows.sort(key=lambda r: r[0])
        if kind == "block":
            f0 = rows[0][0]
            t = np.array([(f - f0) / TPF for f, _ in rows])
            sc = np.array([scales(m) for _, m in rows])
            top = int(np.argmax(sc[:, 1]))
            # first frame the squash visibly starts (y scale below 0.995)
            moving = np.nonzero(sc[:, 1] < 0.995)[0]
            rep["standins"].append({"id": i, "label": label, "f0": f0, "t": t, "sy": sc[:, 1], "sx": sc[:, 0],
                                    "still": (t[moving[0]] if len(moving) else None), "peak_f": rows[top][0],
                                    "min_sy": float(sc[:, 1].min()), "max_sy": float(sc[:, 1].max()),
                                    "last_f": rows[-1][0]})
        elif kind == "item":
            sc = np.array([scales(m)[1] for _, m in rows])
            vis = np.nonzero(sc > 0.02)[0]
            if not len(vis):
                continue
            rows = rows[vis[0]:]
            sc = sc[vis[0]:]
            f0 = rows[0][0]
            o = np.array([m @ np.array([0, 0, 0, 1.0]) for _, m in rows])
            t = np.array([(f - f0) / TPF for f, _ in rows])
            y = o[:, 1] - o[0, 1]
            hd = np.hypot(o[:, 0] - o[0, 0], o[:, 2] - o[0, 2])
            fl = t <= FLIGHT  # the arc itself; the contact squash comes after it
            fit = np.polyfit(t[fl], y[fl], 2) if fl.sum() > 5 else None
            res = float(np.max(np.abs(np.polyval(fit, t[fl]) - y[fl]))) if fit is not None else None
            # per-tick vertical speed (blocks/tick) from positions one tick apart, and its jumps
            fr = {f: k for k, (f, _) in enumerate(rows)}
            vy = [(t[k], (o[fr[f + TPF], 1] - o[k, 1])) for k, (f, _) in enumerate(rows) if f + TPF in fr]
            acc = [(vy[k + TPF][0], vy[k + TPF][1] - vy[k][1]) for k in range(len(vy) - TPF) if vy[k + TPF][0] + 1 <= FLIGHT] if len(vy) > TPF else []
            block = any(label.endswith(b) for b in BLOCK_ITEMS)
            bottom = o[-1, 1] + sc[-1] * (0.0625 if block else -0.125)
            g = {"id": i, "label": label, "f0": f0, "t": t, "y": y, "hd": hd, "vy": vy, "acc": acc, "res": res,
                 "apex": float(y.max()), "flight": float(t[-1]), "last_f": rows[-1][0], "end": o[-1, :3], "bottom": bottom,
                 "scale_end": float(sc[-1])}
            # the real item that takes over: appears within 2 frames of the ghost's last frame, within 0.8 blocks
            best = None
            for eid, er in ents.items():
                if er[0][1] != "minecraft:item" or not (rows[-1][0] - 2 <= er[0][0] <= rows[-1][0] + 4):
                    continue
                dist = math.dist(er[0][2:5], o[-1, :3])
                if dist < 0.8 and (best is None or dist < best[0]):
                    best = (dist, eid)
            if best:
                er = ents[best[1]]
                it = items.get(best[1], {})
                path = []
                for f, _, x, yy, z in er[:40]:
                    lift = it[f][1] if f in it else 0.1625  # 1/16 + mean bob when the probe didn't log items
                    path.append(((f - rows[-1][0]) / TPF, yy + lift))
                g["item"] = {"id": best[1], "dist": best[0], "path": path, "logged": bool(it),
                             "dy": path[0][1] - bottom, "spin0": (it[er[0][0]][2] if er[0][0] in it else None)}
            rep["ghosts"].append(g)
    rep["pops"] = [(p["frame"], p["pitch"]) for p in pops]
    rep["rings"] = [(p["frame"], p["pitch"]) for p in rings]
    return rep


def text(rep: dict) -> str:
    b = rep["break"] or 0
    L = [f"## {rep['take']}", f"break frame {rep['break']}"]
    st = rep["standins"]
    if st:
        L.append(f"stand-ins: {len(st)}; spawn ticks after break {sorted({round((s['f0'] - b) / TPF, 1) for s in st})}")
        L.append(f"  squash min y {min(s['min_sy'] for s in st):.3f}, stretch max y {max(s['max_sy'] for s in st):.3f}")
        stills = [s['still'] for s in st if s['still'] is not None]
        if stills:
            L.append(f"  drawn unsquashed after spawn: {min(stills):.2f}..{max(stills):.2f} ticks")
        life = [(s['last_f'] - s['f0']) / TPF for s in st]
        L.append(f"  drawn for {min(life):.1f}..{max(life):.1f} ticks")
        # pop sound vs each stand-in's stretch peak (frames)
        if rep["pops"]:
            offs = []
            for s in st:
                near = min(rep["pops"], key=lambda p: abs(p[0] - s["peak_f"]))
                offs.append(near[0] - s["peak_f"])
            L.append(f"  pop sound minus stretch-peak frame: {sorted(set(offs))}")
    gh = rep["ghosts"]
    if gh:
        L.append(f"ghosts drawn: {len(gh)}; launch ticks after break {sorted({round((g['f0'] - b) / TPF, 1) for g in gh})}")
        L.append(f"  flight {min(g['flight'] for g in gh):.1f}..{max(g['flight'] for g in gh):.1f} ticks, apex {min(g['apex'] for g in gh):.2f}..{max(g['apex'] for g in gh):.2f}")
        res = [g['res'] for g in gh if g['res'] is not None]
        if res:
            L.append(f"  parabola residual max {max(res):.3f} blocks")
        jumps = [(round(t, 1), round(a, 3)) for g in gh for t, a in g['acc']]
        if jumps:
            a = np.array([j[1] for j in jumps])
            L.append(f"  vertical accel per tick: min {a.min():.3f} max {a.max():.3f} (a parabola is constant); worst {sorted(jumps, key=lambda j: -abs(j[1]))[:4]}")
        hand = [g["item"] for g in gh if "item" in g]
        if hand:
            dys = np.array([h["dy"] for h in hand])
            L.append(f"  hand-off to the real item: {len(hand)}/{len(gh)} matched, drawn bottom jump {dys.min():+.3f}..{dys.max():+.3f} blocks"
                     f" ({'probe-logged bob' if hand[0]['logged'] else 'mean bob assumed, ±0.1'})")
            L.append(f"  ghost scale at its last frame: {sorted({round(g['scale_end'], 3) for g in gh})}")
    if rep["rings"]:
        L.append(f"ring sounds: {len(rep['rings'])}, ticks {[round((f - b) / TPF, 1) for f, _ in rep['rings']]}")
        L.append(f"  pitches {[round(p, 2) for _, p in rep['rings']]}")
        L.append(f"  plops {[round(p, 2) for _, p in rep['pops']]}")
        last = [p for _, p in rep["rings"]]
        flat = sum(1 for k in range(1, len(last)) if abs(last[k] - last[k - 1]) < 1e-3)
        if flat:
            L.append(f"  pitch holds flat for {flat} of {len(last) - 1} steps")
    ends = [s["last_f"] for s in st] + [g["last_f"] for g in gh]
    if ends and rep["break"]:
        L.append(f"chain: last pop {max((s['peak_f'] - b) / TPF for s in st):.1f} ticks, last landing {max((g['last_f'] - b) / TPF for g in gh) if gh else 0:.1f} ticks after the break")
    return "\n".join(L) + "\n"


def plot(reps: list, out: Path) -> None:
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    fig, ax = plt.subplots(5, 1, figsize=(12, 17))
    cols = plt.rcParams["axes.prop_cycle"].by_key()["color"]
    for n, rep in enumerate(reps):
        c = cols[n % len(cols)]
        lab = rep["take"]
        for k, s in enumerate(rep["standins"]):
            ax[0].plot(s["t"], s["sy"], color=c, lw=0.8, alpha=0.6, label=f"{lab} y" if k == 0 else None)
            ax[0].plot(s["t"], s["sx"], color=c, lw=0.8, alpha=0.6, ls=":", label=f"{lab} xz" if k == 0 else None)
        for k, g in enumerate(rep["ghosts"]):
            ax[1].plot(g["t"], g["y"], color=c, lw=0.8, alpha=0.7, label=lab if k == 0 else None)
            ax[2].plot([v[0] for v in g["vy"]], [v[1] for v in g["vy"]], color=c, lw=0.8, alpha=0.7, label=lab if k == 0 else None)
            ax[3].plot(g["t"], g["hd"], color=c, lw=0.8, alpha=0.7, label=lab if k == 0 else None)
            if "item" in g:
                tb = g["t"] - g["t"][-1]
                ax[4].plot(tb[-15:], (g["y"] - g["y"][-1])[-15:], color=c, lw=0.8, alpha=0.7, label=f"{lab} ghost" if k == 0 else None)
                p = g["item"]["path"]
                ax[4].plot([q[0] for q in p], [q[1] - g["bottom"] for q in p], color=c, lw=0.8, ls="--", alpha=0.7,
                           label=f"{lab} real item" if k == 0 else None)
    for a, (title, yl) in zip(ax, [("stand-ins: scale (y solid, xz dotted) since first drawn", "scale"),
                                   ("ghosts: height since launch", "blocks"),
                                   ("ghosts: vertical speed per tick", "blocks/tick"),
                                   ("ghosts: horizontal distance since launch", "blocks"),
                                   ("hand-off: ghost origin height (solid) and real item drawn bottom (dashed), relative to the ghost's last bottom", "blocks")]):
        a.set_title(title, fontsize=9)
        a.set_ylabel(yl)
        a.grid(alpha=0.3)
        a.legend(fontsize=7)
    ax[-1].set_xlabel("ticks")
    fig.tight_layout()
    fig.savefig(out, dpi=80)
    plt.close(fig)


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("takes", nargs="+")
    ap.add_argument("--plot")
    a = ap.parse_args()
    reps = []
    for t in a.takes:
        d = Path(t).resolve()
        rep = analyse(d)
        reps.append(rep)
        txt = text(rep)
        (d / "chain.txt").write_text(txt)
        plot([rep], d / "chain.png")
        sys.stdout.write(txt)
    if a.plot:
        plot(reps, Path(a.plot))


if __name__ == "__main__":
    main()
