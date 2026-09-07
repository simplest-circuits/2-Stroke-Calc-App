#!/usr/bin/env python3
"""Generate premium Play Store phone screenshots (1080x1920) from raw captures."""

from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent
SCREENSHOTS = ROOT / "screenshots"
ICON = ROOT.parent / "AppIcons" / "playstore.png"
DEVICE_TEMPLATE = ROOT / "device-frame-template.png"

CANVAS_W = 1080
CANVAS_H = 1920

# Premium dark marketing palette
BG_TOP = (10, 9, 7)
BG_BOTTOM = (22, 19, 15)
HEADLINE = (250, 246, 236)
SUBLINE = (200, 186, 152)
GOLD = (232, 197, 71)
GOLD_DIM = (180, 150, 72)
GOLD_GLOW = (232, 197, 71, 36)

SCREEN_X = 166
SCREEN_Y = 168
SCREEN_W = 906
SCREEN_H = 2022
SCREEN_RADIUS = 40

FONT_BOLD = Path("C:/Windows/Fonts/segoeuib.ttf")
FONT_SEMIBOLD = Path("C:/Windows/Fonts/seguisb.ttf")
FONT_REGULAR = Path("C:/Windows/Fonts/segoeui.ttf")

PHONE_TARGET_W = 680


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


def add_glow(canvas: Image.Image, x: int, y: int, w: int, h: int, color: tuple[int, int, int], alpha: int = 50) -> None:
    glow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse((x, y, x + w, y + h), fill=(*color, alpha))
    canvas.alpha_composite(glow.filter(ImageFilter.GaussianBlur(52)))


def wrap_headline(lines: list[str], font: ImageFont.FreeTypeFont, max_width: int) -> list[str]:
    wrapped: list[str] = []
    for line in lines:
        words = line.split()
        if not words:
            continue
        current = words[0]
        for word in words[1:]:
            trial = f"{current} {word}"
            if font.getlength(trial) <= max_width:
                current = trial
            else:
                wrapped.append(current)
                current = word
        wrapped.append(current)
    return wrapped


def draw_centered_lines(
    draw: ImageDraw.ImageDraw,
    lines: list[str],
    y_start: int,
    font: ImageFont.FreeTypeFont,
    fill: tuple[int, int, int],
    line_gap: int,
    max_width: int,
) -> int:
    y = y_start
    for line in wrap_headline(lines, font, max_width):
        text_w = font.getlength(line)
        draw.text(((CANVAS_W - text_w) / 2, y), line, font=font, fill=fill)
        y += line_gap
    return y


