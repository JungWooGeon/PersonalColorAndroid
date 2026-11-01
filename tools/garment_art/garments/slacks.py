from kit.art_kit import *

def slacks(color):
    """Wide pintuck slacks: high waist, two front tucks, sharp stitched pintuck down each leg, floor-skimming wide leg."""
    W, H = 300, 520
    pts = [(70, 18, 'c'), (230, 18, 'c'), (236, 60), (248, 160), (262, 320), (276, 506, 'c'), (162, 510, 'c'), (158, 330), (150, 202, 'c'),
           (142, 330), (138, 510, 'c'), (24, 506, 'c'), (38, 320), (52, 160), (64, 60)]
    base = smooth(pts)
    # pintuck path per leg: from tuck at waistband straight to hem centre
    pl, pr = 'M104 46 Q96 280 82 506', f'M{W-104} 46 Q{W-96} 280 {W-82} 506'
    detail = (line('M67 46 L233 46', '#000', .3, 2) + stitch('M70 22 L230 22', '#000', .2, 1) + stitch('M70 42 L230 42', '#000', .2, 1)
              # hook-and-bar closure + extended tab
              + f'<rect x="146" y="26" width="13" height="5" rx="2.5" fill="#B9B6B0"/><rect x="146" y="26" width="13" height="2" rx="1" fill="#fff" opacity=".6"/>'
              + line('M150 48 L150 170', '#000', .3, 1.8) + stitch('M159 48 L159 130 Q159 148 149 158', '#000', .22, 1)
              # slant pockets
              + line('M72 49 Q84 66 88 108', '#000', .3, 1.7) + line(f'M{W-72} 49 Q{W-84} 66 {W-88} 108', '#000', .3, 1.7)
              # pintucks: crisp ridge = dark fold line + white top edge + stitch
              + line(pl, '#000', .32, 1.6) + f'<path d="{pl}" transform="translate(2 0)" fill="none" stroke="#fff" stroke-opacity=".28" stroke-width="1.4"/>'
              + line(pr, '#000', .32, 1.6) + f'<path d="{pr}" transform="translate(-2 0)" fill="none" stroke="#fff" stroke-opacity=".22" stroke-width="1.4"/>'
              + stitch(pl.replace('M104', 'M106').replace(' 82 506', ' 84.5 506'), '#000', .16, .9, '3 2')
              + stitch(pr.replace(f'M{W-104}', f'M{W-106}').replace(f' {W-82} 506', f' {W-84.5} 506'), '#000', .16, .9, '3 2')
              # second tuck (short fold) next to pintuck
              + line('M120 47 Q119 66 117 84', '#000', .26, 1.4) + line(f'M{W-120} 47 Q{W-119} 66 {W-117} 84', '#000', .26, 1.4))
    shade = (K('M150 202 L146 508', .45, 6, 9) + K('M150 202 Q154 340 158 508', .45, 6, 7)
             + K('M52 160 Q36 330 28 502', .3, 12, 16) + K(f'M{W-52} 160 Q{W-36} 330 {W-28} 502', .36, 12, 16)
             + K('M98 60 Q90 280 76 506', .3, 6, 10) + K(f'M{W-98} 60 Q{W-90} 280 {W-76} 506', .32, 6, 10)
             + K('M121 50 Q120 70 116 92', .25, 3, 5) + K(f'M{W-121} 50 Q{W-120} 70 {W-116} 92', .25, 3, 5)
             + K('M68 48 L232 48', .22, 6, 6))
    light = (Wt('M110 60 Q104 280 92 500', .3, 6, 10) + Wt(f'M{W-110} 60 Q{W-104} 280 {W-92} 500', .2, 6, 10)
             + Wt('M66 160 Q54 330 46 490', .22, 22, 26) + Wt(f'M{W-66} 160 Q{W-54} 330 {W-46} 490', .15, 22, 24)
             + Wt('M128 120 Q134 300 140 500', .12, 12, 12) + Wt(f'M{W-128} 120 Q{W-134} 300 {W-140} 500', .1, 12, 12))
    return piece(W, H, base, color, (.16, .3), shade, light, '', detail, grain=(1.7, .2), name='slacks')
