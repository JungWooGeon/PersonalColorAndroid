from kit.art_kit import *

def winter_boots(color):
    W, H = 320, 240
    o = smooth([(102, 62, 'c'), (214, 62, 'c'), (218, 120), (224, 188, 'c'), (34, 194, 'c'), (10, 182), (6, 156), (22, 134), (58, 120), (94, 108), (100, 86)])
    sole = smooth([(4, 178), (6, 216, 'c'), (224, 218, 'c'), (226, 186, 'c'), (24, 190, 'c')])
    cuff = 'M98 58 Q100 50 110 53 Q122 47 134 52 Q148 46 162 51 Q176 46 190 51 Q204 47 214 52 Q222 54 220 60 L220 80 Q160 72 99 82 Z'
    fluff = (f'<path d="{cuff}" fill="#000" opacity=".3" transform="translate(0 3)" filter="url(#b3)"/>'
             f'<path d="{cuff}" fill="#fff" opacity=".86"/><clipPath id="cf"><path d="{cuff}"/></clipPath>'
             f'<g clip-path="url(#cf)"><rect x="90" y="40" width="140" height="46" filter="url(#boucleK)" opacity=".2"/>'
             f'<path d="M100 78 Q160 70 220 76" stroke="#000" stroke-width="8" opacity=".12" fill="none" filter="url(#b3)"/></g>')
    detail = (fluff + stitch('M152 80 Q150 130 160 186', '#000', .3, 1.2, '3 2.5') + line('M150 80 Q148 130 158 186', '#fff', .15, 1.5)
              + stitch('M30 176 Q120 184 218 180', '#000', .25, 1, '3 2.5')
              + line('M4 190 L226 188', '#fff', .3, 1.5) + line('M6 200 L226 200', '#000', .25, 1)
              + f'<rect x="196" y="94" width="18" height="24" rx="3" fill="#1a1a1a" opacity=".85"/><path d="M200 102 L210 102 M200 108 L208 108" stroke="#C8B48A" stroke-width="1.6"/>')
    shade = (K('M214 70 Q222 130 222 186', .4, 12, 18) + K('M30 186 Q120 194 218 188', .3, 6, 12)
             + K('M100 84 Q160 78 218 82', .3, 6, 10) + K('M60 140 Q90 150 120 140', .15, 12, 20))
    light = (Wt('M114 92 Q116 116 100 132 Q60 146 30 160', .45, 12, 14) + Wt('M30 148 Q50 132 80 126', .5, 3, 5) + Wt('M22 168 Q40 150 70 140', .4, 3, 6)
             + Wt('M180 80 Q190 130 186 170', .12, 12, 16))
    return shoe_svg(W, H, 'winter_boots', color, o, sole, detail, shade, light, back=(44, -14), sole_col='#D8CCBA', gloss=.55, grain=(1.6, .32))
