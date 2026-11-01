from kit.art_kit import *

def shirt(color):
    """Oversized boyfriend shirt: dropped shoulders, pointed collar, fine pinstripe, curved shirttail hem."""
    W, H = 420, 440
    left = [(210, 46, 'c'), (176, 36, 'c'), (98, 56, 'c'), (74, 82), (58, 160), (46, 250), (36, 342, 'c'), (32, 392, 'c'), (88, 400, 'c'),
            (90, 350, 'c'), (96, 262), (110, 186, 'c'), (108, 270), (106, 340), (104, 396, 'c'), (132, 412), (172, 424), (210, 428, 'c')]
    base = smooth(sym(left, W))
    strp = ('<pattern id="a06st" width="10" height="10" patternUnits="userSpaceOnUse">'
            '<rect width="1.4" height="10" fill="#000" opacity=".13"/><rect x="5" width="1" height="10" fill="#fff" opacity=".07"/></pattern>')
    tex = (f'<defs>{strp}</defs><rect x="96" width="228" height="{H}" fill="url(#a06st)"/>'
           f'<g transform="rotate(8 60 230)"><rect x="20" y="40" width="80" height="380" fill="url(#a06st)"/></g>'
           f'<g transform="rotate(-8 360 230)"><rect x="320" y="40" width="80" height="380" fill="url(#a06st)"/></g>')
    neck = 'M182 40 Q210 34 238 40 L210 70 Z'
    colL = [(178, 34, 'c'), (158, 46), (164, 100, 'c'), (206, 70, 'c'), (210, 60, 'c')]
    stand = smooth([(176, 36, 'c'), (210, 28), (244, 36, 'c'), (240, 48), (210, 42), (180, 48, 'c')])
    pk = 'M134 150 L178 150 L178 192 L156 200 L134 192 Z'
    cuff = lambda s: (f'M{36 if s else W-36} 352 L{90 if s else W-90} 360')
    fabric = (shape(neck, '#000', .5) + raised(stand, color, .25, .1)
              + line('M200 70 L200 426', '#000', .28, 1.4) + line('M220 70 L220 426', '#000', .22, 1.4)
              + stitch('M203 72 L203 424', '#000', .2, .8, '2.5 2') + stitch('M217 72 L217 424', '#000', .2, .8, '2.5 2')
              + raised(pk, color, .22, .1) + stitch('M134 156 L178 156', '#000', .25, .8, '2.5 2')
              + raised(smooth(mir(colL, W)), color, .42, .12) + raised(smooth(colL), color, .48, .16))
    detail = (''.join(pearl(210, y, 4.6) for y in (92, 150, 208, 266, 324, 382))
              + line(cuff(1), '#000', .3, 1.6) + line(cuff(0), '#000', .3, 1.6)
              + stitch('M98 58 Q100 120 110 184', '#000', .28, 1, '3 2.5') + stitch(f'M{W-98} 58 Q{W-100} 120 {W-110} 184', '#000', .28, 1, '3 2.5')
              + stitch('M108 392 Q150 414 210 422 Q270 414 312 392', '#000', .2, .9, '3 2.5'))
    shade = (K('M110 190 Q104 300 106 400', .45, 12, 14) + K(f'M{W-110} 190 Q{W-104} 300 {W-106} 400', .5, 12, 14)
             + K('M80 100 Q62 240 44 390', .22, 12, 10) + K(f'M{W-80} 100 Q{W-62} 240 {W-44} 390', .28, 12, 10)
             + K('M90 352 Q76 300 70 220', .25, 6, 4) + K('M60 300 Q70 270 84 230', .18, 6, 3)
             + K('M118 200 Q160 230 186 290', .14, 12, 8) + K(f'M{W-118} 220 Q{W-160} 250 {W-188} 320', .16, 12, 8)
             + K('M150 300 Q160 360 150 410', .14, 12, 12) + K('M276 260 Q290 330 284 410', .16, 12, 12)
             + K('M38 356 L90 364 L88 400 L32 392 Z', .14, 3) + K('M100 400 Q150 420 210 426', .2, 6, 8))
    light = (Wt('M150 90 Q166 200 150 320', .22, 22, 44) + Wt('M76 96 Q58 220 44 340', .2, 12, 12)
             + Wt('M132 230 Q166 252 180 300', .14, 6, 6) + Wt('M236 280 Q262 330 254 400', .1, 12, 18)
             + Wt('M64 240 Q74 210 86 190', .14, 6, 4))
    return piece(W, H, base, color, (.16, .28), shade, light, tex, detail, fabric=fabric, grain=(1.5, .2), name='shirt')
