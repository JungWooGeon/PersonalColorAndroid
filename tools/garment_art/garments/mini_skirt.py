from kit.art_kit import *

def mini_skirt(color):
    """A-line wrap mini: curved wrap front panel, high flat waistband, two pearl buttons."""
    W, H = 320, 250
    base = smooth([(82, 14, 'c'), (238, 14, 'c'), (240, 40, 'c'), (262, 130), (286, 214, 'c'), (230, 220), (160, 224),
                   (104, 230), (88, 226), (34, 216, 'c'), (58, 130), (80, 40, 'c')])
    edge = 'M204 40 Q206 120 162 182 Q130 222 104 230'
    detail = (line('M80 40 L240 40', '#000', .3, 2) + stitch('M84 18 L236 18', '#000', .2, 1) + stitch('M84 36 L236 36', '#000', .2, 1)
              + line(edge, '#000', .35, 1.6) + stitch('M198 44 Q200 120 158 180 Q128 216 104 224', '#000', .2, 1, '3 3')
              + pearl(214, 27, 5.5) + pearl(196, 58, 4.5))
    shade = (K('M210 42 Q212 124 168 186 Q138 224 108 236', .45, 3, 8)          # panel drop shadow
             + K('M100 60 Q88 150 72 222', .28, 6, 9) + K('M150 70 Q150 150 140 226', .18, 6, 8)
             + K('M230 70 Q246 150 262 216', .3, 6, 9)
             + K('M60 120 L36 216', .3, 12, 20) + K('M262 120 L286 214', .32, 12, 20) + K('M82 40 L238 40', .28, 6, 8))
    light = (Wt('M200 44 Q202 120 160 182 Q130 220 104 228', .3, 3, 3)            # panel edge catch-light
             + Wt('M175 60 Q178 140 150 200', .2, 12, 26) + Wt('M118 60 Q110 150 98 214', .14, 12, 22)
             + Wt('M100 20 L220 20', .14, 3, 3))
    return piece(W, H, base, color, (.16, .28), shade, light, '', detail, grain=(1.4, .3), name='mini_skirt')
