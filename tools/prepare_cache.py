import re
from pathlib import Path

def extract_dash_lines(src: Path, dst: Path):
    lines = []
    for line in src.read_text(encoding='utf-8', errors='replace').splitlines():
        if line.startswith('- '):
            lines.append(line)
    dst.write_text('\n'.join(lines) + '\n', encoding='utf-8')
    print(dst.name, len(lines))

cache = Path('catalog_cache')
cache.mkdir(exist_ok=True)
tools = Path('.')

# Peugeot from agent-tools copy
peu_src = Path(r'C:\Users\thannedo\.cursor\projects\c-Users-thannedo-Documents-GitHub-2StrokeCalc-App\agent-tools\9225b4d3-ea70-47b7-b093-91a8efdbc11f.txt')
extract_dash_lines(peu_src, cache / 'peugeot.txt')

# Gilera - save inline from fetch (dash lines only)
gilera_text = Path(r'C:\Users\thannedo\.cursor\projects\c-Users-thannedo-Documents-GitHub-2StrokeCalc-App\agent-tools\gilera_temp.txt')
# will create from shell
