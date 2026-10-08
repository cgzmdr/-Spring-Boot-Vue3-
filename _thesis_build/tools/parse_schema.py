# -*- coding: utf-8 -*-
"""Parse backend/schema.sql into a compact JSON description of tables."""
import re, json, io

src = open(r'C:\codeDev\56\app\backend\schema.sql', encoding='utf-8', errors='replace').read()

stmts = re.findall(r'CREATE TABLE[^;]+;', src, re.I | re.S)
print('DBG stmts', len(stmts), 'srclen', len(src))
out = []
for s in stmts:
    s = s.strip().rstrip(';').strip()
    m = re.match(r'CREATE TABLE(?:\s+IF NOT EXISTS)?\s+("?[\w\.]+"?)\s*\((.*)\)\s*$', s, re.I | re.S)
    if not m:
        out.append(dict(name='??', cols=[]))
        continue
    name = m.group(1).strip('"').split('.')[-1]
    body = m.group(2)
    cols = []
    for line in body.split('\n'):
        line = line.strip().rstrip(',')
        if not line:
            continue
        if re.match(r'(CONSTRAINT|PRIMARY KEY|FOREIGN KEY|UNIQUE|CHECK|KEY|INDEX)\b', line, re.I):
            continue
        cm = re.match(r'("?[\w]+"?)\s+([\w]+(?:\s*\([^)]*\))?)(.*)$', line, re.I)
        if not cm:
            continue
        cname = cm.group(1).strip('"')
        ctype = cm.group(2).strip()
        rest = cm.group(3)
        notnull = bool(re.search(r'NOT NULL', rest, re.I))
        pk = bool(re.search(r'PRIMARY KEY', rest, re.I))
        dm = re.search(r"DEFAULT\s+('[^']*'|[^\s,]+)", rest, re.I)
        default = dm.group(1).strip("'") if dm else ''
        cols.append(dict(name=cname, type=ctype, notnull=notnull, pk=pk, default=default))
    out.append(dict(name=name, cols=cols))

# column comments:  COMMENT ON COLUMN tbl.col IS '...';
colcmts = {}
for tbl, col, txt in re.findall(r"COMMENT ON COLUMN\s+([\w\.\"]+)\.([\w\"]+)\s+IS\s+'([^']*)'", src, re.I):
    colcmts[(tbl.strip('"'), col.strip('"'))] = txt
tabcmts = {}
for tbl, txt in re.findall(r"COMMENT ON TABLE\s+([\w\.\"]+)\s+IS\s+'([^']*)'", src, re.I):
    tabcmts[tbl.strip('"')] = txt

for o in out:
    o['comment'] = tabcmts.get(o['name'], '')
    for c in o['cols']:
        c['comment'] = colcmts.get((o['name'], c['name']), '')

with io.open(r'C:\codeDev\56\app\_schema.json', 'w', encoding='utf-8') as f:
    f.write(json.dumps(out, ensure_ascii=False, indent=1))

print('tables', len(out))
for o in out:
    print(o['name'], '|', o.get('comment', ''), '|', len(o['cols']), 'cols')
