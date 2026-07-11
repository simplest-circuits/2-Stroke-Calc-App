#!/usr/bin/env python3
"""Scrape motorcycle models and technical data from motorradonline.de."""
from __future__ import annotations

import argparse
import json
import re
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

TOOLS = Path(__file__).resolve().parent
CACHE = TOOLS / "motorrad_cache"
INDEX_FILE = CACHE / "brands.json"
OUT_FILE = CACHE / "all_entries.jsonl"
BASE = "https://www.motorradonline.de"
UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
DELAY = 0.4


def fetch(url: str) -> str:
    time.sleep(DELAY)
    req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept-Language": "de-DE,de;q=0.9"})
    with urllib.request.urlopen(req, timeout=90) as r:
        return r.read().decode("utf-8", errors="replace")


def next_data(html: str) -> dict | None:
    m = re.search(r'<script id="__NEXT_DATA__"[^>]*>(.*?)</script>', html, re.S)
    if not m:
        return None
    return json.loads(m.group(1))


def find_techdata_records(obj) -> list[dict]:
    found: list[dict] = []
    if isinstance(obj, dict):
        td = obj.get("techdata")
        if isinstance(td, dict) and td.get("Motoren01_Hubraum") not in (None, "", "k.A."):
            found.append(td)
        for v in obj.values():
            found.extend(find_techdata_records(v))
    elif isinstance(obj, list):
        for item in obj:
            found.extend(find_techdata_records(item))
    return found


def discover_brands() -> list[dict]:
    html = fetch(f"{BASE}/marken-modelle/")
    seen: set[str] = set()
    brands: list[dict] = []
    for path, slug in re.findall(r'href="(/marken-modelle/([^"/]+)/)"', html):
        if slug in seen:
            continue
        seen.add(slug)
        brands.append({"slug": slug, "url": f"{BASE}{path}"})
    return sorted(brands, key=lambda b: b["slug"])


def discover_series(brand_slug: str) -> list[str]:
    html = fetch(f"{BASE}/marken-modelle/{brand_slug}/")
    links = sorted(set(re.findall(
        rf'href="(/marken-modelle/{re.escape(brand_slug)}/([^"#?/]+)/)"', html
    )))
    slugs: list[str] = []
    for path, series_slug in links:
        if series_slug == brand_slug or "technische-daten" in path:
            continue
        slugs.append(series_slug)
    return sorted(set(slugs))


def scrape_series(brand_slug: str, series_slug: str) -> list[dict]:
    url = f"{BASE}/marken-modelle/{brand_slug}/{series_slug}/technische-daten/"
    try:
        html = fetch(url)
    except urllib.error.HTTPError as e:
        if e.code == 404:
            return []
        raise
    nd = next_data(html)
    if not nd:
        return []
    records = find_techdata_records(nd)
    for td in records:
        td["_source_url"] = url
        td["_brand_slug"] = brand_slug
        td["_series_slug"] = series_slug
    return records


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--brands", help="Comma-separated brand slugs (default: all)")
    parser.add_argument("--resume", action="store_true", help="Skip already cached series")
    args = parser.parse_args()

    CACHE.mkdir(exist_ok=True)
    if INDEX_FILE.exists():
        brands = json.loads(INDEX_FILE.read_text(encoding="utf-8"))
    else:
        print("Discovering brands...")
        brands = discover_brands()
        INDEX_FILE.write_text(json.dumps(brands, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"Found {len(brands)} brands")

    if args.brands:
        wanted = {s.strip().lower() for s in args.brands.split(",")}
        brands = [b for b in brands if b["slug"] in wanted]

    done_series: set[str] = set()
    if args.resume and OUT_FILE.exists():
        for line in OUT_FILE.read_text(encoding="utf-8").splitlines():
            if line.strip():
                rec = json.loads(line)
                done_series.add(f"{rec.get('_brand_slug')}/{rec.get('_series_slug')}")

    mode = "a" if args.resume and OUT_FILE.exists() else "w"
    total = 0
    with OUT_FILE.open(mode, encoding="utf-8") as out:
        for bi, brand in enumerate(brands, 1):
            slug = brand["slug"]
            print(f"[{bi}/{len(brands)}] {slug}", flush=True)
            try:
                series_list = discover_series(slug)
            except Exception as exc:
                print(f"  series discovery failed: {exc}")
                continue
            print(f"  {len(series_list)} series")
            for si, series in enumerate(series_list, 1):
                key = f"{slug}/{series}"
                if key in done_series:
                    continue
                try:
                    records = scrape_series(slug, series)
                except Exception as exc:
                    print(f"    {series}: FAIL {exc}")
                    continue
                for rec in records:
                    out.write(json.dumps(rec, ensure_ascii=False) + "\n")
                    total += 1
                if records:
                    print(f"    {series}: {len(records)} variants")
    print(f"Done. Wrote {total} records -> {OUT_FILE}")


if __name__ == "__main__":
    main()
