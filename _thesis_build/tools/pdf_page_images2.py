# -*- coding: utf-8 -*-
"""Map PDF pages (in file order) to the images/graphics their content streams draw."""
import re
import zlib

raw = open(r'C:\codeDev\56\app\_thesis_build\qa\thesis.pdf', 'rb').read()


def decompress(body, data):
    if b'FlateDecode' in body:
        try:
            return zlib.decompress(data)
        except Exception:
            return b''
    return data


# Walk the file in order, tracking the current object number.
pages = []
for m in re.finditer(rb'(\d+)\s+0\s+obj(.*?)(?=\d+\s+0\s+obj|$)', raw, re.S):
    num = int(m.group(1))
    body = m.group(2)
    if re.search(rb'/Type\s*/Page[^s]', body):
        pages.append((num, body))

print('pages found (file order):', len(pages))
streams = {}
for m in re.finditer(rb'(\d+)\s+0\s+obj(.*?)endobj', raw, re.S):
    num = int(m.group(1))
    body = m.group(2)
    sm = re.search(rb'stream\r?\n(.*?)\r?\nendstream', body, re.S)
    if sm:
        streams[num] = decompress(body, sm.group(1))

out = []
for i, (num, body) in enumerate(pages, start=1):
    content = b''
    m = re.search(rb'/Contents\s+(\d+)\s+0\s+R', body)
    if m:
        content = streams.get(int(m.group(1)), b'')
    if not content:
        m2 = re.search(rb'/Contents\s*\[(.*?)\]', body, re.S)
        if m2:
            for r in re.findall(rb'(\d+)\s+0\s+R', m2.group(1)):
                content += streams.get(int(r), b'')
    imgs = len(re.findall(rb'/Im\w*\s+Do', content))
    out.append((i, num, imgs, len(content)))

for r in out:
    print('page %3d obj %4d images %2d content %6d' % r)
print()
print('pages with images:', [r[0] for r in out if r[2] > 0])
print('pages with tiny content (<400B):', [(r[0], r[3]) for r in out if r[3] < 400])
print('total image draws:', sum(r[2] for r in out))
