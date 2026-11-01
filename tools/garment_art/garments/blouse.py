from kit.art_kit import *
import math

def _ruffle(cx, cy, rx, ry, depth, waves, a0=math.pi, a1=0, n=60):
    """Ruffle band along a half-ellipse neckline: inner edge smooth, outer edge scalloped."""
    inner, outer = [], []
    for i in range(n + 1):
        t = a0 + (a1 - a0) * i / n
        c, s = math.cos(t), math.sin(t)
        inner.append((cx + rx * c, cy + ry * s))
        d = depth * (.78 + .22 * abs(math.sin(waves * math.pi * i / n)))
        outer.append((cx + (rx + d) * c, cy + (ry + d) * s))
    return 'M' + ' L'.join(f'{x:.1f} {y:.1f}' for x, y in outer + inner[::-1]) + ' Z'

def blouse(color):
    """Romantic puff-sleeve blouse: frill collar, balloon sleeves with ruffle cuffs, pearl buttons."""
    W, H = 400, 400
    left = [(200, 52, 'c'), (166, 48, 'c'), (134, 62, 'c'), (112, 56), (86, 68), (68, 98), (60, 140), (64, 186), (68, 214), (64, 266), (62, 318, 'c'),
            (42, 362), (40, 370, 'c'), (52, 368), (64, 374), (78, 368), (92, 374), (104, 368, 'c'), (100, 360), (98, 322, 'c'), (104, 266), (114, 206), (130, 164, 'c'),
            (126, 220), (124, 280), (128, 340, 'c'), (200, 346, 'c')]
    base = smooth(sym(left, W))
    neck = 'M164 48 Q200 56 236 48 Q234 84 200 88 Q166 84 164 48 Z'
    ruf = _ruffle(200, 48, 38, 40, 24, 11)
    tuck = lambda x: line(f'M{x} 94 L{x} 342', '#000', .2, 1.2) + line(f'M{x+1.5} 94 L{x+1.5} 342', '#fff', .1, 1)
    fabric = (shape(neck, '#000', .38) + K(neck, .25, 6)
              + tuck(176) + tuck(184) + tuck(216) + tuck(224) + line('M200 92 L200 344', '#000', .25, 1.2)
              + raised(ruf, color, .35, .14))
    gathers = ''.join(K(f'M{x} {y0} Q{x+dx} {y0+30} {x+dx*1.6} {y0+56}', .2, 6, 5) for x, y0, dx in
                      ((128, 62, -6), (124, 74, -10), (126, 92, -12), (126, 112, -10), (108, 58, -4)))
    gathers_r = ''.join(K(f'M{W-x} {y0} Q{W-x-dx} {y0+30} {W-x-dx*1.6} {y0+56}', .22, 6, 5) for x, y0, dx in
                        ((128, 62, -6), (124, 74, -10), (126, 92, -12), (126, 112, -10), (108, 58, -4)))
    wrist = lambda s: ''.join(K(f'M{x} 300 Q{x+(1 if s else -1)*2} 318 {x} 334', .3, 3, 2.4) for x in ((66, 76, 86, 96) if s else (W-66, W-76, W-86, W-96)))
    shade = (gathers + gathers_r + wrist(1) + wrist(0)
             + K('M128 170 Q124 260 128 340', .42, 12, 12) + K(f'M{W-128} 170 Q{W-124} 260 {W-128} 340', .48, 12, 12)
             + K('M70 130 Q62 160 68 200', .22, 12, 12) + K(f'M{W-70} 130 Q{W-62} 160 {W-68} 200', .28, 12, 12)
             + K('M66 210 Q70 240 98 250', .2, 6, 6) + K(f'M{W-66} 210 Q{W-70} 240 {W-98} 250', .24, 6, 6)
             + K('M42 362 L104 364 L98 330 L62 326 Z', .12, 3) + K(f'M{W-42} 362 L{W-104} 364 L{W-98} 330 L{W-62} 326 Z', .16, 3)
             + K('M150 240 Q158 300 150 360', .1, 22, 26) + K('M140 342 Q200 334 260 342', .16, 6, 6))
    light = (Wt('M88 80 Q70 120 80 170', .3, 12, 14) + Wt(f'M{W-92} 84 Q{W-76} 120 {W-84} 160', .14, 12, 12)
             + Wt('M156 110 Q166 200 156 300', .2, 22, 34) + Wt('M70 280 Q68 310 70 330', .14, 6, 6)
             + Wt('M54 346 L60 364 M74 344 L80 366 M88 344 L94 364', .2, 3, 2))
    detail = ''.join(pearl(200, y, 3.8) for y in (118, 160, 202, 244, 286, 326))
    return piece(W, H, base, color, (.18, .26), shade, light, '', detail, fabric=fabric, grain=(1.7, .16), name='blouse')
