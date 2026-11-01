from kit.art_kit import *

def umbrella(color):
    W, H = 110, 440
    can = smooth([(55, 22, 'c'), (66, 60), (80, 160), (84, 260), (74, 312), (60, 330, 'c'), (50, 330, 'c'), (36, 312), (28, 250), (34, 150), (46, 60)])
    hw = (line('M55 4 L55 26', '#888', 1, 4) + line('M55 329 L55 386', '#2a2522', 1, 5)
             + '<path d="M55 384 L55 404 Q55 428 76 428 Q94 428 94 410" fill="none" stroke="#3a2a20" stroke-width="10" stroke-linecap="round"/>'
             + Wt('M53 392 Q53 420 72 424', .4, 1, 2))
    detail = (hw + f'<path d="M30 236 Q56 230 84 240 L83 258 Q56 248 30 254 Z" fill="{color}"/>' + K('M30 254 Q56 248 83 258', .5, 1, 2)
              + button(56, 246, 4, metal=True))
    shade = K('M60 40 Q76 160 70 320', .4, 6, 8) + K('M48 40 Q36 150 42 320', .25, 6, 6) + K('M80 150 Q86 240 76 310', .35, 6, 8)
    light = Wt('M54 40 Q58 160 56 320', .45, 3, 4) + Wt('M40 120 Q34 200 38 300', .3, 3, 4)
    return piece(W, H, can, color, (.1, .2), shade, light, '', detail, grain=(1.6, .2), name='umbrella')
