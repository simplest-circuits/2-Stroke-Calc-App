#!/usr/bin/env python3
from __future__ import annotations

import re
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
IOS = ROOT / "iosApp" / "TwoStrokeCalcIOS"
XML = ROOT / "androidApp" / "src" / "main" / "res" / "values" / "strings.xml"
SKIP = {"StringCatalog.swift", "S.swift", "AndroidStrings.swift", "L.swift"}

de: dict[str, str] = {}
for node in ET.parse(XML).getroot().findall("string"):
    name = node.attrib.get("name")
    if name:
        de[name] = "".join(node.itertext()).replace("\\n", "\n")
by_text: dict[str, list[str]] = {}
for key, value in de.items():
    by_text.setdefault(value, []).append(key)

pat = re.compile(r'"((?:[^"\\]|\\.)*)"')
unmatched: set[str] = set()
matched_count = 0
for swift in IOS.rglob("*.swift"):
    if swift.name in SKIP:
        continue
    text = swift.read_text(encoding="utf-8")
    for m in pat.finditer(text):
        if "L.t(" in text[max(0, m.start() - 10) : m.start()]:
            continue
        s = m.group(1).replace("\\n", "\n").replace('\\"', '"')
        if len(s) < 2 or s.startswith("http"):
            continue
        if s in by_text:
            matched_count += 1
            continue
        if re.search(r"[äöüÄÖÜß°€]", s) or any(
            w in s for w in ("Bitte", "Zurück", "Berechnen", "Fahrzeug", "Motor", "Wartung", "Tank", "Verbrauch", "Kosten", "Einlass", "Auslass", "Transfer", "Vorauslass", "Verdichtung", "Getriebe", "Essig", "Reinigung", "Zünd", "Kolben", "Hub", "Pleuel", "Diagramm", "Modus", "Vorwärts", "Nutzer", "Bearbeiten", "Stammdaten", "Katalog", "Manuell", "Marke", "Modell", "Kennzeichen", "Baujahr", "Antrieb", "Fahrwerk", "Elektrik", "Vergaser", "Gemisch", "Gesamt", "Kraftstoff", "Wartung", "Beschreibung", "Datum", "Liter", "Kilometer")
        ):
            unmatched.add(s)

print("still matchable in android:", matched_count)
print("unmatched:", len(unmatched))
for s in sorted(unmatched, key=len):
    print(repr(s))
