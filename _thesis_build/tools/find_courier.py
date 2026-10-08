# -*- coding: utf-8 -*-
"""检查内置模板样式里的字体，定位 Courier 来源。"""
import re, zipfile
p = r'C:\codeDev\56\app\基于SpringBoot+Vue的56个民族文化数字化展示平台的设计与实现.docx'
s = zipfile.ZipFile(p).read('word/styles.xml').decode('utf-8')
for m in re.finditer(r'<w:style [^>]*w:styleId="([^"]+)"[^>]*>.*?</w:style>', s, re.S):
    if 'Courier' in m.group(0):
        name = re.search(r'<w:name w:val="([^"]+)"', m.group(0))
        print('styleId', m.group(1), 'name', name.group(1) if name else '?')
        print(m.group(0)[:400])
