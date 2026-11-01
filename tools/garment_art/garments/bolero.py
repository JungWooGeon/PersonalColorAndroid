from kit.art_kit import *


def rib_band(d, color, w=13):
    """Knit rib trim along a path: dashes across the stroke read as rib ridges perpendicular to the edge."""
    return (f'<path d="{d}" fill="none" stroke="#000" stroke-opacity=".3" stroke-width="{w+2}" transform="translate(1 2.5)" filter="url(#b3)"/>'
            f'<path d="{d}" fill="none" stroke="{color}" stroke-width="{w}"/>'
            f'<path d="{d}" fill="none" stroke="#000" stroke-opacity=".22" stroke-width="{w}" stroke-dasharray="1.6 2.4"/>'
            f'<path d="{d}" fill="none" stroke="#fff" stroke-opacity=".1" stroke-width="{w}" stroke-dasharray="1.2 2.8" stroke-dashoffset="2"/>')


def bolero(color):
    W, H = 400, 330
    left = [(200, 36, 'c'), (160, 30, 'c'), (122, 40), (98, 50), (84, 70), (74, 130), (63, 210), (54, 290, 'c'),
            (55, 290, 'c'), (57, 321, 'c'), (93, 322, 'c'), (94, 292, 'c'), (100, 240), (108, 190), (116, 152, 'c'),
            (128, 160), (160, 154), (186, 140), (200, 132, 'c')]
    base = smooth(sym(left, W))
    opening = 'M160 30 Q166 98 200 124 Q234 98 240 30 Q200 42 160 30 Z'
    edge = f'M160 31 Q166 98 200 124 Q234 98 {W-160} 31'
    hemL = 'M126 154 Q160 152 199 127'
    hemR = f'M{W-126} 154 Q{W-160} 152 {W-199} 127'
    loopL = 'M200 128 Q184 110 172 116 Q166 130 178 138 Q190 138 200 128 Z'
    loopR = 'M200 128 Q216 110 228 116 Q234 130 222 138 Q210 138 200 128 Z'
    tailL = 'M197 130 Q186 150 190 162 Q194 172 182 182 L191 184 Q204 172 199 160 Q196 148 202 132 Z'
    tailR = 'M203 130 Q214 146 212 160 Q210 170 222 178 L214 182 Q202 172 204 160 Q206 148 198 132 Z'
    bow = (raised(tailL, color, .35, .1) + raised(tailR, color, .35, .1) + raised(loopL, color, .35, .14) + raised(loopR, color, .35, .14)
           + f'<path d="{loopL}" fill="#000" opacity=".12"/>' + K('M198 128 Q186 122 176 126', .35, 1, 2) + K('M202 128 Q214 122 224 126', .35, 1, 2)
           + Wt('M178 120 Q186 116 192 122', .3, 1, 2) + Wt('M222 120 Q214 116 208 122', .25, 1, 2)
           + f'<ellipse cx="200" cy="129" rx="6" ry="7" fill="{color}"/><ellipse cx="200" cy="129" rx="6" ry="7" fill="#000" opacity=".2"/>'
           + Wt('M198 125 L198 132', .35, 1, 2))
    tex = f'<rect width="{W}" height="{H}" fill="url(#stock)"/>'
    cuffs = (f'<rect x="40" y="292" width="64" height="36" fill="url(#rib)"/><rect x="{W-104}" y="292" width="64" height="36" fill="url(#rib)"/>'
             + K('M52 292 L96 293', .4, 3, 5) + K(f'M{W-52} 292 L{W-96} 293', .4, 3, 5))
    shade = (cuffs + K('M112 160 Q104 230 98 300', .4, 12, 12) + K(f'M{W-112} 160 Q{W-104} 230 {W-98} 300', .45, 12, 12)
             + K('M82 80 Q70 190 58 290', .22, 12, 10) + K(f'M{W-82} 80 Q{W-70} 190 {W-58} 290', .28, 12, 10)
             + K('M124 44 Q110 100 116 152', .3, 6, 8) + K(f'M{W-124} 44 Q{W-110} 100 {W-116} 152', .35, 6, 8))
    light = (Wt('M140 60 Q146 100 140 140', .2, 12, 30) + Wt('M84 100 Q72 190 62 270', .2, 12, 12)
             + Wt(f'M{W-140} 70 Q{W-146} 100 {W-140} 130', .08, 12, 24))
    detail = (shape(opening, '#000', .42) + K(opening, .3, 6) + K('M168 40 Q200 50 232 40', .25, 3, 6)
              + rib_band(edge, color) + rib_band(hemL, color, 11) + rib_band(hemR, color, 11)
              + stitch('M126 44 Q112 100 118 150', '#000', .25, 1.2, '1.5 2.5') + stitch(f'M{W-126} 44 Q{W-112} 100 {W-118} 150', '#000', .25, 1.2, '1.5 2.5')
              + bow)
    return piece(W, H, base, color, (.14, .26), shade, light, tex, detail, grain=(1.4, .35), name='bolero')
