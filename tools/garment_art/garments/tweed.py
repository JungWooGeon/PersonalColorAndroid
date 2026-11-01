from kit.art_kit import *
import random

def _loops(pid, seed, n=80, T=48):
    """Pattern tile of tiny boucle loops in three tones (bright, soft white, dark), black/white alpha only."""
    r = random.Random(seed); s = ''
    for _ in range(n):
        x, y = r.uniform(0, T), r.uniform(0, T); a = r.uniform(0, 180)
        rx, ry = r.uniform(.8, 1.6), r.uniform(.5, 1.0)
        col, op = r.choice([('#fff', .55), ('#fff', .3), ('#000', .45), ('#000', .3), ('#fff', .4)])
        s += (f'<ellipse cx="{x:.1f}" cy="{y:.1f}" rx="{rx:.1f}" ry="{ry:.1f}" transform="rotate({a:.0f} {x:.1f} {y:.1f})" '
              f'fill="none" stroke="{col}" stroke-opacity="{op}" stroke-width="{r.uniform(.5, .9):.2f}"/>')
    return f'<pattern id="{pid}" width="{T}" height="{T}" patternUnits="userSpaceOnUse">{s}</pattern>'

def tweed(color):
    W, H = 400, 340
    left = [(200, 30, 'c'), (162, 28, 'c'), (108, 46, 'c'), (90, 60), (78, 100), (68, 170), (58, 250), (50, 316, 'c'), (96, 324, 'c'), (102, 250),
            (110, 170), (118, 124, 'c'), (120, 200), (118, 268, 'c'), (200, 272, 'c')]
    base = smooth(sym(left, W))
    neck = 'M162 28 Q166 84 200 88 Q234 84 238 28 Q200 38 162 28 Z'
    # fine multi-tone boucle: tiny high-frequency flecks + looped yarn tiles + faint horizontal weave
    tex = ('<defs><filter id="twW"><feTurbulence type="fractalNoise" baseFrequency=".85" numOctaves="2" seed="4"/>'
           '<feColorMatrix values="0 0 0 0 1 0 0 0 0 1 0 0 0 0 1 4 0 0 0 -2.15"/></filter>'
           '<filter id="twK"><feTurbulence type="fractalNoise" baseFrequency=".75" numOctaves="2" seed="9"/>'
           '<feColorMatrix values="0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 -4 0 0 0 1.95"/></filter>'
           + _loops('lpA', 1) + _loops('lpB', 2, 70, 54)
           + '<pattern id="weave" width="6" height="4" patternUnits="userSpaceOnUse"><rect width="6" height="1.2" fill="#000" opacity=".1"/>'
           '<rect y="2" width="3" height="1" fill="#fff" opacity=".1"/></pattern></defs>'
           f'<rect width="{W}" height="{H}" fill="url(#weave)"/>'
           f'<rect width="{W}" height="{H}" filter="url(#twW)" opacity=".7"/><rect width="{W}" height="{H}" filter="url(#twK)" opacity=".6"/>'
           f'<rect width="{W}" height="{H}" fill="url(#lpA)"/><rect width="{W}" height="{H}" fill="url(#lpB)" transform="translate(17 29)"/>')

    def trim(d, side=1):
        """braided trim with frayed fringe edges"""
        off = lambda h, o: h.replace('stroke-dasharray', f'stroke-dashoffset="{o}" stroke-dasharray').replace('stroke-linecap="round"', 'stroke-linecap="butt"')
        return (stitch(d, '#fff', .3, 11, '.6 1.4') + off(stitch(d, '#000', .25, 11, '.6 1.4'), 1)   # frayed edges
                + line(d, '#000', .22, 7) + off(stitch(d, '#fff', .32, 5, '1.4 1.4'), 0) + off(stitch(d, '#000', .22, 5, '1.4 1.4'), 1.4)
                + stitch(d, '#fff', .35, 1, '1 2'))

    def fringe(x0, y0, x1, y1, n, ln=8):
        """loose yarn fringe hanging below an edge (garment colour + B/W shading)"""
        r = random.Random(x0); s = ''
        for i in range(n):
            t = i / (n - 1); x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t; l = ln * r.uniform(.7, 1.2); dx = r.uniform(-1.5, 1.5)
            d = f'M{x:.1f} {y-2:.1f} L{x+dx:.1f} {y+l:.1f}'
            s += line(d, color, 1, 1.1) + line(d, '#fff' if i % 3 else '#000', .22, 1.1)
        return s

    pocket = lambda x, y, w, h: f'M{x} {y} L{x+w} {y} L{x+w} {y+h-8} Q{x+w} {y+h} {x+w-8} {y+h} L{x+8} {y+h} Q{x} {y+h} {x} {y+h-8} Z'
    fr = fringe(122, 269, 278, 269, 80, 6) + fringe(52, 318, 96, 324, 22, 5) + fringe(W - 52, 318, W - 96, 324, 22, 5)
    detail = (fr + shape(neck, '#000', .5) + K(neck, .3, 6)
              + trim('M162 30 Q166 84 200 88 Q234 84 238 30') + trim('M200 88 L200 268') + trim('M121 265 L279 265')
              + trim('M52 306 Q74 311 96 314') + trim(f'M{W-52} 306 Q{W-74} 311 {W-96} 314')
              + trim('M132 198 L182 198') + trim('M218 198 L268 198')
              + ''.join(button(212, y, 7, metal=True) for y in (118, 168, 218)) + button(157, 196, 5.5, metal=True) + button(243, 196, 5.5, metal=True))
    shade = (K('M118 128 Q122 200 118 266', .45, 12, 12) + K(f'M{W-118} 128 Q{W-122} 200 {W-118} 266', .5, 12, 12)
             + K('M90 110 Q80 210 72 310', .25, 12, 10) + K(f'M{W-90} 110 Q{W-80} 210 {W-72} 310', .3, 12, 10))
    light = Wt('M150 100 Q160 160 150 230', .2, 22, 40) + Wt('M76 90 Q66 180 58 270', .18, 12, 12)
    fabric = raised(pocket(132, 196, 50, 46), color, .25, .05) + raised(pocket(218, 196, 50, 46), color, .25, .05)
    return piece(W, H, base, color, (.14, .28), shade, light, tex, detail, fabric=fabric, grain=(1.3, .35), name='tweed')
