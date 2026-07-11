#!/usr/bin/env python3
"""Create clean social media ad graphics (DE + EN)."""

from __future__ import annotations

from pathlib import Path

import qrcode
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent
ICON = ROOT.parent.parent / "app" / "src" / "main" / "res" / "drawable" / "appstore.png"
PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.simplestsoft.twostrokecalc"

W = 1080
H = 1350

BG_TOP = (8, 8, 7)
BG_BOTTOM = (20, 17, 13)
GOLD = (226, 194, 88)
GOLD_SOFT = (180, 150, 72)
TEXT_MAIN = (250, 246, 236)
TEXT_SUB = (210, 196, 156)
TEXT_MUTED = (168, 154, 124)
PANEL_BG = (30, 26, 20)
PANEL_BORDER = (88, 72, 40)
CHIP_BG = (24, 21, 16)

FONT_BOLD = Path("C:/Windows/Fonts/segoeuib.ttf")
FONT_SEMIBOLD = Path("C:/Windows/Fonts/seguisb.ttf")
FONT_REGULAR = Path("C:/Windows/Fonts/segoeui.ttf")

COPY_BY_LOCALE = {
    "de": {
        "subtitle": "Für 2-Takt-Tuning & Fahrzeugpflege",
        "summary_lines": [
            "Die All-in-one-App für 2-Takter.",
            "Rechnen, verwalten, Kosten im Blick – nichts vergessen.",
        ],
        "chips": [
            "Technische Berechnungen",
            "Fahrzeugverwaltung",
            "Wartungskosten & Verbrauch",
            "Service-Erinnerungen",
        ],
        "cta": "Jetzt im Play Store",
        "qr_hint": "QR scannen & installieren",
        "screenshots": [
            "01_calculator_overview_list.png",
            "04_vehicles_overview.png",
            "07_maintenance_costs.png",
        ],
        "out": ROOT / "social-ad-clean-de-1080x1350.png",
    },
    "en": {
        "subtitle": "For 2-stroke tuning & vehicle care",
        "summary_lines": [
            "Your all-in-one app for 2-stroke projects.",
            "Calculate, manage, track costs – never miss a service.",
        ],
        "chips": [
            "Technical calculations",
            "Vehicle management",
            "Maintenance & fuel tracking",
            "Service reminders",
        ],
        "cta": "Now on Play Store",
        "qr_hint": "Scan QR to install",
        "screenshots": [
            "01_calculator_overview_list.png",
            "04_vehicles_overview.png",
            "07_maintenance_costs.png",
        ],
        "out": ROOT / "social-ad-clean-en-1080x1350.png",
    },
}


def load_font(path: Path, size: int) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    if path.exists():
        return ImageFont.truetype(str(path), size)
    return ImageFont.load_default()


def vertical_gradient(size: tuple[int, int], top: tuple[int, int, int], bottom: tuple[int, int, int]) -> Image.Image:
    img = Image.new("RGB", size, top)
    draw = ImageDraw.Draw(img)
    width, height = size
    for y in range(height):
        t = y / max(1, height - 1)
        color = tuple(int(top[i] * (1 - t) + bottom[i] * t) for i in range(3))
        draw.line([(0, y), (width, y)], fill=color)
    return img


def rounded_mask(size: tuple[int, int], radius: int) -> Image.Image:
    mask = Image.new("L", size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, size[0], size[1]), radius=radius, fill=255)
    return mask


def add_glow(canvas: Image.Image, x: int, y: int, w: int, h: int, color: tuple[int, int, int], alpha: int = 60) -> None:
    glow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse((x, y, x + w, y + h), fill=(*color, alpha))
    canvas.alpha_composite(glow.filter(ImageFilter.GaussianBlur(48)))


def text_size(draw: ImageDraw.ImageDraw, text: str, font: ImageFont.ImageFont) -> tuple[int, int]:
    box = draw.textbbox((0, 0), text, font=font)
    return box[2] - box[0], box[3] - box[1]


def draw_centered_text(
    draw: ImageDraw.ImageDraw,
    text: str,
    center_x: int,
    y: int,
    font: ImageFont.ImageFont,
    fill: tuple[int, int, int],
) -> int:
    bbox = draw.textbbox((0, 0), text, font=font)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]
    x = center_x - tw / 2 - bbox[0]
    draw.text((x, y - bbox[1]), text, font=font, fill=fill)
    return y + th


