from kit.art_kit import *

def tee(color):
    """Boxy semi-oversized tee: dropped shoulder, elbow-ish sleeve with cuff fold, ribbed crew neck, jersey."""
    W, H = 400, 320
    left = [(200, 38, 'c'), (166, 32, 'c'), (108, 48, 'c'), (84, 66), (64, 104), (46, 160, 'c'), (98, 180, 'c'), (106, 156), (112, 134, 'c'),
            (110, 210), (108, 290, 'c'), (200, 294, 'c')]
    base = smooth(sym(left, W))
    neck_in = 'M166 32 Q172 58 200 61 Q228 58 234 32 Q200 44 166 32 Z'
    nb = 'M164 31 Q171 67 200 69 Q229 67 236 31'
    detail = (shape(neck_in, '#000', .5) + K(neck_in, .3, 3)
              + f'<path d="{nb}" fill="none" stroke="{color}" stroke-width="9"/>'
              + f'<path d="{nb}" fill="none" stroke="url(#rib)" stroke-width="9"/>'
              + line('M161 30 Q169 73 200 75 Q231 73 239 30', '#000', .2, 1.5)
              + stitch('M164 34 Q171 71 200 73 Q229 71 236 34', '#000', .25, .9, '2 2')
              # dropped-shoulder armhole seams
              + line('M92 60 Q100 100 112 134', '#000', .22, 1.5) + line(f'M{W-92} 60 Q{W-100} 100 {W-112} 134', '#000', .22, 1.5)
              # sleeve cuff fold + twin-needle hem
              + line('M52 148 L100 166', '#000', .25, 1.6) + stitch('M50 154 L99 172', '#000', .3, 1, '3 2')
              + line(f'M{W-52} 148 L{W-100} 166', '#000', .25, 1.6) + stitch(f'M{W-50} 154 L{W-99} 172', '#000', .3, 1, '3 2')
              + stitch('M112 280 Q200 284 288 280', '#000', .3, 1, '3 2') + stitch('M112 285 Q200 289 288 285', '#000', .25, 1, '3 2'))
    shade = (K('M112 140 Q114 220 110 292', .4, 12, 12) + K(f'M{W-112} 140 Q{W-114} 220 {W-110} 292', .45, 12, 12)
             + K('M64 110 Q54 140 48 158', .2, 6, 8) + K(f'M{W-64} 110 Q{W-54} 140 {W-48} 158', .25, 6, 8)
             + K('M52 149 L100 167', .25, 3, 6) + K(f'M{W-52} 149 L{W-100} 167', .28, 3, 6)
             # soft drape folds from armpit and a hem ripple
             + K('M122 140 Q150 160 160 200', .14, 6, 6) + K(f'M{W-122} 140 Q{W-150} 166 {W-156} 210', .16, 6, 6)
             + K('M170 230 Q176 262 168 300', .12, 6, 7) + K('M250 220 Q244 260 252 300', .14, 6, 7)
             + K('M160 110 Q200 120 240 110', .08, 12, 20))
    light = (Wt('M150 100 Q160 170 148 260', .2, 22, 40) + Wt('M84 70 Q68 110 60 136', .22, 6, 8)
             + Wt('M156 192 Q166 230 160 280', .12, 6, 6) + Wt('M232 120 Q250 190 236 260', .08, 22, 30))
    return piece(W, H, base, color, (.15, .26), shade, light, '', f'<g clip-path="url(#c)">{detail}</g>', grain=(1.7, .25), name='tee')
