#!/usr/bin/env python3
"""Compose the Modrinth description banner (3:1) from Blockbench renders in dev/icon/banner_frames/.

Outputs: dist/banner.png (static, STILL_FRAME) and dist/banner-animated.gif (full loop).
Art: dev/icon/build_scene.js rendered with VM.camera(0.185) -> banner_frames/ (1600px, 1:1, no resampling).
Background, title and tagline: dev/icon/sprites/banner_*.aseprite, scaled up nearest-neighbour."""
import sys
from pathlib import Path

from PIL import Image, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
ICON = ROOT / "dev" / "icon"
SPRITES = ICON / "sprites"
DIST = ROOT / "dist"
W, H = 1536, 512
FPS = 25
STILL_FRAME = 13
ART_OFFSET = (331, -539)  # banner = frame + offset: rest-pose vein centre at x 1150, loop's vertical extent centred
OUTLINE = 17
LAYERS = (("banner_bg", 8, (0, 0)), ("banner_title", 8, (72, 160)), ("banner_tagline", 6, (74, 300)))
GIF_LIMIT = 5 * 1024 * 1024


def sprite(name: str, scale: int) -> Image.Image:
    im = Image.open(SPRITES / f"{name}.png").convert("RGBA")
    return im.resize((im.width * scale, im.height * scale), Image.NEAREST)


def art(frame: Path) -> Image.Image:
    im = Image.open(frame).convert("RGBA")
    dx, dy = ART_OFFSET
    return im.crop((-dx, -dy, W - dx, H - dy))


def compose(a: Image.Image, bg: Image.Image, text: list[tuple[Image.Image, tuple[int, int]]]) -> Image.Image:
    out = bg.copy()
    outline = Image.new("RGBA", (W, H), (10, 12, 18, 0))
    outline.putalpha(a.getchannel("A").filter(ImageFilter.MaxFilter(OUTLINE)))
    out.alpha_composite(outline)
    out.alpha_composite(a)
    for im, pos in text:
        out.alpha_composite(im, pos)
    return out


def main() -> None:
    files = sorted((ICON / "banner_frames").glob("frame_*.png"))
    if not files:
        sys.exit("no frames in dev/icon/banner_frames - render them from Blockbench first (VM.camera(0.185))")
    (bg_name, bg_scale, _), *text_layers = LAYERS
    bg = sprite(bg_name, bg_scale)
    text = [(sprite(name, scale), pos) for name, scale, pos in text_layers]
    frames = [compose(art(f), bg, text).convert("RGB") for f in files]

    DIST.mkdir(exist_ok=True)
    frames[STILL_FRAME].save(DIST / "banner.png", optimize=True)
    sheet = Image.new("RGB", (W, H * len(frames)))
    for i, f in enumerate(frames):
        sheet.paste(f, (0, i * H))
    palette = sheet.quantize(colors=255, method=Image.Quantize.MEDIANCUT)
    gif = [f.quantize(palette=palette, dither=Image.Dither.NONE) for f in frames]
    out = DIST / "banner-animated.gif"
    gif[0].save(out, save_all=True, append_images=gif[1:], duration=1000 // FPS, loop=0, optimize=True, disposal=1)

    size = out.stat().st_size
    print(f"dist/banner.png (frame {STILL_FRAME}); {len(frames)} frames -> {out.relative_to(ROOT)} {size / 1024:.0f} KiB "
          f"({'OK' if size <= GIF_LIMIT else 'OVER'} Modrinth 5 MiB gallery limit)")
    if size > GIF_LIMIT:
        sys.exit(1)


if __name__ == "__main__":
    main()
