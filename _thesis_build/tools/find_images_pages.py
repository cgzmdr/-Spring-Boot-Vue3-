import glob, os
from PIL import Image
src=r'C:\codeDev\56\app\_thesis_build\qa\all'
best=[]
for p in sorted(glob.glob(src+r'\*.png')):
    im=Image.open(p).convert('L'); w,h=im.size; px=im.load()
    run=0; mx=0; at=0
    for y in range(h):
        c=sum(1 for x in range(0,w,4) if px[x,y]<220)
        if c> w/4/3:
            run+=1
            if run>mx: mx=run; at=y-run
        else:
            run=0
    best.append((mx, os.path.basename(p), at))
best.sort(reverse=True)
print('tallest solid ink runs (likely images):')
for b in best[:12]: print(b)
