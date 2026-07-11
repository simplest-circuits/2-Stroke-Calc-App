#!/usr/bin/env python3
"""Parse Scooter Center markdown/text vehicle lists into catalog entries."""
import json
import re
from html import unescape
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "app" / "src" / "main" / "assets" / "vehicle_catalog.json"
TOOLS = Path(__file__).resolve().parent
CACHE = TOOLS / "catalog_cache"
BRANDS_FILE = TOOLS / "scooter_center_brands.json"

HTML_LINK_RE = re.compile(
    r'href="(https://www\.scooter-center\.com/[^"]+/fz-(\d+)\.html)"\s+title="(?P<label>[^"]+)"',
    re.I,
)
MD_LINE_RE = re.compile(
    r"^-\s+(.+?)(?:\s+\(([^)]+)\))?\s*$"
)
DETAILS_LINE_RE = re.compile(
    r"^(.+?)(?:\s+\(([^)]+)\))?\s*Details\s*$"
)
YEAR_RANGE_RE = re.compile(r"(\d{4})\s*-\s*(\d{4})")

# Spec templates by engine class keyword in model name
TEMPLATES = {
    "50_2t_lc": dict(cc="49,9", bore="40", stroke="39,8", carb="Dell'Orto PHBN 12", cooling="Flüssig", cycle="TWO_STROKE", drive="Variomatik"),
    "50_2t_ac": dict(cc="49,9", bore="40", stroke="39,8", carb="Dell'Orto PHVA 12", cooling="Luft", cycle="TWO_STROKE", drive="Variomatik"),
    "50_4t": dict(cc="49,9", bore="39", stroke="41,8", carb="", cooling="Luft", cycle="FOUR_STROKE", drive="Variomatik"),
    "125_2t": dict(cc="125", bore="52,4", stroke="57,8", carb="Dell'Orto PHBL 20", cooling="Luft", cycle="TWO_STROKE", drive="Variomatik"),
    "125_4t": dict(cc="124", bore="52,4", stroke="57,8", carb="", cooling="Luft", cycle="FOUR_STROKE", drive="Variomatik"),
    "150_4t": dict(cc="150", bore="57", stroke="57", carb="", cooling="Luft", cycle="FOUR_STROKE", drive="Variomatik"),
    "250_4t": dict(cc="250", bore="72", stroke="60", carb="", cooling="Flüssig", cycle="FOUR_STROKE", drive="Variomatik"),
    "300_4t": dict(cc="300", bore="72", stroke="60", carb="", cooling="Flüssig", cycle="FOUR_STROKE", drive="Variomatik"),
    "mofa": dict(cc="49,9", bore="40", stroke="39,8", carb="Gurtner AR2 12", cooling="Luft", cycle="TWO_STROKE", drive="Kette"),
}


def slugify(t: str) -> str:
    return re.sub(r"[^a-z0-9]+", "_", t.lower()).strip("_")[:90]


def clean(t: str) -> str:
    return unescape(re.sub(r"\s+", " ", t)).strip()


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


def infer_template(model: str, category: str) -> dict:
    m = model.lower()
    c = category.lower()
    if "mofa" in c or "moped" in c:
        return TEMPLATES["mofa"]
    if "4-takt" in m or "4t" in m or "ie" in m and "50" in m:
        if "125" in m:
            return TEMPLATES["125_4t"]
        if "150" in m:
            return TEMPLATES["150_4t"]
        if any(x in m for x in ("250", "300", "400", "500")):
            return TEMPLATES["300_4t"]
        return TEMPLATES["50_4t"]
    if "125" in m:
        return TEMPLATES["125_2t"]
    if "lc" in m or "lc-" in m or "di-tech" in m or "c-tech" in m:
        return TEMPLATES["50_2t_lc"]
    return TEMPLATES["50_2t_ac"]


