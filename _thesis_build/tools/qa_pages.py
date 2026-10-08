# -*- coding: utf-8 -*-
import glob, os, re, zipfile, collections
from PIL import Image

for p in sorted(glob.glob(r'C:\codeDev\56\app\_thesis_build\qa\preview2\*.png')):
    im = Image.open(p).convert('L'); w, h = im.size
    rows = [y for y in range(0, h, 3) if any(im.getpixel((x, y)) < 200 for x in range(0, w, 6))]
    data = list(im.get_flattened_data()) if hasattr(im, 'get_flattened_data') else list(im.getdata())
    ink = sum(1 for v in data if v < 200) / len(data)
    print(os.path.basename(p), 'ink%%=%.2f' % (ink * 100), 'rows', (rows[0], rows[-1]) if rows else None, 'n', len(rows))

path = r'C:\codeDev\56\app\基于SpringBoot+Vue的56个民族文化数字化展示平台的设计与实现.docx'
z = zipfile.ZipFile(path)
x = z.read('word/document.xml').decode('utf-8')
s = z.read('word/styles.xml').decode('utf-8')
print('doc ascii:', collections.Counter(re.findall(r'w:ascii="([^"]+)"', x)))
print('doc eastAsia:', collections.Counter(re.findall(r'w:eastAsia="([^"]+)"', x)))
print('style ascii:', collections.Counter(re.findall(r'w:ascii="([^"]+)"', s)))
print('style eastAsia:', collections.Counter(re.findall(r'w:eastAsia="([^"]+)"', s)))
print('images:', len([n for n in z.namelist() if n.startswith('word/media/')]))
