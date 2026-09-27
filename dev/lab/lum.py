#!/usr/bin/env python3
"""lum.py TAKE x0,y0,x1,y1 [T0 T1]: mean luminance of a box per tick (every 3rd frame) around the break mark."""
import subprocess, sys, tempfile, numpy as np
from pathlib import Path
from PIL import Image
T, box = sys.argv[1], [int(v) for v in sys.argv[2].split(",")]
t0, t1 = (float(sys.argv[3]), float(sys.argv[4])) if len(sys.argv) > 4 else (-3, 14)
d = Path(__file__).resolve().parent / "out" / T
B = next(int(l.split("\t")[0]) for l in (d / f"{T}.sounds.tsv").read_text().splitlines()[2:] if l.split("\t")[1].endswith(".break") and float(l.split("\t")[4]) > 0.9)
f0, f1 = int(B + 3 * t0), int(B + 3 * t1)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["ffmpeg", "-v", "error", "-i", str(d / f"{T}.mp4"), "-vf", f"select=between(n\\,{f0}\\,{f1})", "-fps_mode", "passthrough", f"{tmp}/f%04d.png"], check=True)
    vals = [np.asarray(Image.open(p).convert("L"), dtype=float)[box[1]:box[3], box[0]:box[2]].mean() for p in sorted(Path(tmp).glob("*.png"))]
print(T, " ".join(f"t{(k * 3 + f0 - B) / 3:+.0f}:{v:.1f}" for k, v in zip(range(0, len(vals)), vals[::3])))
