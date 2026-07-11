#!/usr/bin/env python3
"""Explore motorradonline API and page structure."""
import json
import re
import urllib.request
from pathlib import Path

UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
BASE = "https://www.motorradonline.de"


def fetch(url: str) -> str:
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=60) as r:
        return r.read().decode("utf-8", errors="replace")


def next_data(html: str) -> dict | None:
    m = re.search(r'<script id="__NEXT_DATA__"[^>]*>(.*?)</script>', html, re.S)
    return json.loads(m.group(1)) if m else None


def find_techdata_items(obj, found=None):
    if found is None:
        found = []
    if isinstance(obj, dict):
        if "techdata" in obj and "Motoren01_Hubraum" in obj.get("techdata", {}):
            found.append(obj)
        for v in obj.values():
            find_techdata_items(v, found)
    elif isinstance(obj, list):
        for item in obj:
            find_techdata_items(item, found)
    return found


def extract_series_links(html: str, brand_slug: str) -> list[str]:
  links = sorted(set(re.findall(
      rf'href="(/marken-modelle/{re.escape(brand_slug)}/[^"#?]+/)"', html
  )))
  return [l for l in links if l.count("/") >= 4 and "technische-daten" not in l]


def main():
    # brand index
    html = fetch(f"{BASE}/marken-modelle/")
    brands = sorted(set(re.findall(r'href="(/marken-modelle/([^"/]+)/)"', html)))
    brand_slugs = sorted({b[1] for b in brands if b[1] not in ("",)})
    print(f"Brands found: {len(brand_slugs)}")
    print("Sample:", brand_slugs[:10])

    # KTM series
    brand = "ktm"
    brand_html = fetch(f"{BASE}/marken-modelle/{brand}/")
    series = extract_series_links(brand_html, brand)
    print(f"\nKTM series links: {len(series)}")
    for s in series[:8]:
        print(" ", s)

    # tech page for one model
    model_path = "690-smc-r"
    tech_url = f"{BASE}/marken-modelle/{brand}/{model_path}/technische-daten/"
    tech_html = fetch(tech_url)
    nd = next_data(tech_html)
    items = find_techdata_items(nd) if nd else []
    print(f"\nTech entries for {model_path}: {len(items)}")
    if items:
        td = items[0]["techdata"]
        print(f"  Sample: {td.get('Hersteller_HerstellerName')} {td.get('Modelle_ModellName')} ({td.get('Fahrzeuge_FahrzeugJahr')})")
        print(f"  Hubraum={td.get('Motoren01_Hubraum')} Bohrung={td.get('Motoren01_Bohrung')} Hub={td.get('Motoren01_Hub')}")
        print(f"  Takt={td.get('Motoren01_AnzahlTakte')} Kühlung={td.get('Motoren01_Kuehlung')}")

    # try backend paths
    for url in [
        "https://backend.auto-motor-und-sport.de/MRD/brandtree/series/index.json",
        "https://backend.auto-motor-und-sport.de/brandtree/series/index.json?project=MRD",
    ]:
        try:
            body = fetch(url)
            print(f"\n{url}: len={len(body)} starts={body[:80]!r}")
        except Exception as e:
            print(f"\n{url}: {e}")


if __name__ == "__main__":
    main()
