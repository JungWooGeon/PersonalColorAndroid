from kit.art_kit import *

def sweatshirt(color):
    """Oversized drop-shoulder 맨투맨: rib crew neck with V-gusset, gathered rib cuffs, blouson rib hem, tiny chest embroidery."""
    W, H = 420, 400
    left = [(210, 50, 'c'), (172, 42, 'c'), (96, 60, 'c'), (70, 100), (52, 180), (38, 262), (30, 316, 'c'), (38, 322, 'c'), (40, 362, 'c'),
            (82, 364, 'c'), (82, 324, 'c'), (90, 316, 'c'), (100, 240), (116, 170, 'c'), (110, 250), (104, 334, 'c'), (114, 342, 'c'),
            (116, 384, 'c'), (210, 388, 'c')]
    base = smooth(sym(left, W))
    neck_in = 'M176 44 Q184 80 210 84 Q236 80 244 44 Q210 54 176 44 Z'
    nk = 'M172 42 Q182 90 210 94 Q238 90 248 42'
    rib = (f'<rect x="100" y="340" width="220" height="50" fill="url(#rib)"/>' + line('M106 342 Q210 336 314 342', '#000', .32, 2.5)
           + f'<g transform="rotate(3 60 340)"><rect x="30" y="322" width="56" height="44" fill="url(#rib)"/></g>'
           + f'<g transform="rotate(-3 {W-60} 340)"><rect x="{W-86}" y="322" width="56" height="44" fill="url(#rib)"/></g>'
           + line('M36 322 Q60 326 84 326', '#000', .3, 2.5) + line(f'M{W-36} 322 Q{W-60} 326 {W-84} 326', '#000', .3, 2.5)
           # gathers where the sleeve and body puff over the ribs
           + ''.join(K(f'M{x} {300} L{x+2} 322', .22, 3, 3) for x in (48, 60, 72)) + ''.join(K(f'M{W-x} {300} L{W-x-2} 322', .25, 3, 3) for x in (48, 60, 72))
           + ''.join(K(f'M{x} 316 L{x+1} 340', .2, 3, 3) for x in (130, 160, 196, 232, 264, 292)))
    heart = 'M268 152 Q258 140 264 134 Q270 130 274 138 Q278 130 284 134 Q290 140 280 152 L274 158 Z'  # small embroidered heart, left chest
    detail = (shape(neck_in, '#000', .45) + K(neck_in, .3, 6)
              + f'<path d="{nk}" fill="none" stroke="{color}" stroke-width="12"/><path d="{nk}" fill="none" stroke="url(#rib)" stroke-width="12"/>'
              + line(nk, '#fff', .1, 1) + line('M166 44 Q178 102 210 104 Q242 102 254 44', '#000', .2, 2)
              + line('M200 104 L210 124 L220 104', '#000', .22, 1.5) + stitch('M202 106 L210 120 L218 106', '#fff', .25, 1, '2 2')
              + stitch('M96 62 Q102 120 116 170', '#000', .32, 2, '1 0') + stitch(f'M{W-96} 62 Q{W-102} 120 {W-116} 170', '#000', .32, 2, '1 0')
              + stitch('M98 66 Q104 120 118 168', '#fff', .14, 1, '3 3') + stitch(f'M{W-98} 66 Q{W-104} 120 {W-118} 168', '#fff', .14, 1, '3 3')
              + line(heart, '#000', .35, 3.2) + line(heart, '#fff', .6, 1.6) + stitch(heart, '#000', .25, 1.6, '1 1.6'))
    shade = (rib + K('M114 174 Q106 260 110 340', .4, 12, 14) + K(f'M{W-114} 174 Q{W-106} 260 {W-110} 340', .45, 12, 14)
             + K('M72 120 Q56 220 46 310', .2, 12, 12) + K(f'M{W-72} 120 Q{W-56} 220 {W-46} 310', .25, 12, 12)
             + K('M160 210 Q200 260 176 330', .14, 22, 30) + K('M264 150 Q282 240 272 330', .14, 22, 26)
             + K('M120 334 Q210 322 300 334', .2, 6, 8))
    light = (Wt('M150 110 Q170 180 150 260', .2, 22, 46) + Wt('M72 100 Q54 200 40 290', .2, 12, 14) + Wt('M232 210 Q252 270 238 320', .1, 22, 30)
             + Wt('M130 318 Q210 306 290 318', .12, 6, 6))
    return piece(W, H, base, color, (.16, .28), shade, light, '', detail, grain=(1.6, .4), name='sweatshirt')
