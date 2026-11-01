from kit.art_kit import *
import re


def socks(color):
    """Ribbed crew socks with a ruffled lace-frill cuff (ballet-core), side-view pair."""
    W, H = 340, 300
    body = smooth([(160, 76, 'c'), (240, 76, 'c'), (242, 160), (246, 222), (240, 258), (214, 276), (150, 280), (70, 280), (26, 276),
                   (8, 264), (12, 246), (40, 234), (100, 226), (148, 214), (160, 170)])
    top = [(248, 84, 'c'), (152, 84, 'c'), (148, 66)]
    top += [(151 + i * 6.5, 50 if i % 2 else 55) for i in range(15)] + [(250, 66)]
    frill = smooth(top)
    base = f'{body} {frill}'
    mv = lambda d, ox, oy: re.sub(r'(-?\d+\.?\d*) (-?\d+\.?\d*)', lambda m: f'{float(m[1]) + ox:.1f} {float(m[2]) + oy:.1f}', d)
    ox, oy = 62, -38
    back = mv(base, ox, oy)
    full = f'{back} {base}'
    mask = (f'<mask id="mb" maskUnits="userSpaceOnUse" x="0" y="0" width="{W}" height="{H}"><path d="{back}" fill="#fff"/>'
            f'<path d="{base}" fill="#000"/></mask>')
    tex = f'<rect width="{W}" height="{H}" fill="url(#rib)"/>'

    def deco(dx, dy):
        eyelets = ''.join(f'<circle cx="{158 + i * 9.5 + dx}" cy="{66 + dy}" r="1.6" fill="#000" opacity=".4"/>' for i in range(10))
        pleats = ''.join(line(f'M{154 + i * 6.5 + dx} {56 + dy} L{155 + i * 6.5 + dx} {82 + dy}', '#000', .16, 1.4) for i in range(14))
        band = K(f'M152 {88 + dy} L{248 + dx} {88 + dy}'.replace('M152', f'M{152 + dx}'), .5, 3, 5)
        heel = stitch(f'M{242 + dx} {214 + dy} Q{214 + dx} {226 + dy} {204 + dx} {278 + dy}', '#000', .22, 1, '2 2')
        toe = stitch(f'M{52 + dx} {279 + dy} Q{40 + dx} {256 + dy} {50 + dx} {232 + dy}', '#000', .22, 1, '2 2')
        return eyelets + pleats + band + heel + toe

    shade = (mask + f'<g mask="url(#mb)"><rect width="{W}" height="{H}" fill="#000" opacity=".18"/>'
             + f'<path d="{base}" fill="#000" opacity=".45" transform="translate(5 3)" filter="url(#b6)"/>{deco(ox, oy)}</g>'
             + deco(0, 0)
             + K('M238 96 Q246 180 244 240', .3, 12, 14) + K('M30 274 Q140 284 236 270', .3, 6, 10)
             + K('M160 96 Q158 170 148 214', .18, 12, 12))
    light = (Wt('M190 100 Q194 170 186 220', .28, 12, 22) + Wt('M30 250 Q80 238 140 232', .3, 6, 10)
             + Wt('M150 56 Q200 48 250 56', .45, 1, 2) + Wt(f'M{150 + ox} {56 + oy} Q{200 + ox} {48 + oy} {250 + ox} {56 + oy}', .3, 1, 2)
             + Wt('M20 252 Q30 244 48 240', .5, 1, 2))
    return piece(W, H, full, color, (.14, .26), shade, light, tex, '', grain=(1.4, .25), name='socks')
