#!/usr/bin/env python3
"""Convert motorradonline.de scraped records into vehicle_catalog entries."""
from __future__ import annotations

import json
import re
from pathlib import Path

TOOLS = Path(__file__).resolve().parent
CACHE = TOOLS / "motorrad_cache"
IN_FILE = CACHE / "all_entries.jsonl"
OUT_ENTRIES = CACHE / "motorrad_catalog_entries.json"

KA = {"", "k.A.", "k.a.", None}


def slugify(t: str) -> str:
    return re.sub(r"[^a-z0-9]+", "_", t.lower()).strip("_")[:90]


def val(v) -> str:
    if v in KA:
        return ""
    return str(v).strip()


def infer_vehicle_type(art: str) -> str:
    a = (art or "").lower()
    if any(x in a for x in ("enduro", "motocross", "cross", "supermoto", "trial", "super enduro")):
        return "CROSS"
    if any(x in a for x in ("mofa", "moped")):
        return "MOFA"
    if any(x in a for x in ("roller", "scooter")):
        return "ROLLER"
    return "OTHER"


def infer_cycle(strokes, art: str) -> str:
    try:
        if int(strokes) == 2:
            return "TWO_STROKE"
    except (TypeError, ValueError):
        pass
    return "FOUR_STROKE"


def infer_drive(gear: str) -> str:
    g = (gear or "").lower()
    if "automatik" in g or "cvt" in g or "variomatik" in g:
        return "Variomatik"
    if gear:
        return "Kette"
    return "Kette"


def carb_or_injection(mix: str) -> tuple[str, str]:
    m = (mix or "").lower()
    if "einspritz" in m or "injection" in m:
        return "", mix
    if "vergaser" in m:
        return mix, ""
    return mix, ""


def record_to_entry(td: dict) -> dict | None:
    year = td.get("Fahrzeuge_FahrzeugJahr")
    if not year:
        return None
    brand = val(td.get("Hersteller_HerstellerName"))
    model = val(td.get("Modelle_ModellName")) or val(td.get("Baureihen_BaureiheName"))
    if not brand or not model:
        return None
    variant = val(td.get("Fahrzeuge_FahrzeugVariante"))
    version = val(td.get("Fahrzeuge_FahrzeugVersion"))
    if version and version != variant:
        variant = f"{variant} {version}".strip() if variant else version
    category = val(td.get("Fahrzeuge_FahrzeugArt"))
    strokes = td.get("Motoren01_AnzahlTakte")
    cycle = infer_cycle(strokes, category)
    mix = val(td.get("Motoren01_GemischbildungBauart"))
    carb, intake = carb_or_injection(mix)
    if not intake:
        intake = mix
    gear = val(td.get("Kraftuebertragungen_Getriebeart"))
    weight = val(td.get("MasseGewichte_GewichtTrocken")) or val(td.get("MasseGewichte_GewichtLeer"))
    port_notes = ""
    if cycle == "TWO_STROKE":
        einlass = val(td.get("Motoren01_ZweitakterEinlasssteuerung"))
        auslass = val(td.get("Motoren01_ZweitakterAuslasssteuerung"))
        if einlass or auslass:
            port_notes = ", ".join(x for x in (f"Einlass: {einlass}" if einlass else "", f"Auslass: {auslass}" if auslass else "") if x)

    y = int(year)
    unique = val(td.get("ID")) or val(td.get("Slnr")) or val(td.get("Fahrzeuge_FahrzeugVersion"))
    eid = slugify(f"mrd_{brand}_{model}_{variant}_{y}_{unique or td.get('Motoren01_Hubraum')}")

    entry = {
        "id": eid,
        "brand": brand,
        "model": model,
        "variant": variant,
        "frameCode": "",
        "category": category,
        "yearFrom": y,
        "yearTo": y,
        "vehicleType": infer_vehicle_type(category),
        "cycleType": cycle,
        "engine": {k: v for k, v in {
            "displacementCc": val(td.get("Motoren01_Hubraum")),
            "boreMm": val(td.get("Motoren01_Bohrung")),
            "strokeMm": val(td.get("Motoren01_Hub")),
            "compressionRatio": val(td.get("Motoren01_Verdichtungsverhaeltnis")),
            "coolingType": val(td.get("Motoren01_Kuehlung")),
            "cylinderCount": val(td.get("Motoren01_AnzahlZylinder")),
            "intakeSystem": intake,
            "portTimingNotes": port_notes,
            "engineNotes": val(td.get("Motoren01_Motorschmierung")),
        }.items() if v},
        "carbIgnition": {k: v for k, v in {
            "carbType": carb,
            "ignitionSystem": val(td.get("Motoren01_ZuendungBauart")),
        }.items() if v},
        "drivetrain": {k: v for k, v in {
            "driveType": infer_drive(gear),
            "finalDriveNotes": gear,
        }.items() if v},
        "chassis": {k: v for k, v in {
            "chassisNotes": f"Leergewicht {weight} kg" if weight else "",
        }.items() if v},
        "electrical": {k: v for k, v in {
            "battery": " / ".join(
                x for x in (
                    f"{val(td.get('Motoren01_BatterieKapazitaet'))} Ah" if val(td.get('Motoren01_BatterieKapazitaet')) else "",
                    f"{val(td.get('Motoren01_BatterieSpannung'))} V" if val(td.get('Motoren01_BatterieSpannung')) else "",
                ) if x
            ),
        }.items() if v},
    }
    return entry


def main():
    if not IN_FILE.exists():
        print(f"Missing {IN_FILE} – run scrape_motorradonline.py first")
        return
    entries: list[dict] = []
    seen: set[str] = set()
    for line in IN_FILE.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        td = json.loads(line)
        entry = record_to_entry(td)
        if entry and entry["id"] not in seen:
            seen.add(entry["id"])
            entries.append(entry)
    entries.sort(key=lambda e: (e["brand"].lower(), e["model"].lower(), e["yearFrom"]))
    OUT_ENTRIES.write_text(json.dumps(entries, ensure_ascii=False, indent=2), encoding="utf-8")
    two_stroke = sum(1 for e in entries if e["cycleType"] == "TWO_STROKE")
    print(f"Converted {len(entries)} entries ({two_stroke} two-stroke) -> {OUT_ENTRIES}")


if __name__ == "__main__":
    main()
