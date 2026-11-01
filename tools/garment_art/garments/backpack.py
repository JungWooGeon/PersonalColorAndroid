from kit.art_kit import *

GOLD = '#C8A863'

def _pull(x, y):
    return (f'<rect x="{x-3}" y="{y}" width="6" height="16" rx="3" fill="#000" opacity=".35" transform="translate(1 2)" filter="url(#b1)"/>'
            f'<rect x="{x-3}" y="{y}" width="6" height="16" rx="3" fill="{GOLD}"/><rect x="{x-1.6}" y="{y+2}" width="1.6" height="10" fill="#FFF3C8" opacity=".8"/>')

def backpack(color):
    """Mini backpack: rounded dome, top loop, front zip pocket, gold pulls, straps peeking at the sides."""
    W, H = 300, 250
    base = smooth(sym([(150, 46), (118, 48), (94, 64), (80, 100), (76, 160), (78, 214), (90, 230, 'c'), (150, 232, 'c')], W))
    loop = 'M138 52 C138 22 162 22 162 52'
    straps = ''.join(f'<path d="{d}" fill="none" stroke="#000" stroke-opacity=".3" stroke-width="13" transform="translate(1 3)" filter="url(#b3)"/>'
                     f'<path d="{d}" fill="none" stroke="{color}" stroke-width="12"/><path d="{d}" fill="none" stroke="#000" stroke-opacity=".38" stroke-width="12"/>'
                     for d in ('M108 60 C82 90 66 150 72 222', 'M192 60 C218 90 234 150 228 222'))
    under = (straps + f'<path d="{loop}" fill="none" stroke="#000" stroke-opacity=".28" stroke-width="9" transform="translate(1 2)" filter="url(#b3)"/>'
             f'<path d="{loop}" fill="none" stroke="{color}" stroke-width="8"/><path d="{loop}" fill="none" stroke="#000" stroke-opacity=".18" stroke-width="8"/>'
             + Wt('M141 40 C144 30 156 30 159 38', .45, 1, 2))
    zip_main = 'M84 132 C86 72 118 56 150 56 C182 56 214 72 216 132'
    pocket = smooth([(150, 146), (106, 146, 'c'), (100, 156), (100, 206), (110, 216), (150, 218), (190, 216), (200, 206), (200, 156), (194, 146, 'c')])
    detail = (line(zip_main, '#000', .45, 4) + line(zip_main, '#fff', .18, 1.2) + stitch(zip_main, '#000', .5, 2.2, '1.5 1.5')
              + _pull(86, 130) + _pull(214, 130)
              + raised(pocket, color, .4, .18)
              + line('M106 158 Q150 152 194 158', '#000', .45, 3) + stitch('M106 158 Q150 152 194 158', '#000', .5, 1.8, '1.5 1.5')
              + stitch('M106 164 L106 206 Q108 212 150 213 Q192 212 194 206 L194 164', '#000', .25, 1, '3 2')
              + _pull(186, 154)
              + f'<rect x="138" y="178" width="24" height="12" rx="2" fill="#000" opacity=".3" filter="url(#b1)"/>'
              + f'<rect x="138" y="176" width="24" height="12" rx="2" fill="{GOLD}"/><rect x="141" y="178" width="18" height="3" rx="1" fill="#fff" opacity=".5"/>'
              + Wt('M108 164 Q106 190 110 208', .3, 3, 3))
    shade = (K('M80 214 Q150 240 220 214', .45, 12, 18) + K('M84 100 Q78 160 82 220', .35, 6, 10) + K('M216 100 Q222 160 218 220', .4, 6, 10)
             + K('M96 140 Q150 150 204 140', .2, 6, 8))
    light = Wt('M112 74 Q150 60 186 70', .35, 6, 10) + Wt('M94 96 Q86 130 88 170', .3, 3, 5) + Wt('M120 66 Q140 60 160 60', .6, 1, 2)
    return piece(W, H, base, color, (.2, .32), shade, light, '', detail, under=under, grain=(1.4, .2), name='backpack')
