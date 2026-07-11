#!/usr/bin/env python3
"""Fetch all Scooter Center brand lists via WebFetch proxy and build catalog.

Run from project root after rate-limit cooldown. Uses only cached files in
catalog_cache/ and agent-tools/ — no live HTTP from this machine.
"""
import subprocess
import sys
from pathlib import Path

TOOLS = Path(__file__).resolve().parent


def main():
    steps = [
        [sys.executable, str(TOOLS / "import_webfetch_cache.py")],
        [sys.executable, str(TOOLS / "sync_catalog_cache.py")],
        [sys.executable, str(TOOLS / "build_mass_catalog.py")],
    ]
    for cmd in steps:
        print(">>", " ".join(cmd))
        subprocess.check_call(cmd)


if __name__ == "__main__":
    main()
