"""Generate Google Play feature graphic (1024x500) from app icon assets."""
from __future__ import annotations

import argparse
from dataclasses import dataclass
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app" / "src" / "main" / "res"
OUT_DIR = ROOT / "store-assets" / "google-play"

WIDTH = 1024
HEIGHT = 500
GOLD = (232, 197, 71)
GOLD_DIM = (212, 175, 55)
CREAM = (245, 230, 184)
WHITE = (255, 255, 255)
TITLE = "2-Stroke Calc"


@dataclass(frozen=True)
class LocaleCopy:
    folder: str
    subtitle: str
    pills: list[str]


LOCALES: dict[str, LocaleCopy] = {
    "de": LocaleCopy(
        folder="de-DE",
        subtitle="Dein Werkzeug für 2-Takt-Berechnungen",
        pills=[
            "Getriebe",
            "Steuerzeiten",
            "Zündzeitpunkt",
            "Gemisch",
            "Fahrzeugverwaltung",
            "uvm.",
        ],
    ),
    "en": LocaleCopy(
        folder="en-US",
        subtitle="Your toolkit for 2-stroke calculations",
        pills=[
            "Gearbox",
            "Port timing",
            "Ignition timing",
            "Oil/fuel mix",
            "Vehicles",
            "and more",
        ],
    ),
}


def load_font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    candidates = [
        Path(r"C:\Windows\Fonts\segoeuib.ttf") if bold else Path(r"C:\Windows\Fonts\segoeui.ttf"),
        Path(r"C:\Windows\Fonts\arialbd.ttf") if bold else Path(r"C:\Windows\Fonts\arial.ttf"),
    ]
    for path in candidates:
        if path.exists():
            return ImageFont.truetype(str(path), size=size)
    return ImageFont.load_default()


def draw_pill(
    draw: ImageDraw.ImageDraw,
    xy: tuple[int, int, int, int],
    text: str,
    font: ImageFont.FreeTypeFont | ImageFont.ImageFont,
    pill_h: int = 32,
) -> None:
    x0, y0, x1, _ = xy
    y1 = y0 + pill_h
    draw.rounded_rectangle((x0, y0, x1, y1), radius=14, outline=GOLD + (180,), width=2, fill=(20, 18, 14, 200))
    cx = (x0 + x1) // 2
    cy = y0 + pill_h // 2
    draw.text((cx, cy), text, font=font, fill=CREAM, anchor="mm")


def measure_pills_layout(
    labels: list[str],
    start_x: int,
    max_x: int,
    font: ImageFont.FreeTypeFont | ImageFont.ImageFont,
    pill_h: int = 32,
    pill_h_padding: int = 18,
    pill_gap: int = 5,
    row_gap: int = 10,
    allow_wrap: bool = False,
) -> tuple[int, list[tuple[str, int, int, int, int]]]:
    cursor_x = start_x
    cursor_y = 0
    row_start_x = start_x
    placements: list[tuple[str, int, int, int, int]] = []

    for label in labels:
        bbox = font.getbbox(label) if hasattr(font, "getbbox") else (0, 0, 0, 0)
        pill_w = (bbox[2] - bbox[0]) + pill_h_padding
        if allow_wrap and cursor_x + pill_w > max_x and cursor_x > row_start_x:
            cursor_x = row_start_x
            cursor_y += pill_h + row_gap
        placements.append((label, cursor_x, cursor_y, cursor_x + pill_w, cursor_y + pill_h))
        cursor_x += pill_w + pill_gap

    total_height = cursor_y + pill_h
    return total_height, placements


def draw_pills(
    draw: ImageDraw.ImageDraw,
    labels: list[str],
    start_x: int,
    start_y: int,
    max_x: int,
    font: ImageFont.FreeTypeFont | ImageFont.ImageFont,
    pill_h: int = 32,
    pill_h_padding: int = 18,
    pill_gap: int = 5,
    row_gap: int = 10,
    allow_wrap: bool = False,
) -> None:
    _, placements = measure_pills_layout(
        labels, start_x, max_x, font, pill_h, pill_h_padding, pill_gap, row_gap, allow_wrap
    )
    for label, x0, y0, x1, y1 in placements:
        draw_pill(draw, (x0, start_y + y0, x1, start_y + y1), label, font, pill_h=pill_h)


def text_bbox(font: ImageFont.FreeTypeFont | ImageFont.ImageFont, text: str) -> tuple[int, int, int, int]:
    if hasattr(font, "getbbox"):
        return font.getbbox(text)
    return (0, 0, 0, 0)


