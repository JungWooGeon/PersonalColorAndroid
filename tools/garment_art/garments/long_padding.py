from kit.art_kit import *

SEAMS = list(range(108, 620, 34))   # quilting channel seams (shared by body and sleeves)


def _edge(xf, y0, y1, out, amp=3.2):
    """Puffy edge from y0 to y1: pinched at each seam, bulging between. out=-1 bulges left, +1 right."""
    ys = [y0] + [s for s in SEAMS if y0 < s < y1] + [y1]
    pts = []
    for a, b in zip(ys, ys[1:]):
        pts.append((xf(a) - out * amp * (a != y0), a))
        m = (a + b) / 2; pts.append((xf(m) + out * amp, m))
    return pts


def long_padding(color):
    """롱패딩: slim, waist-belted long puffer with a funnel neck, glossy nylon channels."""
    W, H = 400, 650
    lerp = lambda p, q: (lambda y: p[0] + (q[0] - p[0]) * (y - p[1]) / (q[1] - p[1]))
    body = lambda y: lerp((120, 180), (136, 300))(y) if y < 300 else lerp((136, 300), (106, 622))(y)
    left = ([(200, 18, 'c'), (156, 20, 'c'), (152, 66), (112, 76, 'c'), (92, 96)]
            + _edge(lerp((86, 108), (52, 414)), 108, 414, -1)
            + [(52, 420, 'c'), (102, 426, 'c')]
            + list(reversed(_edge(lerp((122, 182), (104, 420)), 182, 420, 1)))[:-1]
            + [(122, 180, 'c')]
            + _edge(body, 182, 618, -1)[1:]
            + [(106, 622, 'c'), (150, 626), (200, 624, 'c')])
    base = smooth(sym(left, W))
    collar_in = 'M160 22 Q200 34 240 22 Q200 14 160 22 Z'
    belt = 'M126 288 Q200 296 274 288 L274 312 Q200 320 126 312 Z'
    seams = ''.join(K(f'M0 {y} Q200 {y+6} 400 {y}', .5, 3, 5) for y in SEAMS) + K('M150 44 Q200 50 250 44', .4, 3, 4)
    puffs = ''.join(Wt(f'M0 {y+17} Q200 {y+23} 400 {y+17}', .16, 6, 12) for y in [74] + SEAMS[:-1])
    gloss = ''.join(Wt(f'M{144 + (y % 3) * 4} {y+15} Q160 {y+13} {172 + (y % 5) * 3} {y+16}', .22, 3, 4) for y in SEAMS[:-1])
    gloss += ''.join(Wt(f'M{90 - (y - 108) * .11} {y+15} l14 -1', .2, 3, 4) for y in SEAMS if y < 400)
    fabric = (shape(collar_in, '#000', .5) + raised(belt, color, .4, .16))
    zip_ = (line('M200 24 L200 288', '#2b2b2d', 1, 4) + line('M200 312 L200 622', '#2b2b2d', 1, 4)
            + stitch('M200 24 L200 288', '#C9CCD0', .9, 2.4, '1.6 1.6') + stitch('M200 312 L200 622', '#C9CCD0', .9, 2.4, '1.6 1.6'))
    puller = ('<rect x="196" y="26" width="8" height="18" rx="3" fill="#000" opacity=".35" transform="translate(1 2)" filter="url(#b1)"/>'
              '<rect x="196" y="26" width="8" height="18" rx="3" fill="#B8BCC2"/><rect x="197.5" y="28" width="2" height="12" rx="1" fill="#fff" opacity=".8"/>')
    buckle = ('<rect x="186" y="286" width="28" height="30" rx="5" fill="#000" opacity=".35" transform="translate(1 2)" filter="url(#b1)"/>'
              '<rect x="186" y="286" width="28" height="30" rx="5" fill="none" stroke="#2A2A2C" stroke-width="5"/>'
              '<rect x="186" y="286" width="28" height="30" rx="5" fill="none" stroke="#fff" stroke-opacity=".25" stroke-width="1"/>'
              '<line x1="200" y1="288" x2="200" y2="314" stroke="#2A2A2C" stroke-width="3"/>')
    detail = (zip_ + puller + buckle
              + line('M142 352 L152 414', '#000', .3, 2.4) + line('M258 352 L248 414', '#000', .3, 2.4) + line('M144 351 L154 413', '#fff', .1, 1) + line('M260 351 L250 413', '#fff', .1, 1)
              + stitch('M130 292 Q200 300 270 292', '#000', .2, 1, '3 3') + stitch('M130 308 Q200 316 270 308', '#000', .2, 1, '3 3'))
    shade = (seams + K('M120 184 Q130 260 132 300', .45, 12, 12) + K(f'M{W-120} 184 Q{W-130} 260 {W-132} 300', .5, 12, 12)
             + K('M84 120 Q70 270 58 410', .25, 12, 10) + K(f'M{W-84} 120 Q{W-70} 270 {W-58} 410', .3, 12, 10)
             + K('M126 320 Q200 332 274 320', .35, 6, 10) + K('M126 282 Q200 288 274 282', .3, 6, 8)
             + K('M262 330 Q276 470 290 618', .25, 12, 16) + K('M200 70 L200 620', .2, 6, 10) + K('M156 24 L156 66', .3, 6, 8) + K('M244 24 L244 66', .35, 6, 8))
    light = puffs + gloss + Wt('M150 120 Q160 220 150 280', .2, 22, 34) + Wt('M70 110 Q58 240 54 380', .18, 12, 10) + Wt('M170 28 Q176 44 172 60', .3, 6, 8)
    return piece(W, H, base, color, (.2, .32), shade, light, '', detail, fabric=fabric, grain=(1.8, .15), name='long_padding')
