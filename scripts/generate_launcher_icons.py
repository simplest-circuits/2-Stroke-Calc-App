"""Regenerate launcher / splash icons with Android adaptive safe-zone padding.

Adaptive icons (API 26+) mask the outer ~18% on each side. Important artwork
must stay in the center ~66%. This script scales the glyph into that safe zone
for adaptive foreground + legacy mipmaps, while keeping full-bleed store assets.
"""
from __future__ import annotations

from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "androidApp" / "src" / "main" / "res"
DOCS_ANDROID = ROOT / "docs" / "AppIcons" / "android"
SOURCE = ROOT / "docs" / "AppIcons" / "appstore.png"

# Keep additional breathing room for round launcher masks.
SAFE_FRACTION = 0.61
SPLASH_FRACTION = 0.92
BG_THRESHOLD = 32

LEGACY_LAUNCHER_SIZES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}


def remove_near_black(img: Image.Image) -> Image.Image:
    img = img.convert("RGBA")
    pixels = img.load()
    width, height = img.size
    for y in range(height):
        for x in range(width):
            red, green, blue, _alpha = pixels[x, y]
            if red <= BG_THRESHOLD and green <= BG_THRESHOLD and blue <= BG_THRESHOLD:
                pixels[x, y] = (0, 0, 0, 0)
    return img


def fit_in_safe_zone(
    source: Image.Image,
    size: int,
    safe_fraction: float,
    *,
    transparent: bool,
) -> Image.Image:
    content = source.crop(source.getbbox())
    max_side = max(1, int(size * safe_fraction))
    content = content.copy()
    content.thumbnail((max_side, max_side), Image.Resampling.LANCZOS)
    background = (0, 0, 0, 0) if transparent else (0, 0, 0, 255)
    canvas = Image.new("RGBA", (size, size), background)
    offset = ((size - content.width) // 2, (size - content.height) // 2)
    canvas.paste(content, offset, content)
    return canvas


def save_png(image: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, format="PNG", optimize=True)


def main() -> None:
    source_full = Image.open(SOURCE).convert("RGBA")
    source_cut = remove_near_black(source_full)

    adaptive = fit_in_safe_zone(source_cut, 1024, SAFE_FRACTION, transparent=True)
    save_png(adaptive, RES / "drawable" / "adaptive_foreground.png")
    save_png(adaptive, DOCS_ANDROID / "adaptive-foreground.png")

    # Full-bleed marketing / adaptive source copies used elsewhere in the app.
    save_png(source_full, RES / "drawable" / "appstore.png")
    save_png(source_full, RES / "drawable" / "playstore.png")

    for density, size in LEGACY_LAUNCHER_SIZES.items():
        launcher = fit_in_safe_zone(source_cut, size, SAFE_FRACTION, transparent=False)
        save_png(launcher, RES / f"mipmap-{density}" / "ic_launcher.png")
        save_png(launcher, RES / f"mipmap-{density}" / "ic_launcher_round.png")
        save_png(launcher, DOCS_ANDROID / f"mipmap-{density}" / "ic_launcher.png")
        save_png(launcher, RES / f"drawable-{density}" / "ic_launcher.png")

    splash = fit_in_safe_zone(source_cut, 512, SPLASH_FRACTION, transparent=True)
    save_png(splash, RES / "drawable-nodpi" / "ic_splash_icon.png")

    print(f"Generated adaptive/legacy icons from {SOURCE}")
    print(f"Adaptive safe fraction: {SAFE_FRACTION:.0%}")


if __name__ == "__main__":
    main()
