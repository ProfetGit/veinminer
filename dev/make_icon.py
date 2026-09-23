#!/usr/bin/env python3
"""Compose the pack icon from Blockbench renders in dev/icon/frames/ (transparent PNGs, one per animation frame).

Outputs: dist/icon-animated.gif (Modrinth, <=256 KiB), dist/icon-512.png,
pack/pack.png (128px still), dev/icon/contact.png (frame sheet for review).
Source scene + animation: dev/icon/build_scene.js -> dev/icon/veinminer_icon_anim.bbmodel (Aseprite textures in dev/icon/sprites).
Background: dev/icon/sprites/bg_flat.aseprite (64px, flat purple + ground shadow placed for the current crop)."""
import sys
from pathlib import Path

from PIL import Image, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
ICON = ROOT / "dev" / "icon"
BACKGROUND = ICON / "sprites" / "bg_flat.png"
DIST = ROOT / "dist"
S = 512
FPS = 25
STILL_FRAME = 0
GIF_SIZE = 256
GIF_LIMIT = 256 * 1024
PALETTE = 255
CROP_PCT = 0.90
CROP_MARGIN = 1.08
OUTLINE = 19


def background() -> Image.Image:
    """Static pixel-art background, scaled up nearest-neighbour so its pixels match the art's."""
    return Image.open(BACKGROUND).convert("RGBA").resize((S, S), Image.NEAREST)


def compose(art: Image.Image, bg: Image.Image) -> Image.Image:
    outline_layer = Image.new("RGBA", (S, S), (10, 12, 18, 0))
    outline_layer.putalpha(art.getchannel("A").filter(ImageFilter.MaxFilter(OUTLINE)))
    return Image.alpha_composite(Image.alpha_composite(bg, outline_layer), art)


def main() -> None:
    files = sorted((ICON / "frames").glob("frame_*.png"))
    if not files:
        sys.exit("no frames in dev/icon/frames - render them from Blockbench first")
    raw = [Image.open(f).convert("RGBA") for f in files]
    boxes = [im.getchannel("A").getbbox() for im in raw]
    # robust bbox: brief extremes (swoosh tail, highest nugget bounce) may leave the frame
    lo = lambda vals: sorted(vals)[int(len(vals) * (1 - CROP_PCT))]
    hi = lambda vals: sorted(vals)[int(len(vals) * CROP_PCT) - 1]
    box = (lo([b[0] for b in boxes]), lo([b[1] for b in boxes]), hi([b[2] for b in boxes]), hi([b[3] for b in boxes]))
    side = int(max(box[2] - box[0], box[3] - box[1]) * CROP_MARGIN)
    cx, cy = (box[0] + box[2]) // 2, (box[1] + box[3]) // 2
    crop = (cx - side // 2, cy - side // 2, cx - side // 2 + side, cy - side // 2 + side)

    arts = [im.crop(crop).resize((S, S), Image.NEAREST) for im in raw]
    bg = background()
    frames = [compose(a, bg) for a in arts]

    frames[STILL_FRAME].convert("RGB").resize((128, 128), Image.LANCZOS).save(ROOT / "pack" / "pack.png", optimize=True)
    DIST.mkdir(exist_ok=True)
    frames[STILL_FRAME].convert("RGB").save(DIST / "icon-512.png", optimize=True)

    small = [f.convert("RGB").resize((GIF_SIZE, GIF_SIZE), Image.NEAREST) for f in frames]
    sheet = Image.new("RGB", (GIF_SIZE * len(small), GIF_SIZE))
    for i, f in enumerate(small):
        sheet.paste(f, (i * GIF_SIZE, 0))
    palette = sheet.quantize(colors=PALETTE, method=Image.Quantize.MEDIANCUT)
    gif = [f.quantize(palette=palette, dither=Image.Dither.NONE) for f in small]
    out = DIST / "icon-animated.gif"
    gif[0].save(out, save_all=True, append_images=gif[1:], duration=1000 // FPS, loop=0, optimize=True, disposal=1)

    cols = 10
    rows = (len(frames) + cols - 1) // cols
    contact = Image.new("RGB", (cols * 128, rows * 128))
    for i, f in enumerate(frames):
        contact.paste(f.convert("RGB").resize((128, 128), Image.LANCZOS), ((i % cols) * 128, (i // cols) * 128))
    contact.save(ICON / "contact.png")

    size = out.stat().st_size
    print(f"{len(frames)} frames @ {FPS} fps -> {out.relative_to(ROOT)} {size / 1024:.0f} KiB "
          f"({'OK' if size <= GIF_LIMIT else 'OVER'} Modrinth 256 KiB limit); "
          f"still = frame {STILL_FRAME} -> pack/pack.png, dist/icon-512.png")
    if size > GIF_LIMIT:
        sys.exit(1)


if __name__ == "__main__":
    main()
