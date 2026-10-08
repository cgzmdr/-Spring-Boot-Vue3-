# -*- coding: utf-8 -*-
"""两遍构建：先占位生成目录，再从渲染结果中读回各标题页码，重建为带页码的静态目录。"""
import importlib.util
import io
import json
import os
import re
import subprocess
import sys

ROOT = r'C:\codeDev\56\app'
TOOLS = os.path.join(ROOT, '_thesis_build', 'tools')
NODE = r'C:\Users\Administrator\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\node\bin\node.exe'
CLI = r'C:\Users\Administrator\AppData\Local\Programs\DeepSeek Harness\resources\app.asar.unpacked\dsh\node_modules\@deepseek-ai\libreoffice-kit\lib\cli.js'
DOCX = os.environ.get(
    'THESIS_OUT',
    os.path.join(ROOT, '基于SpringBoot+Vue的56个民族文化数字化展示平台的设计与实现.docx'))
PM = os.path.join(TOOLS, 'toc_pages.json')


def load_mod(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def run(cmd):
    p = subprocess.run(cmd, capture_output=True, text=True, encoding='utf-8', errors='replace')
    if p.returncode != 0:
        print('CMD FAILED', cmd, p.stdout[-500:], p.stderr[-500:])
    return p


def build():
    mod = load_mod('build_docx', os.path.join(TOOLS, 'build_docx.py'))
    mod.main()


def to_pdf(tag):
    qa = os.path.join(ROOT, '_thesis_build', 'qa')
    os.makedirs(qa, exist_ok=True)
    out = os.path.join(qa, tag + '.pdf')
    if os.path.exists(out):
        os.remove(out)
    run([NODE, CLI, 'convert', '--input', DOCX, '--output', out])
    return out


def page_texts(pdf):
    mod = load_mod('pdftext', os.path.join(TOOLS, 'pdftext.py'))
    return mod.page_texts(pdf)


def norm(s):
    return re.sub(r'\s+', '', s)


def main():
    headings = None
    for attempt in range(3):
        print('=== build pass', attempt + 1)
        build()
        pdf = to_pdf('pass%d' % (attempt + 1))
        pages = page_texts(pdf)
        npages = len(pages)
        npages_norm = [norm(p) for p in pages]
        if headings is None:
            mod = load_mod('build_docx_h', os.path.join(TOOLS, 'build_docx.py'))
            blocks = mod.load_blocks()
            headings = mod.collect_headings(blocks)
            print('headings:', len(headings))
        # 定位正文起始页：第 1 章正文首句
        body_start = None
        marker = norm('56 个民族共同开拓了我国的疆域')
        for i, t in enumerate(npages_norm, start=1):
            if marker in t:
                body_start = i
                break
        if body_start is None:
            print('!! cannot locate body start')
            return
        offset = body_start - 1
        print('body starts at physical page', body_start, 'offset', offset)
        page_map = {}
        cursor = body_start
        missing = []
        for level, text in headings:
            key = norm(text)
            found = None
            for i in range(cursor, npages + 1):
                if key and key in npages_norm[i - 1]:
                    found = i
                    break
            if found is None:
                missing.append(text)
                found = cursor
            page_map[text] = found - offset
            cursor = found
        print('missing:', missing)
        old = {}
        if os.path.exists(PM):
            old = json.load(open(PM, encoding='utf-8'))
        json.dump(page_map, open(PM, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
        if old == page_map:
            print('page map stable at attempt', attempt + 1)
            break
    print('pages total', npages)
    print('page map sample:', list(page_map.items())[:6], '...', list(page_map.items())[-4:])


if __name__ == '__main__':
    main()
