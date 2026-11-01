from kit.art_kit import *


def zipup(color):
    W, H = 400, 360
    left = [(200, 0, 'c'), (170, 4), (148, 20), (140, 44), (136, 56, 'c'), (98, 70, 'c'), (78, 94), (62, 150), (50, 220), (42, 300, 'c'), (38, 342, 'c'),
            (94, 348, 'c'), (98, 300, 'c'), (104, 220), (112, 170, 'c'), (112, 260), (114, 338, 'c'), (200, 342, 'c')]
    base = smooth(sym(left, W))
    hood_in = smooth([(200, 14), (172, 18), (158, 40), (160, 70), (176, 96), (200, 112, 'c'), (224, 96), (240, 70), (242, 40), (228, 18)])
    rim = smooth([(200, 8), (166, 12), (150, 38), (152, 72), (172, 102), (200, 120, 'c'), (228, 102), (248, 72), (250, 38), (234, 12)])
    rib = '<pattern id="zrib" width="4" height="10" patternUnits="userSpaceOnUse"><rect width="1.6" height="10" fill="#000" opacity=".22"/><rect x="2" width="1.4" height="10" fill="#fff" opacity=".1"/></pattern>'
    hemb = 'M114 306 L286 306 L286 340 L114 340 Z'
    cuff = lambda s: f'<g transform="rotate({4*s} {200-150*s} 326)"><rect x="{200-150*s-30}" y="306" width="60" height="44" fill="url(#zrib)"/></g>'
    pk = lambda s: f'M{200-s*20} 236 L{200-s*68} 236 Q{200-s*86} 260 {200-s*92} 300 L{200-s*20} 300 Z'
    fabric = (rib + K('M134 240 Q116 264 110 300', .3, 3, 6) + K(f'M{W-134} 240 Q{W-116} 264 {W-110} 300', .3, 3, 6) + Wt('M138 242 Q121 266 115 300', .2, 1, 1.5) + Wt(f'M{W-128} 242 Q{W-111} 266 {W-105} 300', .14, 1, 1.5)
              + line('M132 237 Q114 262 108 300', '#000', .45, 2.5) + line(f'M{W-132} 237 Q{W-114} 262 {W-108} 300', '#000', .45, 2.5)
              + f'<rect x="0" y="0" width="0" height="0"/>'
              + f'<path d="{hemb}" fill="url(#zrib)"/>' + cuff(1) + cuff(-1)
              + line('M114 306 L286 306', '#000', .3, 2) + line('M40 300 Q66 304 98 308', '#000', .3, 2) + line(f'M{W-40} 300 Q{W-66} 304 {W-98} 308', '#000', .3, 2)
              + stitch('M98 72 Q106 120 112 170', '#000', .3, 1.2, '1 0') + stitch(f'M{W-98} 72 Q{W-106} 120 {W-112} 170', '#000', .3, 1.2, '1 0'))
    zipx = 200
    cord = lambda x, dx: (f'<path d="M{x} 104 q{dx} 40 {dx*.4} 92" fill="none" stroke="#F4F1EC" stroke-width="3.2" stroke-linecap="round"/>'
                          f'<path d="M{x} 104 q{dx} 40 {dx*.4} 92" fill="none" stroke="#000" stroke-opacity=".18" stroke-width="1" stroke-dasharray="2 2"/>'
                          f'<rect x="{x+dx*.4-2.6}" y="194" width="5.2" height="13" rx="1.6" fill="#BFC2C6" stroke="#77797c" stroke-width=".7"/>'
                          f'<circle cx="{x+dx*.4-.8}" cy="198" r="1" fill="#fff"/><circle cx="{x}" cy="104" r="3" fill="#C9CBCE" stroke="#6d6f72" stroke-width=".8"/>')
    detail = (f'<path d="{rim}" fill="none" stroke="{color}" stroke-width="12"/>'
              f'<path d="{rim}" fill="none" stroke="#fff" stroke-opacity=".12" stroke-width="12"/>'
              + line(rim, '#000', .25, 1.5) + stitch(smooth([(200, 3), (164, 7), (146, 36), (148, 74), (170, 106), (200, 126, 'c'), (230, 106), (252, 74), (254, 36), (236, 7)]), '#000', .25, 1.1, '3 2.5')
              + shape(hood_in, '#000', .5) + K(hood_in, .35, 6) + K('M168 30 Q200 20 232 30 L232 50 Q200 40 168 50 Z', .25, 6)
              + line(f'M{zipx} 118 L{zipx} 340', '#000', .4, 7)
              + f'<path d="M{zipx} 120 L{zipx} 340" stroke="#B9BCC0" stroke-width="4" stroke-dasharray="1.6 1.3"/>'
              + f'<rect x="{zipx-4}" y="124" width="8" height="13" rx="2" fill="#D4D6D9" stroke="#6d6f72" stroke-width=".8"/>'
              + f'<path d="M{zipx} 136 L{zipx} 156" stroke="#C9CBCE" stroke-width="4" stroke-linecap="round"/><circle cx="{zipx}" cy="156" r="3.4" fill="none" stroke="#9a9da1" stroke-width="1.6"/>'
              + cord(184, -10) + cord(216, 10))
    shade = (K('M112 176 Q114 260 112 336', .5, 12, 14) + K(f'M{W-112} 176 Q{W-114} 260 {W-112} 336', .55, 12, 14)
             + K('M80 120 Q66 220 52 320', .22, 12, 10) + K(f'M{W-80} 120 Q{W-66} 220 {W-52} 320', .28, 12, 10)
             + K('M140 12 Q130 40 140 60', .35, 6, 10) + K(f'M{W-140} 12 Q{W-130} 40 {W-140} 60', .4, 6, 10)
             + K('M150 200 Q160 260 150 300', .14, 22, 30))
    light = (Wt('M150 130 Q160 200 150 260', .22, 22, 40) + Wt('M72 100 Q58 200 46 300', .2, 12, 12)
             + Wt('M166 6 Q190 0 214 4', .25, 6, 8) + Wt(f'M{W-150} 200 Q{W-156} 250 {W-160} 290', .1, 22, 26))
    return piece(W, H, base, color, (.16, .3), shade, light, '', detail, fabric=fabric, grain=(1.5, .45), name='zipup')
