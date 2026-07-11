#!/usr/bin/env python3
"""Build vehicle_catalog.json from Scooter Center (1000+ models)."""
from __future__ import annotations

import argparse
import json
import re
import time
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from html import unescape
from pathlib import Path
from typing import Any

BASE = "https://www.scooter-center.com"
INDEX_URL = f"{BASE}/unsere-fahrzeuge/bm-fzg.html"
BRAND_LIST_FILE = Path(__file__).resolve().parent / "scooter_center_brands.json"
OUT = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "assets" / "vehicle_catalog.json"
USER_AGENT = "2StrokeCalc-CatalogBuilder/2.0"
REQUEST_DELAY_S = 0.25

MANUFACTURER_RE = re.compile(
    r'href="(https://www\.scooter-center\.com/[a-z0-9-]+/c-\d+\.html)"\s+title="([^"]+)"',
    re.I,
)
VEHICLE_LINK_RE = re.compile(
    r'href="(https://www\.scooter-center\.com/[^"]+/fz-(?P<id>\d+)\.html)"\s+title="(?P<label>[^"]+)"',
    re.I,
)
FZGROUP_RE = re.compile(
    r'<div class="col-xs-12 fzgroup-table">\s*'
    r'<div class="col-xs-6 fzgroup-info">([^<]+)</div>\s*'
    r'<div class="col-xs-6 fzgroup-info text-right">([^<]+)</div>',
    re.I | re.S,
)
YEAR_RANGE_RE = re.compile(r"(\d{4})\s*-\s*(\d{4})")
SINGLE_YEAR_RE = re.compile(r"^(\d{4})$")


@dataclass
class VehicleRef:
    url: str
    sc_id: str
    label: str
    brand: str
    category: str


def fetch(url: str, retries: int = 5) -> str:
    last: Exception | None = None
    for attempt in range(retries):
        try:
            time.sleep(REQUEST_DELAY_S)
            req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
            with urllib.request.urlopen(req, timeout=60) as resp:
                return resp.read().decode("utf-8", "replace")
        except urllib.error.HTTPError as exc:
            last = exc
            if exc.code in {429, 503, 502}:
                time.sleep(2.0 * (attempt + 1))
                continue
            raise
        except Exception as exc:
            last = exc
            time.sleep(0.6 * (attempt + 1))
    raise RuntimeError(f"fetch failed {url}: {last}")


def slugify(text: str) -> str:
    return re.sub(r"[^a-z0-9]+", "_", text.lower()).strip("_")[:90]


def clean(text: str) -> str:
    text = unescape(text).strip()
    text = re.sub(r"\s+", " ", text)
    if re.fullmatch(r"\d+\.\d+", text):
        return text.replace(".", ",")
    return text


def parse_years(years_text: str) -> tuple[int, int]:
    years_text = years_text.strip()
    m = YEAR_RANGE_RE.search(years_text)
    if m:
        return int(m.group(1)), int(m.group(2))
    m = SINGLE_YEAR_RE.match(years_text)
    if m:
        y = int(m.group(1))
        return y, y
    m = re.match(r"'?(\d{2})\s*-\s*'?(?:(\d{2})|$)", years_text)
    if m:
        y1 = int(m.group(1))
        y1 = 1900 + y1 if y1 >= 30 else 2000 + y1
        if m.group(2):
            y2 = int(m.group(2))
            y2 = 1900 + y2 if y2 >= 30 else 2000 + y2
        else:
            y2 = 2026
        return y1, y2
    return 1970, 2026


def parse_label(label: str, default_brand: str) -> tuple[str, str, str, str, int, int]:
    label = clean(label)
    m = re.match(r"^(.+?)\s+\(([^,]+),\s*([^)]+)\)$", label)
    if not m:
        return default_brand, label, "", "", 1970, 2026
    name = m.group(1).strip()
    years = m.group(2).strip()
    frame = m.group(3).strip()
    yf, yt = parse_years(years)

    brand = default_brand
    model = name
    if name.lower().startswith(default_brand.lower() + " "):
        model = name[len(default_brand):].strip()

    variant = frame
    return brand, model, variant, frame, yf, yt


