#!/usr/bin/env python3
"""Parse cached Scooter Center HTML and merge with expanded static catalog."""
from __future__ import annotations

import json
import re
from html import unescape
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "app" / "src" / "main" / "assets" / "vehicle_catalog.json"
CACHE_DIR = Path(__file__).resolve().parent / "catalog_cache"

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


def clean(t: str) -> str:
    t = unescape(t).strip()
    t = re.sub(r"\s+", " ", t)
    if re.fullmatch(r"\d+\.\d+", t):
        return t.replace(".", ",")
    return t


def slugify(t: str) -> str:
    return re.sub(r"[^a-z0-9]+", "_", t.lower()).strip("_")[:90]


def parse_years(text: str) -> tuple[int, int]:
    text = text.strip()
    m = YEAR_RANGE_RE.search(text)
    if m:
        return int(m.group(1)), int(m.group(2))
    m = re.match(r"^(\d{4})$", text)
    if m:
        y = int(m.group(1))
        return y, y
    m = re.match(r"'?(\d{2})\s*-\s*'?(?:(\d{2})|$)", text)
    if m:
        y1 = int(m.group(1))
        y1 = 1900 + y1 if y1 >= 30 else 2000 + y1
        y2 = 2026
        if m.group(2):
            y2 = int(m.group(2))
            y2 = 1900 + y2 if y2 >= 30 else 2000 + y2
        return y1, y2
    return 1970, 2026


def parse_label(label: str, default_brand: str) -> tuple[str, str, str, str, int, int]:
    label = clean(label)
    m = re.match(r"^(.+?)\s+\(([^,]+),\s*([^)]+)\)$", label)
    if not m:
        return default_brand, label, "", "", 1970, 2026
    name, years, frame = m.group(1).strip(), m.group(2).strip(), m.group(3).strip()
    yf, yt = parse_years(years)
    brand = default_brand
    model = name
    if name.lower().startswith(default_brand.lower() + " "):
        model = name[len(default_brand):].strip()
    return brand, model, frame, frame, yf, yt


def parse_specs(html: str) -> dict[str, str]:
    return {clean(k): clean(v) for k, v in FZGROUP_RE.findall(html)}


def entry_from(label: str, brand: str, sc_id: str, category: str, specs: dict | None = None) -> dict:
    b, model, variant, frame, yf, yt = parse_label(label, brand)
    specs = specs or {}
    motor = specs.get("Motortyp", specs.get("Motorart", ""))
    cycle = "FOUR_STROKE" if "4" in motor.lower() else "TWO_STROKE"
    disp = re.sub(r"[^\d,]", "", specs.get("Hubraum", "").replace(".", ","))
    cat_l = category.lower()
    if "mofa" in cat_l or "moped" in cat_l:
        vtype = "MOFA"
    elif "cross" in cat_l or "ktm" in model.lower():
        vtype = "CROSS"
    else:
        vtype = "ROLLER"
    return {
        "id": slugify(f"sc_{sc_id}_{b}_{model}_{frame}"),
        "brand": b,
        "model": model,
        "variant": "",
        "frameCode": frame,
        "category": category,
        "yearFrom": yf,
        "yearTo": yt,
        "vehicleType": vtype,
        "cycleType": cycle,
        "engine": {k: v for k, v in {
            "displacementCc": disp,
            "boreMm": specs.get("Bohrung", "").replace(" mm", "").replace(".", ","),
            "strokeMm": specs.get("Hub", "").replace(" mm", "").replace(".", ","),
            "compressionRatio": specs.get("Verdichtung", ""),
            "coolingType": specs.get("Art der Kühlung", ""),
            "cylinderCount": specs.get("Anzahl Zylinder", "1"),
        }.items() if v},
        "carbIgnition": {k: v for k, v in {
            "carbType": specs.get("Vergaser", ""),
            "fuelMixRatio": specs.get("Mischungsverhältnis", ""),
            "fuelType": specs.get("Treibstoff", ""),
            "sparkPlug": specs.get("Zündkerze", ""),
            "ignitionTimingDeg": specs.get("Vorzündung", ""),
            "ignitionSystem": specs.get("Art der Zündung", ""),
        }.items() if v},
        "drivetrain": {k: v for k, v in {
            "driveType": "Variomatik" if vtype == "ROLLER" else "Kette",
            "clutchType": specs.get("Kupplung", ""),
        }.items() if v},
        "chassis": {k: v for k, v in {
            "frontTire": specs.get("Reifen vorne", ""),
            "rearTire": specs.get("Reifen hinten", ""),
            "frontBrake": specs.get("Art der Bremse vorne", ""),
            "rearBrake": specs.get("Art der Bremse hinten", ""),
        }.items() if v},
        "electrical": {k: v for k, v in {
            "battery": specs.get("Spannung", ""),
        }.items() if v},
    }


def parse_brand_html(path: Path, brand: str, category: str) -> list[dict]:
    html = path.read_text(encoding="utf-8", errors="replace")
    entries = []
    for m in VEHICLE_LINK_RE.finditer(html):
        entries.append(entry_from(m.group("label"), brand, m.group("id"), category))
    return entries


def load_static_expansion() -> list[dict]:
    """Import the hand-curated expansion from generate_vehicle_catalog."""
    import importlib.util
    spec = importlib.util.spec_from_file_location(
        "gen", Path(__file__).parent / "generate_vehicle_catalog.py"
    )
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod.ENTRIES


def main():
    entries: list[dict] = []
    seen: set[str] = set()

    def add(entry: dict):
        if entry["id"] not in seen:
            seen.add(entry["id"])
            entries.append(entry)

    # Cached Scooter Center brand pages
    cache_map = {
        "sample_aprilia.html": ("Aprilia", "Aprilia"),
    }
    tools = Path(__file__).parent
    for fname, (brand, cat) in cache_map.items():
        p = tools / fname
        if p.exists():
            for e in parse_brand_html(p, brand, cat):
                add(e)
            print(f"Parsed {fname}: {len(entries)} total")

    # Static curated expansion
    for raw in load_static_expansion():
        add(raw)

    entries.sort(key=lambda e: (e["brand"].lower(), e["model"].lower(), e["yearFrom"]))

    catalog = {
        "version": 2,
        "source": "Scooter Center, SIP Modelbase, Scooter Help – kuratiert & erweitert",
        "entryCount": len(entries),
        "entries": entries,
    }
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(catalog, ensure_ascii=False, indent=2), encoding="utf-8")
    brands = len({e["brand"] for e in entries})
    print(f"Wrote {len(entries)} entries ({brands} brands) -> {OUT}")


if __name__ == "__main__":
    main()
