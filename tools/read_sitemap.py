import re, urllib.request
xml = urllib.request.urlopen(urllib.request.Request('https://www.scooter-center.com/sitemap.xml', headers={'User-Agent':'Mozilla/5.0'})).read().decode()
print(xml[:2000])
# find sub-sitemaps
subs = re.findall(r'<loc>([^<]+)</loc>', xml)
print('locs', len(subs))
for s in subs[:20]:
    print(s)
