from kit.art_kit import *


def puff(ys, x0, x1, gloss=.26):
    """Quilted channels between seam ys: crisp seam, pinch shadow, rounded highlight per channel."""
    sh, li = '', ''
    for a, b in zip(ys, ys[1:]):
        h = b - a
        li += Wt(f'M{x0} {a + h * .36:.0f} L{x1} {a + h * .36:.0f}', gloss, 6, h * .32)
        li += Wt(f'M{x0} {a + h * .3:.0f} L{x1} {a + h * .3:.0f}', gloss * .8, 3, h * .08)
        sh += K(f'M{x0} {b - h * .12:.0f} L{x1} {b - h * .12:.0f}', .22, 6, h * .3)
    for y in ys[1:-1]:
        sh += K(f'M{x0} {y} L{x1} {y}', .5, 3, 6) + line(f'M{x0} {y} L{x1} {y}', '#000', .35, 1.2)
    return sh, li


def zip_line(y0, y1, cx=200):
    return (line(f'M{cx} {y0} L{cx} {y1}', '#000', .5, 4) + stitch(f'M{cx} {y0} L{cx} {y1}', '#C9CCD1', .9, 3, '1.2 1.6')
            + f'<rect x="{cx-4}" y="{y0+6}" width="8" height="14" rx="2" fill="#000" opacity=".35" filter="url(#b1)"/>'
            + f'<rect x="{cx-4}" y="{y0+4}" width="8" height="13" rx="2" fill="#C9CCD1"/>'
            + f'<path d="M{cx-3} {y0+16} L{cx+3} {y0+16} L{cx+2.5} {y0+34} Q{cx} {y0+37} {cx-2.5} {y0+34} Z" fill="#B6BAC0"/>'
            + f'<rect x="{cx-1.2}" y="{y0+20}" width="2.4" height="10" rx="1.2" fill="#000" opacity=".35"/>'
            + f'<rect x="{cx-2.4}" y="{y0+5}" width="2" height="9" rx="1" fill="#fff" opacity=".7"/>')


def vest(color):
    W, H = 400, 320
    seams = [140, 176, 212, 248]
    side = [(93, 140, 'c'), (86, 158), (93, 176, 'c'), (85, 194), (92, 212, 'c'), (84, 230), (91, 248, 'c'), (84, 268), (92, 288, 'c')]
    left = ([(200, 50, 'c'), (156, 44, 'c'), (128, 52), (122, 60, 'c'), (118, 86), (108, 116), (93, 140, 'c')] + side[1:]
            + [(128, 293), (164, 291), (200, 294, 'c')])
    base = smooth(sym(left, W))
    collar = smooth([(150, 22), (200, 17), (250, 22), (258, 46), (262, 66), (232, 76), (200, 78), (168, 76), (138, 66), (142, 46)])
    cs, cl = puff([20, 47, 76], 140, 260, .3)
    collar_g = (f'<path d="{collar}" fill="#000" opacity=".35" transform="translate(0 4)" filter="url(#b3)"/><path d="{collar}" fill="{color}"/>'
                f'<clipPath id="cc"><path d="{collar}"/></clipPath><g clip-path="url(#cc)">{cs}{cl}'
                f'{K("M144 24 L144 72", .35, 6, 12)}{K("M256 24 L256 72", .4, 6, 12)}{K("M150 74 Q200 80 250 74", .35, 3, 8)}</g>'
                f'<path d="M152 23 Q200 33 248 23 Q200 15 152 23 Z" fill="#000" opacity=".5"/>'
                f'<path d="{collar}" fill="none" stroke="#000" stroke-opacity=".12" stroke-width="1"/>')
    ps, pl = puff([50, 104] + seams + [294], 60, 340)
    arm = 'M122 60 Q120 90 108 116 Q102 130 93 140'
    shade = (ps + K(arm, .45, 6, 10) + K(f'M{W-122} 60 Q{W-120} 90 {W-108} 116 Q{W-102} 130 {W-93} 140', .5, 6, 10)
             + K('M88 150 Q82 220 86 290', .3, 12, 14) + K(f'M{W-88} 150 Q{W-82} 220 {W-86} 290', .38, 12, 14)
             + K('M196 70 L196 292', .25, 6, 8) + K('M240 120 Q260 200 250 290', .14, 22, 34))
    light = (pl + Wt('M150 110 Q156 190 146 280', .16, 22, 40)
             + ''.join(Wt(f'M{x} {y+5} Q{x+4} {y} {x+22} {y-1}', .4, 1, 2) for x, y in ((106, 156), (102, 192), (100, 229), (102, 266), (146, 84))))
    welt = lambda x: f'M{x} 222 L{x+10} 264'
    detail = (collar_g + zip_line(20, 292)
              + line(welt(132), '#000', .5, 3) + line('M134 221 L144 263', '#fff', .14, 1)
              + line('M268 222 L258 264', '#000', .5, 3)
              + stitch('M127 62 Q125 92 112 119 Q106 132 98 141', '#000', .25, 1, '3 2')
              + stitch(f'M{W-127} 62 Q{W-125} 92 {W-112} 119 Q{W-106} 132 {W-98} 141', '#000', .25, 1, '3 2'))
    return piece(W, H, base, color, (.18, .3), shade, light, '', detail, grain=(1.6, .18), name='vest')
