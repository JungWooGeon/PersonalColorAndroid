from kit.art_kit import *

def longtee(color):
    """Slim long-sleeve tee: close fit, softly nipped waist, long slim sleeves, narrow ribbed crew neck."""
    W, H = 360, 380
    left = [(180, 40, 'c'), (152, 34, 'c'), (116, 46, 'c'), (102, 60), (92, 110), (80, 210), (68, 300), (62, 352, 'c'), (94, 356, 'c'),
            (100, 300), (110, 210), (120, 116, 'c'), (128, 190), (130, 222), (122, 322, 'c'), (180, 326, 'c')]
    base = smooth(sym(left, W))
    neck_in = 'M152 34 Q158 56 180 59 Q202 56 208 34 Q180 45 152 34 Z'
    nb = 'M150 33 Q157 64 180 66 Q203 64 210 33'
    cuffL = 'M64 324 L98 328'
    detail = (shape(neck_in, '#000', .5) + K(neck_in, .3, 3)
              + f'<path d="{nb}" fill="none" stroke="{color}" stroke-width="8"/><path d="{nb}" fill="none" stroke="url(#rib)" stroke-width="8"/>'
              + line('M147 32 Q156 70 180 72 Q204 70 213 32', '#000', .2, 1.4)
              + line('M100 52 Q104 84 118 116', '#000', .2, 1.3) + line(f'M{W-100} 52 Q{W-104} 84 {W-118} 116', '#000', .2, 1.3)
              + line(cuffL, '#000', .25, 1.5) + line(f'M{W-64} 324 L{W-98} 328', '#000', .25, 1.5)
              + stitch('M124 314 Q180 318 236 314', '#000', .3, 1, '3 2'))
    cuffrib = (f'<rect x="56" y="326" width="46" height="34" fill="url(#rib)"/><rect x="{W-102}" y="326" width="46" height="34" fill="url(#rib)"/>')
    shade = (cuffrib + K('M120 122 Q128 220 122 330', .42, 12, 12) + K(f'M{W-120} 122 Q{W-128} 220 {W-122} 330', .48, 12, 12)
             + K('M92 120 Q82 220 72 330', .22, 6, 8) + K(f'M{W-92} 120 Q{W-82} 220 {W-72} 330', .26, 6, 8)
             # sleeve crinkles at the forearm
             + K('M74 250 Q86 246 100 252', .2, 3, 3) + K('M70 282 Q84 278 98 284', .2, 3, 3)
             + K(f'M{W-74} 250 Q{W-86} 246 {W-100} 252', .22, 3, 3) + K(f'M{W-70} 282 Q{W-84} 278 {W-98} 284', .22, 3, 3)
             + K('M130 196 Q150 206 160 236', .12, 6, 6) + K(f'M{W-130} 196 Q{W-150} 206 {W-160} 236', .14, 6, 6)
             + K('M150 120 Q180 300 170 330', .1, 22, 26))
    light = (Wt('M146 100 Q156 180 146 280', .2, 22, 36) + Wt('M94 90 Q84 200 72 320', .2, 6, 7)
             + Wt('M78 264 Q88 262 98 266', .14, 3, 2) + Wt('M214 160 Q228 230 218 300', .08, 22, 26))
    return piece(W, H, base, color, (.15, .28), shade, light, '', f'<g clip-path="url(#c)">{detail}</g>', grain=(1.7, .25), name='longtee')
