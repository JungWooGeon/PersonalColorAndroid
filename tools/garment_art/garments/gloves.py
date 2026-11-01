from kit.art_kit import *
import math

_G = [(26, 250, 'c'), (28, 196), (30, 176), (22, 156), (8, 132), (2, 110), (8, 98), (20, 102), (32, 122), (38, 136, 'c'),
      (36, 96), (34, 52), (38, 28), (46, 20), (55, 24), (58, 40), (58, 92, 'c'),
      (60, 40), (63, 14), (72, 7), (81, 12), (83, 30), (82, 92, 'c'),
      (84, 40), (88, 22), (97, 18), (105, 24), (106, 42), (104, 98, 'c'),
      (108, 62), (113, 50), (121, 49), (126, 58), (126, 80), (124, 116), (124, 156), (118, 178), (120, 196), (122, 250, 'c')]

def _xf(pts, dx, dy, ang, flip=False, cx=64, cy=130):
    a = math.radians(ang); out = []
    for p in pts:
        x, y = (128 - p[0] if flip else p[0]) - cx, p[1] - cy
        out.append((cx + x * math.cos(a) - y * math.sin(a) + dx, cy + x * math.sin(a) + y * math.cos(a) + dy) + tuple(p[2:]))
    return out

def _glove_marks(dx, dy, ang, flip):
    T = lambda pts: ' '.join(f'{"M" if i == 0 else "L"}{x:.1f} {y:.1f}' for i, (x, y, *_) in enumerate(_xf(pts, dx, dy, ang, flip)))
    seams = (line(T([(58, 92), (58, 120)]), '#000', .3, 2) + line(T([(82, 92), (82, 118)]), '#000', .3, 2) + line(T([(104, 98), (104, 122)]), '#000', .3, 2)
             + line(T([(40, 136), (52, 150)]), '#000', .2, 1.5))
    cuff = T([(28, 192), (122, 192)])
    ribs = ''.join(line(T([(x, 196), (x, 248)]), '#000', .2, 2.4) + line(T([(x + 3, 196), (x + 3, 248)]), '#fff', .14, 1.6) for x in range(32, 120, 7))
    # little embroidered heart on the back of the hand
    hx, hy = _xf([(76, 150)], dx, dy, ang, flip)[0]
    heart = (f'<path d="M{hx} {hy+6} C{hx-10} {hy-1} {hx-6} {hy-9} {hx} {hy-4} C{hx+6} {hy-9} {hx+10} {hy-1} {hx} {hy+6} Z" '
             f'fill="none" stroke="#fff" stroke-opacity=".55" stroke-width="1.6" stroke-dasharray="2 1.5"/>')
    return seams + line(cuff, '#000', .3, 3) + line(cuff, '#fff', .15, 1) + ribs + heart

def gloves(color):
    """Pair of soft knit gloves with rib cuffs and a tiny stitched heart; the left one tucked behind."""
    W, H = 280, 280
    back_p = _xf(_G, 108, 2, 14, flip=True)
    front_p = _xf(_G, 30, 14, -10)
    bk, fr = smooth(back_p), smooth(front_p)
    base = f'{bk} {fr}'
    ko = .5 if lum(color) < .08 else 1   # knit reads too loud on black
    texture = f'<rect width="{W}" height="{H}" fill="url(#stock)" opacity="{ko}"/>'
    shade = (_glove_marks(108, 2, 14, True) + K(bk, .18, 12) + K('M200 40 Q230 140 214 250', .35, 12, 16))
    front = (f'<clipPath id="gf"><path d="{fr}"/></clipPath>' + raised(fr, color, .5, .14)
             + f'<g clip-path="url(#gf)"><rect width="{W}" height="{H}" fill="url(#stock)" opacity="{ko}"/><rect width="{W}" height="{H}" filter="url(#grainW)"/>'
             + f'<path d="{fr}" fill="none" stroke="#000" stroke-opacity=".3" stroke-width="14" filter="url(#b6)"/>'
             + Wt('M70 60 Q80 140 70 210', .22, 12, 30) + K('M130 60 Q140 160 128 260', .25, 12, 16) + K('M40 228 L160 236', .2, 6, 30)
             + _glove_marks(30, 14, -10, False) + '</g>')
    light = Wt('M150 40 Q170 120 162 200', .15, 12, 20)
    return _outline_under_detail(piece(W, H, base, color, (.14, .28), shade, light, texture, front, grain=(1.4, .3), name='gloves'), base, [fr])


def _outline_under_detail(svg, base, fronts):
    """Move piece()'s silhouette outline below the detail layer so back parts' edges don't cut across the raised front parts."""
    i = svg.rindex('<path d="' + base + '" fill="none" stroke="#000"')
    tail = svg[i:]; outline = tail[:tail.index('/>') + 2]
    svg = svg[:i] + svg[i + len(outline):]
    edge = ''.join(line(d, '#000', .14, 1) for d in fronts)
    return svg.replace('<g id="detail">', outline + '<g id="detail">', 1).replace('</svg>', f'<g>{edge}</g></svg>')
