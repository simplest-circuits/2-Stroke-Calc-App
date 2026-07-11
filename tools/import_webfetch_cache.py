#!/usr/bin/env python3
"""Import vehicle dash-line lists from agent-tools WebFetch caches into catalog_cache/."""
import re
from pathlib import Path

TOOLS = Path(__file__).resolve().parent
CACHE = TOOLS / "catalog_cache"
AGENT = Path(r"C:\Users\thannedo\.cursor\projects\c-Users-thannedo-Documents-GitHub-2StrokeCalc-App\agent-tools")

# agent-tools file -> (cache filename, brand title from scooter_center_brands)
MAPPINGS = {
    "412266d9-95b1-476d-bbfe-227ef14f433e.txt": ("aprilia.txt", "Aprilia"),
    "9225b4d3-ea70-47b7-b093-91a8efdbc11f.txt": ("peugeot.txt", "Peugeot"),
    "d19d2a4c-861a-4d25-88c9-c5b0a982c4ae.txt": ("yamaha.txt", "Yamaha"),
}


def extract_vehicle_lines(text: str) -> list[str]:
    """Keep only dash-prefixed model lines (skip duplicate Details entries)."""
    lines: list[str] = []
    in_section = False
    for raw in text.splitlines():
        line = raw.strip()
        if line == "Fahrzeuge":
            in_section = True
            continue
        if in_section and line.startswith("#"):
            break
        if in_section and line.startswith("- "):
            lines.append(line)
    if not lines:
        for raw in text.splitlines():
            line = raw.strip()
            if line.startswith("- "):
                lines.append(line)
    return list(dict.fromkeys(lines))


def main():
    CACHE.mkdir(exist_ok=True)
    for src_name, (dst_name, brand) in MAPPINGS.items():
        src = AGENT / src_name
        if not src.exists():
            print(f"skip missing {src_name}")
            continue
        text = src.read_text(encoding="utf-8", errors="replace")
        lines = extract_vehicle_lines(text)
        out = CACHE / dst_name
        out.write_text("\n".join(lines) + "\n", encoding="utf-8")
        print(f"{brand}: {len(lines)} -> {dst_name}")


if __name__ == "__main__":
    main()
