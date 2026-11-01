from kit.art_kit import *

def off_shoulder(color):
    """Off-shoulder rib knit: wide ribbed bardot band, slim cropped bodice, long sleeves with soft flare at the wrist."""
    W, H = 400, 340
    left = [(200, 72, 'c'), (140, 64), (84, 60, 'c'), (64, 84), (52, 140), (46, 210), (40, 262), (24, 318, 'c'), (96, 324, 'c'),
            (88, 264), (94, 200), (110, 136, 'c'), (122, 200), (118, 256), (118, 300, 'c'), (200, 304, 'c')]
    base = smooth(sym(left, W))
    tex = f'<rect width="{W}" height="{H}" fill="url(#rib)"/>'
    band = smooth([(84, 60, 'c'), (140, 64), (200, 72), (260, 64), (316, 60, 'c'), (322, 96, 'c'), (260, 98), (200, 106), (140, 98), (78, 96, 'c')])
    band_g = (f'<path d="{band}" fill="#000" opacity=".35" transform="translate(0 4)" filter="url(#b3)"/><path d="{band}" fill="{color}"/>'
              f'<clipPath id="a06b"><path d="{band}"/></clipPath><g clip-path="url(#a06b)"><rect width="{W}" height="120" fill="url(#rib)"/>'
              f'<rect width="{W}" height="120" fill="url(#rib)" transform="translate(2 0)" opacity=".6"/>'
              + K('M80 62 Q140 64 200 74 Q260 64 320 62', .4, 3, 6) + Wt('M90 76 Q140 78 200 86 Q260 78 310 76', .18, 3, 6) + K('M78 96 Q140 98 200 106 Q260 98 322 96', .3, 3, 6)
              + K('M88 60 L84 94', .3, 6, 12) + K('M312 60 L316 94', .35, 6, 12) + '</g>')
    cuffs = (f'<g transform="rotate(6 60 304)"><rect x="20" y="290" width="78" height="34" fill="url(#rib)"/></g>'
             f'<g transform="rotate(-6 {W-60} 304)"><rect x="{W-98}" y="290" width="78" height="34" fill="url(#rib)"/></g>'
             f'<rect x="110" y="282" width="180" height="26" fill="url(#rib)"/>')
    detail = (line('M32 292 Q60 296 92 298', '#000', .25, 2) + line(f'M{W-32} 292 Q{W-60} 296 {W-92} 298', '#000', .25, 2)
              + line('M120 282 Q200 286 280 282', '#000', .25, 2) + band_g
              + stitch('M86 94 Q100 112 108 132', '#000', .25, 1.2, '1 0') + stitch(f'M{W-86} 94 Q{W-100} 112 {W-108} 132', '#000', .25, 1.2, '1 0'))
    shade = (cuffs + K('M110 136 Q120 210 116 300', .45, 12, 12) + K(f'M{W-110} 136 Q{W-120} 210 {W-116} 300', .5, 12, 12)
             + K('M70 100 Q54 200 40 310', .22, 12, 10) + K(f'M{W-70} 100 Q{W-54} 200 {W-40} 310', .26, 12, 10)
             + K('M88 270 Q80 230 82 190', .2, 6, 5) + K('M120 200 Q170 190 200 196 Q230 190 280 200', .12, 12, 14)
             + K('M100 104 Q200 122 300 104', .25, 12, 12))
    light = (Wt('M150 120 Q160 180 150 260', .22, 22, 38) + Wt('M66 100 Q52 190 42 280', .2, 12, 12)
             + Wt('M200 140 Q206 190 200 240', .08, 22, 30) + Wt(f'M{W-150} 140 Q{W-158} 190 {W-150} 250', .08, 22, 26))
    return piece(W, H, base, color, (.18, .3), shade, light, tex, detail, grain=(1.2, .3), name='off_shoulder')