def draw_centered_lines_in_box(
    draw: ImageDraw.ImageDraw,
    lines: list[str],
    box: tuple[int, int, int, int],
    font: ImageFont.ImageFont,
    fill: tuple[int, int, int],
    line_gap: int,
) -> None:
    left, top, right, bottom = box
    center_x = (left + right) // 2

    metrics: list[tuple[int, int]] = []
    for line in lines:
        bbox = draw.textbbox((0, 0), line, font=font)
        metrics.append((bbox[2] - bbox[0], bbox[3] - bbox[1]))

    block_h = sum(h for _, h in metrics) + line_gap * max(0, len(lines) - 1)
    y = top + (bottom - top - block_h) // 2

    for line, (tw, th) in zip(lines, metrics):
        bbox = draw.textbbox((0, 0), line, font=font)
        x = center_x - tw / 2 - bbox[0]
        draw.text((x, y - bbox[1]), line, font=font, fill=fill)
        y += th + line_gap


def draw_panel(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], radius: int = 24) -> None:
    draw.rounded_rectangle(box, radius=radius, fill=(*PANEL_BG, 235), outline=(*PANEL_BORDER, 255), width=2)


def place_icon(canvas: Image.Image, x: int, y: int, size: int = 132) -> None:
    with Image.open(ICON) as src:
        icon = src.convert("RGBA").resize((size, size), Image.Resampling.LANCZOS)

    frame = size + 16
    card = Image.new("RGBA", (frame, frame), (0, 0, 0, 0))
    draw = ImageDraw.Draw(card)
    draw.rounded_rectangle((0, 0, frame - 1, frame - 1), radius=28, fill=(18, 16, 12, 255), outline=(*GOLD, 200), width=2)
    card.paste(icon, (8, 8), icon)
    canvas.alpha_composite(card, (x, y))


def make_phone_mockup(path: Path, width: int) -> Image.Image:
    with Image.open(path) as src:
        shot = src.convert("RGBA")

    target_h = int(width * (shot.height / shot.width))
    shot = shot.resize((width, target_h), Image.Resampling.LANCZOS)
    frame = Image.new("RGBA", (width + 18, target_h + 18), (0, 0, 0, 0))
    draw = ImageDraw.Draw(frame)
    draw.rounded_rectangle((0, 0, frame.width - 1, frame.height - 1), radius=22, fill=(8, 8, 8, 255), outline=(*GOLD_SOFT, 180), width=2)
    shot.putalpha(rounded_mask((width, target_h), 18))
    frame.paste(shot, (9, 9), shot)
    return frame


