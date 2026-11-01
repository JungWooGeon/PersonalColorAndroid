from kit.art_kit import *

def platform(color):
    """Chunky platform penny loafer: glossy leather, gold bit, black lug sole."""
    W, H = 380, 220
    o = smooth([(8, 150), (14, 128), (46, 112), (104, 102), (150, 98, 'c'), (200, 110), (252, 108), (294, 92, 'c'), (302, 118), (300, 152, 'c'), (12, 156, 'c')])
    sole = smooth([(10, 146, 'c'), (3, 160), (8, 180), (24, 190, 'c'), (300, 190, 'c'), (308, 176), (304, 146, 'c')])
    strap = 'M98 108 Q140 96 180 104 L182 120 Q140 112 98 122 Z'
    lugs = line('M10 182 Q150 184 304 182', '#fff', .1, 1.2) + ''.join(line(f'M{x} 184 L{x} 190', '#fff', .16, 2.2) for x in range(26, 300, 12))
    detail = (raised(strap, color, .38, .16) + '<path d="M124 108 Q140 104 156 107 L156 112 Q140 109 124 113 Z" fill="#000" opacity=".55"/>'
              + '<rect x="132" y="104" width="18" height="10" rx="5" fill="none" stroke="#000" stroke-opacity=".35" stroke-width="3" transform="translate(.5 1.5)"/>'
              + '<rect x="132" y="104" width="18" height="10" rx="5" fill="none" stroke="#C9A85A" stroke-width="2.8"/>'
              + '<path d="M135 105 Q141 103 147 105" stroke="#FFF1C4" stroke-width="1.2" fill="none" opacity=".9"/>'
              + stitch('M16 140 Q34 112 98 106', '#000', .3, 1.2, '3 2') + line('M18 142 Q38 116 100 109', '#fff', .16, 1)
              + line('M150 99 Q200 114 294 93', '#000', .55, 3)
              # sole: welt, stitched rand, lugs
              + line('M10 150 L302 150', '#000', .7, 2) + line('M12 152.5 L300 152.5', '#fff', .14, 1)
              + stitch('M12 160 Q150 162 302 160', '#fff', .35, 1, '3 3') + lugs)
    shade = K('M28 146 Q150 154 292 146', .32, 6, 12) + K('M150 102 Q200 118 292 96', .45, 6, 8)
    light = (Wt('M20 134 Q46 112 96 106', .6, 3, 5) + Wt('M276 100 Q292 114 294 146', .45, 3, 5) + Wt('M190 122 Q220 126 250 118', .22, 6, 8)
             + Wt('M34 124 Q48 116 66 112', .9, 1, 2.5) + Wt('M30 136 Q34 126 46 120', .5, 1, 2))
    return shoe_svg(W, H, 'platform', color, o, sole, detail, shade, light, back=(38, -24), gloss=1.15)
