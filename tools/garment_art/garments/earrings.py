from kit.art_kit import *

def _heart(cx, cy, s):
    p = [(cx, cy - .30 * s, 'c'), (cx - .12 * s, cy - .48 * s), (cx - .34 * s, cy - .54 * s), (cx - .52 * s, cy - .36 * s),
         (cx - .50 * s, cy - .04 * s), (cx - .28 * s, cy + .26 * s), (cx, cy + .52 * s, 'c')]
    return p + [(2 * cx - x, y) + tuple(r) for x, y, *r in reversed(p[1:-1])]

def earrings(color):
    """Puffy enamel heart drops on gold huggie hoops — a pair laid flat."""
    W, H = 200, 200
    hearts = [(60, 128, 74, -8), (142, 120, 74, 7)]
    paths = [(smooth(_heart(cx, cy, s)), cx, cy, s, r) for cx, cy, s, r in hearts]
    # rotate each heart about its own centre by baking the rotation into the points
    import math
    def rot(pts, cx, cy, deg):
        a = math.radians(deg); c, s_ = math.cos(a), math.sin(a)
        return [(cx + (x - cx) * c - (y - cy) * s_, cy + (x - cx) * s_ + (y - cy) * c) + tuple(r) for x, y, *r in pts]
    ds = [smooth(rot(_heart(cx, cy, s), cx, cy, r)) for cx, cy, s, r in hearts]
    base = ' '.join(ds)
    g, gd, gl = '#C9A85C', '#7a5f28', '#FFF3C8'
    def hoop(cx, top, r):
        x, y = cx, top
        return (f'<circle cx="{x}" cy="{y}" r="{r}" fill="none" stroke="#000" stroke-opacity=".3" stroke-width="6" transform="translate(1 2)" filter="url(#b1)"/>'
                f'<circle cx="{x}" cy="{y}" r="{r}" fill="none" stroke="{g}" stroke-width="5.5"/>'
                f'<circle cx="{x}" cy="{y}" r="{r+2.2}" fill="none" stroke="{gd}" stroke-opacity=".45" stroke-width="1"/>'
                f'<path d="M{x-r*.8} {y-r*.6} A{r} {r} 0 0 1 {x+r*.2} {y-r}" fill="none" stroke="{gl}" stroke-width="1.8" stroke-linecap="round"/>')
    def jump(x, y):
        return (f'<ellipse cx="{x}" cy="{y}" rx="3.2" ry="5" fill="none" stroke="{g}" stroke-width="2.4"/>'
                f'<path d="M{x-2} {y-3} Q{x-2.5} {y} {x-1.5} {y+2}" fill="none" stroke="{gl}" stroke-width="1"/>')
    detail = ''
    for (cx, cy, s, r) in hearts:
        import math
        a = math.radians(r); tx, ty = cx + (.30 * s) * math.sin(a), cy - .30 * s * math.cos(a)
        detail += hoop(tx - 2, ty - 34, 15) + jump(tx - 1, ty - 15) + f'<circle cx="{tx}" cy="{ty-4}" r="2.4" fill="{g}"/>'
    shade = ''.join(K(f'M{cx-.42*s} {cy+.02*s} Q{cx-.2*s} {cy+.42*s} {cx} {cy+.5*s} Q{cx+.3*s} {cy+.2*s} {cx+.48*s} {cy-.1*s}', .55, 6, 10) for cx, cy, s, r in hearts)
    shade += ''.join(K(f'M{cx-6} {cy-.34*s} L{cx} {cy-.26*s} L{cx+6} {cy-.34*s}', .4, 3, 4) for cx, cy, s, r in hearts)
    light = ''.join(Wt(f'M{cx-.34*s} {cy-.30*s} Q{cx-.42*s} {cy-.10*s} {cx-.30*s} {cy+.06*s}', .55, 3, 7)
                    + f'<ellipse cx="{cx-.28*s}" cy="{cy-.28*s}" rx="6" ry="3.6" fill="#fff" opacity=".9" transform="rotate(-35 {cx-.28*s} {cy-.28*s})" filter="url(#b1)"/>'
                    + Wt(f'M{cx+.14*s} {cy-.34*s} Q{cx+.3*s} {cy-.38*s} {cx+.38*s} {cy-.26*s}', .4, 3, 4)
                    + Wt(f'M{cx-.1*s} {cy+.0*s} Q{cx} {cy+.1*s} {cx+.1*s} {cy}', .15, 6, 14) for cx, cy, s, r in hearts)
    return piece(W, H, base, color, (.22, .34), shade, light, '', detail, grain=(1.8, .08), name='earrings')
