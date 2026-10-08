# -*- coding: utf-8 -*-
"""List PDF pages and whether each references an image XObject."""
import re

d = open(r'C:\codeDev\56\app\_thesis_build\qa\thesis.pdf', 'rb').read()
idx = [m.start() for m in re.finditer(rb'/Type/Page(?!s)', d)]
print('page objects:', len(idx))
pages = []
for n, i in enumerate(idx):
    end = idx[n + 1] if n + 1 < len(idx) else len(d)
    chunk = d[i:end]
    pages.append(bool(re.search(rb'/XObject', chunk)))
img_pages = [i + 1 for i, v in enumerate(pages) if v]
print('pages referencing XObject:', len(img_pages))
print(img_pages)
