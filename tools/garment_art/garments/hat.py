from kit.art_kit import *

def hat(color):
    """Washed cotton ball cap, low-profile unstructured crown, 3/4 view so the curved brim reads; small tonal embroidery."""
    W, H = 300, 240
    crown = smooth([(70, 174), (72, 122), (98, 72), (150, 44), (206, 48), (246, 84), (264, 130), (266, 170), (226, 184), (150, 186)])
    brim = smooth([(74, 166), (140, 176), (206, 184), (190, 198), (136, 214), (74, 222), (30, 218), (14, 206), (26, 186)])
    base = crown + ' ' + brim
    tex = f'<g opacity=".45"><rect width="{W}" height="{H}" fill="url(#twill)"/></g>'
    seams = ''.join(line(d, '#000', .26, 1.5) + stitch(d, '#fff', .2, 1, '3 2.5') for d in
                    ('M156 44 Q122 90 118 178', 'M158 44 Q206 80 214 182', 'M160 45 Q244 82 262 150'))
    brim_g = (f'<path d="{brim}" fill="#000" opacity=".5" transform="translate(0 4)" filter="url(#b3)"/>'
              f'<path d="{brim}" fill="{color}"/><clipPath id="cb"><path d="{brim}"/></clipPath><g clip-path="url(#cb)">'
              + f'<g opacity=".45"><rect width="{W}" height="{H}" fill="url(#twill)"/></g>'
              + ''.join(stitch(f'M{26+i*9} {206-i*3} Q{70+i*4} {218-i*7} {140+i*2} {208-i*7} Q{180} {196-i*5} {200-i*6} {186-i*1}', '#000', .32, 1.1, '3 2.5') for i in range(4))
              + K('M20 210 Q70 228 140 214 Q180 204 200 190', .5, 3, 8) + K('M110 200 Q160 196 196 186', .22, 12, 22) + K('M16 204 Q30 216 60 220', .3, 6, 10) + Wt('M50 188 Q100 180 150 186', .22, 6, 12) + f'<rect width="{W}" height="{H}" filter="url(#grainK)"/>' + '</g>'
              + f'<path d="{brim}" fill="none" stroke="#fff" stroke-opacity=".12" stroke-width="1.5"/>')
    eyelet = lambda x, y: (f'<ellipse cx="{x}" cy="{y}" rx="3.6" ry="4.2" fill="#000" opacity=".25"/><circle cx="{x}" cy="{y}" r="1.7" fill="#000" opacity=".55"/>')
    top = 'M149 46 Q150 34 158 33 Q166 35 166 46 Q158 49 149 46 Z'
    emb = ('<text x="0" y="0" text-anchor="middle" font-family="Georgia, serif" font-style="italic" font-size="16" transform="translate(126 134) rotate(-3) skewX(-6) scale(.9 1)" '
           'fill="#fff" fill-opacity=".92" stroke="#000" stroke-opacity=".25" stroke-width=".6" letter-spacing=".5">matin</text>')
    fabric = seams
    detail = eyelet(196, 88) + eyelet(240, 110) + raised(top, color, .35, .25) + emb + brim_g
    shade = (K('M76 120 Q70 150 74 176', .35, 12, 16) + K('M250 90 Q270 130 264 172', .5, 12, 22)
             + K('M170 120 Q220 150 230 182', .2, 22, 30) + K('M80 176 Q150 192 230 182', .35, 6, 10))
    light = (Wt('M100 90 Q88 120 92 160', .28, 12, 26) + Wt('M150 60 Q180 66 200 86', .14, 12, 16))
    return piece(W, H, base, color, (.16, .3), shade, light, tex, detail, fabric=fabric, grain=(1.2, .35), name='hat')
