from kit.art_kit import *

def shoes(color):
    """Low-top leather court sneaker on a cream cupsole (minimal court style)."""
    W, H = 380, 200
    o = smooth([(10, 156), (10, 136), (30, 120), (70, 112), (110, 104), (150, 94), (184, 84, 'c'), (198, 90), (230, 104), (266, 96),
                (290, 90), (301, 102), (304, 130), (302, 158, 'c'), (14, 160, 'c')])
    sole = smooth([(12, 148, 'c'), (4, 160), (8, 178), (24, 186, 'c'), (296, 186, 'c'), (306, 174), (306, 148, 'c')])
    toe = 'M11 157 Q6 132 32 120 Q58 113 84 116 Q64 130 60 158 Z'
    eyestay = 'M100 112 Q146 98 184 86 L196 94 Q152 112 108 126 Z'
    tab = 'M284 92 Q294 82 302 90 L304 106 Q296 100 288 104 Z'
    laces = ''.join(f'<path d="M{x-6} {y+2} L{x+7} {y-3}" stroke="#F4F1EA" stroke-width="4.2" stroke-linecap="round"/>'
                    f'<path d="M{x-6} {y+3.5} L{x+7} {y-1.5}" stroke="#000" stroke-opacity=".2" stroke-width="1.1"/>'
                    for x, y in [(116, 110), (134, 104), (152, 98), (170, 92)])
    eyelets = ''.join(f'<circle cx="{x}" cy="{y}" r="2.4" fill="#000" opacity=".45"/><circle cx="{x}" cy="{y}" r="1.4" fill="#E9E4DA"/>'
                      for x, y in [(118, 116), (136, 110), (154, 104), (172, 98)])
    detail = (raised(toe, color, .28, .14) + stitch('M16 154 Q14 132 36 124 Q60 118 78 120', '#000', .25, 1, '3 2')
              + raised(eyestay, color, .3, .16) + stitch('M108 122 Q150 108 190 94', '#000', .25, 1, '3 2')
              + stitch('M250 108 Q262 130 258 156', '#000', .2, 1, '3 2') + stitch('M196 120 Q226 118 250 108', '#000', .18, 1, '3 2')
              + raised(tab, color, .3, .14) + eyelets + laces
              # cupsole: outline, top edge, groove, bottom shade
              + f'<path d="{sole}" fill="none" stroke="#000" stroke-opacity=".22" stroke-width="1.2"/>'
              + line('M8 158 Q150 162 304 158', '#000', .3, 1.6) + line('M10 161 Q150 165 302 161', '#fff', .7, 1.2)
              + line('M10 172 Q150 175 304 172', '#000', .14, 1.2) + line('M18 183 L298 183', '#000', .14, 3))
    shade = (K('M24 152 Q150 160 296 152', .3, 6, 12) + K('M198 98 Q230 116 268 102', .45, 3, 6)
             + K('M240 120 Q276 130 296 150', .2, 12, 20))
    light = (Wt('M20 140 Q30 124 64 118', .55, 3, 5) + Wt('M28 132 Q40 124 58 120', .85, 1, 2.5)
             + Wt('M120 136 Q170 130 220 124', .18, 6, 10) + Wt('M290 104 Q300 124 298 150', .3, 3, 4))
    return shoe_svg(W, H, 'shoes', color, o, sole, detail, shade, light, back=(38, -24), sole_col='#F1EDE4')
