from kit.art_kit import *


def slippers(color):
    """Puffy padded slide (quilted channels), chunky flat sole, tonal footbed."""
    W, H = 380, 200
    sole = smooth([(10, 156, 'c'), (300, 150, 'c'), (304, 166), (300, 182, 'c'), (14, 184, 'c'), (8, 170)])
    bed = smooth([(10, 156, 'c'), (120, 152), (300, 146, 'c'), (302, 154, 'c'), (120, 160), (12, 164, 'c')])
    puff = smooth([(14, 160, 'c'), (18, 134), (40, 110), (80, 94), (128, 92), (170, 104), (196, 128), (206, 156, 'c')])
    o = bed + ' ' + puff
    xs = [52, 92, 132, 170]
    creases = ''.join(K(f'M{x} {100 if 70 < x < 150 else 108} Q{x + (x - 110) * .08} 130 {x + (x - 110) * .12} 158', .55, 3, 4) for x in xs)
    puffs = ''.join(Wt(f'M{a + 8} 110 Q{(a + b) / 2} 102 {b - 8} 110 Q{(a + b) / 2 + 2} 134 {b - 10} 150', .22, 6, 10)
                    for a, b in zip([22] + xs, xs + [204]))
    detail = (line('M10 166 Q150 162 302 158', '#fff', .18, 1.2) + line('M12 176 L300 172', '#000', .25, 1)
              + ''.join(line(f'M{x} 166 L{x} 183', '#fff', .05, 3) for x in range(30, 300, 18)))
    shade = (creases + K('M20 156 Q110 150 204 156', .45, 6, 10) + K('M190 112 Q206 136 204 158', .4, 6, 10)
             + K('M206 154 L300 150', .35, 3, 4))
    light = (puffs + Wt('M30 122 Q70 96 124 94 Q160 96 184 112', .6, 3, 6) + Wt('M44 112 Q76 98 112 96', .8, 1, 2.5)
             + Wt('M210 150 L296 146', .3, 1, 1.5))
    return shoe_svg(W, H, 'slippers', color, o, sole, detail, shade, light, back=(40, -20), sole_col='#2a2624')
