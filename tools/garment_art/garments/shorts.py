from kit.art_kit import *

def shorts(color):
    """High-rise A-line denim mini shorts with raw frayed hem."""
    W, H = 300, 260
    pts = [(68, 20, 'c'), (232, 20, 'c'), (238, 66), (250, 150), (264, 226, 'c'), (216, 232), (164, 222, 'c'), (157, 196), (150, 172, 'c'),
           (143, 196), (136, 222, 'c'), (84, 232), (36, 226, 'c'), (50, 150), (62, 66)]
    base = smooth(pts)
    thread = '#C99A52'
    tex = f'<rect width="{W}" height="{H}" fill="url(#twill)"/>'
    fringe = ''
    for i in range(30):
        t = i / 29
        for x0, x1, y0, y1 in ((36, 136, 226, 222), (164, 264, 222, 226)):
            x = x0 + (x1 - x0) * t; y = y0 + (y1 - y0) * t + 6 * (1 - (2 * t - 1) ** 2)
            fringe += f'<path d="M{x:.1f} {y-2:.1f} l{(i%3-1)*.8:.1f} {5+i%4*1.5:.1f}" stroke="#fff" stroke-opacity=".7" stroke-width="1.1" stroke-linecap="round"/>'
    detail = (line('M67 44 L233 44', '#000', .3, 2) + stitch('M68 24 L232 24', thread, .8, 1.2) + stitch('M68 40 L232 40', thread, .8, 1.2)
              + stitch('M162 46 L162 118 Q162 138 148 146', thread, .85, 1.3) + line('M150 46 L150 168', '#000', .28, 2)
              + stitch('M70 48 Q98 52 106 50 Q96 88 62 100', thread, .8, 1.2) + line('M72 46 Q102 50 108 48 Q98 90 60 104', '#000', .25, 1.5)
              + stitch(f'M{W-70} 48 Q{W-98} 52 {W-106} 50 Q{W-96} 88 {W-62} 100', thread, .8, 1.2) + line(f'M{W-72} 46 Q{W-102} 50 {W-108} 48 Q{W-98} 90 {W-60} 104', '#000', .25, 1.5)
              + stitch('M204 56 L226 56 L227 78 L205 79 Z', thread, .7, 1)
              + ''.join(f'<rect x="{x-4}" y="17" width="8" height="30" rx="2" fill="#000" opacity=".25" filter="url(#b1)"/><rect x="{x-4}" y="16" width="8" height="28" rx="2" fill="{color}"/>' + stitch(f'M{x} 18 L{x} 42', thread, .7, 1, '2 2') for x in (86, 124, 176, 214))
              + button(150, 32, 7, metal=True) + button(108, 52, 3, metal=True) + button(192, 52, 3, metal=True) + fringe)
    shade = (K('M150 172 L140 224', .45, 6, 8) + K('M150 172 L160 224', .45, 6, 8)
             + K('M56 110 Q46 170 40 224', .3, 12, 14) + K(f'M{W-56} 110 Q{W-46} 170 {W-40} 224', .35, 12, 14)
             + K('M44 212 Q90 222 136 214', .25, 6, 8) + K(f'M{W-44} 212 Q{W-90} 222 {W-136} 214', .25, 6, 8))
    light = (Wt('M96 100 Q90 160 84 216', .35, 22, 30) + Wt(f'M{W-96} 100 Q{W-90} 160 {W-84} 216', .25, 22, 26)
             + ''.join(Wt(f'M{140-i*7} {160+i*4} Q{120-i*6} {168+i*4} {100-i*4} {166+i*7}', .35, 3, 3) for i in range(3))
             + ''.join(Wt(f'M{160+i*7} {160+i*4} Q{180+i*6} {168+i*4} {200+i*4} {166+i*7}', .3, 3, 3) for i in range(3))
             + Wt('M38 222 Q86 232 136 222', .45, 3, 6) + Wt(f'M{W-38} 222 Q{W-86} 232 {W-136} 222', .4, 3, 6))
    return piece(W, H, base, color, (.12, .3), shade, light, tex, detail, grain=(1.6, .5), name='shorts')
