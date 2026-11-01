from kit.art_kit import *

def ribbed_top(color):
    """Slim ribbed square-neck long-sleeve top, waist length, body-hugging rib with soft stretch shading."""
    W, H = 360, 360
    left = [(180, 38, 'c'), (140, 34, 'c'), (116, 42, 'c'), (100, 56), (90, 110), (78, 210), (70, 300), (66, 340, 'c'), (94, 344, 'c'),
            (100, 300), (108, 210), (118, 114, 'c'), (122, 170), (120, 214), (124, 316, 'c'), (180, 320, 'c')]
    base = smooth(sym(left, W))
    back = 'M140 34 Q180 40 220 34 L215 66 L145 66 Z'          # inside back neck
    # finer rib than the kit pattern, plus a horizontal stretch ripple
    rib = ('<pattern id="rib2" width="4" height="10" patternUnits="userSpaceOnUse"><rect width="1.5" height="10" fill="#fff" opacity=".12"/>'
           '<rect x="2" width="1.6" height="10" fill="#000" opacity=".2"/></pattern>'
           f'<rect width="{W}" height="{H}" fill="url(#rib2)"/>')
    bind = 'M143 36 L146 64 L214 64 L217 36'
    detail = (f'<path d="{back}" fill="#000" opacity=".5"/>' + K('M146 40 Q180 46 214 40', .35, 3, 6) + line('M142 44 Q180 49 218 44', '#fff', .08, 1)
              + f'<path d="{bind}" fill="none" stroke="{color}" stroke-width="5" stroke-linejoin="round"/>'
              + f'<path d="{bind}" fill="none" stroke="#fff" stroke-opacity=".14" stroke-width="5" stroke-linejoin="round"/>'
              + line('M140 36 L143 68 L217 68 L220 36', '#000', .28, 1.3) + stitch('M146 39 L149 61 L211 61 L214 39', '#000', .28, .8, '2 1.6')
              + line('M102 54 Q106 86 118 114', '#000', .18, 1.2) + line(f'M{W-102} 54 Q{W-106} 86 {W-118} 114', '#000', .18, 1.2)
              + stitch('M124 308 Q180 312 236 308', '#000', .28, 1, '3 2')
              + line('M68 322 L98 326', '#000', .2, 1.2) + line(f'M{W-68} 322 L{W-98} 326', '#000', .2, 1.2))
    shade = (K('M120 120 Q126 214 122 316', .44, 12, 12) + K(f'M{W-120} 120 Q{W-126} 214 {W-122} 316', .5, 12, 12)
             + K('M94 120 Q84 220 74 330', .22, 6, 8) + K(f'M{W-94} 120 Q{W-84} 220 {W-74} 330', .26, 6, 8)
             # waist pull + stretch ripples on the rib
             + K('M126 200 Q180 214 234 200', .1, 12, 12) + K('M124 228 Q180 240 236 228', .08, 6, 5)
             + K('M78 240 Q90 236 104 244', .2, 3, 3) + K(f'M{W-78} 240 Q{W-90} 236 {W-104} 244', .22, 3, 3)
             + K('M74 290 Q86 286 100 292', .18, 3, 3) + K(f'M{W-74} 290 Q{W-86} 286 {W-100} 292', .2, 3, 3)
             + K('M146 70 L214 70', .2, 3, 6))
    light = (Wt('M150 90 Q162 170 150 280', .22, 22, 34) + Wt('M96 90 Q86 200 74 320', .2, 6, 7)
             + Wt('M140 140 Q180 154 220 140', .08, 12, 16) + Wt('M220 160 Q232 220 222 290', .08, 22, 24))
    return piece(W, H, base, color, (.15, .28), shade, light, rib, detail, grain=(1.4, .25), name='ribbed_top')