def parse_parens(inner: str) -> tuple[int, int, str]:
    inner = inner.strip()
    if not inner:
        return 1970, 2026, ""
    parts = [p.strip() for p in inner.split(",")]
    years = ""
    frame = ""
    for p in parts:
        if re.search(r"\d{2,4}", p) and ("'" in p or re.search(r"\d{4}", p)):
            years = p
        elif not frame and not p.lower().startswith("typ") and not p.endswith("km/h"):
            if re.match(r"^[A-Z0-9]", p):
                frame = p
    if not years and parts:
        years = parts[0]
    yf, yt = parse_years(years) if years else (1970, 2026)
    return yf, yt, frame


def parse_markdown_list(text: str, brand: str, category: str) -> list[dict]:
    entries = []
    for line in text.splitlines():
        line = line.strip()
        inner = ""
        name = ""
        if line.startswith("- "):
            m = MD_LINE_RE.match(line)
            if not m:
                continue
            name = clean(m.group(1))
            inner = m.group(2) or ""
        elif line.endswith("Details"):
            m = DETAILS_LINE_RE.match(line)
            if not m:
                core = line[:-7].strip()
                if core and not core.startswith("#"):
                    name = clean(core)
            else:
                name = clean(m.group(1))
                inner = m.group(2) or ""
        else:
            continue
        if not name:
            continue
        yf, yt, frame = parse_parens(inner)
        tmpl = infer_template(name, category)
        vtype = "MOFA" if "mofa" in category.lower() or "moped" in category.lower() else "ROLLER"
        eid = slugify(f"{brand}_{name}_{frame}_{yf}")
        entries.append({
            "id": eid,
            "brand": brand.split()[0] if brand else name.split()[0],
            "model": name,
            "variant": "",
            "frameCode": frame,
            "category": category,
            "yearFrom": yf,
            "yearTo": yt,
            "vehicleType": vtype,
            "cycleType": tmpl["cycle"],
            "engine": {k: v for k, v in {
                "displacementCc": tmpl["cc"],
                "boreMm": tmpl["bore"],
                "strokeMm": tmpl["stroke"],
                "coolingType": tmpl["cooling"],
            }.items() if v},
            "carbIgnition": {k: v for k, v in {"carbType": tmpl.get("carb", "")}.items() if v},
            "drivetrain": {"driveType": tmpl["drive"]} if tmpl.get("drive") else {},
        })
    return entries


def parse_html_file(path: Path, brand: str, category: str) -> list[dict]:
    html = path.read_text(encoding="utf-8", errors="replace")
    entries = []
    for _, sc_id, label in HTML_LINK_RE.findall(html):
        label = clean(label)
        m = re.match(r"^(.+?)\s+\(([^,]+),\s*([^)]+)\)$", label)
        if m:
            name, years, frame = m.group(1).strip(), m.group(2).strip(), m.group(3).strip()
            b = brand
            model = name
            if name.lower().startswith(brand.lower()):
                model = name[len(brand):].strip()
            yf, yt = parse_years(years)
        else:
            b, model, yf, yt, frame = brand, label, 1970, 2026, ""
        tmpl = infer_template(model, category)
        entries.append({
            "id": slugify(f"sc_{sc_id}_{b}_{model}_{frame}"),
            "brand": b,
            "model": model,
            "variant": "",
            "frameCode": frame,
            "category": category,
            "yearFrom": yf,
            "yearTo": yt,
            "vehicleType": "ROLLER",
            "cycleType": tmpl["cycle"],
            "engine": {k: v for k, v in {
                "displacementCc": tmpl["cc"],
                "boreMm": tmpl["bore"],
                "strokeMm": tmpl["stroke"],
                "coolingType": tmpl["cooling"],
            }.items() if v},
            "drivetrain": {"driveType": tmpl["drive"]},
        })
    return entries


