from kit.art_kit import *

def denim_jacket(color):
    """크롭 청자켓: boxy cropped trucker, point collar, pointed chest flaps, yoke + panel seams, waistband, gold thread."""
    W, H = 400, 330
    T = '#C99A52'
    left = [(200, 34, 'c'), (164, 30, 'c'), (100, 46, 'c'), (82, 60), (72, 120), (62, 200), (54, 268, 'c'), (52, 290, 'c'), (100, 298, 'c'),
            (102, 276, 'c'), (106, 200), (118, 128, 'c'), (122, 200), (120, 262, 'c'), (200, 266, 'c')]
    base = smooth(sym(left, W))
    neck = smooth([(166, 30, 'c'), (200, 64, 'c'), (234, 30, 'c'), (200, 36)])
    colL = smooth([(198, 64, 'c'), (168, 22, 'c'), (148, 26), (116, 46, 'c'), (134, 96, 'c'), (170, 80), (198, 68, 'c')])
    flap = lambda x: f'M{x} 104 L{x+46} 104 L{x+46} 120 L{x+23} 132 L{x} 120 Z'
    tex = f'<rect width="{W}" height="{H}" fill="url(#twill)"/><rect width="{W}" height="{H}" fill="url(#twill)" opacity=".6" transform="translate(2 1)"/>'
    seam = lambda d: line(d, '#000', .3, 1.6) + stitch(d, T, .85, 1.1, '3 2.5')
    fabric = (shape(neck, '#000', .5) + K(neck, .3, 6)
              + shape('M120 240 L280 240 L280 266 L120 266 Z', '#000', .08) + line('M121 240 L279 240', '#000', .35, 1.8)
              + raised(flap(140), color, .3, .12) + raised(flap(214), color, .3, .12)
              + raised(colL, color, .4, .14) + raised(smooth(mir([(198, 64, 'c'), (168, 22, 'c'), (148, 26), (116, 46, 'c'), (134, 96, 'c'), (170, 80), (198, 68, 'c')], W)), color, .4, .1))
    detail = (seam('M124 92 Q160 98 194 96') + seam(f'M{W-124} 92 Q{W-160} 98 {W-194} 96')
              + seam('M194 66 L194 240') + stitch('M206 66 L206 240', T, .85, 1.1, '3 2.5')
              + seam('M156 134 L154 240') + seam(f'M{W-156} 134 L{W-154} 240')
              + stitch('M142 106 L184 106 L184 119 L163 130 L142 119 Z', T, .9, 1, '2.5 2') + stitch(f'M216 106 L258 106 L258 119 L237 130 L216 119 Z', T, .9, 1, '2.5 2')
              + stitch('M122 245 L278 245', T, .85, 1.1, '3 2.5') + stitch('M122 261 L278 261', T, .85, 1.1, '3 2.5')
              + line('M56 268 Q78 272 101 276', '#000', .3, 1.6) + stitch('M56 272 Q78 276 101 280', T, .8, 1, '3 2.5')
              + line(f'M{W-56} 268 Q{W-78} 272 {W-101} 276', '#000', .3, 1.6) + stitch(f'M{W-56} 272 Q{W-78} 276 {W-101} 280', T, .8, 1, '3 2.5')
              + stitch('M122 50 L137 88 L170 76 L195 66', T, .8, 1, '2.5 2') + stitch(f'M{W-122} 50 L{W-137} 88 L{W-170} 76 L{W-195} 66', T, .8, 1, '2.5 2')
              + ''.join(button(200, y, 5.5, metal=True) for y in (82, 128, 172, 214, 253))
              + button(163, 124, 4, metal=True) + button(237, 124, 4, metal=True)
              + button(140, 253, 4.5, metal=True) + button(260, 253, 4.5, metal=True)
              + button(78, 284, 3.5, metal=True) + button(W - 78, 284, 3.5, metal=True))
    shade = (K('M120 132 Q124 200 120 260', .5, 12, 12) + K(f'M{W-120} 132 Q{W-124} 200 {W-120} 260', .55, 12, 12)
             + K('M86 100 Q74 190 62 280', .25, 12, 10) + K(f'M{W-86} 100 Q{W-74} 190 {W-62} 280', .3, 12, 10)
             + K('M66 200 Q82 210 100 204', .3, 3, 3) + K(f'M{W-66} 214 Q{W-82} 224 {W-100} 218', .3, 3, 3))
    light = (Wt('M136 140 Q146 190 138 232', .3, 22, 36) + Wt('M76 90 Q66 180 58 260', .25, 12, 12)
             + Wt('M66 194 Q82 204 100 198', .25, 3, 2) + Wt(f'M{W-140} 150 Q{W-146} 190 {W-140} 230', .14, 22, 30)
             + Wt('M130 160 Q150 170 170 160', .12, 6, 10))
    return piece(W, H, base, color, (.14, .3), shade, light, tex, detail, fabric=fabric, grain=(1.6, .5), name='denim_jacket')
