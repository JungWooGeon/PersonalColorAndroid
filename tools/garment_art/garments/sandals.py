from kit.art_kit import *
import math


def band(pts, w):
    """Closed strap shape of width w around a centreline (square ends)."""
    n = len(pts); L, R = [], []
    for i, (x, y) in enumerate(pts):
        ax, ay = pts[max(i - 1, 0)]; bx, by = pts[min(i + 1, n - 1)]
        dx, dy = bx - ax, by - ay; m = math.hypot(dx, dy) or 1
        nx, ny = -dy / m * w / 2, dx / m * w / 2
        c = ('c',) if i in (0, n - 1) else ()
        L.append((x + nx, y + ny) + c); R.append((x - nx, y - ny) + c)
    P = L + R[::-1]
    area = sum(P[i][0] * P[i - 1][1] - P[i - 1][0] * P[i][1] for i in range(len(P)))
    return smooth(P if area < 0 else P[::-1])   # clockwise like the footbed, so nonzero fill unions instead of punching holes


def sandals(color):
    """Strappy flat sandal: wide toe band, crossover instep strap, thin ankle-strap ring with gold buckle, low block heel."""
    W, H = 380, 200
    sole = smooth([(8, 172, 'c'), (298, 164, 'c'), (300, 176, 'c'), (298, 188, 'c'), (252, 188, 'c'), (248, 181, 'c'), (12, 182, 'c'), (7, 177)])
    bed = smooth([(8, 172, 'c'), (100, 168), (298, 162, 'c'), (298, 166, 'c'), (100, 172), (8, 176, 'c')])
    toe = band([(18, 171), (34, 152), (62, 143), (90, 148), (106, 168)], 17)
    toe_far = band([(40, 160), (64, 150), (92, 158)], 5)
    cross = band([(108, 168), (130, 146), (158, 130), (186, 140), (210, 165)], 10)
    ring = ('M226 100 A36 10 0 1 1 298 100 A36 10 0 1 1 226 100 Z '
            'M233 100 A29 5 0 1 0 291 100 A29 5 0 1 0 233 100 Z')
    counter = smooth([(268, 165, 'c'), (276, 132), (284, 106, 'c'), (300, 104, 'c'), (300, 132), (298, 163, 'c')])
    o = ' '.join([bed, toe, toe_far, cross, ring, counter])
    gold = '#C8B48A'
    detail = (f'<rect x="221" y="92" width="11" height="17" rx="2.5" fill="#000" opacity=".3" filter="url(#b1)" transform="translate(1 2)"/>'
              f'<rect x="221" y="92" width="11" height="17" rx="2.5" fill="none" stroke="{gold}" stroke-width="2.6"/>'
              + line('M226.5 92 L226.5 109', gold, 1, 2) + line('M223.5 95 L223.5 100', '#FFF3C8', .9, 1)
              + ''.join(f'<circle cx="{x}" cy="108.5" r="1.3" fill="#000" opacity=".45"/>' for x in (244, 254))
              + line('M8 175 L298 165', '#fff', .25, 1) + line('M250 183 L296 183', '#fff', .1, 1)
              + stitch('M28 164 Q40 148 62 146 Q86 148 98 166', '#fff', .2, 1, '2 2.5'))
    shade = (K(toe_far, .55, 1) + K('M226 100 A36 10 0 0 1 298 100', .5, 1, 4) + K('M8 172 L298 162', .4, 3, 6)
             + K('M262 160 Q272 130 280 106', .35, 3, 6) + K('M104 168 Q94 148 78 146', .3, 3, 6) + K('M206 164 Q190 140 166 134', .3, 3, 5))
    light = (Wt('M22 162 Q34 146 58 140', .75, 1, 3) + Wt('M114 160 Q132 140 156 127', .6, 1, 2) + Wt('M234 106 Q262 113 292 105', .6, 1, 2)
             + Wt('M288 110 Q292 132 294 160', .45, 1, 2.5) + Wt('M14 171 Q120 166 296 161', .4, 1, 1.5) + Wt('M44 146 Q64 140 84 145', .3, 3, 6))
    return shoe_svg(W, H, 'sandals', color, o, sole, detail, shade, light, back=(38, -20), sole_col='#2a2320')