def main():
    entries: list[dict] = []
    seen: set[str] = set()

    def add_all(items: list[dict]):
        for e in items:
            if e["id"] not in seen:
                seen.add(e["id"])
                entries.append(e)

    # HTML caches (skip if markdown cache already present)
    html_sources = [
        (TOOLS / "sample_aprilia.html", "aprilia", "Aprilia", "Aprilia"),
    ]
    for path, slug, brand, cat in html_sources:
        if CACHE / f"{slug}.txt" in [p for p in CACHE.glob("*.txt")]:
            print(f"HTML {path.name}: skip (cache exists)")
            continue
        if path.exists():
            add_all(parse_html_file(path, brand, cat))
            print(f"HTML {path.name}: +{len(entries)}")

    # Markdown/text caches in catalog_cache
    CACHE.mkdir(exist_ok=True)
    brand_slug_map: dict[str, str] = {}
    if BRANDS_FILE.exists():
        import json as _json
        for item in _json.loads(BRANDS_FILE.read_text(encoding="utf-8")):
            slug = item["url"].split("/")[-2]
            brand_slug_map[slug] = item["title"]

    brand_map = {
        "Peugeot": "Peugeot", "Aprilia": "Aprilia", "Gilera": "Gilera",
        "Sym": "SYM", "Mbk": "MBK", "Piaggio": "Piaggio", "Honda": "Honda",
        "Yamaha": "Yamaha", "Kymco": "Kymco", "Derbi": "Derbi",
        "Vespa Klassik Largeframe Pxt5Cosalml 80125150200": "Vespa",
        "Peugeot Mofamoped": "Peugeot", "Puch Mofamoped": "Puch",
        "Piaggio 50 2T": "Piaggio", "Piaggio 50 4T": "Piaggio",
        "Piaggio 100125150200": "Piaggio", "Piaggio 125150180 Aclc": "Piaggio",
        "Piaggio 125150180200": "Piaggio", "Piaggio 250300310": "Piaggio",
        "Piaggio 350400500530": "Piaggio", "Piaggio Mofamoped": "Piaggio",
        "Yamaha Mofamoped": "Yamaha",
    }
    for path in sorted(CACHE.glob("*.txt")):
        slug = path.stem.replace("_", "-")
        brand = brand_slug_map.get(slug, slug.replace("-", " ").title())
        brand = brand_map.get(brand, brand)
        if slug == "piaggio" or slug.startswith("piaggio"):
            brand = "Piaggio"
        if slug == "kymco":
            brand = "Kymco"
        cat = brand_slug_map.get(slug, brand)
        add_all(parse_markdown_list(path.read_text(encoding="utf-8"), brand, cat))
        print(f"MD {path.name}: total {len(entries)}")

    # Hand-curated Vespa/Lambretta/Mofa classics with real specs
    import importlib.util
    spec = importlib.util.spec_from_file_location("gen", TOOLS / "generate_vehicle_catalog.py")
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    add_all(mod.ENTRIES)
    print(f"After static seed: {len(entries)}")

    # MOTORRAD online motorcycles
    motorrad_file = TOOLS / "motorrad_cache" / "motorrad_catalog_entries.json"
    if motorrad_file.exists():
        motorrad = json.loads(motorrad_file.read_text(encoding="utf-8"))
        add_all(motorrad)
        print(f"After motorradonline.de: {len(entries)} (+{len(motorrad)} from cache)")

    entries.sort(key=lambda e: (e["brand"].lower(), e["model"].lower(), e["yearFrom"]))

    catalog = {
        "version": 2,
        "source": "Scooter Center, MOTORRAD online, SIP Modelbase – kuratiert & erweitert",
        "entryCount": len(entries),
        "entries": entries,
    }
    OUT.write_text(json.dumps(catalog, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Wrote {len(entries)} entries ({len({e['brand'] for e in entries})} brands) -> {OUT}")
    if "--seed-firestore" in sys.argv:
        import subprocess
        seed = TOOLS.parent / "functions" / "scripts" / "seedVehicleCatalog.js"
        print("Seeding Firestore vehicle catalog...")
        subprocess.run(["node", str(seed)], check=True)
    else:
        print("Firestore seed: cd functions && npm run seed:catalog")


if __name__ == "__main__":
    main()
