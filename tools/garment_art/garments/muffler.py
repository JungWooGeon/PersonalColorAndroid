from kit.art_kit import *

def _fringe(x0, x1, y, n, s=2.5):
    out = []
    for i in range(n):
        x = x0 + i * (x1 - x0) / (n - 1); dx = s * (i % 3 - 1); L = 30 + (i * 7) % 9
        out.append(f'M{x-3:.1f} {y} L{x+3:.1f} {y} L{x+3+dx:.1f} {y+L} Q{x+dx:.1f} {y+L+4} {x-3+dx:.1f} {y+L} Z')
    return ' '.join(out)

def _ribs(x0, x1, y0, y1, step=12):
    return ''.join(f'<rect x="{x}" y="{y0}" width="{step*.45}" height="{y1-y0}" fill="#fff" opacity=".14"/>'
                   f'<rect x="{x+step*.55}" y="{y0}" width="{step*.4}" height="{y1-y0}" fill="#000" opacity=".22" filter="url(#b1)"/>' for x in range(x0, x1, step))

def muffler(color):
    """Chunky rib-knit muffler worn-flat: wrapped around the neck, two fringed tails hanging (one crossing over)."""
    W, H = 260, 450
    band = smooth([(30, 40, 'c'), (58, 34, 'c'), (80, 80), (130, 104), (180, 80), (202, 34, 'c'), (230, 40, 'c'), (232, 96), (200, 150), (130, 172), (60, 150), (28, 96)])
    tailL = smooth([(38, 90, 'c'), (110, 112, 'c'), (112, 220), (108, 330), (114, 396, 'c'), (40, 398, 'c'), (44, 300), (36, 190)])
    tailR = smooth([(116, 132, 'c'), (186, 108, 'c'), (198, 200), (204, 270), (212, 342, 'c'), (138, 352, 'c'), (132, 270), (124, 200)])
    frL, frR = _fringe(44, 110, 396, 8), _fringe(142, 210, 344, 8)
    base = ' '.join([band, tailL, tailR, frL, frR])
    curved = ''.join(f'<path d="M{24+k} {40+k*.2} Q{130} {110+k*1.6} {236-k} {40+k*.2}" fill="none" stroke="{c}" stroke-opacity="{a}" stroke-width="{w}"/>'
                     for k0 in range(0, 72, 9) for k, c, a, w in ((k0, '#fff', .14, 4), (k0 + 4.5, '#000', .22, 3.5)))
    tex = curved + ''.join(f'<path d="M{x} {y} q3 4 6 0" fill="none" stroke="#fff" stroke-opacity=".1" stroke-width="1.3"/>' for x in range(30, 236, 12) for y in range(40, 170, 6))
    def tail(d, x0, x1, y0, y1, id_, fr):
        return (f'<clipPath id="{id_}"><path d="{d}"/></clipPath>' + raised(d, color, .45, .1)
                + f'<g clip-path="url(#{id_})">{_ribs(x0, x1, y0, y1)}<rect width="{W}" height="{H}" filter="url(#grainW)"/>'
                + K(f'M{x0+4} {y0} L{x0+8} {y1}', .3, 12, 14) + K(f'M{x1-4} {y0} L{x1} {y1}', .35, 12, 14)
                + Wt(f'M{(x0+x1)/2} {y0+20} L{(x0+x1)/2+4} {y1-20}', .2, 22, 30) + K(f'M{x0} {y0+4} L{x1} {y0+10}', .35, 6, 12)
                + K(f'M{x0} {y1-4} L{x1} {y1-4}', .3, 3, 10) + '</g>'
                + f'<path d="{fr}" fill="{color}"/><path d="{fr}" fill="#000" opacity=".16"/>')
    detail = tail(tailL, 34, 120, 88, 400, 'mtl', frL) + tail(tailR, 118, 216, 104, 352, 'mtr', frR)
    shade = K('M40 46 Q130 140 222 46', .35, 6, 10) + K('M40 120 Q130 190 222 120', .25, 12, 20)
    light = Wt('M50 80 Q130 140 210 80', .2, 12, 16)
    return _outline_under_detail(piece(W, H, base, color, (.14, .26), shade, light, tex, detail, grain=(1.4, .3), name='muffler'), base, [tailL, tailR, frL, frR])


def _outline_under_detail(svg, base, fronts):
    """Move piece()'s silhouette outline below the detail layer so back parts' edges don't cut across the raised front parts."""
    i = svg.rindex('<path d="' + base + '" fill="none" stroke="#000"')
    tail = svg[i:]; outline = tail[:tail.index('/>') + 2]
    svg = svg[:i] + svg[i + len(outline):]
    edge = ''.join(line(d, '#000', .14, 1) for d in fronts)
    return svg.replace('<g id="detail">', outline + '<g id="detail">', 1).replace('</svg>', f'<g>{edge}</g></svg>')
