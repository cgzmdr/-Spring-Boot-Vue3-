# -*- coding: utf-8 -*-
"""Show the placement matrices of images on specific pages of the current PDF."""
import re
import zlib

raw = open(r'C:\codeDev\56\app\_thesis_build\qa\v3.pdf', 'rb').read()
objs = {}
for m in re.finditer(rb'(\d+)\s+0\s+obj(.*?)endobj', raw, re.S):
    objs[int(m.group(1))] = m.group(2)


def stream(num):
    b = objs.get(num, b'')
    m = re.search(rb'stream\r?\n(.*?)\r?\nendstream', b, re.S)
    if not m:
        return b''
    d = m.group(1)
    if b'FlateDecode' in b:
        try:
            d = zlib.decompress(d)
        except Exception:
            return b''
    return d


kids = None
for n, b in objs.items():
    if b'/Type/Pages' in b and b'/Kids' in b:
        kids = [int(x) for x in re.findall(rb'(\d+)\s+0\s+R', re.search(rb'/Kids\s*\[(.*?)\]', b, re.S).group(1))]
        break

for pno in [44, 47, 39]:
    page = objs[kids[pno - 1]]
    c = stream(int(re.search(rb'/Contents\s+(\d+)\s+0\s+R', page).group(1)))
    print('===== page', pno, 'content', len(c))
    # find each image draw with the preceding cm
    idx = 0
    for m in re.finditer(rb'/(Im\d+)\s+Do', c):
        start = max(0, m.start() - 300)
        ctx = c[start:m.end()].decode('latin-1')
        cms = re.findall(r'([\d\.\-]+ [\d\.\-]+ [\d\.\-]+ [\d\.\-]+ [\d\.\-]+ [\d\.\-]+) cm', ctx)
        print('  draw', m.group(1).decode(), 'last cm:', cms[-1] if cms else 'NONE')
    # text operators count
    print('  Tj/TJ count:', len(re.findall(rb'\]\s*TJ', c)) + len(re.findall(rb'\(.*?\)\s*Tj', c)))
