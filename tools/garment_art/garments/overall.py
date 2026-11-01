from kit.art_kit import *

def overall(color):
    """Denim overall mini dress (멜빵 원피스): bib, crossed straps with gold buckles, A-line skirt."""
    W, H = 320, 480
    thread = '#C99A52'
    left = [(160, 74), (108, 74, 'c'), (110, 176, 'c'), (96, 178, 'c'), (92, 210), (74, 320), (54, 430), (40, 458, 'c'), (100, 464), (160, 466, 'c')]
    body = smooth(sym(left, W))
    strap = lambda a, b, c, d: f'M{a[0]} {a[1]} L{b[0]} {b[1]} L{c[0]} {c[1]} L{d[0]} {d[1]} Z'
    sL = strap((110, 82), (96, 6), (122, 6), (130, 82))
    sR = strap((W - 110, 82), (W - 96, 6), (W - 122, 6), (W - 130, 82))
    base = body + ' ' + sL + ' ' + sR
    tex = f'<rect width="{W}" height="{H}" fill="url(#twill)"/>'
    pocket = 'M132 104 L188 104 L188 140 Q160 150 132 140 Z'
    skp = lambda x: f'M{x} 250 L{x+50} 250 L{x+50} 290 Q{x+25} 300 {x} 290 Z'
    fabric = raised(pocket, color, .25, .1) + raised(skp(92), color, .25, .1) + raised(skp(178), color, .25, .1)
    buckle = lambda x, y: (f'<rect x="{x-12}" y="{y-9}" width="24" height="18" rx="3" fill="#000" opacity=".35" filter="url(#b1)" transform="translate(1 2)"/>'
                           f'<rect x="{x-12}" y="{y-9}" width="24" height="18" rx="3" fill="none" stroke="#B89650" stroke-width="4"/>'
                           f'<rect x="{x-12}" y="{y-9}" width="24" height="18" rx="3" fill="none" stroke="#FFF3C8" stroke-width="1" opacity=".6"/>'
                           f'<line x1="{x-12}" y1="{y}" x2="{x+12}" y2="{y}" stroke="#B89650" stroke-width="3"/>')
    detail = (stitch('M112 80 L114 172', thread, .85, 1.2) + stitch(f'M{W-112} 80 L{W-114} 172', thread, .85, 1.2)
              + stitch('M112 80 L208 80', thread, .85, 1.2) + stitch('M136 108 L184 108 L184 138 Q160 146 136 138 Z', thread, .85, 1.1)
              + stitch('M101 12 L114 80', thread, .8, 1.1) + stitch('M117 12 L126 80', thread, .8, 1.1)
              + stitch(f'M{W-101} 12 L{W-114} 80', thread, .8, 1.1) + stitch(f'M{W-117} 12 L{W-126} 80', thread, .8, 1.1)
              + line('M96 180 L224 180', '#000', .3, 2) + stitch('M96 184 L224 184', thread, .8, 1.2) + stitch('M95 204 L225 204', thread, .8, 1.2)
              + line('M94 206 L226 206', '#000', .22, 1.5)
              + stitch('M157 206 L157 462', thread, .8, 1.2) + line('M160 206 L160 464', '#000', .25, 1.6)
              + stitch('M96 254 L138 254 L138 288 Q117 296 96 288', thread, .8, 1.1) + stitch(f'M{W-96} 254 L{W-138} 254 L{W-138} 288 Q{W-117} 296 {W-96} 288', thread, .8, 1.1)
              + stitch('M44 450 Q100 456 160 458 Q220 456 276 450', thread, .8, 1.2)
              + buckle(120, 86) + buckle(W - 120, 86) + button(98, 194, 5, metal=True) + button(W - 98, 194, 5, metal=True))
    shade = (K('M110 90 L110 176', .3, 6, 8) + K(f'M{W-110} 90 L{W-110} 176', .35, 6, 8)
             + K('M94 210 Q74 330 46 456', .3, 12, 14) + K(f'M{W-94} 210 Q{W-74} 330 {W-46} 456', .35, 12, 14)
             + K('M124 300 Q112 380 98 458', .25, 12, 8) + K('M196 300 Q210 380 222 458', .28, 12, 8)
             + K('M100 182 L220 182', .25, 6, 6))
    light = (Wt('M134 220 Q126 340 114 450', .25, 22, 30) + Wt(f'M{W-150} 230 Q{W-144} 340 {W-136} 450', .15, 22, 24)
             + Wt('M130 90 Q160 96 190 90', .2, 6, 10) + Wt('M104 16 L118 78', .2, 3, 6))
    return piece(W, H, base, color, (.14, .3), shade, light, tex, detail, fabric=fabric, grain=(1.6, .45), name='overall')
