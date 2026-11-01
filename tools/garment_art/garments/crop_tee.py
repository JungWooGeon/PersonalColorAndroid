from kit.art_kit import *

def crop_tee(color):
    """Cropped baby tee: fitted, short cap sleeves with banded hem, narrow ribbed neck, tiny tonal heart embroidery."""
    W, H = 340, 230
    left = [(170, 34, 'c'), (144, 29, 'c'), (110, 38, 'c'), (96, 48), (82, 74), (72, 100, 'c'), (100, 112, 'c'), (110, 96, 'c'),
            (114, 140), (112, 172), (114, 204, 'c'), (170, 200, 'c')]
    base = smooth(sym(left, W))
    neck_in = 'M144 29 Q149 56 170 59 Q191 56 196 29 Q170 39 144 29 Z'
    nb = 'M142 28 Q148 63 170 65 Q192 63 198 28'
    heart = 'M206 92 C202 86 194 88 195 94 C196 99 202 102 206 106 C210 102 216 99 217 94 C218 88 210 86 206 92 Z'
    detail = (shape(neck_in, '#000', .5) + K(neck_in, .3, 3)
              + f'<path d="{nb}" fill="none" stroke="{color}" stroke-width="7"/><path d="{nb}" fill="none" stroke="url(#rib)" stroke-width="7"/>'
              + line('M139 27 Q147 69 170 71 Q193 69 201 27', '#000', .2, 1.3)
              + line('M99 46 Q102 74 110 96', '#000', .2, 1.3) + line(f'M{W-99} 46 Q{W-102} 74 {W-110} 96', '#000', .2, 1.3)
              # banded sleeve hems
              + line('M76 92 L104 103', '#000', .28, 1.5) + line(f'M{W-76} 92 L{W-104} 103', '#000', .28, 1.5)
              + stitch('M118 192 Q170 188 222 192', '#000', .3, 1, '3 2')
              # tonal embroidered heart (works on any colour)
              + f'<path d="{heart}" fill="none" stroke="#000" stroke-opacity=".38" stroke-width="2.2" stroke-dasharray="1.6 1.2" transform="translate(0 .8)"/>'
              + f'<path d="{heart}" fill="none" stroke="#fff" stroke-opacity=".32" stroke-width="1.4" stroke-dasharray="1.6 1.2"/>')
    sleeve_rib = (f'<g transform="rotate(22 90 104)"><rect x="70" y="94" width="40" height="14" fill="url(#rib)"/></g>'
                  f'<g transform="rotate(-22 {W-90} 104)"><rect x="{W-110}" y="94" width="40" height="14" fill="url(#rib)"/></g>')
    shade = (sleeve_rib + K('M112 100 Q118 150 114 204', .4, 12, 12) + K(f'M{W-112} 100 Q{W-118} 150 {W-114} 204', .46, 12, 12)
             + K('M116 104 Q134 116 140 140', .14, 6, 5) + K(f'M{W-116} 104 Q{W-134} 118 {W-138} 144', .16, 6, 5)
             + K('M150 170 Q154 186 150 202', .12, 6, 6) + K('M200 160 Q196 182 202 200', .12, 6, 6)
             + K('M118 200 Q170 192 222 200', .18, 6, 8))
    light = (Wt('M140 80 Q150 130 140 180', .2, 22, 34) + Wt('M90 56 Q80 76 76 90', .2, 6, 7)
             + Wt('M200 100 Q212 140 204 176', .08, 12, 20))
    return piece(W, H, base, color, (.15, .26), shade, light, '', f'<g clip-path="url(#c)">{detail}</g>', grain=(1.7, .25), name='crop_tee')