def draw_text_block(
    draw: ImageDraw.ImageDraw,
    *,
    text_x: int,
    start_y: int,
    title_font: ImageFont.FreeTypeFont | ImageFont.ImageFont,
    subtitle_font: ImageFont.FreeTypeFont | ImageFont.ImageFont,
    pill_font: ImageFont.FreeTypeFont | ImageFont.ImageFont,
    subtitle: str,
    pills: list[str],
    max_x: int,
    title_subtitle_gap: int = 14,
    subtitle_pills_gap: int = 26,
) -> None:
    title_box = text_bbox(title_font, TITLE)
    subtitle_box = text_bbox(subtitle_font, subtitle)

    y = start_y
    draw.text((text_x, y - title_box[1]), TITLE, font=title_font, fill=GOLD + (255,))
    y += title_box[3] - title_box[1] + title_subtitle_gap
    draw.text((text_x, y - subtitle_box[1]), subtitle, font=subtitle_font, fill=CREAM + (230,))
    y += subtitle_box[3] - subtitle_box[1] + subtitle_pills_gap
    draw_pills(
        draw,
        pills,
        start_x=text_x,
        start_y=y,
        max_x=max_x,
        font=pill_font,
        allow_wrap=False,
    )


def measure_text_block_height(
    title_font: ImageFont.FreeTypeFont | ImageFont.ImageFont,
    subtitle_font: ImageFont.FreeTypeFont | ImageFont.ImageFont,
    pill_font: ImageFont.FreeTypeFont | ImageFont.ImageFont,
    subtitle: str,
    pills: list[str],
    text_x: int,
    max_x: int,
    title_subtitle_gap: int = 14,
    subtitle_pills_gap: int = 26,
) -> int:
    title_box = text_bbox(title_font, TITLE)
    subtitle_box = text_bbox(subtitle_font, subtitle)
    pills_height, _ = measure_pills_layout(
        pills, text_x, max_x, pill_font, allow_wrap=False
    )
    title_h = title_box[3] - title_box[1]
    subtitle_h = subtitle_box[3] - subtitle_box[1]
    return title_h + title_subtitle_gap + subtitle_h + subtitle_pills_gap + pills_height


def generate_feature_graphic(locale: LocaleCopy) -> Path:
    icon_path = RES / "drawable-nodpi" / "ic_splash_icon.png"
    if not icon_path.exists():
        icon_path = RES / "drawable" / "appstore.png"

    icon = Image.open(icon_path).convert("RGBA")
    icon_height = 360
    icon_width = int(icon.width * icon_height / icon.height)
    icon = icon.resize((icon_width, icon_height), Image.Resampling.LANCZOS)

    canvas = Image.new("RGB", (WIDTH, HEIGHT), (0, 0, 0))

    icon_x = 8
    icon_y = (HEIGHT - icon.height) // 2
    canvas.paste(icon, (icon_x, icon_y), icon)

    overlay = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)

    title_font = load_font(54, bold=True)
    subtitle_font = load_font(24)
    pill_font = load_font(14)
    text_x = 430
    max_x = WIDTH - 24
    pills = list(locale.pills)

    text_block_height = measure_text_block_height(
        title_font,
        subtitle_font,
        pill_font,
        locale.subtitle,
        pills,
        text_x,
        max_x,
    )
    text_start_y = (HEIGHT - text_block_height) // 2

    draw_text_block(
        draw,
        text_x=text_x,
        start_y=text_start_y,
        title_font=title_font,
        subtitle_font=subtitle_font,
        pill_font=pill_font,
        subtitle=locale.subtitle,
        pills=pills,
        max_x=max_x,
    )

    canvas = Image.alpha_composite(canvas.convert("RGBA"), overlay).convert("RGB")

    out_dir = OUT_DIR / locale.folder
    out_dir.mkdir(parents=True, exist_ok=True)
    out_path = out_dir / "feature-graphic-1024x500.png"
    canvas.save(out_path, format="PNG", optimize=True)
    print(f"Saved {out_path} ({canvas.width}x{canvas.height}) from {icon_path.name}")
    return out_path


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate Google Play feature graphics.")
    parser.add_argument(
        "--locale",
        choices=sorted(LOCALES),
        action="append",
        dest="locales",
        help="Locale to generate (default: all). Repeat for multiple locales.",
    )
    args = parser.parse_args()
    selected = args.locales or sorted(LOCALES)
    for key in selected:
        generate_feature_graphic(LOCALES[key])


if __name__ == "__main__":
    main()
