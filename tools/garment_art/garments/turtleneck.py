from kit.art_kit import *

def turtleneck(color):
    W, H = 360, 450
    left = [(180, 62, 'c'), (146, 58, 'c'), (104, 74, 'c'), (90, 90), (78, 150), (64, 250), (52, 350), (44, 418, 'c'), (84, 424, 'c'),
            (90, 350), (100, 250), (112, 160, 'c'), (114, 260), (114, 340), (112, 424, 'c'), (180, 428, 'c')]
    base = smooth(sym(left, W))
    collar = smooth([(140, 14, 'c'), (180, 10), (220, 14, 'c'), (224, 72, 'c'), (180, 78), (136, 72, 'c')])
    tex = f'<rect width="{W}" height="{H}" fill="url(#rib)"/>'
    collar_g = (f'<path d="{collar}" fill="#000" opacity=".35" transform="translate(0 4)" filter="url(#b3)"/><path d="{collar}" fill="{color}"/>'
                f'<clipPath id="cc"><path d="{collar}"/></clipPath><g clip-path="url(#cc)"><rect width="{W}" height="100" fill="url(#rib)"/>'
                f'{K("M136 46 Q180 54 224 46", .35, 3, 6)}{Wt("M136 52 Q180 60 224 52", .2, 3, 4)}{K("M140 14 Q180 24 220 14", .5, 3, 10)}'
                f'{Wt("M150 30 L150 70", .2, 6, 14)}{K("M216 20 L216 74", .3, 6, 14)}</g>')
    detail = (collar_g + line('M114 404 L246 404', '#000', .22, 2) + line('M48 396 Q66 398 86 402', '#000', .22, 2)
              + line(f'M{W-48} 396 Q{W-66} 398 {W-86} 402', '#000', .22, 2))
    shade = (K('M112 164 Q118 280 112 420', .45, 12, 12) + K(f'M{W-112} 164 Q{W-118} 280 {W-112} 420', .5, 12, 12)
             + K('M86 120 Q76 260 64 410', .2, 12, 10) + K(f'M{W-86} 120 Q{W-76} 260 {W-64} 410', .25, 12, 10)
             + K('M150 240 Q180 300 170 400', .14, 22, 30))
    light = Wt('M140 110 Q150 200 140 300', .2, 22, 40) + Wt('M82 110 Q70 230 56 360', .2, 12, 10) + Wt('M216 300 Q230 340 220 400', .08, 12, 20)
    return piece(W, H, base, color, (.16, .3), shade, light, tex, detail, grain=(1.2, .3), name='turtleneck')
