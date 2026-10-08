import re
raw=open(r'C:\codeDev\56\app\_thesis_build\qa\thesis.pdf','rb').read()
objs={}
for m in re.finditer(rb'(\d+)\s+0\s+obj(.*?)endobj', raw, re.S):
    objs[int(m.group(1))]=m.group(2)
for num,b in objs.items():
    if b'/Type/Pages' in b or b'/Type /Pages' in b:
        cnt=re.search(rb'/Count\s+(\d+)',b)
        kids=re.search(rb'/Kids\s*\[(.*?)\]',b,re.S)
        n=len(re.findall(rb'\d+\s+0\s+R',kids.group(1))) if kids else 0
        print('Pages obj',num,'count',cnt.group(1).decode() if cnt else '?','kids',n)
print('---- check one kid type')
m=re.search(rb'/Kids\s*\[(.*?)\]',objs[[n for n,b in objs.items() if b'/Type/Pages' in b][0]],re.S)
k=[int(x) for x in re.findall(rb'(\d+)\s+0\s+R',m.group(1))][:5]
for kk in k:
    print(kk, objs[kk][:120])
