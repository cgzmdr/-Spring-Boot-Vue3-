import re, zlib
raw=open(r'C:\codeDev\56\app\_thesis_build\qa\v4.pdf','rb').read()
objs={}
for m in re.finditer(rb'(\d+)\s+0\s+obj(.*?)endobj', raw, re.S): objs[int(m.group(1))]=m.group(2)
def sb(body):
    m=re.search(rb'stream\r?\n(.*?)\r?\nendstream', body, re.S)
    if not m: return None
    d=m.group(1)
    if b'FlateDecode' in body:
        try: d=zlib.decompress(d)
        except Exception: return None
    return d
kids=None
for n,b in objs.items():
    if b'/Type/Pages' in b and b'/Kids' in b:
        kids=[int(x) for x in re.findall(rb'(\d+)\s+0\s+R', re.search(rb'/Kids\s*\[(.*?)\]',b,re.S).group(1))]
        print('pages obj',n); print(b[:400]); break
p1=objs[kids[0]]
print('PAGE1 OBJ', kids[0], p1[:300])
m=re.search(rb'/Contents\s+(\d+)\s+0\s+R', p1)
c=sb(objs[int(m.group(1))])
print('content len', len(c) if c else None)
print(c[:600] if c else '')
# resources
rm=re.search(rb'/Resources\s+(\d+)\s+0\s+R', p1)
if rm:
    rb=objs[int(rm.group(1))]
    print('RESOURCES', rb[:600])
