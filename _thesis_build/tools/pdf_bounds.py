import re, zlib
raw=open(r'C:\codeDev\56\app\_thesis_build\qa\v4.pdf','rb').read()
objs={}
for m in re.finditer(rb'(\d+)\s+0\s+obj(.*?)endobj', raw, re.S): objs[int(m.group(1))]=m.group(2)
def stream(num):
    b=objs.get(num,b''); m=re.search(rb'stream\r?\n(.*?)\r?\nendstream', b, re.S)
    if not m: return b''
    d=m.group(1)
    if b'FlateDecode' in b:
        try: d=zlib.decompress(d)
        except Exception: return b''
    return d
kid=None
for n,b in objs.items():
    if b'/Type/Pages' in b and b'/Kids' in b:
        kid=[int(x) for x in re.findall(rb'(\d+)\s+0\s+R', re.search(rb'/Kids\s*\[(.*?)\]',b,re.S).group(1))]; break
print('pages', len(kid))
bad=[]
for i,pn in enumerate(kid,1):
    m=re.search(rb'/Contents\s+(\d+)\s+0\s+R', objs[pn])
    c=stream(int(m.group(1))) if m else b''
    nimg=len(re.findall(rb'/(Im\d+)\s+Do', c))
    ytop=[]
    for mm in re.finditer(rb'([\d\.\- ]+) cm\s*/(Im\d+)\s+Do', c):
        parts=mm.group(1).split()
        if len(parts)==6:
            try: y=float(parts[5]); h=float(parts[3])
            except: continue
            ytop.append(round(y+h,1))
    over=[t for t in ytop if t>842.5]
    if over or (len(c)<600 and i>4): bad.append((i,nimg,len(c),over[:3]))
print('suspect pages:', bad)
