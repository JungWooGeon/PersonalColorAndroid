from kit.art_kit import *

def slip_dress(color):
    """Bias-cut satin midi slip with cowl neck and spaghetti straps."""
    W, H = 300, 570
    left = [(150, 132), (122, 124), (98, 110, 'c'), (92, 150), (96, 205), (96, 245), (88, 320), (78, 420), (64, 510), (54, 548, 'c'),
            (88, 556), (120, 551), (150, 556, 'c')]
    body = smooth(sym(left, W))
    strap = lambda x0, x1: f'M{x0} 112 L{x1-1.6} 14 L{x1+1.6} 14 L{x0+3.2} 112 Z'
    base = body + ' ' + strap(99, 116) + ' ' + strap(W - 102, W - 116)
    # cowl drape at neckline
    cowl = ''.join(K(f'M{110+i*6} {128+i*9} Q150 {150+i*16} {190-i*6} {128+i*9}', .22 - i * .04, 3, 3) for i in range(3))
    cowl_l = ''.join(Wt(f'M{112+i*6} {134+i*9} Q150 {158+i*16} {188-i*6} {134+i*9}', .35 - i * .07, 3, 3) for i in range(3))
    detail = (line('M98 110 Q124 128 150 132 Q176 128 202 110', '#000', .22, 1.4)
              + stitch('M48 542 Q100 552 150 550 Q200 552 252 542', '#000', .18, 1, '3 3'))
    # satin: long bias highlights + soft folds
    shade = (cowl + K('M96 150 Q100 200 98 250', .35, 6, 10) + K(f'M{W-96} 150 Q{W-100} 200 {W-98} 250', .4, 6, 10)
             + K('M130 260 Q122 400 108 550', .35, 12, 14) + K('M176 270 Q186 400 196 550', .4, 12, 16)
             + K('M152 300 Q154 430 152 552', .2, 12, 8) + K('M82 360 Q64 460 52 548', .35, 12, 16)
             + K(f'M{W-82} 360 Q{W-64} 460 {W-52} 548', .4, 12, 16))
    light = (cowl_l + Wt('M118 160 Q112 330 92 540', .55, 6, 7) + Wt('M118 160 Q112 330 92 540', .3, 12, 22)
             + Wt('M162 280 Q168 420 172 548', .45, 6, 5) + Wt('M162 280 Q168 420 172 548', .2, 12, 18)
             + Wt('M210 330 Q226 440 236 536', .3, 6, 5) + Wt('M140 170 Q150 180 160 172', .3, 6, 10)
             + Wt('M108 20 L102 108', .35, 1, 1.2) + Wt(f'M{W-108} 20 L{W-102} 108', .2, 1, 1.2))
    return piece(W, H, base, color, (.22, .3), shade, light, '', detail, grain=(1.8, .12), name='slip_dress')
