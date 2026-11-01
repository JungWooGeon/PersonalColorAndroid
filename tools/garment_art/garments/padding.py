from kit.art_kit import *
from garments.vest import puff, zip_line


def padding(color):
    W, H = 440, 350
    C = W // 2
    left = [(C, 50, 'c'), (172, 44, 'c'), (132, 52), (98, 64), (76, 86), (66, 100, 'c'), (56, 120), (58, 140, 'c'), (48, 160), (52, 180, 'c'),
            (41, 200), (46, 220, 'c'), (35, 240), (41, 260, 'c'), (32, 279), (40, 297, 'c'),
            (48, 297, 'c'), (50, 326, 'c'), (100, 328, 'c'), (100, 299, 'c'), (107, 299, 'c'), (111, 279), (108, 260, 'c'), (115, 240), (113, 220, 'c'),
            (120, 200), (118, 184), (124, 170, 'c'), (124, 182), (120, 200), (126, 220, 'c'), (119, 240), (125, 260, 'c'), (118, 279), (124, 298, 'c'),
            (172, 303), (C, 305, 'c')]
    base = smooth(sym(left, W))
    collar = smooth([(168, 20), (C, 15), (272, 20), (280, 46), (284, 68), (252, 78), (C, 80), (188, 78), (156, 68), (160, 46)])
    cs, cl = puff([18, 47, 78], 150, 290, .3)
    collar_g = (f'<path d="{collar}" fill="#000" opacity=".35" transform="translate(0 4)" filter="url(#b3)"/><path d="{collar}" fill="{color}"/>'
                f'<clipPath id="cc"><path d="{collar}"/></clipPath><g clip-path="url(#cc)">{cs}{cl}'
                f'{K("M162 24 L162 74", .35, 6, 12)}{K("M278 24 L278 74", .4, 6, 12)}{K("M170 76 Q220 82 270 76", .35, 3, 8)}</g>'
                f'<path d="M170 21 Q{C} 31 270 21 Q{C} 13 170 21 Z" fill="#000" opacity=".5"/>'
                f'<path d="{collar}" fill="none" stroke="#000" stroke-opacity=".12" stroke-width="1"/>')
    ps, pl = puff([48, 100, 140, 180, 220, 260, 300], 0, W)
    seamL = 'M134 54 Q116 104 124 170'
    seamR = f'M{W-134} 54 Q{W-116} 104 {W-124} 170'
    cuffs = (f'<rect x="30" y="298" width="80" height="34" fill="url(#rib)"/><rect x="{W-110}" y="298" width="80" height="34" fill="url(#rib)"/>'
             + K('M46 300 L102 300', .45, 3, 6) + K(f'M{W-46} 300 L{W-102} 300', .45, 3, 6))
    shade = (ps + cuffs + K(seamL, .45, 6, 10) + K(seamR, .5, 6, 10)
             + K('M112 190 Q108 250 112 298', .45, 12, 14) + K(f'M{W-112} 190 Q{W-108} 250 {W-112} 298', .5, 12, 14)
             + K('M130 180 Q126 240 128 300', .35, 12, 12) + K(f'M{W-130} 180 Q{W-126} 240 {W-128} 300', .4, 12, 12)
             + K('M50 120 Q40 210 36 296', .25, 12, 12) + K(f'M{W-50} 120 Q{W-40} 210 {W-36} 296', .32, 12, 12)
             + K('M216 80 L216 302', .22, 6, 8) + K('M270 110 Q290 200 280 300', .14, 22, 34))
    light = (pl + Wt('M170 100 Q176 190 166 290', .16, 22, 44) + Wt('M76 120 Q62 200 56 280', .14, 12, 16)
             + ''.join(Wt(f'M{x} {y+5} Q{x+4} {y} {x+22} {y-1}', .4, 1, 2) for x, y in ((146, 115), (142, 155), (140, 194), (140, 233), (140, 272), (62, 155), (54, 233))))
    cord = lambda x, s: (f'<path d="M{x} 300 Q{x-2*s} 318 {x-4*s} 330" fill="none" stroke="#1E1E1E" stroke-width="2.2" stroke-linecap="round"/>'
                         f'<rect x="{x-4*s-4}" y="322" width="8" height="12" rx="3.5" fill="#000" opacity=".35" filter="url(#b1)" transform="rotate({8*s} {x-4*s} 328)"/>'
                         f'<rect x="{x-4*s-4}" y="320" width="8" height="12" rx="3.5" fill="#C9CCD1" transform="rotate({8*s} {x-4*s} 326)"/>'
                         f'<rect x="{x-4*s-2.5}" y="322" width="2" height="7" rx="1" fill="#fff" opacity=".7" transform="rotate({8*s} {x-4*s} 326)"/>')
    detail = (collar_g + zip_line(18, 303, C)
              + stitch('M138 56 Q120 106 128 168', '#000', .25, 1, '3 2') + stitch(f'M{W-138} 56 Q{W-120} 106 {W-128} 168', '#000', .25, 1, '3 2')
              + line('M150 226 L160 268', '#000', .5, 3) + line(f'M{W-150} 226 L{W-160} 268', '#000', .5, 3)
              + cord(150, 1) + cord(W - 150, -1))
    return piece(W, H, base, color, (.18, .3), shade, light, '', detail, grain=(1.6, .18), name='padding')
