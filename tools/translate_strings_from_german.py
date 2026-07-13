#!/usr/bin/env python3
"""Translate Android strings.xml from German (values/) to Nordic locales.

Reference language: German (values/strings.xml)
Targets: Swedish (values-sv), Danish (values-da), Norwegian Bokmål (values-nb)

Preserves Android format placeholders (%s, %1$d, etc.) and XML escaping.
"""
from __future__ import annotations

import re
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

from deep_translator import GoogleTranslator

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "androidApp" / "src" / "main" / "res" / "values" / "strings.xml"

TARGETS: dict[str, tuple[str, Path]] = {
    "sv": ("sv", ROOT / "androidApp" / "src" / "main" / "res" / "values-sv" / "strings.xml"),
    "da": ("da", ROOT / "androidApp" / "src" / "main" / "res" / "values-da" / "strings.xml"),
    "nb": ("no", ROOT / "androidApp" / "src" / "main" / "res" / "values-nb" / "strings.xml"),
}

SKIP_KEYS = {"app_name"}
PLACEHOLDER_RE = re.compile(r"%(?:\d+\$)?[sd]|%[sd]")
TOKEN_PREFIX = "XPH"
BATCH_SIZE = 40


def protect_placeholders(text: str, counter: list[int]) -> tuple[str, dict[str, str]]:
    tokens: dict[str, str] = {}

    def repl(match: re.Match[str]) -> str:
        token = f"{TOKEN_PREFIX}{counter[0]}"
        counter[0] += 1
        tokens[token] = match.group(0)
        return token

    return PLACEHOLDER_RE.sub(repl, text), tokens


def restore_placeholders(text: str, tokens: dict[str, str]) -> str:
    for token, placeholder in tokens.items():
        text = text.replace(token, placeholder)
    return text


def xml_escape(text: str) -> str:
    return (
        text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("'", "\\'")
        .replace('"', '\\"')
        .replace("\n", "\\n")
    )


def translate_batch(texts: list[str], translator: GoogleTranslator) -> list[str]:
    if not texts:
        return []
    counter = [0]
    protected: list[str] = []
    token_maps: list[dict[str, str]] = []
    for text in texts:
        if not text.strip():
            protected.append(text)
            token_maps.append({})
            continue
        p, tokens = protect_placeholders(text, counter)
        protected.append(p)
        token_maps.append(tokens)

    for attempt in range(4):
        try:
            translated = translator.translate_batch(protected)
            results: list[str] = []
            for original, result, tokens in zip(texts, translated, token_maps, strict=True):
                if result is None or not str(result).strip():
                    results.append(original)
                elif tokens:
                    results.append(restore_placeholders(str(result), tokens))
                else:
                    results.append(str(result))
            return results
        except Exception as exc:  # noqa: BLE001
            if attempt == 3:
                print(f"  WARN: batch failed ({exc!r}), keeping German for {len(texts)} strings")
                return texts
            time.sleep(2 * (attempt + 1))
    return texts


def write_strings_xml(path: Path, entries: list[tuple[str, str]]) -> None:
    lines = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"]
    for name, value in entries:
        safe_value = value if value is not None else ""
        lines.append(f'    <string name="{name}">{xml_escape(safe_value)}</string>')
    lines.append("</resources>")
    lines.append("")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines), encoding="utf-8")


def main() -> int:
    if not SOURCE.exists():
        print(f"Source not found: {SOURCE}", file=sys.stderr)
        return 1

    tree = ET.parse(SOURCE)
    source_entries: list[tuple[str, str]] = []
    for node in tree.getroot().findall("string"):
        name = node.attrib.get("name")
        if not name:
            continue
        source_entries.append((name, "".join(node.itertext())))

    print(f"Source: {len(source_entries)} strings from {SOURCE.relative_to(ROOT)}", flush=True)

    for locale_folder, (google_code, out_path) in TARGETS.items():
        print(f"\nTranslating de -> {locale_folder} ({google_code}) ...", flush=True)
        translator = GoogleTranslator(source="de", target=google_code)
        translated: list[tuple[str, str]] = []

        pending_names: list[str] = []
        pending_texts: list[str] = []
        for name, text in source_entries:
            if name in SKIP_KEYS:
                translated.append((name, text))
                continue
            pending_names.append(name)
            pending_texts.append(text)
            if len(pending_texts) >= BATCH_SIZE:
                results = translate_batch(pending_texts, translator)
                translated.extend(zip(pending_names, results, strict=True))
                print(f"  {len(translated)}/{len(source_entries)}", flush=True)
                pending_names, pending_texts = [], []
                time.sleep(0.2)

        if pending_texts:
            results = translate_batch(pending_texts, translator)
            translated.extend(zip(pending_names, results, strict=True))
            print(f"  {len(translated)}/{len(source_entries)}", flush=True)

        write_strings_xml(out_path, translated)
        print(f"Wrote {out_path.relative_to(ROOT)}", flush=True)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
