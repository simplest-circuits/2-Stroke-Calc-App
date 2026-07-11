import re, json
from pathlib import Path
html = open(Path(__file__).parent / 'sample_fahrzeuge.html', encoding='utf-8').read()
m = re.search(r'<div class="kategorien">(.*?)</div>', html, re.S)
block = m.group(1)
links = re.findall(r'href="(https://www\.scooter-center\.com/[a-z0-9-]+/c-\d+\.html)"\s+title="([^"]+)"', block)
out = Path(__file__).parent / 'scooter_center_brands.json'
out.write_text(json.dumps([{'url':u,'title':t} for u,t in links], ensure_ascii=False, indent=2), encoding='utf-8')
print(len(links), '->', out)
