from kit.art_kit import *

def beanie(color):
    """Short fisherman beanie: chunky rib knit, deep fold-up cuff, small woven label."""
    W, H = 300, 230
    base = smooth(sym([(150, 36, 'c'), (104, 42), (72, 62), (54, 94), (48, 124), (42, 132, 'c'), (40, 200, 'c'), (150, 204)], W))
    cuff = smooth([(41, 130, 'c'), (150, 140), (259, 130, 'c'), (260, 200, 'c'), (150, 204), (40, 200, 'c')])
    ribs = ''
    for i in range(23):
        x = 50 + i * 9.1; tx = 150 + (x - 150) * .2; mx = 150 + (x - 150) * .95
        d = f'M{x:.1f} 140 Q{mx:.1f} 70 {tx:.1f} 38'
        ribs += line(d, '#000', .22, 3.2) + f'<path d="{d}" transform="translate(3.6 0)" fill="none" stroke="#fff" stroke-opacity=".12" stroke-width="2.6"/>'
    cribs = ''.join(line(f'M{x:.1f} 128 L{x:.1f} 212', '#000', .24, 3.4) + line(f'M{x+3.8:.1f} 128 L{x+3.8:.1f} 212', '#fff', .13, 2.6) for x in [38 + i * 8 for i in range(29)])
    fabric = (ribs + K('M48 140 Q150 152 252 140', .5, 6, 10)
              + f'<clipPath id="cf"><path d="{cuff}"/></clipPath>' + raised(cuff, color, .5, .16)
              + f'<g clip-path="url(#cf)">{cribs}{K("M40 202 Q150 214 260 202", .35, 6, 10)}{Wt("M44 136 Q150 146 256 136", .3, 3, 5)}</g>')
    label = ('<rect x="186" y="156" width="34" height="18" rx="1.5" fill="#000" opacity=".3" transform="translate(1 2)" filter="url(#b1)"/>'
             '<rect x="186" y="156" width="34" height="18" rx="1.5" fill="#F4F1EA"/>'
             '<rect x="192" y="162" width="22" height="2.2" fill="#2a2724" opacity=".75"/><rect x="196" y="167" width="14" height="1.6" fill="#2a2724" opacity=".45"/>'
             + stitch('M188 158 L188 172 M218 158 L218 172', '#000', .35, .8, '1.5 1.5'))
    shade = (K('M58 90 Q48 130 52 200', .45, 12, 20) + K('M242 90 Q252 130 248 200', .5, 12, 20)
             + K('M120 46 Q150 36 180 46', .2, 6, 10))
    light = Wt('M110 60 Q96 90 98 126', .28, 12, 30) + Wt('M96 156 Q100 180 96 196', .2, 12, 22)
    return piece(W, H, base, color, (.16, .3), shade, light, '', label, fabric=fabric, grain=(1.3, .3), name='beanie')
