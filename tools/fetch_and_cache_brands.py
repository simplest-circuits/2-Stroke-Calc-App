#!/usr/bin/env python3
"""Slow-fetch brand pages and cache dash-line vehicle lists."""
import json
import re
import time
import urllib.request
from pathlib import Path

BRANDS = Path(__file__).parent / "scooter_center_brands.json"
CACHE = Path(__file__).parent / "catalog_cache"
DELAY = 8.0

def fetch(url: str) -> str:
    time.sleep(DELAY)
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, timeout=90) as r:
        return r.read().decode("utf-8", "replace")

def extract_lines(html: str) -> list[str]:
    lines = []
    for m in re.finditer(
        r'href="https://www\.scooter-center\.com/[^"]+/fz-\d+\.html"\s+title="([^"]+)"',
        html,
    ):
        lines.append(f"- {m.group(1)}")
    if lines:
        return lines
    for line in html.splitlines():
        line = line.strip()
        if line.startswith("- "):
            lines.append(line)
    return lines

def main():
    CACHE.mkdir(exist_ok=True)
    brands = json.loads(BRANDS.read_text(encoding="utf-8"))
    for i, item in enumerate(brands, 1):
        slug = item["url"].split("/")[-2]
        out = CACHE / f"{slug}.txt"
        if out.exists() and out.stat().st_size > 100:
            print(f"[{i}/{len(brands)}] skip {slug}")
            continue
        try:
            html = fetch(item["url"])
            lines = extract_lines(html)
            out.write_text("\n".join(lines) + "\n", encoding="utf-8")
            print(f"[{i}/{len(brands)}] {slug}: {len(lines)} models")
        except Exception as exc:
            print(f"[{i}/{len(brands)}] {slug}: FAIL {exc}")

if __name__ == "__main__":
    main()
