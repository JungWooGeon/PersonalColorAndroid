from kit.art_kit import *

def running(color):
    """Slim retro terrace sneaker (Samba type): long low last, suede T-toe, side stripes, gum sole."""
    W, H = 380, 190
    o = smooth([(8, 156), (14, 144), (44, 134), (96, 124), (140, 114), (176, 104, 'c'), (192, 110), (232, 120), (270, 110), (294, 108, 'c'), (302, 130), (300, 158, 'c'), (10, 160, 'c')])
    sole = smooth([(10, 152, 'c'), (4, 162), (12, 172), (40, 174, 'c'), (298, 174, 'c'), (304, 166), (302, 152, 'c')])
    toe = 'M9 157 Q6 140 40 134 Q76 128 110 122 L116 130 Q90 138 72 158 Z'
    heel = 'M266 112 Q286 106 296 108 Q304 128 302 150 L276 150 Q280 128 266 112 Z'
    stripes = ''
    for x0 in (138, 156):   # ponytail: 2 stripes on purpose — 3 is the adidas mark
        d = f'M{x0} 118 L{x0+9} 116 L{x0+46} 156 L{x0+36} 156 Z'
        stripes += f'<path d="{d}" fill="#000" opacity=".25" transform="translate(1 1.5)" filter="url(#b1)"/><path d="{d}" fill="#F6F3EC"/>' + stitch(d, '#000', .3, .8, '2 2')
    laces = ''.join(f'<path d="M{x-5} {y+1} L{x+6} {y-3}" stroke="#F4F1EA" stroke-width="3.6" stroke-linecap="round"/>'
                    for x, y in [(120, 120), (136, 116), (152, 112), (168, 107)])
    detail = (stripes + raised(toe, color, .32, .14) + f'<path d="{toe}" fill="#000" opacity=".1"/>'
              + stitch('M14 152 Q14 140 42 137 Q78 132 108 126', '#000', .3, 1, '3 2')
              + raised(heel, color, .3, .14) + f'<path d="{heel}" fill="#000" opacity=".1"/>'
              + '<rect x="214" y="126" width="26" height="4" rx="1.5" fill="#C8A85E" opacity=".9"/>'  # gold foil logo bar
              + laces + line('M10 154 L300 154', '#000', .25, 1.2) + line('M12 157 L298 157', '#fff', .25, 1)
              + ''.join(line(f'M{x} 164 L{x+3} 172', '#000', .14, 1.6) for x in range(24, 296, 9)))
    shade = K('M24 150 Q150 158 296 150', .3, 6, 12) + K('M180 110 Q228 126 270 112', .45, 3, 6)
    light = (Wt('M22 142 Q40 134 90 128', .4, 3, 5) + Wt('M180 140 Q220 138 256 132', .18, 6, 8)
             + Wt('M286 116 Q296 130 296 146', .3, 3, 4))
    return shoe_svg(W, H, 'running', color, o, sole, detail, shade, light, back=(40, -22), sole_col='#B5865A', gloss=.6, grain=(1.6, .3))
