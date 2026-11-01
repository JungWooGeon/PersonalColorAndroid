from kit.art_kit import *

def knit(color):
    W, H = 420, 400
    left = [(210, 52, 'c'), (172, 44, 'c'), (90, 64, 'c'), (66, 118), (46, 210), (32, 300), (24, 352, 'c'), (84, 368, 'c'), (90, 300),
            (100, 220), (110, 176, 'c'), (104, 250), (102, 330), (110, 356), (114, 386, 'c'), (210, 390, 'c')]
    base = smooth(sym(left, W))
    neck_in = 'M174 46 Q182 84 210 88 Q238 84 246 46 Q210 56 174 46 Z'
    tex = f'<rect width="{W}" height="{H}" fill="url(#stock)"/><rect width="{W}" height="{H}" fill="url(#stock)" transform="translate(4 4)" opacity=".5"/>'
    rib = (f'<rect x="100" y="352" width="220" height="40" fill="url(#rib)"/>' + line('M104 352 Q210 345 316 352', '#000', .3, 2.5)
           + f'<g transform="rotate(8 54 360)"><rect x="20" y="336" width="70" height="40" fill="url(#rib)"/></g>'
           + f'<g transform="rotate(-8 {W-54} 360)"><rect x="{W-90}" y="336" width="70" height="40" fill="url(#rib)"/></g>'
           + line('M28 330 Q56 336 88 344', '#000', .25, 2) + line(f'M{W-28} 330 Q{W-56} 336 {W-88} 344', '#000', .25, 2))
    detail = (shape(neck_in, '#000', .45) + K(neck_in, .3, 6)
              + f'<path d="M172 44 Q182 92 210 96 Q238 92 248 44" fill="none" stroke="{color}" stroke-width="13"/>'
              + f'<path d="M172 44 Q182 92 210 96 Q238 92 248 44" fill="none" stroke="url(#rib)" stroke-width="13"/>'
              + line('M166 46 Q178 102 210 104 Q242 102 254 46', '#000', .18, 2)
              + stitch('M90 66 Q96 120 110 176', '#000', .3, 2.5, '1 0') + stitch(f'M{W-90} 66 Q{W-96} 120 {W-110} 176', '#000', .3, 2.5, '1 0'))
    shade = (rib + K('M108 180 Q100 270 108 380', .4, 12, 14) + K(f'M{W-108} 180 Q{W-100} 270 {W-108} 380', .45, 12, 14)
             + K('M70 130 Q54 240 44 340', .2, 12, 12) + K(f'M{W-70} 130 Q{W-54} 240 {W-44} 340', .25, 12, 12)
             + K('M150 200 Q200 260 170 350', .16, 22, 30) + K('M260 150 Q280 240 270 340', .14, 22, 26))
    light = Wt('M150 110 Q170 180 150 260', .2, 22, 46) + Wt('M70 100 Q52 200 38 300', .2, 12, 14) + Wt('M230 220 Q250 280 236 330', .1, 22, 30)
    return piece(W, H, base, color, (.16, .28), shade, light, tex, detail, grain=(1.2, .3), name='knit')
