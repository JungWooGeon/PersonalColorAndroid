from kit.art_kit import *

def _pocket(x, y, color, flip=False):
    """Bellows cargo pocket with flap + snap, left leg at x (right leg mirrored)."""
    s = -1 if flip else 1
    body = smooth([(x, y + 16, 'c'), (x + s * 66, y + 16, 'c'), (x + s * 68, y + 96, 'c'), (x + s * 2, y + 98, 'c')])
    flap = smooth([(x - s * 3, y, 'c'), (x + s * 70, y, 'c'), (x + s * 70, y + 26, 'c'), (x + s * 35, y + 32, 'c'), (x - s * 3, y + 26, 'c')])
    return (raised(body, color, .3, .1) + f'<path d="{body}" fill="url(#fg)"/><path d="{body}" fill="#fff" opacity=".06"/>' + stitch(smooth([(x + s * 5, y + 20, 'c'), (x + s * 61, y + 20, 'c'), (x + s * 63, y + 92, 'c'), (x + s * 7, y + 93, 'c')]), '#000', .3, 1)
            + line(f'M{x+s*20} {y+30} L{x+s*21} {y+96}', '#000', .14, 3) + line(f'M{x+s*46} {y+30} L{x+s*47} {y+96}', '#000', .14, 3)
            + raised(flap, color, .4, .14) + f'<path d="{flap}" fill="url(#fg)"/><path d="{flap}" fill="#fff" opacity=".09"/>' + stitch(smooth([(x + s * 2, y + 4, 'c'), (x + s * 65, y + 4, 'c'), (x + s * 65, y + 22, 'c'), (x + s * 35, y + 27, 'c'), (x + s * 2, y + 22, 'c')]), '#000', .3, 1)
            + f'<circle cx="{x+s*35}" cy="{y+20}" r="4.5" fill="#B9BCC0"/><circle cx="{x+s*35}" cy="{y+20}" r="2" fill="#7d8186"/>')

def cargo(color):
    """Low-rise wide parachute cargo: curved low waist, big bellows pockets, toggle-cinched hem."""
    W, H = 300, 520
    pts = [(56, 40, 'c'), (150, 52), (244, 40, 'c'), (246, 62, 'c'), (254, 120), (264, 230), (272, 380), (270, 470), (262, 500, 'c'), (214, 508), (166, 502, 'c'),
           (160, 340), (150, 188, 'c'), (140, 340), (134, 502, 'c'), (86, 508), (38, 500, 'c'), (30, 470), (28, 380), (36, 230), (46, 120), (54, 62, 'c')]
    base = smooth(pts)
    tex = '<rect width="300" height="520" fill="url(#twill)" opacity=".6"/>'
    detail = (line('M55 62 Q150 76 245 62', '#000', .3, 2) + stitch('M57 45 Q150 58 243 45', '#000', .25, 1) + stitch('M56 58 Q150 72 244 58', '#000', .25, 1)
              + ''.join(f'<rect x="{x-4}" y="{y-2}" width="8" height="26" rx="2" fill="#000" opacity=".25" filter="url(#b1)"/><rect x="{x-4}" y="{y-3}" width="8" height="24" rx="2" fill="{color}"/>'
                        for x, y in ((84, 47), (126, 53), (174, 53), (216, 47)))
              + f'<circle cx="150" cy="62" r="6" fill="#B9BCC0"/><circle cx="148" cy="60" r="2.2" fill="#fff" opacity=".7"/>'
              + stitch('M160 72 L160 140 Q160 160 150 168', '#000', .3, 1.1) + line('M150 72 L150 186', '#000', .25, 1.6)
              + line('M60 66 Q92 76 100 72 Q86 104 52 116', '#000', .25, 1.5) + line(f'M{W-60} 66 Q{W-92} 76 {W-100} 72 Q{W-86} 104 {W-52} 116', '#000', .25, 1.5)
              + _pocket(34, 238, color) + _pocket(W - 34, 238, color, True)
              + stitch('M36 486 L132 490', '#000', .3, 1) + stitch(f'M{W-36} 486 L{W-132} 490', '#000', .3, 1)
              + line('M46 490 Q40 504 34 512', '#1d1d1d', .9, 2.2) + '<rect x="28" y="508" width="10" height="9" rx="3" fill="#2b2b2b"/>'
              + line(f'M{W-46} 490 Q{W-40} 504 {W-34} 512', '#1d1d1d', .9, 2.2) + f'<rect x="{W-38}" y="508" width="10" height="9" rx="3" fill="#2b2b2b"/>')
    shade = (K('M150 188 L146 500', .45, 6, 10) + K('M150 188 Q154 330 158 500', .45, 6, 8)
             + K('M42 150 Q30 330 30 470', .3, 12, 14) + K(f'M{W-42} 150 Q{W-30} 330 {W-30} 470', .35, 12, 14)
             + ''.join(K(f'M{50+i*20} 498 L{56+i*18} {452-i%2*10}', .28, 3, 3) for i in range(5))
             + ''.join(K(f'M{W-50-i*20} 498 L{W-56-i*18} {452-i%2*10}', .3, 3, 3) for i in range(5))
             + K('M60 420 Q96 410 134 426', .2, 6, 6) + K(f'M{W-60} 420 Q{W-96} 410 {W-134} 426', .22, 6, 6))
    light = (Wt('M96 110 Q90 300 92 470', .3, 22, 34) + Wt(f'M{W-96} 110 Q{W-90} 300 {W-92} 470', .2, 22, 30)
             + ''.join(Wt(f'M{60+i*20} 496 L{64+i*18} {458-i%2*10}', .22, 3, 3) for i in range(4))
             + ''.join(Wt(f'M{W-60-i*20} 496 L{W-64-i*18} {458-i%2*10}', .18, 3, 3) for i in range(4))
             + Wt('M136 196 Q114 212 92 214', .28, 3, 4) + Wt('M164 196 Q186 212 208 214', .22, 3, 4)
             + Wt('M60 400 Q96 392 132 404', .18, 6, 8) + Wt(f'M{W-60} 400 Q{W-96} 392 {W-132} 404', .14, 6, 8))
    return piece(W, H, base, color, (.14, .3), shade, light, tex, detail, grain=(1.5, .35), name='cargo')
