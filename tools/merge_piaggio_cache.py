#!/usr/bin/env python3
"""Merge multiple markdown dumps into one catalog_cache file."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from save_brand_cache import extract_lines  # noqa: E402

CACHE = Path(__file__).resolve().parent / "catalog_cache"


def merge(slug: str, *sources: Path) -> int:
    lines: list[str] = []
    for src in sources:
        if src.exists():
            lines.extend(extract_lines(src.read_text(encoding="utf-8", errors="replace")))
    unique = list(dict.fromkeys(lines))
    out = CACHE / f"{slug}.txt"
    out.write_text("\n".join(unique) + "\n", encoding="utf-8")
    return len(unique)


def main():
    dumps = Path(__file__).resolve().parent / "webfetch_dumps"
    n = merge(
        "piaggio",
        dumps / "piaggio-50-2t.md",
        dumps / "piaggio-50-4t.md",
        dumps / "piaggio-125150180-aclc.md",
    )
    print(f"piaggio: {n} lines merged")


if __name__ == "__main__":
    main()
