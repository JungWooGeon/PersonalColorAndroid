from kit.art_kit import *

def shoulder_bag(color, hardware='#C8B48A'):
    W, H = 300, 250
    body = smooth([(46, 110, 'c'), (254, 110, 'c'), (262, 196), (240, 226), (150, 232), (60, 226), (38, 196)])
    flap = smooth([(46, 110, 'c'), (254, 110, 'c'), (250, 150), (230, 172), (150, 178), (70, 172), (50, 150)])
    strap_d = 'M64 114 Q70 30 150 26 Q230 30 236 114'
    strap = (f'<path d="{strap_d}" fill="none" stroke="#000" stroke-opacity=".25" stroke-width="16" transform="translate(1 3)" filter="url(#b3)"/>'
             f'<path d="{strap_d}" fill="none" stroke="{color}" stroke-width="14"/><path d="{strap_d}" fill="none" stroke="#000" stroke-opacity=".25" stroke-width="14"/>'
             + stitch('M58 112 Q64 24 150 20 Q236 24 242 112', '#fff', .2, 1, '3 3') + Wt('M74 70 Q100 34 150 30', .35, 1, 3))
    detail = (strap + raised(flap, color, .4, .2) + f'<path d="{flap}" fill="none" stroke="#000" stroke-opacity=".08" stroke-width="10" filter="url(#b3)"/>'
              + stitch('M54 116 L246 116 L244 148 Q228 168 150 172 Q72 168 56 148 Z', '#000', .25, 1, '3 2')
              + Wt('M70 124 Q150 116 230 124', .35, 3, 6) + Wt('M90 128 Q150 122 200 128', .6, 1, 2)
              + f'<rect x="136" y="164" width="28" height="18" rx="4" fill="#000" opacity=".3" filter="url(#b1)"/><rect x="136" y="162" width="28" height="17" rx="4" fill="{hardware}"/><rect x="140" y="165" width="20" height="5" rx="2" fill="#fff" opacity=".5"/>')
    shade = K('M50 200 Q150 236 250 200', .4, 12, 20) + K('M60 182 Q150 190 240 182', .3, 6, 8)
    light = Wt('M70 196 Q150 214 230 196', .15, 6, 8)
    return piece(W, H, body, color, (.18, .3), shade, light, '', detail, grain=(1.4, .2), name='shoulder_bag')
