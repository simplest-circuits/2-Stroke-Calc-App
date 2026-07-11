import re, urllib.request
html = urllib.request.urlopen(urllib.request.Request('https://www.scooter-center.com/de/', headers={'User-Agent':'Mozilla/5.0'})).read().decode('utf-8','replace')
# top-level brand pages: /brand/c-XX.html where brand page lists fz links
brands = sorted(set(re.findall(r'https://www\.scooter-center\.com/([a-z0-9-]+)/c-(\d+)\.html', html)))
print('brand pages', len(brands))
for b in brands[:40]:
    print(b)
# count fz links on homepage
fz = re.findall(r'/fz-(\d+)\.html', html)
print('fz on homepage', len(fz))
