# -*- coding: utf-8 -*-
"""Inspect how images are placed on a page: transformation matrices and XObject sizes."""
import re
import zlib

raw = open(r'C:\codeDev\56\app\_thesis_build\qa\thesis.pdf', 'rb').read()
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


def page_content(page_obj):
    body = objs[page_obj]
    m = re.search(rb'/Contents\s+(\d+)\s+0\s+R', body)
    return stream(int(m.group(1))), body


for page_no, obj in [(47, 177), (45, 162), (44, 155), (48, 185)]:
    c, body = page_content(obj)
    print('==== page', page_no, 'obj', obj, 'contentlen', len(c))
    # page media box
    print('   mediabox:', re.search(rb'/MediaBox\s*\[([^\]]*)\]', body or b''))
    for m in re.finditer(rb'([\d\.\-]+ [\d\.\-]+ [\d\.\-]+ [\d\.\-]+ ([\d\.\-]+) ([\d\.\-]+)) cm\s*/(\w+)\s+Do', c):
        print('   draw', m.group(4).decode(), 'cm=', m.group(1).decode())
    # also print all cm+Do pairs loosely
    draws = re.findall(rb'cm\s*/(\w+)\s+Do', c)
    print('   loose draws:', [d.decode() for d in draws][:10])
    # resource dict of the page
    res = re.search(rb'/Resources\s*<<(.*?)>>\s*/Type|/Resources\s*<<(.*?)>>', body, re.S)
    xo = re.findall(rb'/(Im\d+)\s+(\d+)\s+0\s+R', body)
    print('   page xobject refs:', xo[:10])
    for name, onum in xo[:6]:
        ob = objs.get(int(onum), b'')
        wh = re.search(rb'/Width\s+(\d+).*?/Height\s+(\d+)', ob, re.S)
        flt = re.search(rb'/Filter\s*/(\w+)', ob)
        print('      ', name.decode(), 'w/h', wh.groups() if wh else None, 'filter', flt.group(1).decode() if flt else None)
