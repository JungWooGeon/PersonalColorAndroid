from kit.art_kit import *

def knit_dress(color: str) -> str:
    """Rib-knit long dress: henley placket with pearl buttons, fitted long sleeves, body-skimming column with soft flare and side slit."""
    W, H = 380, 690
    left = [(190, 56, 'c'), (160, 50, 'c'), (118, 64, 'c'), (104, 80), (92, 140), (82, 230), (74, 300), (70, 336, 'c'), (100, 340, 'c'),
            (104, 290), (112, 210), (124, 130, 'c'), (126, 190), (134, 250), (126, 340), (120, 460), (108, 600), (98, 660, 'c'),
            (140, 666), (190, 664, 'c')]
    base = smooth(sym(left, W))
    neck_in = 'M160 50 Q166 78 190 80 Q214 78 220 50 Q190 58 160 50 Z'
    band = 'M160 50 Q166 78 190 80 Q214 78 220 50'
    plk = 'M182 80 L198 80 L198 168 Q190 172 182 168 Z'
    tex = f'<rect width="{W}" height="{H}" fill="url(#rib)"/>'
    slit = 'M262 560 Q266 610 270 662 L276 662 Q272 610 262 560 Z'
    detail = (shape(neck_in, '#000', .45)
              + f'<path d="{band}" fill="none" stroke="{color}" stroke-width="11"/><path d="{band}" fill="none" stroke="url(#ribH)" stroke-width="11" transform="rotate(0)"/>'
              + line('M154 50 Q160 90 190 92 Q220 90 226 50', '#000', .2, 1.5)
              + raised(plk, color, .25, .1) + stitch('M184 84 L184 166', '#000', .2, 1, '2 2') + stitch('M196 84 L196 166', '#000', .2, 1, '2 2')
              + ''.join(pearl(190, y, 4.2) for y in (100, 124, 148))
              + shape(slit, '#000', .45) + line('M262 560 L270 662', '#000', .3, 1.5)
              + line('M72 316 Q86 320 102 322', '#000', .25, 1.5) + line(f'M{W-72} 316 Q{W-86} 320 {W-102} 322', '#000', .25, 1.5))
    cuffs = (f'<g transform="rotate(4 86 330)"><rect x="66" y="318" width="38" height="24" fill="url(#rib)"/></g>'
             + f'<g transform="rotate(-4 {W-86} 330)"><rect x="{W-104}" y="318" width="38" height="24" fill="url(#rib)"/></g>')
    shade = (cuffs + K(neck_in, .3, 6) + K('M124 134 Q130 200 134 250', .42, 12, 12) + K(f'M{W-124} 134 Q{W-130} 200 {W-134} 250', .48, 12, 12)
             + K('M132 260 Q122 420 106 650', .32, 12, 14) + K(f'M{W-132} 260 Q{W-122} 420 {W-106} 650', .38, 12, 14)
             + K('M100 90 Q86 200 76 320', .22, 12, 10) + K(f'M{W-100} 90 Q{W-86} 200 {W-76} 320', .26, 12, 10)
             + K('M150 250 Q190 262 230 250', .16, 12, 14) + K('M160 400 Q166 520 150 650', .14, 22, 20) + K('M226 420 Q236 540 250 650', .2, 12, 12)
             + K('M100 652 Q190 644 280 652', .2, 6, 10))
    light = (Wt('M156 110 Q166 180 156 240', .22, 22, 36) + Wt('M98 100 Q86 210 76 300', .2, 12, 10)
             + Wt('M170 290 Q176 340 166 380', .16, 22, 30) + Wt('M210 290 Q204 340 214 380', .1, 22, 26)
             + Wt('M150 420 Q144 540 132 640', .14, 12, 16) + Wt('M206 440 Q212 540 222 640', .1, 12, 14))
    return piece(W, H, base, color, (.16, .3), shade, light, tex, detail, grain=(1.2, .3), name='knit_dress')
