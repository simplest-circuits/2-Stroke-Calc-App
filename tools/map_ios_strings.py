#!/usr/bin/env python3
"""Map hardcoded German strings in iOS Swift files to Android string keys."""
from __future__ import annotations

import re
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
IOS = ROOT / "iosApp" / "TwoStrokeCalcIOS"
XML = ROOT / "androidApp" / "src" / "main" / "res" / "values" / "strings.xml"

SKIP_FILES = {"StringCatalog.swift", "S.swift", "AndroidStrings.swift", "L.swift"}

def load_de() -> dict[str, str]:
    result: dict[str, str] = {}
    for node in ET.parse(XML).getroot().findall("string"):
        name = node.attrib.get("name")
        if name:
            result[name] = "".join(node.itertext()).replace("\\n", "\n")
    return result


def main() -> None:
    de = load_de()
    by_text: dict[str, list[str]] = {}
    for key, value in de.items():
        by_text.setdefault(value, []).append(key)

    pat = re.compile(r'"((?:[^"\\]|\\.)*)"')
    matched = 0
    unmatched: list[tuple[str, str]] = []

    for swift in sorted(IOS.rglob("*.swift")):
        if swift.name in SKIP_FILES:
            continue
        rel = swift.relative_to(ROOT)
        text = swift.read_text(encoding="utf-8")
        for m in pat.finditer(text):
            s = m.group(1).replace("\\n", "\n").replace('\\"', '"')
            if len(s) < 2 or s.startswith("http"):
                continue
            if not any(ord(c) > 127 for c in s) and s.replace(" ", "").isascii():
                # skip pure ASCII unless known UI word from de file
                if s not in by_text and not re.search(r"[äöüÄÖÜß]", s):
                    continue
            keys = by_text.get(s)
            if keys:
                matched += 1
                print(f"MATCH {rel}: {keys[0]!r} <- {s[:60]!r}")
            elif re.search(r"[äöüÄÖÜß]", s) or s in by_text:
                unmatched.append((str(rel), s))

    print(f"\nMatched occurrences: {matched}")
    print(f"Unmatched unique ({len(set(t for _, t in unmatched))}):")
    for s in sorted(set(t for _, t in unmatched))[:80]:
        print(f"  {s[:100]!r}")


if __name__ == "__main__":
    main()
