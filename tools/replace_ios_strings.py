#!/usr/bin/env python3
"""Replace hardcoded German string literals in iOS Swift with L.t("android_key")."""
from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
IOS = ROOT / "iosApp" / "TwoStrokeCalcIOS"
XML = ROOT / "androidApp" / "src" / "main" / "res" / "values" / "strings.xml"

SKIP_FILES = {
    "StringCatalog.swift",
    "S.swift",
    "AndroidStrings.swift",
    "L.swift",
}

# Prefer longer / more specific keys when multiple keys share the same German text.
PREFERRED_KEYS: dict[str, str] = {
    "Serie": "port_area_assessment_series",
    "Sportlich": "port_area_assessment_sporty",
    "Werte eingeben…": "pt_not_calculated",
    "Ergebnis": "pt_result_title",
    "Diagramm": "pt_section_diagram",
    "Modus": "flywheel_section_mode",
    "Leistung": "dc_cable_input_mode_power",
    "PS": "flywheel_unit_ps",
    "Hubraum": "compression_result_displacement",
    "ccm": "exhaust_unit_cc",
    "Koeffizienten": "exhaust_section_coefficients",
    "bar": "exhaust_unit_bar",
    "°C": "carb_jet_unit_celsius",
    "m/s": "exhaust_unit_ms",
    "mm": "pt_mm",
    "Zurück": "walkthrough_back",
    "Zurück zum Login": "forgot_password_back_to_login",
}

# Manual mapping for strings that differ slightly from Android or are composite.
MANUAL: dict[str, str] = {
    "Kritisch niedrig": "port_area_assessment_critical",
    "Serie": "port_area_assessment_series",
    "Sportlich": "port_area_assessment_sporty",
    "Aggressiv": "port_area_assessment_aggressive",
    "Vorwärts": "compression_mode_forward",
    "Ziel-CR": "compression_mode_target",
    "Ändern": "compression_mode_change",
    "Prozent": "squish_band_mode_percent",
    "Breite": "squish_band_mode_width",
    "Manuell": "vehicles_add_mode_manual",
    "Katalog": "vehicles_add_mode_catalog",
    "Gemisch": "vehicles_maint_fuel_mix",
    "Motornotizen": "vehicles_field_engine_notes",
    "Vergaser/Zündung": "vehicles_tab_tuning",
    "Kurzinfo": "vehicles_tab_overview",
    "KM-Stand": "vehicles_field_odometer",
    "Liter": "vehicles_fuel_log_liters",
    "Stromstärke": "electrolyte_current_label",
    "Gesamtvolumen": "electrolyte_total_volume_label",
    "Säurekonzentration": "electrolyte_acid_concentration_label",
    "Gleitstücke": "vehicles_field_variator_rollers",
    "Fahrzeugmasse": "vehicle_dynamics_mass_label",
    "Kolbengewicht": "counterweight_piston_weight_label",
    "Primär Ritzel": "vehicles_field_front_sprocket",
    "Sekundär Ritzel": "vehicles_field_rear_sprocket",
    "Primär Ritzel/Rad": "gear_primary_label",
    "Sekundär Ritzel/Rad": "gear_secondary_label",
    "Primär Rad": "gear_primary_label",
    "Sekundär Rad": "gear_secondary_label",
    "Pleuelhälfte": "counterweight_rod_half_label",
    "Referenz-Höhe": "carb_jet_reference_altitude_label",
    "Ziel-Höhe": "carb_jet_target_altitude_label",
    "Port-Höhe": "port_area_exhaust_port_height_label",
    "Basis-Hauptdüse": "carb_jet_main_jet_label",
    "Fahrzeugdaten": "vehicles_section_basic",
    "Demo-Fahrzeuge": "admin_demo_vehicles_section_title",
    "Nutzer suchen…": "admin_search_users_hint",
    "Nutzer": "admin_users_heading",
    "Tankbuch-Eintrag": "vehicles_fuel_log_edit",
    "Kostenposition": "vehicles_maintenance_cost",
    "Kostenposition hinzufügen": "vehicles_maintenance_add",
    "Kosten (€)": "vehicles_maintenance_cost",
    "Preis (€)": "vehicles_fuel_log_price",
    "Noch keine Einträge": "vehicles_cost_overview_empty",
    "Noch keine Wartungseinträge": "vehicles_maintenance_empty",
    "Mindestens 2 Einträge für Verbrauch": "vehicles_fuel_log_average_hint",
    "Spritverbrauch erfassen": "vehicles_fuel_log_add",
    "Wartung und Betriebskosten": "vehicles_cost_overview_title",
    "Suche Marke/Modell…": "vehicles_catalog_search_hint",
    "Bitte Name oder Marke+Modell angeben.": "vehicles_add_validation_error",
    "Glockendurchmesser": "compression_dome_diameter_label",
    "Glockenvolumen": "compression_dome_volume_label",
    "Quetschband": "compression_squish_band_label",
    "Kolbenkonstante (optional)": "compression_piston_constant_label",
    "Kanäle": "pt_section_diagram",
    "Stammdaten": "vehicles_tab_basic",
    "Rechner-Verfügbarkeit": "admin_calculator_availability_title",
    "Rechner noch nicht verfügbar.": "calculator_not_available",
    "Pro-Produkt nicht verfügbar.": "billing_product_unavailable",
    "Bitte eine Nachricht eingeben.": "settings_contact_message_required",
    "Erste Veröffentlichung": "settings_changelog_v1_item_1",
    "Segment antippen für Details": "exhaust_diagram_segment_hint",
}

