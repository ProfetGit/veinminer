#!/usr/bin/env python3
"""strip.py TAKE T0 T1 [--crop x0,y0,x1,y1] [--step 1] [--cols 6] [--out file]: every video frame from tick T0 to T1
after the break mark, full resolution (optionally cropped), in one labelled sheet (default out/TAKE/strip_T0_T1.png)."""
import argparse, subprocess, tempfile
from pathlib import Path
from PIL import Image, ImageDraw

ap = argparse.ArgumentParser()
ap.add_argument("take"); ap.add_argument("t0", type=float); ap.add_argument("t1", type=float)
ap.add_argument("--crop"); ap.add_argument("--step", type=int, default=1); ap.add_argument("--cols", type=int, default=6)
ap.add_argument("--scale", type=float, default=1.0); ap.add_argument("--out")
a = ap.parse_args()
d = Path(__file__).resolve().parent / "out" / a.take
B = next(int(l.split("\t")[0]) for l in (d / f"{a.take}.sounds.tsv").read_text().splitlines()[2:]
         if l.split("\t")[1].endswith(".break") and float(l.split("\t")[4]) > 0.9)
f0, f1 = int(round(B + 3 * a.t0)), int(round(B + 3 * a.t1))
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", str(d / f"{a.take}.mp4"), "-vf",
                    f"select=between(n\\,{f0}\\,{f1})", "-fps_mode", "passthrough", f"{tmp}/f%04d.png"], check=True)
    files = sorted(Path(tmp).glob("f*.png"))[::a.step]
    ims = []
    for k, p in enumerate(files):
        im = Image.open(p).convert("RGB")
        if a.crop:
            im = im.crop(tuple(int(v) for v in a.crop.split(",")))
        if a.scale != 1:
            im = im.resize((int(im.width * a.scale), int(im.height * a.scale)), Image.NEAREST)
        f = f0 + k * a.step
        ImageDraw.Draw(im).text((4, 2), f"f{f} t{(f - B) / 3:+.2f}", fill=(255, 255, 0))
        ims.append(im)
W, H = ims[0].size
rows = (len(ims) + a.cols - 1) // a.cols
out = Image.new("RGB", (W * a.cols, H * rows))
for k, im in enumerate(ims):
    out.paste(im, ((k % a.cols) * W, (k // a.cols) * H))
o = Path(a.out) if a.out else d / f"strip_{a.t0:g}_{a.t1:g}.png"
out.save(o)
print(o, out.size)
