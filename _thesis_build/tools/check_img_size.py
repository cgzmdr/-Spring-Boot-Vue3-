# -*- coding: utf-8 -*-
"""Check the display size of every inline image stored in the DOCX."""
import collections
import re
import zipfile

z = zipfile.ZipFile(r'C:\codeDev\56\app\基于SpringBoot+Vue的56个民族文化数字化展示平台的设计与实现.docx')
x = z.read('word/document.xml').decode('utf-8')
ext = re.findall(r'<wp:extent cx="(\d+)" cy="(\d+)"', x)
print('images with extent:', len(ext))
cm = lambda v: round(int(v) / 360000, 2)
sizes = [(cm(a), cm(b)) for a, b in ext]
print(collections.Counter(sizes).most_common())
rels = z.read('word/_rels/document.xml.rels').decode('utf-8')
media = re.findall(r'media/([^"]+)', rels)
print('media referenced:', len(media))
print(media[:5], '...', media[-3:])
names = [n for n in z.namelist() if n.startswith('word/media/')]
print('media files present:', len(names))
print('docx bytes:', z.fp.seek(0, 2) if hasattr(z, 'fp') else '')
