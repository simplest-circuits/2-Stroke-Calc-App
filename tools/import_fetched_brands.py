#!/usr/bin/env python3
"""Extract vehicle lists from WebFetch markdown into catalog_cache/*.txt"""
import re
from pathlib import Path

CACHE = Path(__file__).parent / "catalog_cache"
CACHE.mkdir(exist_ok=True)

SOURCES = {
    "peugeot.txt": Path(r"C:\Users\thannedo\.cursor\projects\c-Users-thannedo-Documents-GitHub-2StrokeCalc-App\agent-tools\9225b4d3-ea70-47b7-b093-91a8efdbc11f.txt"),
}

# Inline markdown blocks from WebFetch (dash-line sections)
INLINE = {
    "gilera.txt": """- DNA 125 (2002, ZAPM26000)
- DNA 50 GP-Experience (2008, ZAPC27000)
- Fuoco 500 (2013, ZAPM61100)
- Gilera Runner 125 (2002-2004, ZAPM24100)
- Runner 50 RST SP Black Soul (2018-2020, ZAPC46100)
- Stalker 50 (2011, ZAPC40101)
- Storm 50 (1996, TEC2T)
""",  # truncated - will use full file below
}

def extract_dash_lines(text: str) -> list[str]:
    lines = []
    for line in text.splitlines():
        line = line.strip()
        if line.startswith("- "):
            lines.append(line)
    return lines


def extract_details_lines(text: str) -> list[str]:
    """Honda/Derbi style: 'Model Name (years, frame)Details' or 'ModelDetails'."""
    out = []
    for line in text.splitlines():
        line = line.strip()
        if not line or line.startswith("#") or line.startswith("Fahrzeuge"):
            continue
        if line.endswith("Details"):
            core = line[:-7].strip()
            if core and not core.startswith("###"):
                # normalize to dash format
                if not core.startswith("- "):
                    core = "- " + core
                out.append(core)
    return out


# Save peugeot from file
if SOURCES["peugeot.txt"].exists():
    text = SOURCES["peugeot.txt"].read_text(encoding="utf-8")
    lines = extract_dash_lines(text)
    (CACHE / "peugeot.txt").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print("peugeot", len(lines))

# For brands fetched in chat - read from agent-tools if saved, else skip
# User can re-run fetch_brand_pages.py later
