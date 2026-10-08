# -*- coding: utf-8 -*-
"""Profile rendered pages: count text lines, their vertical position and horizontal extent."""
import glob, os, sys
from PIL import Image

d = sys.argv[1] if len(sys.argv) > 1 else r'C:\codeDev\56\app\_thesis_build\qa\preview4'
for p in sorted(glob.glob(os.path.join(d, '*.png'))):
    im = Image.open(p).convert('L')
    w, h = im.size
    px = im.load()
    rowink = []
    for y in range(h):
        cnt = 0
        for x in range(0, w, 2):
            if px[x, y] < 200:
                cnt += 1
        rowink.append(cnt)
    # group consecutive ink rows into lines
    lines = []
    y = 0
    while y < h:
        if rowink[y] > 0:
            y0 = y
            while y < h and rowink[y] > 0:
                y += 1
            y1 = y - 1
            # horizontal extent of this line
            xs = [x for x in range(0, w, 2) if any(px[x, yy] < 200 for yy in range(y0, y1 + 1))]
            lines.append((y0, y1, xs[0] if xs else -1, xs[-1] if xs else -1))
        else:
            y += 1
    print('==', os.path.basename(p), 'lines', len(lines))
    for (a, b, x0, x1) in lines[:4]:
        print('   line y=%d-%d x=%d-%d' % (a, b, x0, x1))
    if lines:
        print('   ... last line y=%d-%d x=%d-%d' % lines[-1])
    if lines:
        print('   footer-band lines:', [l for l in lines if l[0] > 1030])
