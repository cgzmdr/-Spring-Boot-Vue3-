# -*- coding: utf-8 -*-
"""列出尚未带口语标记的长句，便于最后一轮定点润色。"""
import importlib.util
import os
import re

TOOLS = r'C:\codeDev\56\app\_thesis_build\tools'
spec = importlib.util.spec_from_file_location('sc', os.path.join(TOOLS, 'style_check.py'))
sc = importlib.util.module_from_spec(spec)
spec.loader.exec_module(sc)
SPOKEN = sc.SPOKEN

for name in ['content_ch1_3', 'content_ch3', 'content_ch4', 'content_ch5', 'content_ch6']:
    spec = importlib.util.spec_from_file_location(name, os.path.join(TOOLS, name + '.py'))
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    print('=====', name)
    for b in mod.BLOCKS:
        if b[0] not in ('p', 'p_ind'):
            continue
        txt = b[1]
        if not re.search(r'[\u4e00-\u9fa5]', txt):
            continue
        if any(k in txt for k in SPOKEN):
            continue
        if len(txt) < 60:
            continue
        print('  -', txt[:96])
