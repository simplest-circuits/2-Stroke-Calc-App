import re, urllib.request
html = urllib.request.urlopen(urllib.request.Request('https://www.scooter-center.com/unsere-fahrzeuge/bm-fzg.html', headers={'User-Agent':'Mozilla/5.0'})).read().decode('utf-8','replace')
m = re.search(r'<div class="kategorien">(.*?)</div>', html, re.S)
block = m.group(1) if m else html
links = re.findall(r'href="(https://www\.scooter-center\.com/[a-z0-9-]+/c-\d+\.html)"\s+title="([^"]+)"', block)
print('sidebar brands', len(links))
for u,t in links[:10]:
    print(t, u)

# count fz per first 5 brands
for u,t in links[:8]:
    h = urllib.request.urlopen(urllib.request.Request(u, headers={'User-Agent':'Mozilla/5.0'})).read().decode('utf-8','replace')
    fz = re.findall(r'/fz-\d+\.html', h)
    print(t, len(fz))