def draw_app_badge(canvas: Image.Image, label: str) -> int:
    font = load_font(FONT_SEMIBOLD, 26)
    icon_size = 32
    pad_x = 20
    gap = 10
    text_w = font.getlength(label)
    pill_w = int(pad_x * 2 + icon_size + gap + text_w)
    pill_h = 50
    pill_x = (CANVAS_W - pill_w) // 2
    pill_y = 52

    draw = ImageDraw.Draw(canvas)
    draw.rounded_rectangle(
        (pill_x, pill_y, pill_x + pill_w, pill_y + pill_h),
        radius=pill_h // 2,
        fill=(24, 21, 16, 230),
        outline=(*GOLD_DIM, 220),
        width=2,
    )

    if ICON.exists():
        with Image.open(ICON) as icon_src:
            icon = icon_src.convert("RGBA").resize((icon_size, icon_size), Image.Resampling.LANCZOS)
            icon.putalpha(rounded_mask((icon_size, icon_size), 8))
            canvas.paste(icon, (pill_x + pad_x, pill_y + (pill_h - icon_size) // 2), icon)

    text_x = pill_x + pad_x + icon_size + gap
    text_y = pill_y + (pill_h - 26) // 2 - 2
    draw.text((text_x, text_y), label, font=font, fill=GOLD)
    return pill_y + pill_h


def draw_feature_chips(canvas: Image.Image, chips: list[str], y: int) -> int:
    if not chips:
        return y
    font = load_font(FONT_SEMIBOLD, 22)
    draw = ImageDraw.Draw(canvas)
    chip_h = 40
    gap = 12
    pad_x = 16

    chip_widths = [font.getlength(c) + pad_x * 2 for c in chips]
    total_w = sum(chip_widths) + gap * (len(chips) - 1)
    x = (CANVAS_W - total_w) // 2

    for chip, w in zip(chips, chip_widths):
        draw.rounded_rectangle(
            (x, y, x + w, y + chip_h),
            radius=chip_h // 2,
            fill=(30, 26, 20, 220),
            outline=(*GOLD_DIM, 180),
            width=1,
        )
        tw = font.getlength(chip)
        draw.text((x + (w - tw) / 2, y + (chip_h - 22) / 2 - 2), chip, font=font, fill=SUBLINE)
        x += w + gap
    return y + chip_h


def draw_accent_line(canvas: Image.Image, y: int) -> None:
    draw = ImageDraw.Draw(canvas)
    line_w = 72
    x0 = (CANVAS_W - line_w) // 2
    draw.rounded_rectangle((x0, y, x0 + line_w, y + 4), radius=2, fill=GOLD)


def build_device_mockup(screenshot: Image.Image, template: Image.Image) -> Image.Image:
    screen = screenshot.resize((SCREEN_W, SCREEN_H), Image.Resampling.LANCZOS).convert("RGBA")
    screen.putalpha(rounded_mask((SCREEN_W, SCREEN_H), SCREEN_RADIUS))

    mockup = template.copy()
    mockup.paste(screen, (SCREEN_X, SCREEN_Y), screen)
    return mockup


def place_phone(canvas: Image.Image, phone: Image.Image, top: int) -> None:
    scale = PHONE_TARGET_W / phone.width
    target_h = int(phone.height * scale)
    max_h = CANVAS_H - top - 16
    if target_h > max_h:
        scale = max_h / phone.height
        target_h = int(phone.height * scale)

    target_w = int(phone.width * scale)
    phone = phone.resize((target_w, target_h), Image.Resampling.LANCZOS)

    shadow = Image.new("RGBA", (target_w + 40, target_h + 40), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle(
        (12, 16, target_w + 28, target_h + 24),
        radius=32,
        fill=(0, 0, 0, 110),
    )
    shadow = shadow.filter(ImageFilter.GaussianBlur(14))

    x = (CANVAS_W - target_w) // 2
    canvas.alpha_composite(shadow, (x - 8, top + 6))
    canvas.paste(phone, (x, top), phone)


LOCALE_COPY: dict[str, dict[str, dict[str, object]]] = {
    "en": {
        "01_calculator_overview_list.png": {
            "badge": True,
            "lines": ["Welcome!", "Your complete", "2-stroke toolkit"],
            "chips": ["Calculators", "Tools", "Garage"],
        },
        "02_calculator_overview_grid.png": {
            "lines": ["Clear & intuitive", "15+ calculators"],
            "chips": ["Port timing", "Compression", "Jetting"],
        },
        "03_calculator_detail.png": {
            "lines": ["Precise port timing", "from gauge measurements"],
            "chips": ["Diagram", "Transfer", "Duration"],
        },
        "04_tools_overview.png": {
            "lines": ["Built-in tools", "for the track & workshop"],
            "chips": ["Dyno", "Tachometer", "Community"],
        },
        "05_gps_dyno.png": {
            "lines": ["Street dyno", "power from GPS"],
            "chips": ["0–100", "HP estimate", "Sessions"],
        },
        "06_vehicles_overview.png": {
            "lines": ["Manage your fleet", "maintenance & documents"],
            "chips": ["Fuel log", "Costs", "Reminders"],
        },
        "07_fuel_log.png": {
            "lines": ["Fuel log & consumption", "per vehicle"],
            "chips": ["L/100 km", "History", "Stats"],
        },
        "08_settings.png": {
            "lines": ["Ready to ride?", "We've got the numbers!"],
            "chips": ["Free", "Pro", "Cloud sync"],
        },
    },
    "de": {
        "01_calculator_overview_list.png": {
            "badge": True,
            "lines": ["Willkommen!", "Dein komplettes", "2-Takt-Toolkit"],
            "chips": ["Rechner", "Tools", "Garage"],
        },
        "02_calculator_overview_grid.png": {
            "lines": ["Übersichtlich", "& intuitiv"],
            "chips": ["Steuerzeiten", "Verdichtung", "Vergaser"],
        },
        "03_calculator_detail.png": {
            "lines": ["Präzise Steuerzeiten", "aus Stichmaßen"],
            "chips": ["Diagramm", "Überström", "Dauer"],
        },
        "04_tools_overview.png": {
            "lines": ["Eingebaute Tools", "für Werkstatt & Strecke"],
            "chips": ["Dyno", "Drehzahl", "Community"],
        },
        "05_gps_dyno.png": {
            "lines": ["Straßen-Dyno", "Leistung per GPS"],
            "chips": ["0–100", "PS-Schätzung", "Sessions"],
        },
        "06_vehicles_overview.png": {
            "lines": ["Fahrzeugverwaltung", "Flotte & Wartung"],
            "chips": ["Tankbuch", "Kosten", "Erinnerungen"],
        },
        "07_fuel_log.png": {
            "lines": ["Tankbuch & Verbrauch", "pro Fahrzeug"],
            "chips": ["L/100 km", "Historie", "Statistik"],
        },
        "08_settings.png": {
            "lines": ["Ready?", "Wir sind es!"],
            "chips": ["Free", "Pro", "Cloud-Sync"],
        },
    },
}


def build_store_screenshot(
    locale: str,
    filename: str,
    src: Path,
    dst: Path,
    template: Image.Image,
) -> None:
    meta = LOCALE_COPY[locale][filename]
    lines = list(meta["lines"])  # type: ignore[arg-type]
    chips = list(meta.get("chips", []))  # type: ignore[arg-type]
    show_badge = bool(meta.get("badge"))

    canvas = vertical_gradient((CANVAS_W, CANVAS_H), BG_TOP, BG_BOTTOM).convert("RGBA")
    add_glow(canvas, 760, 20, 240, 200, GOLD, alpha=28)
    add_glow(canvas, 20, 480, 220, 180, GOLD, alpha=18)

    y = 52
    if show_badge:
        y = draw_app_badge(canvas, "2-Stroke Lab") + 28
    else:
        y = 72

    headline_font = load_font(FONT_BOLD, 54)
    draw = ImageDraw.Draw(canvas)
    y = draw_centered_lines(
        draw,
        lines,
        y,
        headline_font,
        HEADLINE,
        line_gap=62,
        max_width=CANVAS_W - 100,
    )

    draw_accent_line(canvas, y + 8)
    y += 28
    y = draw_feature_chips(canvas, chips, y)

    with Image.open(src) as shot:
        phone = build_device_mockup(shot, template)

    place_phone(canvas, phone, top=y + 20)

    dst.parent.mkdir(parents=True, exist_ok=True)
    canvas.convert("RGB").save(dst, format="PNG", optimize=True)


def main() -> None:
    if not DEVICE_TEMPLATE.exists():
        raise SystemExit(f"Missing device frame template: {DEVICE_TEMPLATE}")

    with Image.open(DEVICE_TEMPLATE) as template_src:
        template = template_src.convert("RGBA")

    for locale in ("en", "de"):
        src_dir = SCREENSHOTS / locale
        out_dir = src_dir / "store"
        copy = LOCALE_COPY[locale]

        for filename in sorted(copy):
            src = src_dir / filename
            if not src.exists():
                print(f"SKIP missing: {src}")
                continue
            dst = out_dir / filename
            build_store_screenshot(locale, filename, src, dst, template)
            print(f"{src.relative_to(ROOT)} -> {dst.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
