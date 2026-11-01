from kit.art_kit import *

def scarf(color):
    """Square silk twill scarf: tone-on-tone mini polka dots, dotted border band, rolled hem, soft satin ripples."""
    W, H = 320, 320
    base = smooth([(22, 26, 'c'), (90, 20), (170, 28), (240, 20), (298, 26, 'c'), (292, 100), (300, 180), (294, 298, 'c'),
                   (220, 292), (150, 300), (80, 292), (26, 298, 'c'), (30, 220), (20, 140), (28, 70)])
    frame = smooth([(50, 52, 'c'), (270, 52, 'c'), (270, 270, 'c'), (50, 270, 'c')])
    inner = smooth([(60, 62, 'c'), (260, 62, 'c'), (260, 260, 'c'), (60, 260, 'c')])
    pts = [(x + (7 if (y // 14) % 2 else 0), y) for y in range(76, 252, 14) for x in range(74, 252, 14) if x + (7 if (y // 14) % 2 else 0) < 252]
    dots = ''.join(f'<circle cx="{x}" cy="{y + .8}" r="3" fill="#000" opacity=".12"/><circle cx="{x}" cy="{y}" r="2.7" fill="#fff" opacity=".36"/>' for x, y in pts)
    rim = ''.join(f'<circle cx="{a}" cy="{b}" r="2.4" fill="#fff" opacity=".42"/>'
                  for t in range(44, 280, 12) for a, b in ((t, 37), (t, 283), (37, t), (283, t)))
    texture = (f'<rect width="{W}" height="{H}" fill="url(#twill)" opacity=".45"/>'
               + f'<path d="M0 0 H{W} V{H} H0 Z M50 52 L270 52 L270 270 L50 270 Z" fill="#000" fill-rule="evenodd" opacity=".16"/>'
               + dots + rim + line(frame, '#fff', .5, 2) + line(inner, '#fff', .32, 1) + line('M50 272 L270 272 L270 54', '#000', .18, 3))
    # satin ripples
    shade = (K('M10 110 Q100 150 170 90 Q240 30 310 60', .28, 12, 22) + K('M10 230 Q90 250 160 200 Q240 140 310 170', .3, 12, 22)
             + K('M40 310 Q150 280 300 300', .18, 12, 20))
    light = (Wt('M10 60 Q90 100 160 50 Q220 10 300 20', .32, 12, 22) + Wt('M10 180 Q100 200 170 140 Q240 90 310 110', .3, 12, 20)
             + Wt('M30 176 Q100 192 160 146', .45, 3, 4) + Wt('M30 58 Q90 90 140 60', .4, 3, 3))
    hem = line(base, '#fff', .3, 2.4) + line(base, '#000', .2, 1)
    return piece(W, H, base, color, (.2, .3), shade, light, texture, hem, grain=(1.6, .18), name='scarf')
