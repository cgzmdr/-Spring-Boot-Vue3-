# -*- coding: utf-8 -*-
"""Sanity-check generated PNGs: size, colour variety, ink coverage."""
import glob, os, json
from PIL import Image, ImageStat

rows = []
for p in sorted(glob.glob(r'C:\codeDev\56\app\_thesis_build\figures\*.png') + glob.glob(r'C:\codeDev\56\app\_thesis_build\screenshots\*.png')):
    if os.path.basename(p).startswith('t-') or os.path.basename(p).startswith('test-') or os.path.basename(p).startswith('dbg-'):
        continue
    im = Image.open(p).convert('RGB')
    w, h = im.size
    small = im.resize((min(w, 320), min(h, 320)))
    cols = small.getcolors(maxcolors=1000000)
    stat = ImageStat.Stat(im)
    # ink = fraction of pixels darker than 200 in any channel
    g = im.convert('L').resize((200, 200))
    px = list(g.getdata())
    ink = sum(1 for v in px if v < 200) / len(px)
    white = sum(1 for v in px if v > 250) / len(px)
    rows.append(dict(f=os.path.relpath(p, r'C:\codeDev\56\app'), w=w, h=h,
                     colors=len(cols), mean=[round(x, 1) for x in stat.mean],
                     inkPct=round(ink * 100, 1), whitePct=round(white * 100, 1)))

bad = [r for r in rows if r['inkPct'] < 1.0 or r['colors'] < 12 or r['whitePct'] > 99.5]
print(json.dumps(rows, ensure_ascii=False, indent=0))
print('\nSUSPECT (blank/uniform):', json.dumps(bad, ensure_ascii=False))
print('total', len(rows))
