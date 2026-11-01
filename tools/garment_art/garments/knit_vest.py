from kit.art_kit import *

def cable(x, y0, y1, w=10, step=20):
    """Rope cable: stacked diagonal twists (lit top-left, shadowed bottom-right) between two purl channels."""
    out = ''
    for y in range(y0, y1, step):
        d = f'M{x-w} {y+4} Q{x-w+2} {y-2} {x} {y+4} L{x+w} {y+step-2} Q{x+w-2} {y+step+4} {x} {y+step-2} Z'
        out += (f'<path d="{d}" fill="#000" opacity=".22" transform="translate(1.5 3)" filter="url(#b1)"/>'
                + f'<path d="{d}" fill="#fff" opacity=".13"/>' + line(f'M{x-w+2} {y+2} Q{x-w/2} {y-1} {x} {y+4}', '#fff', .3, 1.4))
    return out + K(f'M{x-w-4} {y0} L{x-w-4} {y1}', .3, 1, 3) + K(f'M{x+w+4} {y0} L{x+w+4} {y1}', .3, 1, 3)

def knit_vest(color):
    """Cropped V-neck cable knit vest with rib trims (preppy 니트 베스트)."""
    W, H = 360, 280
    left = [(180, 34, 'c'), (146, 28, 'c'), (118, 34, 'c'), (114, 64), (104, 98), (86, 118, 'c'), (90, 180), (92, 234, 'c'), (94, 264, 'c'), (180, 268, 'c')]
    base = smooth(sym(left, W))
    v = 'M146 28 L180 128 L214 28 Q180 40 146 28 Z'
    vtrim = 'M144 28 L180 138 L216 28'
    arm = 'M120 34 Q116 96 86 120'
    tex = (f'<rect width="{W}" height="{H}" fill="url(#stock)"/>' + cable(180, 146, 234) + cable(130, 46, 234) + cable(230, 46, 234))
    rib = (f'<rect x="0" y="234" width="{W}" height="50" fill="url(#rib)"/>' + line('M90 236 Q180 232 270 236', '#000', .3, 2.5))
    trim = lambda d: (f'<path d="{d}" fill="none" stroke="#000" stroke-opacity=".3" stroke-width="13" transform="translate(1 2)" filter="url(#b3)"/>'
                      f'<path d="{d}" fill="none" stroke="{color}" stroke-width="11"/><path d="{d}" fill="none" stroke="url(#ribH)" stroke-width="11"/>')
    detail = (shape(v, '#000', .45) + K(v, .3, 6) + trim(vtrim) + line('M154 30 L180 112 L206 30', '#000', .2, 1.2)
              + trim(arm) + trim(f'M{W-120} 34 Q{W-116} 96 {W-86} 120'))
    shade = (rib + K('M94 126 Q96 200 96 266', .35, 12, 12) + K(f'M{W-94} 126 Q{W-96} 200 {W-96} 266', .4, 12, 12)
             + K('M150 170 Q160 220 150 280', .12, 22, 24))
    light = Wt('M150 70 Q160 140 150 220', .18, 22, 36) + Wt('M100 140 Q96 200 100 250', .14, 12, 10)
    return piece(W, H, base, color, (.14, .26), shade, light, tex, detail, grain=(1.4, .35), name='knit_vest')
