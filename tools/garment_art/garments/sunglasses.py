from kit.art_kit import *

def _cw(pts, cw=True):
    """Force winding so frame parts union and lens holes cut out under the nonzero rule."""
    a = sum(pts[i][0] * pts[(i + 1) % len(pts)][1] - pts[(i + 1) % len(pts)][0] * pts[i][1] for i in range(len(pts)))
    return pts if (a > 0) == cw else list(reversed(pts))

def sunglasses(color):
    """Chunky soft cat-eye acetate sunglasses (Gentle Monster mood), front view, smoky brown lenses."""
    W, H = 300, 130
    outer = [(146, 46), (130, 32), (84, 28), (36, 26), (12, 22), (14, 44), (28, 80), (58, 106), (106, 108), (136, 92), (148, 66)]
    hole = [(134, 48), (120, 39), (84, 37), (40, 36), (26, 36), (30, 60), (44, 86), (66, 98), (104, 98), (126, 86), (136, 64)]
    bridge = [(138, 40, 'c'), (150, 33), (162, 40, 'c'), (162, 56, 'c'), (150, 47), (138, 56, 'c')]
    oR, hR = mir(outer, W), mir(hole, W)
    base = ' '.join([smooth(_cw(outer)), smooth(_cw(oR)), smooth(_cw(bridge)), smooth(_cw(hole, False)), smooth(_cw(hR, False))])
    lens = lambda h, i: (f'<linearGradient id="lg{i}" x1="0" y1="0" x2=".3" y2="1"><stop offset="0" stop-color="#15110f"/><stop offset="1" stop-color="#5a4a40"/></linearGradient>'
                         f'<path d="{smooth(h)}" fill="url(#lg{i})" opacity=".94"/>')
    lens_fx = lambda h, s: (f'<clipPath id="lc{s}"><path d="{smooth(h)}"/></clipPath><g clip-path="url(#lc{s})">'
                            f'<path d="{smooth(h)}" fill="none" stroke="#000" stroke-opacity=".55" stroke-width="8" filter="url(#b3)"/></g>')
    glare = (Wt('M40 52 Q60 42 92 42', .5, 3, 7) + Wt('M44 50 Q62 44 84 44', .8, 1, 2) + Wt('M104 92 L122 72', .2, 3, 6)
             + Wt(f'M{W-118} 50 Q{W-96} 42 {W-70} 44', .35, 3, 6) + Wt(f'M{W-112} 48 Q{W-96} 44 {W-80} 45', .6, 1, 1.6))
    pin = lambda x: (f'<circle cx="{x}" cy="38" r="2.6" fill="#B8B8B8"/><circle cx="{x-.8}" cy="37.2" r="1" fill="#fff" opacity=".9"/>')
    detail = lens(hole, 0) + lens(hR, 1) + lens_fx(hole, 0) + lens_fx(hR, 1) + glare + pin(26) + pin(W - 26)
    shade = (K('M30 92 Q62 116 112 112 Q140 96 148 66', .3, 3, 5) + K(f'M{W-30} 92 Q{W-62} 116 {W-112} 112 Q{W-140} 96 {W-148} 66', .34, 3, 5)
             + K('M28 42 Q80 40 132 46', .35, 1, 3) + K(f'M{W-28} 42 Q{W-80} 40 {W-132} 46', .35, 1, 3) + K('M140 52 Q150 46 160 52', .4, 1, 3))
    light = (Wt('M22 32 Q70 24 130 32', .55, 3, 5) + Wt('M30 29 Q70 25 110 28', .9, 1, 1.8)
             + Wt(f'M{W-22} 32 Q{W-70} 24 {W-130} 32', .4, 3, 5) + Wt(f'M{W-40} 29 Q{W-80} 25 {W-110} 28', .7, 1, 1.6)
             + Wt('M18 44 Q20 70 34 90', .4, 3, 3) + Wt('M142 37 Q150 34 158 37', .6, 1, 1.5))
    # thin piece: the kit's 16px edge-darkening would grey out the whole part, so narrow it
    return piece(W, H, base, color, (.18, .2), shade, light, '', detail, grain=(1.8, .1), name='sunglasses').replace('stroke-width="16" filter="url(#b6)"', 'stroke-width="7" filter="url(#b3)"')
