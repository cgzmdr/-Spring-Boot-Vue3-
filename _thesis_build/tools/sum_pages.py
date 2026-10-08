# -*- coding: utf-8 -*-
"""Summarise every rendered page: ink ratio, content band, footer band presence."""
import glob, os, sys
from PIL import Image

d = sys.argv[1]
scale = float(sys.argv[2]) if len(sys.argv) > 2 else 1.0
files = sorted(glob.glob(os.path.join(d, '*.png')))
flagged = []
for p in files:
    im = Image.open(p).convert('L')
    w, h = im.size
    px = im.load()
    total = w * h
    ink = 0
    rowink = [0] * h
    for y in range(h):
        c = 0
        for x in range(0, w, 2):
            if px[x, y] < 200:
                c += 1
        rowink[y] = c
        ink += c * 2
    ratio = ink / total
    footer_lo = int(h * 0.94)
    footer = sum(1 for y in range(footer_lo, h) if rowink[y] > 0) > 0
    body_rows = [y for y in range(0, int(h * 0.94)) if rowink[y] > 0]
    span = (body_rows[0], body_rows[-1]) if body_rows else None
    page = int(os.path.basename(p).split('-')[1].split('.')[0])
    if ratio < 0.004 or (span and span[0] > h * 0.45):
        flagged.append((page, round(ratio * 100, 2), span, footer))
    print('p%-3d ink=%5.2f%% body=%s footer=%s' % (page, ratio * 100, span, 'Y' if footer else '-'))
print('\nFLAGGED (near-empty or large top gap):', flagged)
