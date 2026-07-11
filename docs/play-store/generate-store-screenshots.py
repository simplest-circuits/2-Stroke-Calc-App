#!/usr/bin/env python3
"""Generate Play Store phone screenshots in mint-fresh style (Ai Bewerbung layout)."""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent
SCREENSHOTS = ROOT / "screenshots"
ICON = ROOT.parent / "AppIcons" / "playstore.png"
DEVICE_TEMPLATE = ROOT / "device-frame-template.png"

CANVAS_W = 1080
CANVAS_H = 1920

# 2-Stroke Calc light-theme palette (warm cream + gold accents)
BG = (250, 247, 242)  # #FAF7F2
HEADLINE = (31, 27, 22)  # #1F1B16
BADGE_TEXT = (31, 23, 8)  # #1F1708
GOLD = (212, 175, 55)  # #D4AF37
GOLD_BRIGHT = (232, 197, 71)  # #E8C547

# Samsung portrait mockup screen cut-out (device-frame-template.png)
SCREEN_X = 166
SCREEN_Y = 168
SCREEN_W = 906
SCREEN_H = 2022
SCREEN_RADIUS = 40

FONT_BOLD = Path("C:/Windows/Fonts/segoeuib.ttf")
FONT_SEMIBOLD = Path("C:/Windows/Fonts/seguisb.ttf")

PHONE_TARGET_W = 700


def load_font(path: Path, size: int) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    if path.exists():
        return ImageFont.truetype(str(path), size)
    return ImageFont.load_default()


def rounded_mask(size: tuple[int, int], radius: int) -> Image.Image:
    mask = Image.new("L", size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, size[0], size[1]), radius=radius, fill=255)
    return mask


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
    font = load_font(FONT_SEMIBOLD, 28)
    icon_size = 34
    pad_x = 22
    gap = 10
    text_w = font.getlength(label)
    pill_w = int(pad_x * 2 + icon_size + gap + text_w)
    pill_h = 52
    pill_x = (CANVAS_W - pill_w) // 2
    pill_y = 56

    draw = ImageDraw.Draw(canvas)
    draw.rounded_rectangle(
        (pill_x, pill_y, pill_x + pill_w, pill_y + pill_h),
        radius=pill_h // 2,
        fill=(255, 253, 249),
        outline=GOLD,
        width=2,
    )

    if ICON.exists():
        with Image.open(ICON) as icon_src:
            icon = icon_src.convert("RGBA").resize((icon_size, icon_size), Image.Resampling.LANCZOS)
            icon.putalpha(rounded_mask((icon_size, icon_size), 8))
            canvas.paste(icon, (pill_x + pad_x, pill_y + (pill_h - icon_size) // 2), icon)

    text_x = pill_x + pad_x + icon_size + gap
    text_y = pill_y + (pill_h - 28) // 2 - 2
    draw.text((text_x, text_y), label, font=font, fill=BADGE_TEXT)
    return pill_y + pill_h


def build_device_mockup(screenshot: Image.Image, template: Image.Image) -> Image.Image:
    screen = screenshot.resize((SCREEN_W, SCREEN_H), Image.Resampling.LANCZOS).convert("RGBA")
    screen.putalpha(rounded_mask((SCREEN_W, SCREEN_H), SCREEN_RADIUS))

    mockup = template.copy()
    mockup.paste(screen, (SCREEN_X, SCREEN_Y), screen)
    return mockup


def place_phone(canvas: Image.Image, phone: Image.Image, top: int) -> None:
    scale = PHONE_TARGET_W / phone.width
    target_h = int(phone.height * scale)
    max_h = CANVAS_H - top - 24
    if target_h > max_h:
        scale = max_h / phone.height
        target_h = int(phone.height * scale)

    target_w = int(phone.width * scale)
    phone = phone.resize((target_w, target_h), Image.Resampling.LANCZOS)
    x = (CANVAS_W - target_w) // 2
    canvas.paste(phone, (x, top), phone)


LOCALE_COPY: dict[str, dict[str, dict[str, object]]] = {
    "en": {
        "01_calculator_overview_list.png": {
            "badge": True,
            "lines": [
                "Welcome!",
                "Your complete toolkit",
                "for 2-stroke engines",
            ],
        },
        "02_calculator_overview_grid.png": {
            "lines": ["Clear & intuitive", "14 calculators"],
        },
        "03_calculator_detail.png": {
            "lines": ["Precise port timing", "from gauge measurements"],
        },
        "04_vehicles_overview.png": {
            "lines": ["Manage your fleet", "maintenance & documents"],
        },
        "05_vehicle_master_data.png": {
            "lines": ["All vehicle details", "in one place"],
        },
        "06_fuel_log.png": {
            "lines": ["Fuel log & consumption", "per vehicle"],
        },
        "07_maintenance_costs.png": {
            "lines": ["Maintenance & costs", "always in view"],
        },
        "08_settings.png": {
            "lines": ["Ready?", "We've got the numbers!"],
        },
    },
    "de": {
        "01_calculator_overview_list.png": {
            "badge": True,
            "lines": [
                "Willkommen!",
                "Dein Werkzeug für",
                "2-Takt-Berechnungen",
            ],
        },
        "02_calculator_overview_grid.png": {
            "lines": ["Übersichtlich", "& intuitiv"],
        },
        "03_calculator_detail.png": {
            "lines": ["Präzise Steuerzeiten", "aus Stichmaßen"],
        },
        "04_vehicles_overview.png": {
            "lines": ["Fahrzeugverwaltung", "Flotte & Wartung"],
        },
        "05_vehicle_master_data.png": {
            "lines": ["Alle Stammdaten", "an einem Ort"],
        },
        "06_fuel_log.png": {
            "lines": ["Tankbuch & Verbrauch", "pro Fahrzeug"],
        },
        "07_maintenance_costs.png": {
            "lines": ["Wartung & Kosten", "im Blick"],
        },
        "08_settings.png": {
            "lines": ["Ready?", "Wir sind es!"],
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
    show_badge = bool(meta.get("badge"))

    canvas = Image.new("RGBA", (CANVAS_W, CANVAS_H), (*BG, 255))

    y = 56
    if show_badge:
        y = draw_app_badge(canvas, "2-Stroke Calc") + 36
    else:
        y = 88

    headline_font = load_font(FONT_BOLD, 56)
    draw = ImageDraw.Draw(canvas)
    y = draw_centered_lines(
        draw,
        lines,
        y,
        headline_font,
        HEADLINE,
        line_gap=68,
        max_width=CANVAS_W - 120,
    )

    with Image.open(src) as shot:
        phone = build_device_mockup(shot, template)

    place_phone(canvas, phone, top=y + 28)

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
