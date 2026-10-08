from PIL import Image
src=r'C:\codeDev\56\app\_thesis_build\qa\all2'
for p in [3,4,18,74,75,76,77,84]:
    im=Image.open(src+r'\page-%04d.png'%p).convert('L'); w,h=im.size; px=im.load()
    rows=[sum(1 for x in range(0,w,2) if px[x,y]<200) for y in range(h)]
    lines=[];y=0
    while y<h:
        if rows[y]>0:
            y0=y
            while y<h and rows[y]>0: y+=1
            y1=y-1
            xs=[x for x in range(0,w,2) if any(px[x,yy]<200 for yy in range(y0,y1+1))]
            lines.append((y0,y1,xs[0],xs[-1],sum(rows[y0:y1+1])))
        else: y+=1
    n240=sum(1 for yy in range(120,int(h*0.9)) for xx in range(0,w,3) if px[xx,yy]<245)
    print('p%-3d lines=%2d light-ink=%.1f%%'%(p,len(lines),100*n240/((int(h*0.9)-120)*(w//3))))
    for l in lines[:8]: print('     y=%d-%d x=%d-%d ink=%d'%l)
