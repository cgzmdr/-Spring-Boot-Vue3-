import re, zlib
raw=open(r'C:\codeDev\56\app\_thesis_build\qa\thesis.pdf','rb').read()
objs={}
for m in re.finditer(rb'(\d+)\s+0\s+obj(.*?)endobj', raw, re.S):
    objs[int(m.group(1))]=m.group(2)
def stream(num):
    b=objs.get(num,b''); m=re.search(rb'stream\r?\n(.*?)\r?\nendstream', b, re.S)
    if not m: return b''
    d=m.group(1)
    if b'FlateDecode' in b:
        try: d=zlib.decompress(d)
        except Exception: return b''
    return d
kids=None
for num,b in objs.items():
    if re.search(rb'/Type\s*/Pages',b) and b'/Kids' in b:
        arr=re.search(rb'/Kids\s*\[(.*?)\]', b, re.S).group(1)
        kids=[int(x) for x in re.findall(rb'(\d+)\s+0\s+R', arr)]; break
seen={}
for i,pn in enumerate(kids,1):
    body=objs[pn]
    m=re.search(rb'/Contents\s+(\d+)\s+0\s+R', body); cid=int(m.group(1)) if m else None
    seen.setdefault(cid,[]).append(i)
    if 38<=i<=50:
        c=stream(cid) if cid else b''
        names=sorted(set(re.findall(rb'/(\w+)\s+Do', c)))
        print('page',i,'contentobj',cid,'len',len(c),'Do-names',names[:8])
print()
dup={k:v for k,v in seen.items() if len(v)>1}
print('shared content objects:', dup)
