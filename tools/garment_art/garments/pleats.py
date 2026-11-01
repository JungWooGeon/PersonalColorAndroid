from kit.art_kit import *

def pleats(color):
    W, H = 320, 250
    n = 16
    hem = []
    for i in range(n + 1):
        t = i / n; x = 20 + 280 * t; y = 226 + 14 * (1 - (2 * t - 1) ** 2) * .5 + (6 if i % 2 else 0)
        hem.append((x, y, 'c'))
    base = smooth([(78, 14, 'c'), (242, 14, 'c'), (244, 40, 'c')] + list(reversed(hem)) + [(76, 40, 'c')])
    pleats_s, pleats_l = '', ''
    for i in range(n):
        t0, t1 = i / n, (i + 1) / n
        x0w, x1w = 78 + 164 * t0, 78 + 164 * t1
        x0, x1 = hem[i][0], hem[i + 1][0]
        poly = f'M{x0w} 40 L{x1w} 40 L{x1} {hem[i+1][1]} L{x0} {hem[i][1]} Z'
        if i % 2: pleats_s += f'<path d="{poly}" fill="#000" opacity=".2"/>'
        else: pleats_l += f'<path d="{poly}" fill="#fff" opacity=".07"/>'
        pleats_s += line(f'M{x0w} 42 L{x0} {hem[i][1]}', '#000', .25, 1.2)
    detail = (line('M77 40 L243 40', '#000', .3, 2) + stitch('M80 18 L240 18', '#000', .2, 1) + stitch('M80 36 L240 36', '#000', .2, 1))
    shade = pleats_s + K('M60 120 L30 230', .25, 12, 20) + K('M260 120 L292 230', .3, 12, 20) + K('M80 40 L242 40', .25, 6, 8)
    light = pleats_l + Wt('M120 60 Q110 150 100 220', .18, 22, 40)
    return piece(W, H, base, color, (.14, .26), shade, light, '', detail, grain=(1.3, .3), name='pleats')
