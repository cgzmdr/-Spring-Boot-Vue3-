# -*- coding: utf-8 -*-
"""Resolve the true page order from /Pages /Kids, then report image draws per page."""
import re
import zlib

raw = open(r'C:\codeDev\56\app\_thesis_build\qa\thesis.pdf', 'rb').read()

objs = {}
for m in re.finditer(rb'(\d+)\s+0\s+obj(.*?)endobj', raw, re.S):
    objs[int(m.group(1))] = m.group(2)


def stream(num):
    body = objs.get(num, b'')
    m = re.search(rb'stream\r?\n(.*?)\r?\nendstream', body, re.S)
    if not m:
        return b''
    data = m.group(1)
    if b'FlateDecode' in body:
        try:
            data = zlib.decompress(data)
        except Exception:
            return b''
    return data


# find the root pages node: an object with /Type /Pages and /Kids [...]
kids = None
for num, b in objs.items():
    if re.search(rb'/Type\s*/Pages', b) and b'/Kids' in b:
        arr = re.search(rb'/Kids\s*\[(.*?)\]', b, re.S).group(1)
        kids = [int(x) for x in re.findall(rb'(\d+)\s+0\s+R', arr)]
        print('pages node obj', num, 'kids', len(kids))
        break

order = kids if kids else sorted(n for n, b in objs.items() if re.search(rb'/Type\s*/Page[^s]', b))
print('page order resolved:', len(order))

rows = []
for i, pn in enumerate(order, start=1):
    body = objs[pn]
    content = b''
    m = re.search(rb'/Contents\s+(\d+)\s+0\s+R', body)
    if m:
        content = stream(int(m.group(1)))
    if not content:
        m2 = re.search(rb'/Contents\s*\[(.*?)\]', body, re.S)
        if m2:
            for r in re.findall(rb'(\d+)\s+0\s+R', m2.group(1)):
                content += stream(int(r))
    imgs = re.findall(rb'/(\w*Im\w*)\s+Do', content)
    rows.append((i, pn, len(imgs), len(content)))

for r in rows:
    print('page %3d obj %4d images %2d content %6d' % r)
print('total draws', sum(r[2] for r in rows))
print('image pages', [(r[0], r[2]) for r in rows if r[2] > 0])
print('tiny pages', [(r[0], r[3]) for r in rows if r[3] < 500])
