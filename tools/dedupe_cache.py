#!/usr/bin/env python3
"""Deduplicate dash lines in catalog_cache/*.txt files."""
from pathlib import Path

CACHE = Path(__file__).resolve().parent / "catalog_cache"


def dedupe_file(path: Path) -> int:
    lines = [l for l in path.read_text(encoding="utf-8").splitlines() if l.strip()]
    unique = list(dict.fromkeys(lines))
    if len(unique) != len(lines):
        path.write_text("\n".join(unique) + "\n", encoding="utf-8")
    return len(lines) - len(unique)


def main():
    total = 0
    for path in sorted(CACHE.glob("*.txt")):
        removed = dedupe_file(path)
        if removed:
            print(f"{path.name}: removed {removed} duplicates")
            total += removed
    print(f"Done, {total} duplicate lines removed")


if __name__ == "__main__":
    main()
