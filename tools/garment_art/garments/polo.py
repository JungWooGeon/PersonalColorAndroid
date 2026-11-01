from kit.art_kit import *

def polo(color):
    """Cropped collared knit: slim rib body, wide flat collar with tipping, pearl button placket, short rib sleeves."""
    W, H = 360, 320
    left = [(180, 40, 'c'), (150, 36, 'c'), (106, 50, 'c'), (86, 62), (70, 90), (52, 126, 'c'), (98, 146, 'c'), (110, 120, 'c'),
            (114, 180), (118, 230), (114, 270), (112, 296, 'c'), (180, 300, 'c')]
    base = smooth(sym(left, W))
    tex = f'<rect width="{W}" height="{H}" fill="url(#rib)"/>'
    neck = 'M156 36 Q180 30 204 36 L180 64 Z'
    colL = [(152, 32, 'c'), (118, 46), (114, 84), (134, 100, 'c'), (176, 72, 'c'), (180, 60, 'c')]
    def collar(pts):
        d = smooth(pts)
        cid = 'a06c' + str(int(pts[3][0]))
        return (raised(d, color, .45, .14) + f'<clipPath id="{cid}"><path d="{d}"/></clipPath><g clip-path="url(#{cid})">'
                f'<rect width="{W}" height="120" fill="url(#rib)" transform="rotate(30 150 60)"/></g>'
                + line(smooth(pts[1:5], False), '#fff', .3, 2.5) + line(smooth(pts[1:5], False), '#000', .25, 1))
    placket = 'M172 62 L188 62 L188 140 L172 140 Z'
    band = lambda y, h: f'<rect x="0" y="{y}" width="{W}" height="{h}" fill="url(#rib)"/><rect x="0" y="{y}" width="{W}" height="{h}" fill="url(#rib)" transform="translate(1 0)"/>'
    fabric = (shape(neck, '#000', .5)
              + raised(placket, color, .25, .1) + stitch('M172 140 L188 140', '#000', .3, 1, '2 1.5')
              + collar(mir(colL, W)) + collar(colL))
    detail = (pearl(180, 82, 4) + pearl(180, 106, 4) + pearl(180, 128, 4)
              + line('M112 274 Q180 270 248 274', '#000', .28, 2)
              + line('M60 114 L102 134', '#000', .28, 2) + line(f'M{W-60} 114 L{W-102} 134', '#000', .28, 2)
              + stitch('M106 52 Q110 90 110 118', '#000', .25, 1.2, '1 0') + stitch(f'M{W-106} 52 Q{W-110} 90 {W-110} 118', '#000', .25, 1.2, '1 0'))
    shade = (band(274, 30) + f'<g transform="rotate(26 78 132)"><rect x="40" y="120" width="76" height="22" fill="url(#rib)"/></g><g transform="rotate(-26 {W-78} 132)"><rect x="{W-116}" y="120" width="76" height="22" fill="url(#rib)"/></g>'
             + K('M112 124 Q106 200 112 296', .42, 12, 12) + K(f'M{W-112} 124 Q{W-106} 200 {W-112} 296', .48, 12, 12)
             + K('M118 140 Q140 160 160 200', .14, 6, 6) + K(f'M{W-118} 140 Q{W-140} 160 {W-160} 200', .16, 6, 6)
             + K('M82 70 Q70 96 62 124', .22, 12, 10) + K(f'M{W-82} 70 Q{W-70} 96 {W-62} 124', .26, 12, 10))
    light = (Wt('M146 100 Q156 170 146 250', .2, 22, 36) + Wt('M90 70 Q74 96 66 120', .2, 6, 8)
             + Wt('M224 160 Q236 210 228 260', .08, 12, 18))
    return piece(W, H, base, color, (.16, .28), shade, light, tex, detail, fabric=fabric, grain=(1.3, .3), name='polo')
