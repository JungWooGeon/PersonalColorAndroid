from kit.art_kit import *

def leather(color):
    """레더 자켓: cropped biker, wide snap lapels, asymmetric silver zip, belted hem, zip cuffs, glossy sheen."""
    W, H = 400, 330
    S1, S2 = '#C9CCD1', '#6E7178'
    left = [(200, 32, 'c'), (166, 28, 'c'), (106, 44, 'c'), (88, 56), (76, 110), (66, 190), (58, 268), (54, 292, 'c'), (102, 300, 'c'),
            (104, 270), (108, 200), (120, 130, 'c'), (122, 200), (120, 264, 'c'), (200, 268, 'c')]
    base = smooth(sym(left, W))
    vopen = smooth([(170, 30, 'c'), (214, 122, 'c'), (232, 30, 'c'), (200, 38)])
    lapL = smooth([(168, 28, 'c'), (140, 42), (120, 62, 'c'), (126, 102, 'c'), (176, 112), (214, 124, 'c'), (180, 44, 'c')])
    lapR = smooth([(232, 28, 'c'), (262, 46), (284, 62, 'c'), (292, 98, 'c'), (270, 96, 'c'), (276, 114, 'c'), (214, 124, 'c'), (222, 44, 'c')])
    collar_back = smooth([(166, 28, 'c'), (200, 22), (234, 28, 'c'), (226, 38), (200, 34), (174, 38, 'c')])
    zip_d = 'M214 124 Q222 190 226 266'
    stud = lambda x, y: (f'<circle cx="{x}" cy="{y+1}" r="3.6" fill="#000" opacity=".35" filter="url(#b1)"/><circle cx="{x}" cy="{y}" r="3.4" fill="{S1}"/>'
                         f'<circle cx="{x-1}" cy="{y-1}" r="1.3" fill="#fff" opacity=".9"/>')
    zipline = lambda d, w=4: (line(d, '#000', .5, w + 2) + f'<path d="{d}" fill="none" stroke="{S1}" stroke-width="{w}" stroke-dasharray="1.6 1.4"/>'
                              + line(d, '#fff', .25, 1))
    pull = lambda x, y, r=0: (f'<g transform="rotate({r} {x} {y})"><rect x="{x-2.5}" y="{y}" width="5" height="14" rx="2" fill="{S2}"/>'
                              f'<rect x="{x-1.5}" y="{y+1}" width="2" height="10" rx="1" fill="#fff" opacity=".6"/></g>')
    fabric = (raised(collar_back, color, .2) + shape(vopen, '#000', .45) + K(vopen, .35, 6)
              + raised(lapR, color, .4, .12) + raised(lapL, color, .45, .16)
              + shape('M120 244 L280 244 L280 266 L120 266 Z', '#000', .1) + line('M121 244 L279 244', '#000', .45, 2)
              + line('M121 246 L279 246', '#fff', .15, 1))
    detail = (zipline(zip_d) + pull(214, 124, -8)
              + zipline('M134 150 L166 196', 3) + pull(134, 150, -35)
              + zipline('M250 196 L276 162', 3) + pull(276, 162, 40)
              + zipline('M60 250 L62 286', 2.5) + zipline(f'M{W-60} 250 L{W-62} 286', 2.5)
              + stud(128, 92) + stud(286, 90) + stud(176, 108)
              + f'<rect x="138" y="242" width="18" height="26" rx="2" fill="none" stroke="{S1}" stroke-width="3"/><rect x="138" y="242" width="18" height="26" rx="2" fill="none" stroke="#fff" stroke-opacity=".5" stroke-width="1"/>'
              + f'<path d="M146 246 L146 264" stroke="{S2}" stroke-width="2"/>'
              + stitch('M120 249 L280 249', '#000', .3, 1, '3 2') + stitch('M120 262 L280 262', '#000', .3, 1, '3 2')
              + stitch('M124 92 Q160 98 196 94', '#000', .2, 1, '3 2') + stitch(f'M{W-124} 92 Q{W-160} 98 {W-196} 94', '#000', .2, 1, '3 2'))
    shade = (K('M120 134 Q124 200 120 260', .55, 12, 12) + K(f'M{W-120} 134 Q{W-124} 200 {W-120} 260', .6, 12, 12)
             + K('M90 100 Q78 190 64 284', .3, 12, 10) + K(f'M{W-90} 100 Q{W-78} 190 {W-64} 284', .35, 12, 10)
             + K('M70 160 Q86 170 104 164', .35, 3, 3) + K('M66 214 Q84 224 104 218', .3, 3, 3)
             + K(f'M{W-70} 176 Q{W-86} 186 {W-104} 180', .35, 3, 3) + K(f'M{W-64} 228 Q{W-84} 238 {W-104} 232', .3, 3, 3))
    light = (Wt('M144 140 Q152 190 146 232', .45, 6, 8) + Wt('M148 150 Q154 190 150 220', .5, 3, 2)
             + Wt('M80 80 Q70 160 64 240', .4, 6, 6) + Wt('M82 100 Q74 160 68 220', .55, 3, 2)
             + Wt(f'M{W-80} 90 Q{W-72} 160 {W-66} 240', .22, 6, 6)
             + Wt('M70 154 Q86 164 104 158', .35, 3, 2) + Wt('M66 208 Q84 218 104 212', .3, 3, 2)
             + Wt('M248 140 Q258 180 252 220', .2, 6, 8) + Wt('M96 50 Q84 56 80 72', .4, 3, 4))
    return piece(W, H, base, color, (.22, .34), shade, light, '', detail, fabric=fabric, grain=(2.2, .18), name='leather')
