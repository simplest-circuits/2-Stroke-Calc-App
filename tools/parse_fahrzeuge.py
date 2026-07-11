import re
html = open(r'C:\Users\thannedo\Documents\GitHub\2StrokeCalc App\tools\sample_fahrzeuge.html', encoding='utf-8').read()
fz = re.findall(r'href="(https://www\.scooter-center\.com/[^"]+/fz-\d+\.html)"\s+title="([^"]+)"', html)
mfr = re.findall(r'href="(https://www\.scooter-center\.com/[a-z0-9-]+/c-\d+\.html)"\s+title="([^"]+Fahrzeuge[^"]*)"', html)
mfr2 = re.findall(r'href="(https://www\.scooter-center\.com/[a-z0-9-]+/c-\d+\.html)"[^>]*title="([^"]+)"', html)
print('fz', len(fz))
print('mfr fahrzeuge', len(mfr))
# titles with Fahrzeuge
veh = [t for u,t in mfr2 if 'Fahrzeuge' in t or 'fahrzeuge' in t]
print('vehicle brand pages', len(veh))
for t in veh[:30]:
    print(t)
# also sh links
sh = re.findall(r'href="(https://www\.scooter-center\.com/[^"]+/sh-\d+\.html)"', html)
print('sh links', len(set(sh)))
