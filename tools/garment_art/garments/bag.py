from kit.art_kit import *

def _charm(x, y):
    """Bag charm: little gold chain, pearl, gold heart (fixed colours)."""
    s = ''.join(f'<ellipse cx="{x}" cy="{y + i * 6}" rx="2.2" ry="3.2" fill="none" stroke="#B89650" stroke-width="1.4"/>' for i in range(4))
    hx, hy = x, y + 40
    heart = f'M{hx} {hy + 14} C{hx - 16} {hy + 2} {hx - 10} {hy - 9} {hx} {hy - 2} C{hx + 10} {hy - 9} {hx + 16} {hy + 2} {hx} {hy + 14} Z'
    return (s + pearl(x, y + 27, 4.5) + f'<path d="{heart}" fill="#000" opacity=".35" transform="translate(1 2)" filter="url(#b1)"/>'
            + f'<path d="{heart}" fill="#C9A85C"/><path d="{heart}" fill="none" stroke="#7a5f28" stroke-opacity=".5" stroke-width="1"/>'
            + f'<ellipse cx="{hx - 4}" cy="{hy}" rx="3" ry="2" fill="#FFF3C8" opacity=".85" filter="url(#b1)"/>')

def bag(color):
    """Soft leather shopper tote: trapezoid body, two rolled handles, gold rivets, heart bag charm."""
    W, H = 320, 300
    body = smooth([(84, 122, 'c'), (236, 122, 'c'), (258, 262), (250, 284), (226, 288), (160, 290), (94, 288), (70, 284), (62, 262)])
    hb = 'M118 128 Q118 34 160 34 Q202 34 202 128'          # back handle
    hf = 'M104 132 Q102 40 160 40 Q218 40 216 132'          # front handle
    under = (f'<path d="{hb}" fill="none" stroke="{color}" stroke-width="11"/><path d="{hb}" fill="none" stroke="#000" stroke-opacity=".38" stroke-width="11"/>'
             + f'<path d="M84 122 L236 122 L232 132 L88 132 Z" fill="{color}"/><path d="M84 122 L236 122 L232 132 L88 132 Z" fill="#000" opacity=".45"/>')
    tabs = ''
    for x in (104, 216):
        tab = f'M{x - 9} 128 L{x + 9} 128 L{x + 8} 158 Q{x} 164 {x - 8} 158 Z'
        tabs += raised(tab, color, .35, .18) + stitch(f'M{x - 5} 132 L{x - 5} 155 M{x + 5} 132 L{x + 5} 155', '#000', .3, .9, '2 2') + button(x, 148, 3.4, metal=True)
    detail = (f'<path d="{hf}" fill="none" stroke="#000" stroke-opacity=".3" stroke-width="13" transform="translate(1 3)" filter="url(#b3)"/>'
              + f'<path d="{hf}" fill="none" stroke="{color}" stroke-width="12"/>'
              + f'<path d="{hf}" fill="none" stroke="#000" stroke-opacity=".12" stroke-width="12"/>'
              + Wt('M106 110 Q104 50 160 45', .5, 1, 2.5) + K('M213 70 Q216 100 215 128', .35, 1, 4)
              + stitch('M98 128 Q96 36 160 35', '#fff', .25, .8, '3 2.5')
              + tabs + _charm(216, 162))
    texture = stitch('M88 130 L232 130', '#000', .3, 1, '3 2') + line('M86 125 L234 125', '#fff', .18, 1.5)
    shade = (K('M66 270 Q160 300 254 270', .45, 12, 22) + K('M64 140 L60 270', .3, 12, 18) + K('M256 140 L260 270', .45, 12, 22)
             + K('M140 140 Q130 200 128 280', .14, 12, 10) + K('M190 150 Q200 210 196 280', .12, 12, 8))
    light = (Wt('M100 150 Q96 210 94 270', .2, 12, 16) + Wt('M150 150 Q148 210 152 275', .12, 12, 10)
             + Wt('M96 136 Q160 130 224 136', .25, 3, 4))
    return piece(W, H, body, color, (.2, .3), shade, light, texture, detail, under=under, grain=(1.4, .22), name='bag')
