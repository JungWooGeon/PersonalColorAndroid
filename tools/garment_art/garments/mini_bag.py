from kit.art_kit import *

GOLD = '#C8A863'

def _ring(x, y):
    return (f'<rect x="{x-7}" y="{y-4}" width="14" height="10" rx="5" fill="none" stroke="#000" stroke-opacity=".35" stroke-width="3" filter="url(#b1)"/>'
            f'<rect x="{x-7}" y="{y-5}" width="14" height="10" rx="5" fill="none" stroke="{GOLD}" stroke-width="2.6"/>'
            f'<path d="M{x-5} {y-5} L{x+3} {y-5}" stroke="#FFF3C8" stroke-width="1.2" opacity=".8"/>')

def mini_bag(color):
    """Mini top-handle bag: soft trapeze, rolled handle, rounded flap, gold turn-lock."""
    W, H = 300, 250
    body = smooth(sym([(150, 112), (94, 112), (86, 120), (78, 170), (72, 208), (80, 222), (104, 226), (150, 228)], W))
    flap = smooth(sym([(150, 110), (94, 110), (86, 118), (84, 140), (96, 160), (124, 172), (150, 176)], W))
    hd = 'M112 116 C112 52 188 52 188 116'
    under = (f'<path d="{hd}" fill="none" stroke="#000" stroke-opacity=".28" stroke-width="13" transform="translate(1 3)" filter="url(#b3)"/>'
             f'<path d="{hd}" fill="none" stroke="{color}" stroke-width="12"/>'
             f'<path d="{hd}" fill="none" stroke="#000" stroke-opacity=".22" stroke-width="12"/>'
             + Wt('M116 92 C122 68 150 62 168 66', .45, 1, 3) + K('M184 80 C188 92 188 104 188 116', .3, 3, 6))
    lock = (f'<rect x="138" y="166" width="24" height="18" rx="5" fill="#000" opacity=".35" filter="url(#b1)"/>'
            f'<rect x="138" y="164" width="24" height="17" rx="5" fill="{GOLD}"/>'
            f'<rect x="146" y="160" width="8" height="18" rx="4" fill="#B08A45"/><rect x="147" y="161" width="3" height="14" rx="1.5" fill="#FFF3C8" opacity=".7"/>'
            f'<rect x="141" y="166" width="18" height="3" rx="1.5" fill="#fff" opacity=".45"/>')
    tabs = ''.join(raised(f'M{x-8} 108 L{x+8} 108 L{x+7} 126 Q{x} 130 {x-7} 126 Z', color, .3, .15) for x in (112, 188))
    detail = (tabs + _ring(112, 110) + _ring(188, 110)
              + raised(flap, color, .45, .2)
              + f'<path d="{flap}" fill="none" stroke="#000" stroke-opacity=".1" stroke-width="10" filter="url(#b3)"/>'
              + stitch('M96 117 L204 117 L210 140 Q200 160 150 170 Q100 160 90 140 Z', '#000', .28, 1, '3 2')
              + Wt('M100 122 Q150 116 200 122', .5, 3, 5) + Wt('M106 126 Q150 121 190 126', .7, 1, 1.6)
              + K('M96 162 Q150 182 204 162', .25, 3, 4)
              + lock)
    shade = K('M74 214 Q150 236 226 214', .45, 12, 18) + K('M80 150 Q74 190 76 220', .3, 6, 8) + K('M220 150 Q226 190 224 220', .35, 6, 8)
    light = Wt('M98 194 Q150 208 202 194', .14, 6, 8) + Wt('M88 180 Q86 200 88 214', .3, 3, 4)
    return piece(W, H, body, color, (.18, .3), shade, light, '', detail, under=under, grain=(1.4, .2), name='mini_bag')
