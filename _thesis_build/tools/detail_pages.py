import sys
from PIL import Image
src=r'C:\codeDev\56\app\_thesis_build\qa\all'
for p in [45,46,47,48,52,53,54,55,56,57,63]:
    im=Image.open(src+r'\page-%04d.png'%p).convert('L'); w,h=im.size; px=im.load()
    rows=[]
    for y in range(h):
        c=sum(1 for x in range(0,w,2) if px[x,y]<200)
        rows.append(c)
    lines=[];y=0
    while y<h:
        if rows[y]>0:
            y0=y
            while y<h and rows[y]>0: y+=1
            y1=y-1
            xs=[x for x in range(0,w,2) if any(px[x,yy]<200 for yy in range(y0,y1+1))]
            lines.append((y0,y1,xs[0],xs[-1]))
        else: y+=1
    print('p%d lines=%d'%(p,len(lines)), [(a,b,x0,x1) for a,b,x0,x1 in lines[:6]])
