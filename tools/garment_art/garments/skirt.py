from kit.art_kit import *

def skirt(color):
    """Satin bias-cut midi skirt: flat waistband, soft flare, fluted hem, liquid sheen."""
    W, H = 320, 430
    n = 5
    hem = []
    for i in range(n * 2 + 1):            # crests (forward folds) dip lower, troughs rise
        t = i / (n * 2); x = 30 + 260 * t
        hem.append((x, 404 + (5 if i % 2 else -2) + 6 * (1 - (2 * t - 1) ** 2), 'c' if i in (0, n * 2) else None))
    hem = [p if p[2] else p[:2] for p in hem]
    base = smooth([(98, 14, 'c'), (222, 14, 'c'), (224, 40, 'c'), (238, 130), (262, 270), (290, 400, 'c')]
                  + list(reversed(hem[1:-1])) + [(30, 400, 'c'), (58, 270), (82, 130), (96, 40, 'c')])
    shade, light = '', ''
    for i in range(1, n * 2, 2):          # fold valleys from hip to hem troughs
        x = hem[i][0]; xt = 100 + 120 * (i / (n * 2))
        shade += K(f'M{xt:.0f} 70 Q{(xt + x) / 2:.0f} 250 {x:.0f} {hem[i][1] + 4:.0f}', .32, 6, 9)
    for i in range(2, n * 2 - 1, 2):      # satin sheen on the fold ridges
        x = hem[i][0]; xt = 100 + 120 * (i / (n * 2))
        light += Wt(f'M{xt:.0f} 90 Q{(xt + x) / 2:.0f} 260 {x:.0f} {hem[i][1] - 10:.0f}', .30, 3, 4)
        light += Wt(f'M{xt:.0f} 100 Q{(xt + x) / 2:.0f} 260 {x:.0f} {hem[i][1] - 20:.0f}', .16, 6, 12)
    shade += (K('M96 40 L224 40', .3, 6, 8) + K('M70 200 Q48 320 34 404', .3, 12, 18) + K('M250 200 Q272 320 288 404', .35, 12, 18)
              + K('M40 396 Q160 412 282 396', .18, 6, 10))
    light += Wt('M128 50 Q118 220 104 400', .32, 12, 18) + Wt('M118 22 L200 22', .18, 3, 3)
    detail = (line('M96 40 L224 40', '#000', .3, 2) + stitch('M100 18 L220 18', '#000', .18, 1) + stitch('M100 36 L220 36', '#000', .18, 1)
              + line('M216 18 L216 40', '#000', .18, 1))   # side zip line at waistband
    return piece(W, H, base, color, (.2, .3), shade, light, '', detail, grain=(1.6, .12), name='skirt')
