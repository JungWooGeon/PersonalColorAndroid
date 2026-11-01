from kit.art_kit import *

def denim_skirt(color):
    """Long denim maxi skirt with front slit, 5-pocket jean details, contrast topstitch."""
    W, H = 280, 500
    base = smooth(sym([(140, 14, 'c'), (80, 14, 'c'), (78, 44, 'c'), (60, 200), (28, 482, 'c'), (88, 488), (137.5, 485, 'c'), (140, 418, 'c')], W))
    thread = '#C99A52'
    tex = f'<rect width="{W}" height="{H}" fill="url(#twill)"/>'
    loops = ''.join(f'<rect x="{x-4}" y="11" width="8" height="34" rx="2" fill="#000" opacity=".25" filter="url(#b1)"/>'
                    f'<rect x="{x-4}" y="10" width="8" height="32" rx="2" fill="{color}"/>' + stitch(f'M{x} 13 L{x} 39', thread, .7, 1, '2 2')
                    for x in (90, 118, 162, 190))
    detail = (line('M78 44 L202 44', '#000', .3, 2) + stitch('M80 18 L200 18', thread, .8, 1.2) + stitch('M80 40 L200 40', thread, .8, 1.2)
              + stitch('M150 46 L150 116 Q150 134 138 140', thread, .85, 1.3)
              + line('M140 46 L140 418', '#000', .28, 1.8) + stitch('M136 140 L136 414', thread, .75, 1.2) + stitch('M144 140 L144 414', thread, .75, 1.2)
              + stitch('M82 50 Q104 52 112 50 Q104 84 76 98', thread, .8, 1.2) + line('M84 48 Q108 50 114 48 Q106 86 74 102', '#000', .25, 1.5)
              + stitch(f'M{W-82} 50 Q{W-104} 52 {W-112} 50 Q{W-104} 84 {W-76} 98', thread, .8, 1.2) + line(f'M{W-84} 48 Q{W-108} 50 {W-114} 48 Q{W-106} 86 {W-74} 102', '#000', .25, 1.5)
              + stitch(f'M{W-98} 56 L{W-84} 56 L{W-86} 72', thread, .6, 1)
              + loops + button(140, 29, 7, metal=True) + button(112, 52, 2.6, metal=True) + button(W - 112, 52, 2.6, metal=True)
              + line('M136 418 L144 418', thread, .9, 2.4)
              + stitch('M32 472 Q90 478 134 475', thread, .7, 1.2) + stitch(f'M{W-32} 472 Q{W-90} 478 {W-134} 475', thread, .7, 1.2))
    shade = (K('M140 400 L140 484', .4, 3, 5)
             + K('M104 120 Q90 300 72 480', .3, 12, 12) + K(f'M{W-104} 120 Q{W-90} 300 {W-72} 480', .35, 12, 12)
             + K('M66 220 Q48 360 38 480', .3, 12, 16) + K(f'M{W-66} 220 Q{W-48} 360 {W-38} 480', .36, 12, 16)
             + K('M84 44 L196 44', .28, 6, 8) + K('M40 466 Q90 458 132 468', .2, 6, 6) + K(f'M{W-40} 466 Q{W-90} 458 {W-132} 468', .2, 6, 6))
    light = (Wt('M112 130 Q104 300 96 470', .3, 22, 30) + Wt(f'M{W-118} 130 Q{W-110} 300 {W-102} 470', .2, 22, 26)
             + Wt('M60 330 Q78 326 100 332', .16, 6, 10) + Wt('M128 420 L134 474', .16, 3, 3))
    return piece(W, H, base, color, (.12, .3), shade, light, tex, detail, grain=(1.6, .5), name='denim_skirt')
