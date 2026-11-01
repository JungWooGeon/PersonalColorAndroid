from kit.art_kit import *

def beret(color):
    """Soft wool felt beret, slightly tilted, with a little stalk and a thin bound headband."""
    W, H = 300, 200
    body = smooth([(30, 100), (46, 70), (96, 46), (160, 40), (222, 48), (268, 76), (284, 114), (272, 140), (240, 150), (150, 150), (72, 140), (38, 122)])
    band = 'M98 140 Q150 150 206 142 L205 154 Q150 164 99 154 Z'
    stalk = 'M148 46 Q146 34 151 30 Q156 33 155 46 Z'
    base = body + ' ' + band + ' ' + stalk
    tex = f'<rect width="{W}" height="{H}" filter="url(#boucleK)" opacity=".07"/>'
    fabric = (f'<path d="{band}" fill="#000" opacity=".22"/>' + K('M98 144 Q150 154 206 146', .55, 3, 7)
              + stitch('M100 151 Q150 160 204 151', '#fff', .2, 1, '2.5 2') + Wt('M100 153 Q150 162 204 153', .1, 1, 2)
              + raised(body, color, .5, .1)
              + f'<path d="{stalk}" fill="#000" opacity=".25"/>' + K('M120 50 Q150 54 182 50', .2, 3, 6))
    shade = (K('M60 128 Q150 160 250 132', .5, 12, 20) + K('M262 74 Q280 104 262 128', .4, 12, 16)
             + K('M40 108 Q46 124 64 134', .3, 6, 10) + K('M180 70 Q230 90 240 120', .15, 22, 30) + K('M196 56 Q222 96 214 140', .18, 6, 8))
    light = (Wt('M78 74 Q120 52 168 54', .35, 12, 22) + Wt('M60 96 Q110 80 150 82', .12, 22, 30)
             + Wt('M210 58 Q244 70 256 90', .12, 6, 8) + Wt('M186 56 Q210 96 202 138', .14, 6, 8))
    return piece(W, H, base, color, (.18, .32), shade, light, tex, '', fabric=fabric, grain=(1.6, .45), name='beret')
