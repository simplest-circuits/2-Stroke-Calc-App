#!/usr/bin/env python3
"""Crop emulator screenshots to 1080x1920 (9:16) for Google Play phone listings."""

from __future__ import annotations

from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent
SRC = ROOT / "screenshots"
TARGET_W = 1080
TARGET_H = 1920


def crop_center_phone(src: Path, dst: Path) -> None:
    with Image.open(src) as img:
        w, h = img.size
        target_ratio = TARGET_W / TARGET_H
        current_ratio = w / h

        if current_ratio > target_ratio:
            # Too wide: crop width
            new_w = int(h * target_ratio)
            left = (w - new_w) // 2
            box = (left, 0, left + new_w, h)
        else:
            # Too tall: crop height (keep top-heavy for app content)
            new_h = int(w / target_ratio)
            top = min(int(h * 0.08), h - new_h)
            box = (0, top, w, top + new_h)

        cropped = img.crop(box).resize((TARGET_W, TARGET_H), Image.Resampling.LANCZOS)
        dst.parent.mkdir(parents=True, exist_ok=True)
        cropped.save(dst, format="PNG", optimize=True)


def main() -> None:
    for locale_dir in sorted(SRC.iterdir()):
        if not locale_dir.is_dir() or locale_dir.name.startswith("_"):
            continue
        phone_dir = locale_dir / "phone"
        for png in sorted(locale_dir.glob("*.png")):
            if png.parent.name == "phone":
                continue
            out = phone_dir / png.name
            crop_center_phone(png, out)
            print(f"{png.relative_to(ROOT)} -> {out.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
