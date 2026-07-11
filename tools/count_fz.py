import re, sys
from pathlib import Path
text = Path(sys.argv[1]).read_text(encoding='utf-8', errors='replace')
links = re.findall(r'href="(https://www\.scooter-center\.com/[^"]+/fz-(\d+)\.html)"\s+title="([^"]+)"', text)
print(len(links))
if links:
    print(links[0])
