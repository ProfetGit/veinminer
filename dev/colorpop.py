#!/usr/bin/env python3
"""Colour pop meter: consecutive-frame difference of a still scene, to find the frame where the real block turns into the stand-in
and say how far the block's own pixels moved (linear light, 0..1) against the frame-to-frame noise of the same scene.

  colorpop.py <frames dir> [x0 y0 x1 y1]     region (default: whole frame minus the hotbar/hand)
"""
import sys, os
import numpy as np
from PIL import Image

def lin(a):
    a = a / 255.0
    return np.where(a <= 0.04045, a / 12.92, ((a + 0.055) / 1.055) ** 2.4)

def load(path, box):
    x = np.asarray(Image.open(path).convert("RGB")).astype(float)
    x0, y0, x1, y1 = box
    return lin(x[y0:y1, x0:x1])

def main():
    d = sys.argv[1]
    box = tuple(int(v) for v in sys.argv[2:6]) if len(sys.argv) >= 6 else (200, 40, 1000, 560)
    fs = sorted(f for f in os.listdir(d) if f.endswith(".png"))
    prev = None
    rows = []
    for i, f in enumerate(fs):
        cur = load(os.path.join(d, f), box)
        if prev is not None:
            diff = np.abs(cur - prev).max(2)
            rows.append((i, diff.mean(), np.percentile(diff, 99.9), diff.max()))
        prev = cur
    mean = np.array([r[1] for r in rows])
    k = int(np.argmax(mean))
    base = np.median(mean)
    print(f"frames {len(fs)}  baseline mean step {base:.5f}  max step {mean[k]:.5f} at frame {rows[k][0]}  ratio {mean[k] / max(base, 1e-9):.1f}x")
    for r in rows[max(0, k - 3):k + 4]:
        print(f"  f{r[0]:03d}  mean {r[1]:.5f}  p99.9 {r[2]:.4f}  max {r[3]:.4f}")
    a = load(os.path.join(d, fs[rows[k][0] - 1]), box)
    b = load(os.path.join(d, fs[rows[k][0] + 1 if rows[k][0] + 1 < len(fs) else rows[k][0]]), box)
    # the block's own pixels: where the scene is saturated (ore) or bright
    sat = (a.max(2) - a.min(2)) > 0.15
    for name, m in (("ore pixels", sat), ("all pixels", np.ones(sat.shape, bool))):
        if m.sum():
            d2 = np.abs(b - a).max(2)[m]
            print(f"  across the step, {name} ({int(m.sum())}): mean {d2.mean():.5f}  p95 {np.percentile(d2, 95):.4f}  max {d2.max():.4f}")
    lum = lambda x: 0.2126 * x[..., 0] + 0.7152 * x[..., 1] + 0.0722 * x[..., 2]
    if sat.sum():
        print(f"  ore luminance (linear): before {lum(a)[sat].mean():.5f}  after {lum(b)[sat].mean():.5f}  ratio {lum(b)[sat].mean() / lum(a)[sat].mean():.4f}")


def pops(d, box=(200, 40, 1000, 560), win=6):
    """Mean of the `win` frames before the swap vs after it on the ore pixels, against the same two windows with no swap between."""
    fs = sorted(f for f in os.listdir(d) if f.endswith(".png"))
    imgs = [load(os.path.join(d, f), box) for f in fs]
    n = len(imgs)
    steps = [np.abs(imgs[i + 1] - imgs[i]).mean() for i in range(n - 1)]
    # the swap: the biggest change of the 5-frame average
    avg = lambda a, b: np.mean(imgs[a:b], axis=0)
    best, k = -1, 0
    for i in range(win, n - win):
        v = np.abs(avg(i, i + win) - avg(i - win, i)).mean()
        if v > best:
            best, k = v, i
    before, after = avg(k - win, k), avg(k, k + win)
    sat = (before.max(2) - before.min(2)) > 0.12
    base_a, base_b = avg(2, 2 + win), avg(2 + win, 2 + 2 * win)
    lum = lambda x: 0.2126 * x[..., 0] + 0.7152 * x[..., 1] + 0.0722 * x[..., 2]
    for name, a, b in (("swap", before, after), ("noise (no swap)", base_a, base_b)):
        dd = np.abs(b - a).max(2)[sat]
        sign = (lum(b) - lum(a))[sat]
        print(f"  {name:16s} swap@{k}: ore px {int(sat.sum())}  mean |dRGB| {dd.mean():.5f}  p95 {np.percentile(dd, 95):.4f}  mean dLum {sign.mean():+.5f} ({100 * sign.mean() / lum(a)[sat].mean():+.2f} %)")

if len(sys.argv) > 1 and sys.argv[1] == "pops":
    sys.argv.pop(1)
    pops(sys.argv[1])
else:
    main()
