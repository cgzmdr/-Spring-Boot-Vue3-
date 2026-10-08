# -*- coding: utf-8 -*-
"""Minimal PDF text extractor (Flate streams + ToUnicode CMaps) for layout QA."""
import re
import sys
import zlib


def load(path):
    raw = open(path, 'rb').read()
    objs = {}
    for m in re.finditer(rb'(\d+)\s+0\s+obj(.*?)endobj', raw, re.S):
        objs[int(m.group(1))] = m.group(2)
    return raw, objs


def stream_bytes(body):
    m = re.search(rb'stream\r?\n(.*?)\r?\nendstream', body, re.S)
    if not m:
        return None
    data = m.group(1)
    if b'FlateDecode' in body:
        try:
            data = zlib.decompress(data)
        except Exception:
            try:
                data = zlib.decompressobj().decompress(data)
            except Exception:
                return None
    return data


def parse_cmap(txt):
    table = {}
    for blk in re.findall(rb'beginbfchar(.*?)endbfchar', txt, re.S):
        for src, dst in re.findall(rb'<([0-9A-Fa-f]+)>\s*<([0-9A-Fa-f]+)>', blk):
            table[int(src, 16)] = bytes.fromhex(dst.decode()).decode('utf-16-be', 'ignore')
    for blk in re.findall(rb'beginbfrange(.*?)endbfrange', txt, re.S):
        for lo, hi, dst in re.findall(rb'<([0-9A-Fa-f]+)>\s*<([0-9A-Fa-f]+)>\s*<([0-9A-Fa-f]+)>', blk):
            lo_i, hi_i, base = int(lo, 16), int(hi, 16), int(dst, 16)
            for k in range(lo_i, min(hi_i, lo_i + 65535) + 1):
                try:
                    table[k] = chr(base + (k - lo_i))
                except Exception:
                    pass
    return table


def font_map(objs, num):
    body = objs.get(num, b'')
    cmap, two = {}, False
    m = re.search(rb'/ToUnicode\s+(\d+)\s+0\s+R', body)
    if m:
        data = stream_bytes(objs.get(int(m.group(1)), b''))
        if data:
            cmap = parse_cmap(data)
    if re.search(rb'/Encoding\s*/Identity-H', body):
        two = True
    if not cmap:
        two = True
    return cmap, two


def decode_hex(hexs, cmap, two):
    try:
        b = bytes.fromhex(hexs.decode())
    except Exception:
        return ''
    out = []
    if two:
        for i in range(0, len(b) - 1, 2):
            out.append(cmap.get((b[i] << 8) | b[i + 1], ''))
    else:
        for ch in b:
            out.append(cmap.get(ch, ''))
    return ''.join(out)


def page_texts(pdf):
    raw, objs = load(pdf)
    kids, root = None, None
    for n, b in objs.items():
        if b'/Type/Pages' in b and b'/Kids' in b:
            kids = [int(x) for x in re.findall(rb'(\d+)\s+0\s+R', re.search(rb'/Kids\s*\[(.*?)\]', b, re.S).group(1))]
            root = n
            break
    # shared font dictionary (may sit in the /Pages resources object)
    fonts = {}
    res_body = objs[root]
    rref = re.search(rb'/Resources\s+(\d+)\s+0\s+R', res_body)
    if rref:
        res_body = objs.get(int(rref.group(1)), b'')
    rm = re.search(rb'/Font\s+(\d+)\s+0\s+R', res_body)
    if rm:
        fd = objs.get(int(rm.group(1)), b'')
        for name, num in re.findall(rb'/(F\d+)\s+(\d+)\s+0\s+R', fd):
            fonts[name.decode()] = font_map(objs, int(num))
    pages = []
    for pn in kids:
        body = objs[pn]
        contents = b''
        m = re.search(rb'/Contents\s+(\d+)\s+0\s+R', body)
        if m:
            contents = stream_bytes(objs.get(int(m.group(1)), b'')) or b''
        else:
            m2 = re.search(rb'/Contents\s*\[(.*?)\]', body, re.S)
            if m2:
                for r in re.findall(rb'(\d+)\s+0\s+R', m2.group(1)):
                    contents += (stream_bytes(objs.get(int(r), b'')) or b'')
        text = []
        cur = None
        pat = re.compile(rb'/(F\d+)\s+[\d\.]+\s+Tf|<([0-9A-Fa-f]*)>\s*Tj|\[([^\]]*)\]\s*TJ', re.S)
        for m in pat.finditer(contents):
            if m.group(1):
                cur = m.group(1).decode()
            elif m.group(2) is not None:
                cmap, two = fonts.get(cur, ({}, False))
                text.append(decode_hex(m.group(2), cmap, two))
            else:
                cmap, two = fonts.get(cur, ({}, False))
                for hx in re.findall(rb'<([0-9A-Fa-f]*)>', m.group(3)):
                    text.append(decode_hex(hx, cmap, two))
        pages.append(''.join(text))
    return pages


if __name__ == '__main__':
    pages = page_texts(sys.argv[1])
    lo = int(sys.argv[2]) if len(sys.argv) > 2 else 1
    hi = int(sys.argv[3]) if len(sys.argv) > 3 else len(pages)
    print('pages:', len(pages))
    for i in range(lo, hi + 1):
        t = re.sub(r'\s+', ' ', pages[i - 1]).strip()
        print('--- p%d [%d]: %s' % (i, len(t), t[:200]))