SKIP_LITERAL_PATTERNS = [
    re.compile(r"^[a-z0-9_.]+$"),  # bundle ids, keys
    re.compile(r"^[A-Z][a-zA-Z]+$"),  # single English words like Tab, PS ambiguous handled via PREFERRED
    re.compile(r"^https?://"),
    re.compile(r"^[0-9.]+$"),
    re.compile(r"\\"),
]


def load_de() -> dict[str, str]:
    result: dict[str, str] = {}
    for node in ET.parse(XML).getroot().findall("string"):
        name = node.attrib.get("name")
        if name:
            result[name] = "".join(node.itertext()).replace("\\n", "\n")
    return result


def build_reverse_map(de: dict[str, str]) -> dict[str, str]:
    by_text: dict[str, list[str]] = {}
    for key, value in de.items():
        by_text.setdefault(value, []).append(key)
    mapping: dict[str, str] = {}
    for text, keys in by_text.items():
        if text in PREFERRED_KEYS:
            mapping[text] = PREFERRED_KEYS[text]
        elif len(keys) == 1:
            mapping[text] = keys[0]
        else:
            # pick shortest key name as heuristic for labels
            mapping[text] = sorted(keys, key=len)[0]
    mapping.update(MANUAL)
    return mapping


def should_skip_literal(s: str, mapping: dict[str, str]) -> bool:
    if s in mapping:
        return False
    if len(s) < 2:
        return True
    for pat in SKIP_LITERAL_PATTERNS:
        if pat.search(s):
            return True
    return False


def replace_in_file(path: Path, mapping: dict[str, str]) -> int:
    text = path.read_text(encoding="utf-8")
    original = text
    count = 0

    # Sort by length descending to replace longer strings first.
    for german, key in sorted(mapping.items(), key=lambda item: len(item[0]), reverse=True):
        if should_skip_literal(german, mapping):
            continue
        escaped = re.escape(german)
        # Only replace normal double-quoted literals, not already L.t(
        pattern = re.compile(rf'(?<!L\.t\()"({escaped})"')
        replacement = f'L.t("{key}")'
        new_text, n = pattern.subn(replacement, text)
        if n:
            count += n
            text = new_text

    if text != original:
        path.write_text(text, encoding="utf-8")
    return count


def main() -> int:
    de = load_de()
    mapping = build_reverse_map(de)
    total = 0
    files_changed = 0

    for swift in sorted(IOS.rglob("*.swift")):
        if swift.name in SKIP_FILES:
            continue
        n = replace_in_file(swift, mapping)
        if n:
            files_changed += 1
            total += n
            print(f"{swift.relative_to(ROOT)}: {n} replacements")

    print(f"\nDone: {total} replacements in {files_changed} files")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
