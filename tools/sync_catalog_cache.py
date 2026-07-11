#!/usr/bin/env python3
"""Batch-fetch brand pages via WebFetch proxy pattern: read scooter_center_brands.json,
write URL list for manual/agent fetch, and import any agent-tools dumps."""
import json
import re
import subprocess
import sys
from pathlib import Path

TOOLS = Path(__file__).resolve().parent
CACHE = TOOLS / "catalog_cache"
BRANDS = TOOLS / "scooter_center_brands.json"
AGENT = Path(r"C:\Users\thannedo\.cursor\projects\c-Users-thannedo-Documents-GitHub-2StrokeCalc-App\agent-tools")

from save_brand_cache import extract_lines, save  # noqa: E402


def import_agent_tools():
    """Auto-detect brand pages saved by WebFetch in agent-tools."""
    if not AGENT.exists():
        return
    brands = json.loads(BRANDS.read_text(encoding="utf-8"))
    title_to_slug = {b["title"].lower(): b["url"].split("/")[-2] for b in brands}
    for path in sorted(AGENT.glob("*.txt")):
        text = path.read_text(encoding="utf-8", errors="replace")
        lines = extract_lines(text)
        if len(lines) < 10:
            continue
        # detect brand from first heading or title line
        slug = None
        m = re.search(r"^#\s+(.+?)\s+[-–]", text, re.M)
        if m:
            head = m.group(1).strip().lower()
            for title, s in title_to_slug.items():
                if title in head or head.startswith(title.split()[0]):
                    slug = s
                    break
        if not slug:
            for line in text.splitlines()[:5]:
                for title, s in title_to_slug.items():
                    if title in line.lower():
                        slug = s
                        break
        if slug:
            n = save(slug, text)
            print(f"auto {path.name} -> {slug}.txt ({n})")


def main():
    import_agent_tools()
    # known mappings
    known = {
        "412266d9-95b1-476d-bbfe-227ef14f433e.txt": "aprilia",
        "9225b4d3-ea70-47b7-b093-91a8efdbc11f.txt": "peugeot",
        "d19d2a4c-861a-4d25-88c9-c5b0a982c4ae.txt": "yamaha",
    }
    for fname, slug in known.items():
        p = AGENT / fname
        if p.exists():
            n = save(slug, p.read_text(encoding="utf-8", errors="replace"))
            print(f"known {slug}: {n}")


if __name__ == "__main__":
    main()
