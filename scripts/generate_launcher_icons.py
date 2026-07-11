"""Regenerate splash icon and legacy launcher mipmaps from drawable/appstore.png.

The adaptive launcher icon (API 26+) uses appstore.png directly without modification.
Only the splash icon is cropped/fitted; legacy mipmaps are downscaled copies of the full image.
"""
from __future__ import annotations

from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app" / "src" / "main" / "res"
SOURCE = RES / "drawable" / "appstore.png"

LEGACY_LAUNCHER_SIZES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

BG_THRESHOLD = 28


def load_icon_with_transparency(path: Path) -> Image.Image:
    img = Image.open(path).convert("RGBA")
    pixels = img.load()
    width, height = img.size
    for y in range(height):
        for x in range(width):
            red, green, blue, alpha = pixels[x, y]
            if red <= BG_THRESHOLD and green <= BG_THRESHOLD and blue <= BG_THRESHOLD:
                pixels[x, y] = (0, 0, 0, 0)
    return img


def fit_in_square(image: Image.Image, size: int) -> Image.Image:
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    content = image.crop(image.getbbox())
    content.thumbnail((size, size), Image.Resampling.LANCZOS)
    offset = ((size - content.width) // 2, (size - content.height) // 2)
    canvas.paste(content, offset, content)
    return canvas


def resize_full_image(image: Image.Image, size: int) -> Image.Image:
    resized = image.resize((size, size), Image.Resampling.LANCZOS)
    return resized.convert("RGBA")


def save_png(image: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, format="PNG", optimize=True)


def main() -> None:
    source = Image.open(SOURCE).convert("RGBA")
    splash_source = load_icon_with_transparency(SOURCE)

    for density, size in LEGACY_LAUNCHER_SIZES.items():
        launcher = resize_full_image(source, size)
        save_png(launcher, RES / f"mipmap-{density}" / "ic_launcher.png")
        save_png(launcher, RES / f"mipmap-{density}" / "ic_launcher_round.png")

    splash = fit_in_square(splash_source, 512)
    save_png(splash, RES / "drawable-nodpi" / "ic_splash_icon.png")

    print("Generated legacy mipmaps and splash icon from", SOURCE)
    print("Adaptive icon uses appstore.png directly (no transformation).")


if __name__ == "__main__":
    main()
