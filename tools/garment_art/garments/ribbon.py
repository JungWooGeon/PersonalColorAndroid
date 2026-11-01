from kit.art_kit import *

def ribbon(color):
    """Satin hair ribbon: big double-loop bow with long notched tails."""
    W, H = 280, 300
    cx, cy = 140, 104
    loopL = smooth([(cx - 8, cy - 10, 'c'), (90, 50), (40, 30), (16, 56), (20, 120), (48, 150), (96, 132), (cx - 8, cy + 12, 'c')])
    loopR = smooth([(cx + 8, cy - 10, 'c'), (190, 50), (240, 30), (264, 56), (260, 120), (232, 150), (184, 132), (cx + 8, cy + 12, 'c')])
    tailL = smooth([(cx - 6, cy + 4, 'c'), (cx + 2, cy + 18), (112, 180), (86, 236), (66, 290, 'c'), (84, 272, 'c'), (100, 290, 'c'), (124, 232), (144, 170), (cx + 10, cy + 14, 'c')])
    tailR = smooth([(cx + 2, cy + 6, 'c'), (cx + 14, cy + 16), (166, 160), (186, 220), (210, 280, 'c'), (192, 266, 'c'), (176, 286, 'c'), (158, 228), (140, 170), (cx - 6, cy + 16, 'c')])
    knot = smooth([(cx - 16, cy - 20), (cx, cy - 24), (cx + 16, cy - 20), (cx + 18, cy + 4), (cx + 14, cy + 22), (cx, cy + 25), (cx - 14, cy + 22), (cx - 18, cy + 2)])
    base = ' '.join([tailL, tailR, loopL, loopR])
    shade = (K(tailR, .2, 3) + K(f'M{cx-10} {cy} Q100 70 40 40', .35, 6, 10) + K(f'M{cx+10} {cy} Q180 70 240 40', .35, 6, 10)
             + K('M100 120 Q70 140 40 140', .35, 6, 12) + K('M180 120 Q210 140 240 140', .4, 6, 12)
             + K('M30 70 Q60 100 110 104', .25, 6, 8) + K('M250 70 Q220 100 170 104', .25, 6, 8)
             + K('M120 140 Q110 200 80 270', .3, 6, 8) + K('M160 140 Q170 200 196 270', .35, 6, 8))
    light = (Wt('M34 56 Q70 50 104 86', .5, 3, 7) + Wt('M44 60 Q70 56 96 82', .45, 1, 1.5) + Wt('M246 56 Q210 50 176 86', .4, 3, 7)
             + Wt('M30 110 Q60 126 100 118', .3, 3, 5) + Wt('M118 160 Q104 210 84 260', .45, 3, 5) + Wt('M154 160 Q168 200 186 250', .35, 3, 5))
    # gathers pleating into the knot + the loop's underside peeking at the inner end
    folds = ''.join(line(f'M{cx-14} {cy+o*.3} Q{cx-50} {cy+o} {cx-50-abs(o)*1.4} {cy+o*2.2}', '#000', .22, 1.6) + line(f'M{cx-14} {cy+o*.3+2} Q{cx-50} {cy+o+2} {cx-50-abs(o)*1.4} {cy+o*2.2+2}', '#fff', .12, 1.2)
                    + line(f'M{cx+14} {cy+o*.3} Q{cx+50} {cy+o} {cx+50+abs(o)*1.4} {cy+o*2.2}', '#000', .22, 1.6) + line(f'M{cx+14} {cy+o*.3+2} Q{cx+50} {cy+o+2} {cx+50+abs(o)*1.4} {cy+o*2.2+2}', '#fff', .12, 1.2)
                    for o in (-10, 0, 10))
    folds += K(f'M{cx-10} {cy-8} Q{cx-30} {cy} {cx-10} {cy+10}', .5, 3, 8) + K(f'M{cx+10} {cy-8} Q{cx+30} {cy} {cx+10} {cy+10}', .5, 3, 8)
    knot_g = (raised(knot, color, .5, .2) + f'<clipPath id="rk"><path d="{knot}"/></clipPath><g clip-path="url(#rk)">'
              + K(f'M{cx-18} {cy+16} Q{cx} {cy+28} {cx+18} {cy+16}', .4, 3, 8) + Wt(f'M{cx-8} {cy-18} Q{cx-12} {cy} {cx-8} {cy+16}', .5, 3, 5)
              + line(f'M{cx-10} {cy-16} Q{cx} {cy} {cx-6} {cy+20}', '#000', .2, 1.2) + line(f'M{cx+8} {cy-18} Q{cx+12} {cy} {cx+8} {cy+18}', '#000', .2, 1.2) + '</g>')
    shade += folds
    return piece(W, H, base, color, (.2, .3), shade, light, '', knot_g, grain=(1.6, .12), name='ribbon')
