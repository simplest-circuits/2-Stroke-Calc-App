#!/usr/bin/env python3
import json
import re
from pathlib import Path

html = Path(__file__).parent / "motorrad_tech.html"
text = html.read_text(encoding="utf-8")
m = re.search(r'<script id="__NEXT_DATA__"[^>]*>(.*?)</script>', text, re.S)
data = json.loads(m.group(1))
props = data["props"]["pageProps"]
out = Path(__file__).parent / "motorrad_tech_pageprops.json"
out.write_text(json.dumps(props, ensure_ascii=False, indent=2), encoding="utf-8")
print("wrote", out, "size", out.stat().st_size)

# explore pageData
pd = props.get("pageData", {}).get("data", {})
print("pageData.data keys:", list(pd.keys()) if isinstance(pd, dict) else type(pd))

def walk(obj, path="", depth=0, max_depth=4):
    if depth > max_depth:
        return
    if isinstance(obj, dict):
        for k, v in obj.items():
            p = f"{path}.{k}" if path else k
            if k.lower() in ("hubraum", "bohrung", "hub", "technical", "specs", "variants", "years", "models", "motorcycles"):
                print(f"  ** {p}: {type(v).__name__}", str(v)[:120] if not isinstance(v, (dict, list)) else f"len={len(v)}")
            elif depth < 3 and isinstance(v, (dict, list)):
                walk(v, p, depth + 1, max_depth)
    elif isinstance(obj, list) and obj and depth < 3:
        walk(obj[0], path + "[0]", depth + 1, max_depth)

walk(pd)
