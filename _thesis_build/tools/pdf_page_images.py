# -*- coding: utf-8 -*-
"""Determine, for each PDF page, whether its content stream draws an image XObject."""
import re
import zlib

raw = open(r'C:\codeDev\56\app\_thesis_build\qa\thesis.pdf', 'rb').read()

# index all indirect objects
objs = {}
for m in re.finditer(rb'(\d+)\s+0\s+obj(.*?)endobj', raw, re.S):
    objs[int(m.group(1))] = m.group(2)
print('objects:', len(objs))


def stream_of(num):
    body = objs.get(num)
    if body is None:
        return b''
    m = re.search(rb'stream\r?\n(.*?)\r?\nendstream', body, re.S)
    if not m:
        return b''
    data = m.group(1)
    if b'FlateDecode' in body:
        try:
            data = zlib.decompress(data)
        except Exception:
            try:
                data = zlib.decompressobj().decompress(data)
            except Exception:
                return b''
    return data


def refs(num_list_str):
    return [int(x) for x in re.findall(rb'(\d+)\s+0\s+R', num_list_str)]


page_nums = [n for n, b in objs.items() if re.search(rb'/Type\s*/Page[^s]', b)]
# order pages by their /Contents object number as a proxy for document order
page_nums.sort()
result = []
for i, pn in enumerate(page_nums, start=1):
    body = objs[pn]
    m = re.search(rb'/Contents\s+(\d+)\s+0\s+R', body)
    content = b''
    if m:
        content = stream_of(int(m.group(1)))
    if not content:
        m2 = re.search(rb'/Contents\s*\[(.*?)\]', body, re.S)
        if m2:
            for r in refs(m2.group(1)):
                content += stream_of(r)
    ndraw = len(re.findall(rb'/Im\w*\s+Do', content)) + len(re.findall(rb'/(?:Image|Img)\w*\s+Do', content))
    result.append((i, pn, ndraw, len(content)))

print('page  obj   imagedraws  contentlen')
for r in result:
    print('%4d %5d %6d %8d' % r)
print('pages with image draws:', [r[0] for r in result if r[2] > 0])
