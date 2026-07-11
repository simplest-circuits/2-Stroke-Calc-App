import re, urllib.request

def fetch(url):
    return urllib.request.urlopen(urllib.request.Request(url, headers={'User-Agent':'Mozilla/5.0'})).read().decode('utf-8','replace')

SH_PAGES = [
    'https://www.scooter-center.com/vespa-klassik/sh-50.html',
    'https://www.scooter-center.com/vespa-modern/sh-51.html',
    'https://www.scooter-center.com/moped-klassik/sh-52.html',
    'https://www.scooter-center.com/lambretta/sh-54.html',
    'https://www.scooter-center.com/scooter-850ccm/sh-55.html',
]

all_mfr = set()
all_fz = set()
for sh in SH_PAGES:
    html = fetch(sh)
    mfr = re.findall(r'https://www\.scooter-center\.com/([a-z0-9-]+)/c-\d+\.html', html)
    fz = re.findall(r'href="(https://www\.scooter-center\.com/[^"]+/fz-\d+\.html)"\s+title="([^"]+)"', html)
    print(sh, 'manufacturers', len(set(mfr)), 'fz', len(fz))
    all_mfr.update(mfr)
    all_fz.update((u,t) for u,t in fz)

print('total manufacturers', len(all_mfr))
print('total fz on sh pages', len(all_fz))

# sample manufacturer page counts
for m in ['aprilia','vespa-klassik','piaggio','peugeot','yamaha','puch','kymco']:
    for slug in [m]:
        url = f'https://www.scooter-center.com/{slug}/c-'
        # find c-id from aprilia known
        pass

html = fetch('https://www.scooter-center.com/aprilia/c-33.html')
fz = re.findall(r'href="(https://www\.scooter-center\.com/[^"]+/fz-\d+\.html)"\s+title="([^"]+)"', html)
print('aprilia fz', len(fz))

html2 = fetch('https://www.scooter-center.com/vespa-klassik/sh-50.html')
mfr_links = sorted(set(re.findall(r'(https://www\.scooter-center\.com/[a-z0-9-]+/c-\d+\.html)', html2)))
print('vespa klassik mfr links', len(mfr_links))
print('\n'.join(mfr_links[:15]))
