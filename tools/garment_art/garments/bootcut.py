from kit.art_kit import *

def bootcut(color):
    """Bootcut / flare denim: mid rise, fitted hip and thigh, slim knee, long flare that kisses the floor."""
    W, H = 300, 520
    pts = [(66, 26, 'c'), (234, 26, 'c'), (242, 70), (250, 140), (246, 230), (240, 330), (250, 420), (268, 508, 'c'), (160, 512, 'c'),
           (162, 420), (162, 330), (150, 196, 'c'), (138, 330), (138, 420), (140, 512, 'c'), (32, 508, 'c'), (50, 420), (60, 330), (54, 230), (50, 140), (58, 70)]
    base = smooth(pts)
    thread = '#C99A52'
    tex = f'<rect width="{W}" height="{H}" fill="url(#twill)"/>'
    loops = ''.join(f'<rect x="{x-4}" y="23" width="8" height="28" rx="2" fill="#000" opacity=".25" filter="url(#b1)"/><rect x="{x-4}" y="22" width="8" height="26" rx="2" fill="{color}"/>'
                    + stitch(f'M{x} 24 L{x} 46', thread, .7, 1, '2 2') for x in (84, 122, 178, 216))
    detail = (line('M63 48 L237 48', '#000', .3, 2) + stitch('M66 30 L234 30', thread, .8, 1.2) + stitch('M66 44 L234 44', thread, .8, 1.2)
              + loops
              + stitch('M161 50 L161 124 Q161 144 149 154', thread, .85, 1.3) + line('M150 50 L150 166', '#000', .28, 2)
              + stitch('M66 52 Q92 56 100 54 Q92 90 58 104', thread, .8, 1.2) + line('M66 50 Q96 54 102 52 Q94 92 56 108', '#000', .25, 1.5)
              + stitch(f'M{W-66} 52 Q{W-92} 56 {W-100} 54 Q{W-92} 90 {W-58} 104', thread, .8, 1.2) + line(f'M{W-66} 50 Q{W-96} 54 {W-102} 52 Q{W-94} 92 {W-56} 108', '#000', .25, 1.5)
              # coin pocket
              + stitch(f'M{W-78} 56 L{W-80} 76 L{W-102} 76', thread, .7, 1)
              + button(150, 37, 7, metal=True) + button(102, 55, 2.8, metal=True) + button(198, 55, 2.8, metal=True)
              # outseam topstitch
              + stitch('M53 150 Q58 240 58 330 Q52 420 36 500', thread, .45, 1) + stitch(f'M{W-53} 150 Q{W-58} 240 {W-58} 330 Q{W-52} 420 {W-36} 500', thread, .45, 1)
              + stitch('M35 496 L139 500', thread, .7, 1.2) + stitch(f'M{W-35} 496 L{W-139} 500', thread, .7, 1.2))
    shade = (K('M150 196 L146 508', .5, 6, 9) + K('M150 196 Q155 330 158 508', .5, 6, 8)
             + K('M54 150 Q60 330 40 500', .3, 12, 14) + K(f'M{W-54} 150 Q{W-60} 330 {W-40} 500', .36, 12, 14)
             # knee break + flare folds
             + K('M62 330 Q100 324 138 332', .2, 6, 6) + K(f'M{W-62} 330 Q{W-100} 324 {W-138} 332', .22, 6, 6)
             + K('M88 380 Q74 450 60 506', .28, 6, 7) + K('M116 390 Q118 450 120 506', .24, 6, 6)
             + K(f'M{W-88} 380 Q{W-74} 450 {W-60} 506', .3, 6, 7) + K(f'M{W-116} 390 Q{W-118} 450 {W-120} 506', .26, 6, 6)
             + K('M40 480 Q90 470 138 482', .2, 6, 6) + K(f'M{W-40} 480 Q{W-90} 470 {W-138} 482', .2, 6, 6))
    light = (Wt('M96 110 Q100 220 98 320', .45, 22, 30) + Wt(f'M{W-96} 110 Q{W-100} 220 {W-98} 320', .32, 22, 28)
             + Wt('M100 360 Q92 430 86 500', .3, 12, 18) + Wt(f'M{W-100} 360 Q{W-92} 430 {W-86} 500', .2, 12, 18)
             + ''.join(Wt(f'M{140-i*7} {168+i*4} Q{118-i*6} {178+i*6} {98-i*4} {176+i*12}', .38, 3, 3) for i in range(4))
             + ''.join(Wt(f'M{160+i*7} {168+i*4} Q{182+i*6} {178+i*6} {202+i*4} {176+i*12}', .32, 3, 3) for i in range(4))
             + Wt('M70 336 Q100 330 132 338', .22, 6, 8) + Wt(f'M{W-70} 336 Q{W-100} 330 {W-132} 338', .16, 6, 8))
    return piece(W, H, base, color, (.12, .3), shade, light, tex, detail, grain=(1.6, .5), name='bootcut')
