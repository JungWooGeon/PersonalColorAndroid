from kit.art_kit import *

CURL = ('<filter id="plW" x="0" y="0" width="100%" height="100%"><feTurbulence type="fractalNoise" baseFrequency=".42" numOctaves="2" seed="5"/><feColorMatrix values="0 0 0 0 1 0 0 0 0 1 0 0 0 0 1 0 0 0 2.6 -1.2"/><feGaussianBlur stdDeviation=".7"/></filter><filter id="plK" x="0" y="0" width="100%" height="100%"><feTurbulence type="fractalNoise" baseFrequency=".5" numOctaves="2" seed="9"/><feColorMatrix values="0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 2.4 -1.2"/><feGaussianBlur stdDeviation=".8"/></filter><pattern id="mcurl" width="13" height="12" patternUnits="userSpaceOnUse">'
        '<path d="M2 7 a3.5 3.5 0 1 1 6 -2 M8 12 a3 3 0 1 1 5 -1.5" fill="none" stroke="#fff" stroke-width="2.2" stroke-linecap="round" opacity=".3"/>'
        '<path d="M1 9 a3.5 3.5 0 0 0 6 1 M7 3 a3 3 0 0 0 5 1" fill="none" stroke="#000" stroke-width="1.8" stroke-linecap="round" opacity=".18"/></pattern>')


def _shear(d, color, cid, sh=.3):
    """Tonal shearling patch: shadow, fill, curls, soft cream lift, fluffy dotted edge."""
    return (f'<path d="{d}" fill="#000" opacity="{sh}" transform="translate(1 3)" filter="url(#b6)"/>'
            f'<path d="{d}" fill="{color}" stroke="{color}" stroke-width="7" stroke-dasharray="0 5.5" stroke-linecap="round"/>'
            f'<path d="{d}" fill="#fff" stroke="#fff" stroke-width="7" stroke-dasharray="0 5.5" stroke-linecap="round" opacity=".17"/>'
            f'<clipPath id="{cid}"><path d="{d}"/></clipPath><g clip-path="url(#{cid})">'
            f'<g filter="url(#b1)" opacity=".7"><rect width="400" height="380" fill="url(#mcurl)"/></g>'
            f'<rect width="400" height="380" filter="url(#plW)" opacity=".55"/><rect width="400" height="380" filter="url(#plK)" opacity=".4"/>'
            f'{Wt(d, .12, 3, 6)}</g>'
            f'<path d="{d}" fill="none" stroke="#fff" stroke-opacity=".28" stroke-width="3" stroke-dasharray="0 5.5" stroke-linecap="round"/>')


def mustang(color):
    W, H = 400, 380
    left = [(200, 40, 'c'), (160, 34, 'c'), (98, 52, 'c'), (78, 72), (64, 130), (54, 210), (46, 290), (40, 340, 'c'), (96, 348, 'c'), (100, 280),
            (106, 200), (114, 150, 'c'), (114, 240), (112, 336, 'c'), (200, 340, 'c')]
    base = smooth(sym(left, W))
    # wide shearling notch collar + lapels
    lapL = smooth([(162, 30, 'c'), (126, 44), (108, 70, 'c'), (124, 92, 'c'), (112, 108, 'c'), (150, 168), (196, 206, 'c'), (186, 120), (176, 54)])
    lapR = smooth(mir([(162, 30, 'c'), (126, 44), (108, 70, 'c'), (124, 92, 'c'), (112, 108, 'c'), (150, 168), (196, 206, 'c'), (186, 120), (176, 54)], W))
    collar = smooth([(156, 32, 'c'), (200, 20), (244, 32, 'c'), (236, 50), (200, 44), (164, 50, 'c')])
    vopen = 'M168 36 L200 206 L232 36 Q200 46 168 36 Z'
    hem = smooth([(112, 316, 'c'), (200, 318), (288, 316, 'c'), (288, 340, 'c'), (200, 342), (112, 340, 'c')])
    cuffL = smooth([(43, 318, 'c'), (98, 324, 'c'), (96, 350, 'c'), (40, 342, 'c')])
    cuffR = smooth(mir([(43, 318, 'c'), (98, 324, 'c'), (96, 350, 'c'), (40, 342, 'c')], W))
    facing = smooth([(194, 206, 'c'), (206, 206, 'c'), (208, 318, 'c'), (194, 318, 'c')])
    welt = lambda x0, s: f'M{x0} 236 L{x0 + s * 22} 290'
    fabric = (CURL + shape(vopen, '#000', .5) + K(vopen, .35, 6)
              + line('M200 206 L200 320', '#000', .35, 2)
              + line(welt(140, -1), '#000', .4, 3) + line(welt(260, 1), '#000', .4, 3)
              + Wt(welt(142, -1), .25, 1, 1.5) + Wt(welt(258, 1), .25, 1, 1.5)
              + stitch('M150 112 Q140 220 136 314', '#000', .22, 1.2) + stitch(f'M{W-150} 112 Q{W-140} 220 {W-136} 314', '#000', .22, 1.2))
    detail = (CURL + _shear(collar, color, 'ms0', .25) + _shear(facing, color, 'ms5', .2)
              + _shear(lapR, color, 'ms1') + _shear(lapL, color, 'ms2', .45)
              + _shear(hem, color, 'ms3', .3) + _shear(cuffL, color, 'ms4', .3) + _shear(cuffR, color, 'ms6', .3)
              + button(214, 232, 6.5) + button(214, 282, 6.5))
    shade = (K('M112 156 Q116 250 112 330', .5, 12, 14) + K(f'M{W-112} 156 Q{W-116} 250 {W-112} 330', .55, 12, 14)
             + K('M82 110 Q70 220 58 320', .25, 12, 10) + K(f'M{W-82} 110 Q{W-70} 220 {W-58} 320', .3, 12, 10)
             + K('M160 200 Q168 260 150 316', .16, 22, 30))
    light = (Wt('M148 150 Q156 230 146 300', .22, 22, 40) + Wt('M76 90 Q62 200 50 300', .22, 12, 12)
             + Wt(f'M{W-150} 200 Q{W-150} 250 {W-160} 300', .1, 22, 30) + Wt('M86 64 Q100 56 120 54', .15, 6, 8))
    return piece(W, H, base, color, (.18, .3), shade, light, '', detail, fabric=fabric, grain=(1.5, .4), name='mustang')
