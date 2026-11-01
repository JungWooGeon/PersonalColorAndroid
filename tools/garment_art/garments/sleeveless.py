from kit.art_kit import *

def sleeveless(color):
    """Fitted cropped ribbed knit tank (골지 민소매) with a square neck, bound edges and a tiny coquette bow."""
    W, H = 320, 300
    left = [(160, 84, 'c'), (124, 84, 'c'), (122, 30, 'c'), (98, 30, 'c'), (96, 70), (86, 104), (72, 124, 'c'), (80, 180), (78, 232), (82, 272, 'c'), (160, 276, 'c')]
    base = smooth(sym(left, W))
    tex = f'<rect width="{W}" height="{H}" fill="url(#rib)"/>'
    neck = 'M122 32 L124 84 L196 84 L198 32'
    armL, armR = 'M98 32 Q96 82 86 104 Q80 118 72 124', f'M{W-98} 32 Q{W-96} 82 {W-86} 104 Q{W-80} 118 {W-72} 124'
    bind = lambda d: line(d, '#000', .3, 6) + line(d, '#fff', .14, 2) + stitch(d, '#000', .3, 1, '2 2')
    bow = ('M160 92 Q144 80 138 87 Q136 98 160 94 Z M160 92 Q176 80 182 87 Q184 98 160 94 Z'
           ' M158 94 Q150 108 145 122 L151 123 Q156 108 160 96 Z M162 94 Q170 108 175 122 L169 123 Q164 108 160 96 Z')
    fabric = (raised(bow, color, .45, .25) + shape('M155 88 Q160 86 165 88 L165 97 Q160 99 155 97 Z', color)
              + K('M155 89 L165 97', .25, 1, 2) + Wt('M144 88 Q149 85 154 90', .35, 1, 1.5) + Wt('M166 90 Q171 85 176 88', .3, 1, 1.5))
    detail = bind(neck) + bind(armL) + bind(armR) + line('M84 258 Q160 254 236 258', '#000', .18, 1.5)
    shade = (K('M78 132 Q86 200 82 270', .35, 12, 12) + K(f'M{W-78} 132 Q{W-86} 200 {W-82} 270', .4, 12, 12)
             + K('M90 190 Q160 202 230 190', .12, 22, 20) + K('M104 110 Q118 128 118 152', .16, 12, 14) + K(f'M{W-104} 110 Q{W-118} 128 {W-118} 152', .2, 12, 14))
    light = (Wt('M126 116 Q134 170 126 240', .22, 22, 34) + Wt('M108 128 Q122 118 138 124', .16, 12, 12) + Wt('M184 128 Q200 118 214 124', .1, 12, 12)
             + Wt('M210 200 Q220 230 212 262', .08, 12, 16))
    return piece(W, H, base, color, (.16, .3), shade, light, tex, detail, fabric=fabric, grain=(1.3, .3), name='sleeveless')
