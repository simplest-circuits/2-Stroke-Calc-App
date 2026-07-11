import re
from pathlib import Path
h = Path('sample_aprilia.html').read_text(encoding='utf-8')
m = re.findall(r'href="(https://www\.scooter-center\.com/[^"]+/fz-(\d+)\.html)"\s+title="([^"]+)"', h)
print('count', len(m))
print('sample', m[0])
