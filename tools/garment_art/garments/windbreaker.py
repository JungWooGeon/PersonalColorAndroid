from kit.art_kit import *


def _zip(x, y0, y1):
    teeth = f'<path d="M{x} {y0} L{x} {y1}" stroke="#8C8C8C" stroke-width="4.5" stroke-dasharray="1.6 1.4"/>'
    return (line(f'M{x} {y0} L{x} {y1}', '#000', .45, 8) + teeth
            + f'<rect x="{x-3.5}" y="{y0+2}" width="7" height="12" rx="2" fill="#C9CBCE" stroke="#6d6f72" stroke-width=".8"/>'
            + f'<path d="M{x} {y0+13} L{x} {y0+32}" stroke="#1b1b1b" stroke-width="3" stroke-linecap="round"/>'
            + f'<rect x="{x-4}" y="{y0+28}" width="8" height="9" rx="2" fill="#1b1b1b"/>')


def _toggle(x, y, dx):
    return (f'<path d="M{x} {y} q{dx*.3} 10 {dx} 24" fill="none" stroke="#1b1b1b" stroke-width="2.2" stroke-linecap="round"/>'
            f'<rect x="{x+dx-4}" y="{y+18}" width="8" height="11" rx="3" fill="#2a2a2a"/><rect x="{x+dx-2.5}" y="{y+20}" width="2" height="6" rx="1" fill="#fff" opacity=".35"/>'
            f'<circle cx="{x+dx}" cy="{y+33}" r="2.4" fill="#2a2a2a"/>')


def windbreaker(color):
    W, H = 400, 340
    left = [(200, 40, 'c'), (166, 36, 'c'), (110, 54, 'c'), (84, 74), (64, 124), (48, 196), (40, 256), (44, 290), (52, 304, 'c'), (52, 322, 'c'),
            (98, 326, 'c'), (100, 306), (106, 290), (108, 240), (114, 178), (120, 140, 'c'), (122, 220), (124, 280), (130, 300, 'c'), (134, 322, 'c'),
            (200, 324, 'c')]
    base = smooth(sym(left, W))
    collar = smooth([(162, 2, 'c'), (200, 0), (238, 2, 'c'), (246, 54, 'c'), (200, 62), (154, 54, 'c')])
    collar_in = 'M164 4 Q200 -2 236 4 Q232 16 200 18 Q168 16 164 4 Z'
    flap = 'M138 168 L262 168 L262 196 Q200 204 138 196 Z'
    pocket = 'M138 168 L262 168 L270 250 Q200 258 130 250 Z'
    # shirred hem + cuffs: little gather ripples
    gath = ''.join(line(f'M{x} 302 L{x + 1} 322', '#000', .22, 2) + line(f'M{x + 3} 303 L{x + 4} 321', '#fff', .14, 1.5) for x in range(136, 266, 7))
    gcuff = lambda s: ''.join(line(f'M{x} 304 L{x + 1} 324', '#000', .2, 2) for x in range(s, s + 44, 6))
    fabric = (raised(pocket, color, .3, .14) + raised(flap, color, .35, .16)
              + stitch('M142 172 L258 172', '#000', .3, 1.1, '3 2.5') + stitch('M140 194 Q200 201 260 194', '#000', .3, 1.1, '3 2.5')
              + gath + gcuff(54) + gcuff(W - 98)
              + line('M126 298 Q200 304 274 298', '#000', .3, 2) + line('M50 300 Q75 304 102 304', '#000', .3, 2)
              + line(f'M{W-50} 300 Q{W-75} 304 {W-102} 304', '#000', .3, 2)
              # raglan seams
              + line('M166 40 Q140 90 120 140', '#000', .3, 1.6) + line(f'M{W-166} 40 Q{W-140} 90 {W-120} 140', '#000', .3, 1.6)
              + stitch('M162 42 Q136 92 116 140', '#000', .22, 1, '3 2.5') + stitch(f'M{W-162} 42 Q{W-136} 92 {W-116} 140', '#000', .22, 1, '3 2.5'))
    detail = (f'<path d="{collar}" fill="#000" opacity=".35" transform="translate(0 4)" filter="url(#b3)"/><path d="{collar}" fill="{color}"/>'
              f'<clipPath id="wbc"><path d="{collar}"/></clipPath><g clip-path="url(#wbc)">{Wt("M168 14 L168 56", .25, 6, 12)}{K("M234 10 L234 58", .3, 6, 14)}'
              f'{line("M156 44 Q200 52 244 44", "#000", .25, 1.4)}<rect width="400" height="70" filter="url(#grainK)" opacity=".5"/></g>'
              + shape(collar_in, '#000', .5) + K(collar_in, .3, 3)
              + _zip(200, 6, 160)
              + f'<rect x="183" y="179" width="34" height="12" rx="2" fill="#1b1b1b" opacity=".85"/><rect x="186" y="182" width="28" height="2" fill="#fff" opacity=".25"/>'
              + button(150, 184, 3.6, '#2a2a2a') + button(250, 184, 3.6, '#2a2a2a')
              + _toggle(166, 50, -6) + _toggle(234, 50, 6) + _toggle(130, 312, -4) + _toggle(270, 312, 4))
    # nylon: crisp crinkle highlights + soft folds
    crinkle = (Wt('M150 70 Q144 110 152 150', .35, 3, 5) + Wt('M232 214 Q246 250 238 290', .25, 3, 4)
               + Wt('M60 150 Q52 200 50 250', .4, 3, 6) + Wt(f'M{W-70} 160 Q{W-62} 210 {W-60} 250', .18, 3, 5)
               + Wt('M84 230 Q78 260 84 290', .3, 3, 4) + Wt('M176 260 Q170 280 178 300', .2, 3, 4))
    shade = (K('M120 146 Q124 230 128 310', .5, 12, 14) + K(f'M{W-120} 146 Q{W-124} 230 {W-128} 310', .55, 12, 14)
             + K('M98 110 Q80 200 70 296', .22, 6, 6) + K(f'M{W-98} 110 Q{W-80} 200 {W-70} 296', .28, 6, 6)
             + K('M64 260 Q80 270 100 266', .25, 6, 6) + K(f'M{W-64} 260 Q{W-80} 270 {W-100} 266', .25, 6, 6)
             + K('M140 252 Q200 262 262 252', .25, 6, 6) + K('M190 200 Q196 250 190 300', .12, 12, 14))
    light = crinkle + Wt('M150 100 Q160 160 150 230', .2, 22, 40) + Wt('M70 100 Q56 190 46 270', .18, 12, 12)
    return piece(W, H, base, color, (.22, .3), shade, light, '', detail, fabric=fabric, grain=(2.2, .15), name='windbreaker')
