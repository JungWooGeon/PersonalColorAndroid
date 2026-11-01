from kit.art_kit import *

def clutch(color):
    """Soft leather pouch clutch (29CM / 'the pouch' style): slouchy rectangle gathered into a slim top frame, irregular soft folds."""
    W, H = 300, 250
    body = smooth([(52, 86), (76, 92), (100, 88), (122, 93), (150, 89), (176, 93), (200, 88), (226, 92), (248, 86),
                   (256, 104), (258, 150), (262, 198), (246, 216), (150, 220), (54, 216), (38, 198), (42, 150), (44, 104)])
    frame = 'M60 92 Q150 86 240 92'
    base = body
    # irregular gathers: different lengths/angles, a few deep valleys, a few soft ridges
    valleys = [('M88 96 Q76 118 60 140', .3, 6, 8), ('M112 96 Q104 128 96 162', .36, 6, 9), ('M138 96 Q136 116 132 132', .26, 6, 6),
               ('M166 96 Q172 130 178 168', .38, 6, 10), ('M196 96 Q208 118 226 138', .32, 6, 9), ('M216 96 Q232 104 246 118', .25, 6, 6)]
    ridges = [('M100 98 Q90 124 78 150', .42, 6, 9), ('M126 98 Q120 126 116 156', .38, 6, 8),
              ('M152 98 Q154 128 156 150', .45, 6, 9), ('M182 98 Q190 124 204 152', .36, 6, 8)]
    crisp = [('M101 100 Q93 120 84 138', .55, 1, 1.4), ('M152 100 Q153 120 154 136', .6, 1, 1.4), ('M183 100 Q189 118 198 136', .4, 1, 1.2)]
    shade = (''.join(K(d, a, b, w) for d, a, b, w in valleys)
             + K('M56 104 Q150 112 244 104', .4, 3, 7)                       # shadow cast by the frame
             + K('M46 196 Q150 232 254 196', .5, 12, 22) + K('M44 120 Q30 166 50 206', .32, 6, 12)
             + K('M256 120 Q270 166 250 206', .42, 6, 12) + K('M120 196 Q170 204 220 192', .16, 6, 8))
    light = (''.join(Wt(d, a, b, w) for d, a, b, w in ridges + crisp)
             + Wt('M70 168 Q100 194 160 196', .2, 12, 28)                    # broad leather sheen
             + Wt('M84 186 Q116 198 156 198', .45, 3, 3) + Wt('M96 190 Q120 197 140 197', .7, 1, 1.4)
             + Wt('M52 130 Q42 166 54 198', .3, 3, 4))
    detail = (line('M60 100 Q150 95 240 100', '#000', .18, 1)          # soft rolled top edge over the hidden frame
              + Wt('M62 94 Q100 90 122 96 M150 92 Q176 90 198 94 M206 92 Q226 90 244 90', .4, 1, 1.6))
    return piece(W, H, base, color, (.2, .3), shade, light, '', detail, grain=(1.3, .22), name='clutch')
