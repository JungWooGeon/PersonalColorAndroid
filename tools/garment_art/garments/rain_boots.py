from kit.art_kit import *

def rain_boots(color):
    W, H = 330, 230
    o = smooth([(136, 14, 'c'), (212, 14, 'c'), (212, 70), (214, 130), (222, 194, 'c'), (16, 198, 'c'), (6, 188), (22, 174), (70, 160), (112, 136), (134, 90)])
    sole = smooth([(4, 186), (6, 202, 'c'), (180, 204, 'c'), (184, 212, 'c'), (220, 212, 'c'), (224, 192, 'c'), (16, 195, 'c')])
    gore = 'M160 14 L190 14 Q183 62 175 92 Q167 62 160 14 Z'
    detail = (f'<path d="{gore}" fill="#000" opacity=".5"/><clipPath id="g"><path d="{gore}"/></clipPath><g clip-path="url(#g)"><rect x="155" width="70" height="120" fill="url(#rib)"/></g>'
              + line('M136 16 L212 16', '#000', .4, 2.5) + f'<path d="M208 4 Q222 6 221 28 L213 28 Q214 12 207 11 Z" fill="{color}"/>' + line('M213 28 L221 28', '#000', .3, 1)
              + line('M5 195 L223 195', '#fff', .28, 1.2) + ''.join(line(f'M{x} 198 L{x+3} 203', '#fff', .14, 1.4) for x in range(14, 180, 9)))
    shade = K('M206 26 Q210 120 218 188', .45, 12, 14) + K('M24 188 Q120 194 222 188', .3, 6, 10)
    light = (Wt('M144 26 Q146 80 130 118 Q94 150 34 170', .7, 3, 6) + Wt('M150 36 Q150 80 140 108', .9, 1, 2.2)
             + Wt('M204 60 Q210 120 212 170', .3, 6, 7) + Wt('M24 176 Q42 166 62 160', .8, 1, 2.5))
    return shoe_svg(W, H, 'rain_boots', color, o, sole, detail, shade, light, back=(46, -18), gloss=1.25)
