import re
html = open(r'C:\Users\thannedo\Documents\GitHub\2StrokeCalc App\tools\sample_fahrzeuge.html', encoding='utf-8').read()
m = re.search(r'<div class="kategorien">(.*?)</div>', html, re.S)
block = m.group(1)
links = re.findall(r'href="(https://www\.scooter-center\.com/[a-z0-9-]+/c-\d+\.html)"\s+title="([^"]+)"', block)
print('sidebar brands', len(links))
for u,t in links:
    print(f'  "{u}",  # {t}')
