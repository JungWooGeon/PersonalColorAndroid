from kit.art_kit import *

def hobo(color):
    """Slouchy crescent hobo: soft leather, gathered ends into a short shoulder strap, zip top, drape folds."""
    W, H = 320, 240
    body = smooth([(160, 124), (232, 102), (262, 92, 'c'), (292, 128), (286, 172), (244, 210), (160, 224), (76, 210), (34, 172), (28, 128), (58, 92, 'c'), (88, 102)])
    strap = 'M66 98 Q70 20 160 18 Q250 20 254 98'
    under = (f'<path d="{strap}" fill="none" stroke="#000" stroke-opacity=".25" stroke-width="18" transform="translate(1 3)" filter="url(#b3)"/>'
             f'<path d="{strap}" fill="none" stroke="{color}" stroke-width="16"/><path d="{strap}" fill="none" stroke="#000" stroke-opacity=".18" stroke-width="16"/>'
             + Wt('M74 70 Q90 30 160 26', .4, 3, 4) + stitch('M58 96 Q62 12 160 9 Q258 12 262 96', '#fff', .18, .9, '3 3')
             + stitch('M76 98 Q80 28 160 27 Q240 28 244 98', '#fff', .18, .9, '3 3')
             # inside of the opening
             + f'<path d="M88 104 Q160 140 232 104 Q160 118 88 104 Z" fill="#000" opacity=".6"/>')
    # gathered ends: knot-like wrapped bands
    ends = ''
    for cx, s in ((66, 1), (254, -1)):
        band = f'M{cx - 15} 88 Q{cx} 82 {cx + 15} 88 L{cx + 14} 108 Q{cx} 114 {cx - 14} 108 Z'
        ends += raised(band, color, .4, .2) + K(f'M{cx - 12} 104 Q{cx} 110 {cx + 12} 104', .3, 1, 2) + Wt(f'M{cx - 10} 89 Q{cx} 85 {cx + 10} 89', .4, 1, 2)
    zip_ = (line('M90 106 Q160 142 230 106', '#000', .55, 3) + line('M90 104 Q160 140 230 104', '#fff', .14, 1)
            + '<path d="M90 106 Q160 142 230 106" fill="none" stroke="#B89650" stroke-width="2" stroke-dasharray="1.5 2"/>')
    pull = (f'<rect x="196" y="122" width="6" height="22" rx="3" fill="#000" opacity=".3" filter="url(#b1)" transform="translate(1 2)"/>'
            f'<rect x="196" y="122" width="6" height="22" rx="3" fill="#C9A85C"/><rect x="197" y="124" width="2" height="16" rx="1" fill="#FFF3C8" opacity=".7"/>')
    detail = ends + zip_ + pull
    # drape folds radiating from the gathered ends
    folds_k = (K('M74 110 Q100 160 132 210', .32, 6, 8) + K('M246 110 Q220 160 188 210', .32, 6, 8)
               + K('M66 112 Q60 150 80 196', .25, 3, 4) + K('M254 112 Q260 150 240 196', .25, 3, 4)
               + K('M84 110 Q130 170 160 220', .14, 6, 6))
    folds_w = (Wt('M86 114 Q110 166 146 212', .3, 6, 8) + Wt('M234 114 Q210 166 174 212', .18, 6, 8)
               + Wt('M56 116 Q48 150 64 184', .2, 3, 4) )
    shade = K('M44 190 Q160 250 276 190', .45, 12, 22) + K('M288 130 Q284 180 236 214', .3, 12, 14) + folds_k
    light = folds_w
    return piece(W, H, body, color, (.2, .3), shade, light, '', detail, under=under, grain=(1.3, .22), name='hobo')