def discover_manufacturers() -> list[tuple[str, str]]:
    if BRAND_LIST_FILE.exists():
        data = json.loads(BRAND_LIST_FILE.read_text(encoding="utf-8"))
        return [(item["url"], item["title"]) for item in data]

    html = fetch(INDEX_URL)
    block_match = re.search(r'<div class="kategorien">(.*?)</div>', html, re.S)
    block = block_match.group(1) if block_match else html
    found: dict[str, str] = {}
    for url, title in MANUFACTURER_RE.findall(block):
        found[url] = clean(title)
    brands = sorted(found.items(), key=lambda x: x[1].lower())
    BRAND_LIST_FILE.write_text(
        json.dumps([{"url": u, "title": t} for u, t in brands], ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    return brands


def scrape_manufacturer(url: str, brand: str) -> list[VehicleRef]:
    try:
        html = fetch(url)
    except Exception:
        return []
    refs: list[VehicleRef] = []
    for match in VEHICLE_LINK_RE.finditer(html):
        refs.append(
            VehicleRef(
                url=match.group(1),
                sc_id=match.group("id"),
                label=match.group("label"),
                brand=brand,
                category=brand,
            )
        )
    return refs


def parse_specs(html: str) -> dict[str, str]:
    specs: dict[str, str] = {}
    for key, value in FZGROUP_RE.findall(html):
        specs[clean(key)] = clean(value)
    return specs


def map_specs(specs: dict[str, str], ref: VehicleRef, brand: str, model: str, variant: str, frame: str, yf: int, yt: int) -> dict[str, Any]:
    motor = specs.get("Motortyp", specs.get("Motorart", ""))
    cycle = "FOUR_STROKE" if "4" in motor.lower() or "4-takt" in motor.lower() else "TWO_STROKE"
    disp = specs.get("Hubraum", "")
    disp = re.sub(r"[^\d,]", "", disp.replace(".", ","))

    cat = ref.category.lower()
    model_l = model.lower()
    if "mofa" in cat or "moped" in cat:
        vtype = "MOFA"
    elif "mokick" in cat:
        vtype = "MOKICK"
    elif any(x in model_l for x in ("senda", "gpr", "sx ", "cross", "ktm")):
        vtype = "CROSS"
    else:
        vtype = "ROLLER"

    drive = "Variomatik" if "roller" in model_l or "scooter" in cat or brand.lower() in {
        "aprilia", "piaggio", "gilera", "yamaha", "honda", "kymco", "sym", "mbk", "peugeot"
    } else ""
    if not drive and vtype in {"MOFA", "MOKICK", "CROSS"}:
        drive = "Kette"

    entry_id = slugify(f"sc_{ref.sc_id}_{brand}_{model}_{frame}")
    return {
        "id": entry_id,
        "brand": brand,
        "model": model,
        "variant": variant if variant != frame else "",
        "frameCode": frame,
        "category": ref.category,
        "yearFrom": yf,
        "yearTo": yt,
        "vehicleType": vtype,
        "cycleType": cycle,
        "engine": {
            k: v
            for k, v in {
                "displacementCc": disp,
                "boreMm": specs.get("Bohrung", "").replace(" mm", "").replace(".", ","),
                "strokeMm": specs.get("Hub", "").replace(" mm", "").replace(".", ","),
                "compressionRatio": specs.get("Verdichtung", ""),
                "coolingType": specs.get("Art der Kühlung", ""),
                "cylinderCount": specs.get("Anzahl Zylinder", "1"),
            }.items()
            if v
        },
        "carbIgnition": {
            k: v
            for k, v in {
                "carbType": specs.get("Vergaser", ""),
                "fuelMixRatio": specs.get("Mischungsverhältnis", ""),
                "fuelType": specs.get("Treibstoff", ""),
                "sparkPlug": specs.get("Zündkerze", ""),
                "ignitionTimingDeg": specs.get("Vorzündung", ""),
                "ignitionSystem": specs.get("Art der Zündung", ""),
            }.items()
            if v
        },
        "drivetrain": {
            k: v
            for k, v in {
                "driveType": drive,
                "clutchType": specs.get("Kupplung", ""),
                "gearingPrimary": specs.get("Übersetzung Primär - 1:", ""),
                "finalDriveNotes": specs.get("Getriebeart", ""),
            }.items()
            if v
        },
        "chassis": {
            k: v
            for k, v in {
                "frontTire": specs.get("Reifen vorne", ""),
                "rearTire": specs.get("Reifen hinten", ""),
                "frontBrake": specs.get("Art der Bremse vorne", ""),
                "rearBrake": specs.get("Art der Bremse hinten", ""),
                "chassisNotes": specs.get("V max", ""),
            }.items()
            if v
        },
        "electrical": {
            k: v
            for k, v in {
                "battery": specs.get("Spannung", ""),
                "electricalNotes": specs.get("Leistung (Licht)", ""),
            }.items()
            if v
        },
    }


def build_entry_from_ref(ref: VehicleRef, specs: dict[str, str] | None = None) -> dict[str, Any]:
    brand, model, variant, frame, yf, yt = parse_label(ref.label, ref.brand)
    if specs:
        return map_specs(specs, ref, brand, model, variant, frame, yf, yt)
    return map_specs({}, ref, brand, model, variant, frame, yf, yt)


def fetch_detail(ref: VehicleRef) -> dict[str, Any]:
    try:
        html = fetch(ref.url)
        specs = parse_specs(html)
        return build_entry_from_ref(ref, specs)
    except Exception:
        return build_entry_from_ref(ref, None)


def main(workers: int = 12, fetch_details: bool = True) -> None:
    print("Discovering manufacturers...")
    manufacturers = discover_manufacturers()
    print(f"Found {len(manufacturers)} manufacturer pages")

    all_refs: list[VehicleRef] = []
    seen: set[str] = set()

    print("Collecting vehicle model links...")
    with ThreadPoolExecutor(max_workers=workers) as pool:
        futures = {
            pool.submit(scrape_manufacturer, url, brand): brand
            for url, brand in manufacturers
        }
        done = 0
        for fut in as_completed(futures):
            done += 1
            for ref in fut.result():
                if ref.url not in seen:
                    seen.add(ref.url)
                    all_refs.append(ref)
            if done % 10 == 0:
                print(f"  manufacturers {done}/{len(manufacturers)} -> {len(all_refs)} models")

    print(f"Total unique models: {len(all_refs)}")

    entries: list[dict[str, Any]] = []
    if fetch_details:
        print(f"Fetching technical details ({workers} workers)...")
        with ThreadPoolExecutor(max_workers=workers) as pool:
            futures = [pool.submit(fetch_detail, ref) for ref in all_refs]
            done = 0
            for fut in as_completed(futures):
                entries.append(fut.result())
                done += 1
                if done % 100 == 0:
                    print(f"  details {done}/{len(all_refs)}")
    else:
        entries = [build_entry_from_ref(ref) for ref in all_refs]

    entries.sort(key=lambda e: (e["brand"].lower(), e["model"].lower(), e["yearFrom"], e["id"]))

    catalog = {
        "version": 2,
        "source": "Scooter Center Fahrzeugdatenbank (scooter-center.com/unsere-fahrzeuge)",
        "entryCount": len(entries),
        "entries": entries,
    }

    OUT.parent.mkdir(parents=True, exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(catalog, f, ensure_ascii=False, indent=2)

    brands = len({e["brand"] for e in entries})
    with_specs = sum(1 for e in entries if e.get("engine", {}).get("displacementCc"))
    print(f"Done: {len(entries)} entries, {brands} brands, {with_specs} with displacement -> {OUT}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--no-details", action="store_true")
    parser.add_argument("--workers", type=int, default=12)
    args = parser.parse_args()
    main(workers=args.workers, fetch_details=not args.no_details)
