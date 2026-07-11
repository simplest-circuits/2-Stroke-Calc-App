#!/usr/bin/env python3
"""Try cloudscraper to fetch Scooter Center brand lists."""
import json
import re
import time
from pathlib import Path

try:
    import cloudscraper
except ImportError:
    raise SystemExit("pip install cloudscraper")

TOOLS = Path(__file__).resolve().parent
CACHE = TOOLS / "catalog_cache"
BRANDS = TOOLS / "scooter_center_brands.json"

HTML_TITLE_RE = re.compile(
    r'href="https://www\.scooter-center\.com/[^"]+/fz-\d+\.html"\s+title="([^"]+)"',
    re.I,
)


def extract(html: str) -> list[str]:
    lines = [f"- {m}" for m in HTML_TITLE_RE.findall(html)]
    if lines:
        return list(dict.fromkeys(lines))
    for line in html.splitlines():
        line = line.strip()
        if line.startswith("- "):
            lines.append(line)
    return list(dict.fromkeys(lines))


def main():
    CACHE.mkdir(exist_ok=True)
    brands = json.loads(BRANDS.read_text(encoding="utf-8"))
    scraper = cloudscraper.create_scraper()
    for i, item in enumerate(brands, 1):
        slug = item["url"].split("/")[-2]
        out = CACHE / f"{slug}.txt"
        if out.exists() and out.stat().st_size > 200:
            print(f"[{i}] skip {slug}")
            continue
        time.sleep(6)
        try:
            r = scraper.get(item["url"], timeout=90)
            print(f"[{i}] {slug}: HTTP {r.status_code}")
            if r.status_code != 200:
                continue
            lines = extract(r.text)
            if len(lines) < 3:
                print(f"  too few lines ({len(lines)})")
                continue
            out.write_text("\n".join(lines) + "\n", encoding="utf-8")
            print(f"  saved {len(lines)}")
        except Exception as exc:
            print(f"[{i}] {slug}: {exc}")


if __name__ == "__main__":
    main()
