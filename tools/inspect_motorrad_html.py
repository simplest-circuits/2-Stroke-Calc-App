#!/usr/bin/env python3
import json
import re
from pathlib import Path

html = Path(__file__).parent / "motorrad_sample.html"
text = html.read_text(encoding="utf-8")
m = re.search(r'<script id="__NEXT_DATA__"[^>]*>(.*?)</script>', text, re.S)
if m:
    data = json.loads(m.group(1))
    props = data.get("props", {}).get("pageProps", {})
    print("pageProps keys:", list(props.keys()))
    for k, v in props.items():
        if isinstance(v, dict):
            print(f"  {k}: dict keys {list(v.keys())[:15]}")
        elif isinstance(v, list):
            print(f"  {k}: list len {len(v)}")
        else:
            print(f"  {k}: {type(v).__name__} = {str(v)[:80]}")
    # dump interesting parts
    out = Path(__file__).parent / "motorrad_pageprops.json"
    out.write_text(json.dumps(props, ensure_ascii=False, indent=2)[:50000], encoding="utf-8")
    print("wrote", out)
else:
    print("no __NEXT_DATA__")
    links = sorted(set(re.findall(r'href="(/marken-modelle/[^"]+)"', text)))
    print("links", len(links))
    for l in links[:20]:
        print(l)
