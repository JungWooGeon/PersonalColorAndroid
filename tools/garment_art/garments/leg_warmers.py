from kit.art_kit import *

JIT = [0, 0, 1, 0, 1, 0, 3, -1, 5, -1, 7, 1, 8, 4, 0]   # hand-tuned slouch so the folds don't read as stripes


def _tube(cx, y0=20, y1=460, n=14):
    L, R = [], []
    for i in range(n + 1):
        t = i / n; y = y0 + (y1 - y0) * t
        w = 56 + 10 * t + 16 * max(0, t - .5) ** 1.5
        j = JIT[i] if 0 < i < n else 0
        c = ('c',) if i in (0, n) else ()
        L.append((cx - w - j, y + (j * .4)) + c); R.append((cx + w + j * .8, y - j * .3) + c)
    return smooth(L + R[::-1])


def leg_warmers(color):
    """Slouchy ribbed knit leg warmers (Y2K / ballet-core): fold-over cuff, soft scrunches, flared ankle."""
    W, H = 340, 480
    cxs = (84, 256)
    base = ' '.join(_tube(cx) for cx in cxs)
    tex = (f'<pattern id="rib2" width="7" height="10" patternUnits="userSpaceOnUse"><rect width="3" height="10" fill="#fff" opacity=".13"/>'
           f'<rect x="3.5" width="3" height="10" fill="#000" opacity=".2"/></pattern><rect width="{W}" height="{H}" fill="url(#rib2)"/>')
    shade, light, detail = '', '', ''
    folds = [(236, 6, .3, -1), (290, -5, .4, 1), (330, 4, .45, -1), (364, -6, .5, 1), (394, 5, .5, -1), (420, -3, .45, 1), (444, 4, .4, -1)]
    for k, cx in enumerate(cxs):
        s = 1 if k == 0 else -1
        shade += K(f'M{cx - 60} 86 Q{cx} 92 {cx + 60} 86', .5, 3, 6)
        light += Wt(f'M{cx - 56} 76 Q{cx} 80 {cx + 56} 76', .3, 3, 7) + Wt(f'M{cx - 50} 26 L{cx + 50} 26', .2, 3, 6)
        detail += line(f'M{cx - 56} 84 Q{cx} 89 {cx + 56} 84', '#000', .25, 1.2)
        for y, tilt, a, side in folds:   # partial, tilted creases: one side gathers more than the other
            t, sd = tilt * s, side * s
            x0, x1 = (cx - 76, cx + 30) if sd < 0 else (cx - 30, cx + 78)
            shade += K(f'M{x0} {y - t} Q{(x0 + x1) / 2} {y + 8} {x1} {y + t}', a * .9, 3, 4)
            light += Wt(f'M{x0 + 6} {y - 12 - t} Q{(x0 + x1) / 2} {y - 6} {x1 - 6} {y - 12 + t}', .14, 6, 10)
        shade += K(f'M{cx + 54} 96 Q{cx + 62} 260 {cx + 78} 450', .35, 12, 18) + K(f'M{cx - 54} 96 Q{cx - 62} 260 {cx - 74} 450', .15, 12, 12)
        light += Wt(f'M{cx - 20} 100 Q{cx - 26} 260 {cx - 30} 440', .2, 22, 30)
    return piece(W, H, base, color, (.12, .24), shade, light, tex, detail, grain=(1.3, .3), name='leg_warmers')
