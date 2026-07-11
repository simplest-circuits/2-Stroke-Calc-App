#!/usr/bin/env python3
"""Save vehicle list lines from markdown/text into catalog_cache by brand slug."""
import re
import sys
from pathlib import Path

TOOLS = Path(__file__).resolve().parent
CACHE = TOOLS / "catalog_cache"


def extract_lines(text: str) -> list[str]:
    lines: list[str] = []
    in_section = False
    for raw in text.splitlines():
        line = raw.strip()
        if line == "Fahrzeuge":
            in_section = True
            continue
        if in_section and line.startswith("#"):
            break
        if in_section and line.startswith("- "):
            lines.append(line)
    if not lines:
        for raw in text.splitlines():
            line = raw.strip()
            if line.startswith("- "):
                lines.append(line)
            elif line.endswith("Details"):
                core = line[:-7].strip()
                if core and core not in ("Explos", "Fahrzeuge") and not core.startswith("#"):
                    lines.append(f"- {core}")
    return list(dict.fromkeys(lines))


def save(slug: str, text: str) -> int:
    CACHE.mkdir(exist_ok=True)
    lines = extract_lines(text)
    out = CACHE / f"{slug}.txt"
    out.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return len(lines)


def main():
    if len(sys.argv) < 3:
        print("usage: save_brand_cache.py <slug> <markdown-file>")
        sys.exit(1)
    slug, src = sys.argv[1], Path(sys.argv[2])
    n = save(slug, src.read_text(encoding="utf-8", errors="replace"))
    print(f"{slug}: {n} lines")


if __name__ == "__main__":
    main()
