import re, time, urllib.request, urllib.error

def fetch(url):
    time.sleep(0.5)
    req = urllib.request.Request(url, headers={'User-Agent':'Mozilla/5.0'})
    return urllib.request.urlopen(req, timeout=60).read().decode('utf-8','replace')

url = 'https://www.scooter-center.com/aprilia/c-33.html'
try:
    html = fetch(url)
    print('len', len(html))
    p1 = len(re.findall(r'/fz-\d+\.html', html))
    p2 = len(re.findall(r'href="(https://www\.scooter-center\.com/[^"]+/fz-\d+\.html)"\s+title="([^"]+)"', html))
    print('fz any', p1, 'fz titled', p2)
    open('sample_aprilia_live.html','w',encoding='utf-8').write(html)
except Exception as e:
    print('error', e)
