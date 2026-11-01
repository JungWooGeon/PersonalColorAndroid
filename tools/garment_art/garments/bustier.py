from kit.art_kit import *

def bustier(color):
    """Cropped satin sweetheart bustier with spaghetti straps, princess seams, boning channels and pointed basque hem."""
    W, H = 320, 250
    left = [(160, 86, 'c'), (150, 70), (130, 56), (106, 54), (88, 64), (80, 80, 'c'), (82, 140), (84, 196, 'c'), (124, 206), (160, 226, 'c')]
    base = smooth(sym(left, W)) + ' M104 56 L110 6 L113 6 L108 56 Z' + f' M{W-104} 56 L{W-110} 6 L{W-113} 6 L{W-108} 56 Z'
    seams = ['M118 56 Q126 110 120 210', f'M{W-118} 56 Q{W-126} 110 {W-120} 210', 'M160 86 L160 226']
    cups = 'M84 104 Q122 124 160 98 Q198 124 236 104'
    fabric = (''.join(line(s, '#000', .3, 1.6) + line(s, '#fff', .12, 1, ) for s in seams)
              + line(cups, '#000', .28, 1.6) + Wt('M90 108 Q124 126 156 104', .12, 1, 1.5) + Wt(f'M{W-90} 108 Q{W-124} 126 {W-156} 104', .1, 1, 1.5)
              + ''.join(stitch(f'M{x} 112 Q{x+(4 if x<160 else -4)} 160 {x} {204 + (160 - abs(160-x)) * .25 - 20}', '#000', .3, 1, '3 2')
                        for x in (100, 140, 180, 220)))
    edge = smooth(left[:6], False) + ' ' + smooth(mir(left[:6], W), False)
    hook = ''.join(f'<rect x="158" y="{y}" width="4" height="3" rx="1" fill="#C8B48A"/>' for y in range(120, 212, 14))
    detail = (line(edge, '#000', .25, 4) + line(edge, '#fff', .18, 1.2) + hook
              + f'<circle cx="160" cy="92" r="3.2" fill="#F3EEE6"/><circle cx="159" cy="91" r="1.2" fill="#fff"/>')
    shade = (K('M84 90 Q90 150 88 200', .35, 12, 12) + K(f'M{W-84} 90 Q{W-90} 150 {W-88} 200', .4, 12, 12)
             + K('M118 70 Q126 120 120 200', .25, 6, 8) + K(f'M{W-118} 70 Q{W-126} 120 {W-120} 200', .25, 6, 8)
             + K('M100 112 Q130 130 158 106', .25, 6, 8) + K(f'M{W-100} 112 Q{W-130} 130 {W-158} 106', .3, 6, 8))
    light = (Wt('M104 70 Q112 76 116 96', .5, 6, 10) + Wt(f'M{W-108} 68 Q{W-114} 76 {W-118} 92', .28, 6, 8)
             + Wt('M136 120 Q144 160 138 200', .32, 6, 8) + Wt(f'M{W-136} 120 Q{W-144} 160 {W-138} 200', .18, 6, 8)
             + Wt('M98 130 Q102 160 100 190', .2, 6, 6) + Wt('M108 8 L110 54', .35, 1, 1))
    return piece(W, H, base, color, (.18, .3), shade, light, '', detail, fabric=fabric, grain=(1.8, .15), name='bustier')
