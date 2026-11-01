from kit.art_kit import *

def bucket_hat(color):
    """Cotton twill bucket hat: soft rounded crown, wide drooping brim with rows of top-stitching."""
    W, H = 300, 220
    base = smooth(sym([(150, 52, 'c'), (110, 54), (92, 60), (86, 80), (84, 104), (80, 134), (60, 152), (42, 174), (38, 190), (84, 204), (150, 210, 'c')], W))
    brim = smooth([(82, 132), (150, 142), (218, 132), (240, 152), (258, 174), (262, 190), (216, 204), (150, 210), (84, 204), (38, 190), (42, 174), (60, 152)])
    tex = f'<g opacity=".45"><rect width="{W}" height="{H}" fill="url(#twill)"/></g>'
    sts = ''.join(stitch(f'M{44+i*8} {188-i*4} Q150 {214-i*9} {256-i*8} {188-i*4}', '#000', .3, 1.1, '3 2.5') for i in range(5))
    crown_sts = ''.join(stitch(f'M{88+i*.6:.1f} {122-i*7} Q150 {130-i*7} {212-i*.6:.1f} {122-i*7}', '#000', .22, 1, '3 2.5') for i in range(3))
    fabric = (line('M90 68 Q150 76 210 68', '#000', .3, 1.6) + stitch('M91 72 Q150 80 209 72', '#fff', .25, 1, '3 2.5')
              + line('M150 76 L150 138', '#000', .2, 1.4) + crown_sts
              + raised(brim, color, .5, .15) + sts + Wt('M60 176 Q150 192 240 176', .14, 6, 10))
    eyelet = lambda x, y: f'<circle cx="{x}" cy="{y}" r="3.8" fill="#000" opacity=".3"/><circle cx="{x}" cy="{y}" r="1.7" fill="#000" opacity=".6"/>'
    detail = eyelet(104, 92) + eyelet(196, 92)
    shade = (K('M92 70 Q86 104 84 136', .4, 12, 16) + K('M208 70 Q214 104 216 136', .45, 12, 16)
             + K('M84 140 Q150 156 216 140', .45, 6, 10) + K('M42 190 Q150 216 258 190', .3, 6, 8)
             + K('M236 156 Q252 174 258 188', .25, 6, 10))
    light = Wt('M118 74 Q110 98 112 126', .26, 12, 26) + Wt('M60 168 Q90 160 120 162', .14, 6, 10) + Wt('M110 60 Q150 56 190 60', .14, 6, 8)
    return piece(W, H, base, color, (.16, .3), shade, light, tex, detail, fabric=fabric, grain=(1.2, .35), name='bucket_hat')
