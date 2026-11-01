from kit.art_kit import *

def cardigan(color):
    W, H = 380, 300
    left = [(190, 30, 'c'), (156, 28, 'c'), (104, 44, 'c'), (88, 58), (78, 100), (68, 170), (58, 240), (52, 282, 'c'), (96, 290, 'c'), (100, 240),
            (106, 170), (114, 118, 'c'), (116, 190), (118, 252, 'c'), (190, 256, 'c')]
    base = smooth(sym(left, W))
    v = 'M156 28 L190 150 L224 28 Q190 40 156 28 Z'
    tex = f'<rect width="{W}" height="{H}" fill="url(#stock)"/>'
    detail = (shape(v, '#000', .45) + K(v, .3, 6)
              + f'<path d="M156 28 L190 150 L190 255 M224 28 L190 150" fill="none" stroke="url(#ribH)" stroke-width="0"/>'
              + line('M154 30 L186 148', '#000', .25, 10) + line(f'M{W-154} 30 L{W-186} 148', '#000', .25, 10)
              + stitch('M156 30 L188 146', '#fff', .25, 2, '1.5 2.5') + stitch(f'M{W-156} 30 L{W-188} 146', '#fff', .25, 2, '1.5 2.5')
              + line('M190 150 L190 254', '#000', .3, 1.5)
              + f'<g clip-path="url(#c)"><rect x="100" y="232" width="180" height="26" fill="url(#ribH)" transform="rotate(90 190 245)" opacity="0"/></g>'
              + ''.join(pearl(196, y, 5.5) for y in (164, 190, 216, 240)))
    rib = (f'<rect x="0" y="230" width="{W}" height="30" fill="url(#rib)"/>' + line('M116 231 L264 231', '#000', .25, 2)
           + f'<rect x="0" y="262" width="{W}" height="40" fill="url(#rib)"/>')
    shade = (K('M114 122 Q118 190 118 250', .4, 12, 12) + K(f'M{W-114} 122 Q{W-118} 190 {W-118} 250', .45, 12, 12)
             + K('M88 100 Q78 180 70 280', .22, 12, 10) + K(f'M{W-88} 100 Q{W-78} 180 {W-70} 280', .26, 12, 10) + rib)
    light = Wt('M140 80 Q150 140 140 210', .2, 22, 36) + Wt('M78 90 Q68 170 60 250', .18, 12, 12)
    return piece(W, H, base, color, (.14, .26), shade, light, tex, detail, grain=(1.4, .35), name='cardigan')
