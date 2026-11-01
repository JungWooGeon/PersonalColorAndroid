from kit.art_kit import *

def jeans(color):
    W, H = 300, 520
    pts = [(58, 20, 'c'), (242, 20, 'c'), (248, 70), (256, 160), (262, 300), (270, 504, 'c'), (164, 510, 'c'), (158, 320), (150, 196, 'c'),
           (142, 320), (136, 510, 'c'), (30, 504, 'c'), (38, 300), (44, 160), (52, 70)]
    base = smooth(pts)
    thread = '#C99A52'
    tex = f'<rect width="{W}" height="{H}" fill="url(#twill)"/>'
    detail = (line('M57 44 L243 44', '#000', .3, 2) + stitch('M58 24 L242 24', thread, .8, 1.2) + stitch('M58 40 L242 40', thread, .8, 1.2)
              + stitch('M162 46 L162 128 Q162 150 148 160', thread, .85, 1.3) + line('M150 46 L150 170', '#000', .28, 2)
              + stitch('M60 48 Q92 52 100 50 Q90 92 54 108', thread, .8, 1.2) + line('M62 46 Q96 50 102 48 Q92 94 52 112', '#000', .25, 1.5)
              + stitch(f'M{W-60} 48 Q{W-92} 52 {W-100} 50 Q{W-90} 92 {W-54} 108', thread, .8, 1.2) + line(f'M{W-62} 46 Q{W-96} 50 {W-102} 48 Q{W-92} 94 {W-52} 112', '#000', .25, 1.5)
              + ''.join(f'<rect x="{x-4}" y="17" width="8" height="30" rx="2" fill="#000" opacity=".25" filter="url(#b1)"/><rect x="{x-4}" y="16" width="8" height="28" rx="2" fill="{color}"/>' + stitch(f'M{x} 18 L{x} 42', thread, .7, 1, '2 2') for x in (78, 120, 180, 222))
              + button(150, 32, 7, metal=True) + button(100, 52, 3, metal=True) + button(200, 52, 3, metal=True)
              + stitch('M33 492 L138 497', thread, .7, 1.2) + stitch(f'M{W-33} 492 L{W-138} 497', thread, .7, 1.2))
    shade = (K('M150 196 L148 500', .5, 6, 10) + K('M150 196 Q152 330 156 505', .5, 6, 8)
             + K('M40 160 Q34 330 32 500', .3, 12, 14) + K(f'M{W-40} 160 Q{W-34} 330 {W-32} 500', .35, 12, 14)
             + K('M40 470 Q90 460 136 474', .2, 6, 6) + K(f'M{W-40} 470 Q{W-90} 460 {W-136} 474', .2, 6, 6))
    light = (Wt('M92 120 Q88 300 86 480', .35, 22, 34) + Wt(f'M{W-92} 120 Q{W-88} 300 {W-86} 480', .25, 22, 30)
             + ''.join(Wt(f'M{140-i*7} {168+i*4} Q{118-i*6} {178+i*6} {96-i*4} {176+i*12}', .35, 3, 3) for i in range(4))
             + ''.join(Wt(f'M{160+i*7} {168+i*4} Q{182+i*6} {178+i*6} {204+i*4} {176+i*12}', .3, 3, 3) for i in range(4))
             + Wt('M60 300 Q90 296 120 302', .2, 6, 10) + Wt(f'M{W-60} 300 Q{W-90} 296 {W-120} 302', .16, 6, 10))
    return piece(W, H, base, color, (.12, .3), shade, light, tex, detail, grain=(1.6, .5), name='jeans')