def paste_with_shadow(base: Image.Image, overlay: Image.Image, x: int, y: int, angle: float = 0.0) -> None:
    if angle:
        overlay = overlay.rotate(angle, resample=Image.Resampling.BICUBIC, expand=True)

    shadow = Image.new("RGBA", overlay.size, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle((8, 10, overlay.width - 8, overlay.height - 4), radius=20, fill=(0, 0, 0, 120))
    shadow = shadow.filter(ImageFilter.GaussianBlur(10))
    base.alpha_composite(shadow, (x + 5, y + 8))
    base.alpha_composite(overlay, (x, y))


def draw_chip_grid(
    draw: ImageDraw.ImageDraw,
    chips: list[str],
    y: int,
    font: ImageFont.ImageFont,
    cols: int = 2,
    chip_w: int = 470,
    chip_h: int = 56,
    gap_x: int = 20,
    gap_y: int = 14,
) -> int:
    grid_w = cols * chip_w + (cols - 1) * gap_x
    start_x = (W - grid_w) // 2

    for idx, label in enumerate(chips[:4]):
        row = idx // cols
        col = idx % cols
        x = start_x + col * (chip_w + gap_x)
        cy = y + row * (chip_h + gap_y)

        draw.rounded_rectangle((x, cy, x + chip_w, cy + chip_h), radius=18, fill=(*CHIP_BG, 255), outline=(*PANEL_BORDER, 255), width=2)

        bullet_d = 8
        tw, th = text_size(draw, label, font)
        content_w = bullet_d + 10 + tw
        content_x = x + (chip_w - content_w) // 2
        bullet_y = cy + (chip_h - bullet_d) // 2
        draw.ellipse((content_x, bullet_y, content_x + bullet_d, bullet_y + bullet_d), fill=GOLD)

        text_box = draw.textbbox((0, 0), label, font=font)
        text_x = content_x + bullet_d + 10
        text_y = cy + (chip_h - (text_box[3] - text_box[1])) // 2 - text_box[1]
        draw.text((text_x, text_y), label, font=font, fill=TEXT_MAIN)

    rows = (len(chips[:4]) + cols - 1) // cols
    return y + rows * chip_h + (rows - 1) * gap_y


def build_qr_image(url: str, size: int) -> Image.Image:
    qr = qrcode.QRCode(version=None, error_correction=qrcode.constants.ERROR_CORRECT_M, box_size=10, border=1)
    qr.add_data(url)
    qr.make(fit=True)
    return qr.make_image(fill_color="black", back_color="white").convert("RGBA").resize((size, size), Image.Resampling.NEAREST)


def screenshot_paths(locale: str) -> list[Path]:
    copy = COPY_BY_LOCALE[locale]
    folder = ROOT / "screenshots" / locale
    return [folder / name for name in copy["screenshots"]]


def build(locale: str) -> None:
    copy = COPY_BY_LOCALE[locale]
    shots = screenshot_paths(locale)

    canvas = vertical_gradient((W, H), BG_TOP, BG_BOTTOM).convert("RGBA")
    draw = ImageDraw.Draw(canvas)

    add_glow(canvas, 760, 40, 220, 180, GOLD, alpha=34)
    add_glow(canvas, 40, 520, 200, 160, GOLD, alpha=22)

    title_font = load_font(FONT_BOLD, 72)
    subtitle_font = load_font(FONT_SEMIBOLD, 34)
    summary_font = load_font(FONT_REGULAR, 26)
    chip_font = load_font(FONT_SEMIBOLD, 28)
    cta_font = load_font(FONT_BOLD, 36)
    hint_font = load_font(FONT_SEMIBOLD, 20)

    # Header block (centered)
    header_top = 56
    icon_size = 132
    place_icon(canvas, x=(W - icon_size - 16) // 2, y=header_top, size=icon_size)

    y = header_top + icon_size + 34
    y = draw_centered_text(draw, "2-Stroke Calc", W // 2, y, title_font, TEXT_MAIN) + 32
    y = draw_centered_text(draw, str(copy["subtitle"]), W // 2, y, subtitle_font, TEXT_SUB) + 28

    summary_lines = [str(line) for line in copy["summary_lines"]]
    summary_panel_w = 920
    summary_panel_h = 96
    summary_panel_x = (W - summary_panel_w) // 2
    summary_panel_y = y
    draw_panel(draw, (summary_panel_x, summary_panel_y, summary_panel_x + summary_panel_w, summary_panel_y + summary_panel_h), radius=20)
    draw_centered_lines_in_box(
        draw,
        summary_lines,
        (summary_panel_x, summary_panel_y, summary_panel_x + summary_panel_w, summary_panel_y + summary_panel_h),
        summary_font,
        TEXT_MUTED,
        line_gap=10,
    )

    # Phones
    phone_w = 220
    phones = [make_phone_mockup(path, phone_w) for path in shots]
    phone_y = summary_panel_y + summary_panel_h + 34
    positions = [
        (72, phone_y, -7),
        ((W - phones[1].width) // 2, phone_y - 18, 0),
        (W - phones[2].width - 72, phone_y, 7),
    ]
    for phone, (x, y_pos, angle) in zip(phones, positions):
        paste_with_shadow(canvas, phone, x, y_pos, angle=angle)

    phone_block_bottom = max(y_pos + phone.height for (_, y_pos, _), phone in zip(positions, phones))
    chips_y = phone_block_bottom + 28
    chips_bottom = draw_chip_grid(draw, [str(c) for c in copy["chips"]], chips_y, chip_font)

    # Footer
    footer_h = 148
    footer_y = H - footer_h - 42
    footer_x = 54
    footer_w = W - 108
    draw_panel(draw, (footer_x, footer_y, footer_x + footer_w, footer_y + footer_h), radius=28)

    qr_size = 104
    qr_pad = 18
    qr_x = footer_x + footer_w - qr_size - qr_pad
    qr_y = footer_y + (footer_h - qr_size) // 2
    draw.rounded_rectangle((qr_x - 8, qr_y - 8, qr_x + qr_size + 8, qr_y + qr_size + 8), radius=18, fill=(255, 255, 255, 255))
    canvas.alpha_composite(build_qr_image(PLAY_STORE_URL, qr_size), (qr_x, qr_y))

    cta_x = footer_x + 34
    cta_y = footer_y + 34
    draw.polygon([(cta_x, cta_y), (cta_x, cta_y + 44), (cta_x + 34, cta_y + 22)], fill=GOLD)
    draw.text((cta_x + 52, cta_y + 2), str(copy["cta"]), font=cta_font, fill=TEXT_MAIN)
    draw.text((cta_x + 52, cta_y + 48), str(copy["qr_hint"]), font=hint_font, fill=TEXT_SUB)

    out = Path(copy["out"])
    out.parent.mkdir(parents=True, exist_ok=True)
    canvas.convert("RGB").save(out, format="PNG", optimize=True)
    print(f"Created ({locale}): {out}")


if __name__ == "__main__":
    build("de")
    build("en")
