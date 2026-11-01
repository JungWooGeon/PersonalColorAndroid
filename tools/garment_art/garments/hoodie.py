from kit.art_kit import *

def hoodie(color):
    """Oversized 후드티: lay-flat hood with ribbed rim, white drawcords, kangaroo pocket, rib cuffs and hem."""
    W, H = 420, 440
    left = [(210, 10, 'c'), (176, 14), (152, 32), (144, 64), (134, 90), (98, 100, 'c'), (72, 138), (54, 216), (40, 296), (32, 350, 'c'),
            (40, 356, 'c'), (42, 398, 'c'), (84, 400, 'c'), (84, 358, 'c'), (92, 352, 'c'), (102, 274), (118, 204, 'c'), (112, 284),
            (106, 370, 'c'), (116, 378, 'c'), (118, 422, 'c'), (210, 426, 'c')]
    base = smooth(sym(left, W))
    op = 'M176 46 Q210 22 244 46 Q254 100 210 126 Q166 100 176 46 Z'
    rim = 'M210 126 Q166 100 176 46 Q210 22 244 46 Q254 100 210 126'
    hood = (shape(op, '#000', .26) + K('M180 50 Q210 30 240 50', .5, 6, 16) + K('M196 70 Q210 60 224 70 Q226 100 210 114 Q194 100 196 70 Z', .2, 12)
            + f'<path d="{rim}" fill="none" stroke="#000" stroke-opacity=".3" stroke-width="16" transform="translate(1 3)" filter="url(#b3)"/>'
            + f'<path d="{rim}" fill="none" stroke="{color}" stroke-width="14"/>'
            + Wt('M168 46 Q210 14 252 46', .2, 3, 4) + line(rim, '#000', .22, 1.2)
            + line('M162 50 Q156 92 210 136 Q264 92 258 50', '#000', .18, 1.5)
            + K('M152 40 Q140 70 150 100', .25, 6, 6) + K(f'M{W-152} 40 Q{W-140} 70 {W-150} 100', .3, 6, 6))
    pk = 'M138 290 L282 290 L300 372 L120 372 Z'
    pocket = (raised(pk, color, .3, .1) + K('M138 292 L120 370', .35, 3, 4) + K('M282 292 L300 370', .4, 3, 4)
              + stitch('M140 296 L282 296', '#000', .25, 1.2) + stitch('M126 366 L294 366', '#000', .2, 1.2)
              + Wt('M150 300 L270 300', .12, 3, 6))
    rib = (f'<rect x="100" y="376" width="220" height="56" fill="url(#rib)"/>' + line('M108 378 Q210 372 312 378', '#000', .32, 2.5)
           + f'<g transform="rotate(3 60 376)"><rect x="32" y="356" width="56" height="46" fill="url(#rib)"/></g>'
           + f'<g transform="rotate(-3 {W-60} 376)"><rect x="{W-88}" y="356" width="56" height="46" fill="url(#rib)"/></g>'
           + line('M38 356 Q62 360 86 360', '#000', .3, 2.5) + line(f'M{W-38} 356 Q{W-62} 360 {W-86} 360', '#000', .3, 2.5)
           + ''.join(K(f'M{x} 334 L{x+2} 356', .22, 3, 3) for x in (50, 62, 74)) + ''.join(K(f'M{W-x} 334 L{W-x-2} 356', .25, 3, 3) for x in (50, 62, 74)))
    cord = lambda x0, x1: (f'<path d="M{x0} 118 Q{x0-2} 160 {x1} 200" fill="none" stroke="#000" stroke-opacity=".3" stroke-width="5" transform="translate(1.5 2)" filter="url(#b1)"/>'
                           f'<path d="M{x0} 118 Q{x0-2} 160 {x1} 200" fill="none" stroke="#F4F1EA" stroke-width="4" stroke-linecap="round"/>'
                           f'<path d="M{x0} 118 Q{x0-2} 160 {x1} 200" fill="none" stroke="#B9B1A4" stroke-width="1.2" stroke-dasharray="2 3"/>'
                           f'<rect x="{x1-2.6}" y="198" width="5.2" height="14" rx="2" fill="#B8BBC0"/><rect x="{x1-1.6}" y="199" width="1.4" height="12" fill="#fff" opacity=".7"/>')
    eyelets = ''.join(f'<circle cx="{x}" cy="114" r="3.4" fill="#C9CCD1"/><circle cx="{x}" cy="114" r="1.8" fill="#222"/>' for x in (192, 228))
    detail = (eyelets + cord(192, 188) + cord(228, 234)
              + stitch('M98 102 Q104 150 118 204', '#000', .32, 2, '1 0') + stitch(f'M{W-98} 102 Q{W-104} 150 {W-118} 204', '#000', .32, 2, '1 0'))
    shade = (rib + K('M116 208 Q108 300 112 376', .4, 12, 14) + K(f'M{W-116} 208 Q{W-108} 300 {W-112} 376', .45, 12, 14)
             + K('M74 156 Q56 250 46 344', .2, 12, 12) + K(f'M{W-74} 156 Q{W-56} 250 {W-46} 344', .25, 12, 12)
             + K('M264 170 Q282 240 272 290', .14, 22, 26) + K('M120 368 Q210 358 300 368', .2, 6, 8))
    light = (Wt('M150 140 Q166 200 150 280', .2, 22, 46) + Wt('M74 140 Q56 230 42 320', .2, 12, 14) + Wt('M176 24 Q160 44 158 80', .16, 12, 14))
    return piece(W, H, base, color, (.16, .28), shade, light, '', detail, fabric=hood + pocket, grain=(1.6, .4), name='hoodie')
