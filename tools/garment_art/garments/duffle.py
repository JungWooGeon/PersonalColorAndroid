from kit.art_kit import *


def _toggle(x, y, tilt):
    """Horn toggle on natural rope, tan leather tabs either side (fixed neutral colours)."""
    rope = (f'<path d="M{x-40} {y-3} Q{x-20} {y+5} {x} {y} M{x-40} {y+3} Q{x-20} {y+9} {x} {y+2}" fill="none" stroke="#D8C9AC" stroke-width="2.4" stroke-linecap="round"/>'
            f'<path d="M{x+42} {y-3} Q{x+22} {y-6} {x+4} {y-1} M{x+42} {y+3} Q{x+22} {y} {x+4} {y+2}" fill="none" stroke="#D8C9AC" stroke-width="2.4" stroke-linecap="round"/>')
    tab = lambda cx: (f'<rect x="{cx-9}" y="{y-9}" width="18" height="18" rx="5" fill="#000" opacity=".35" transform="translate(1 2)" filter="url(#b1)"/>'
                      f'<rect x="{cx-9}" y="{y-9}" width="18" height="18" rx="5" fill="#9C744C"/>'
                      f'<rect x="{cx-6.5}" y="{y-6.5}" width="13" height="13" rx="3.5" fill="none" stroke="#F1E2C4" stroke-opacity=".55" stroke-width="1" stroke-dasharray="2 2"/>'
                      f'<rect x="{cx-8}" y="{y-8}" width="16" height="6" rx="3" fill="#fff" opacity=".15"/>')
    tog = (f'<g transform="rotate({tilt} {x} {y})">'
           f'<rect x="{x-4}" y="{y-22}" width="15" height="46" rx="7.5" fill="#000" opacity=".35" filter="url(#b1)"/>'
           f'<rect x="{x-7}" y="{y-24}" width="14" height="48" rx="7" fill="#EDE3CF"/>'
           f'<rect x="{x-7}" y="{y-24}" width="14" height="48" rx="7" fill="none" stroke="#8C7B60" stroke-opacity=".5" stroke-width="1"/>'
           f'<rect x="{x+1}" y="{y-22}" width="5" height="44" rx="2.5" fill="#B5A080" opacity=".45"/>'
           f'<rect x="{x-5}" y="{y-20}" width="3.5" height="36" rx="1.7" fill="#fff" opacity=".85"/>'
           f'<rect x="{x-7}" y="{y-24}" width="14" height="7" rx="3.5" fill="#7A6548" opacity=".35"/></g>')
    return rope + tab(x - 42) + tab(x + 44) + tog


def duffle(color):
    """떡볶이 코트: roomy knee-length duffle, hood resting behind the neck, horn toggles, big patch pockets."""
    W, H = 400, 570
    left = [(200, 4, 'c'), (156, 8), (126, 26), (114, 50, 'c'), (92, 60, 'c'), (72, 84), (58, 140), (46, 230), (36, 320), (28, 404, 'c'), (90, 414, 'c'),
            (96, 320), (104, 230), (114, 178, 'c'), (110, 300), (102, 420), (90, 542, 'c'), (146, 548), (200, 546, 'c')]
    base = smooth(sym(left, W))
    hood_in = smooth([(132, 44, 'c'), (152, 24), (200, 16), (248, 24), (268, 44, 'c'), (236, 50), (200, 52), (164, 50)])
    collar = smooth([(140, 44, 'c'), (200, 54), (260, 44, 'c'), (256, 68), (200, 84), (144, 68)])
    flap = 'M226 46 L250 50 L250 548 L226 548 Z'          # front placket edge (right panel wraps over)
    yoke = 'M120 46 Q114 98 112 140 L288 140 Q286 98 280 46 Q240 64 200 64 Q160 64 120 46 Z'
    pocket = lambda x: (f'M{x} 378 L{x+70} 376 L{x+70} 462 Q{x+70} 470 {x+62} 470 L{x+8} 470 Q{x} 470 {x} 462 Z')
    pflap = lambda x: f'M{x-2} 370 L{x+72} 368 L{x+72} 396 Q{x+35} 400 {x-2} 398 Z'
    fabric = (shape(hood_in, '#000', .42) + K(hood_in, .35, 6)
              + Wt('M134 40 Q150 16 200 12 Q250 16 266 40', .2, 3, 5) + stitch('M126 46 Q140 14 200 9 Q260 14 274 46', '#000', .2, 1, '3 3')
              + raised(yoke, color, .25, .08) + raised(collar, color, .4, .12)
              + raised(pocket(108), color, .3, .08) + raised(pocket(222), color, .3, .08)
              + raised(pflap(108), color, .35, .12) + raised(pflap(222), color, .35, .12))
    detail = (stitch('M120 50 Q114 98 114 136 L286 136 Q286 98 280 50', '#000', .2, 1, '3 3')
              + line('M226 84 L226 546', '#000', .32, 2) + line('M228 84 L228 546', '#fff', .08, 1)
              + stitch('M220 86 L220 544', '#000', .18, 1, '3 3')
              + stitch('M30 380 Q58 386 92 390', '#000', .22) + stitch(f'M{W-30} 380 Q{W-58} 386 {W-92} 390', '#000', .22)
              + stitch('M112 382 L112 464 L174 464 L174 382', '#000', .18, 1, '3 3') + stitch('M226 380 L226 464 L288 464 L288 380', '#000', .18, 1, '3 3')
              + stitch('M94 536 Q200 530 306 536', '#000', .18, 1, '3 3')
              + _toggle(212, 128, -8) + _toggle(212, 206, -6) + _toggle(212, 284, -8))
    shade = (K('M112 180 Q112 300 104 420', .5, 12, 14) + K(f'M{W-112} 180 Q{W-112} 300 {W-104} 420', .55, 12, 14)
             + K('M86 130 Q76 270 66 400', .25, 12, 10) + K(f'M{W-86} 130 Q{W-76} 270 {W-66} 400', .3, 12, 10)
             + K('M234 100 L234 546', .25, 6, 8) + K('M150 300 Q140 420 128 540', .2, 12, 12) + K('M270 300 Q284 420 296 540', .22, 12, 14)
             + K('M100 536 Q200 528 300 536', .25, 6, 10) + K('M124 70 Q112 100 110 140', .2, 12, 10))
    light = (Wt('M150 150 Q158 240 146 340', .22, 22, 40) + Wt('M70 100 Q56 220 44 330', .2, 12, 12)
             + Wt('M136 420 Q128 480 120 536', .12, 12, 16) + Wt(f'M{W-150} 160 Q{W-150} 240 {W-160} 330', .1, 22, 30)
             + Wt('M150 30 Q200 14 250 30', .16, 6, 8))
    return piece(W, H, base, color, (.18, .32), shade, light, '', detail, fabric=fabric, grain=(1.0, .55), name='duffle')
