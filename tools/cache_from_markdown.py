#!/usr/bin/env python3
"""Save WebFetch markdown dump to catalog_cache/{slug}.txt"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from save_brand_cache import save  # noqa: E402


def main():
    if len(sys.argv) < 3:
        print("usage: cache_from_markdown.py <slug> <markdown-file>")
        sys.exit(1)
    slug, src = sys.argv[1], Path(sys.argv[2])
    n = save(slug, src.read_text(encoding="utf-8", errors="replace"))
    print(f"{slug}: {n} lines")


if __name__ == "__main__":
    main()
