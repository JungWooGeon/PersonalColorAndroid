from kit.art_kit import *

def mary_jane(color):
    """Ballet-style mary jane: almond toe, low vamp, thin instep strap, slim low heel."""
    W, H = 380, 190
    o = smooth([(6, 156, 'c'), (14, 146), (40, 136), (80, 130), (104, 129, 'c'), (170, 144), (240, 138), (290, 116, 'c'), (300, 138), (298, 160, 'c'), (12, 162, 'c')])
    sole = smooth([(4, 154), (8, 165, 'c'), (246, 166, 'c'), (256, 166, 'c'), (261, 178, 'c'), (290, 178, 'c'), (298, 158, 'c'), (12, 159, 'c')])
    strap = 'M158 142 Q182 98 214 141 L207 142 Q182 107 165 143 Z'
    lining = 'M104 129 Q170 136 240 134 L290 116 Q240 140 170 144 Q130 138 104 129 Z'
    detail = (f'<path d="{lining}" fill="#000" opacity=".45"/>' + raised(strap, color, .3, .18)
              + '<rect x="161" y="121" width="9" height="8" rx="2" fill="none" stroke="#C8B48A" stroke-width="1.8" transform="rotate(-55 165.5 125)"/>'
              + line('M104 130 Q170 146 290 117', '#000', .5, 2.4) + line('M8 159.5 L298 159.5', '#fff', .2, 1) + line('M261 167 L291 167', '#000', .3, 1)
              + stitch('M24 150 Q60 136 102 132', '#000', .2, 1, '3 2'))
    shade = K('M30 156 Q150 164 292 154', .28, 6, 10) + K('M108 132 Q180 150 286 120', .38, 6, 7)
    light = Wt('M22 146 Q50 134 96 131', .6, 3, 4) + Wt('M276 124 Q292 136 294 152', .45, 3, 4) + Wt('M38 140 Q52 135 68 133', .85, 1, 2)
    return shoe_svg(W, H, 'mary_jane', color, o, sole, detail, shade, light, back=(38, -20))
