from kit.art_kit import *

def _ell(cx, cy, rx, ry, rev=False):
    s = 0 if rev else 1
    return f'M{cx-rx} {cy} A{rx} {ry} 0 1 {s} {cx+rx} {cy} A{rx} {ry} 0 1 {s} {cx-rx} {cy} Z'

def belt(color, metal='#C8B48A'):
    """Slim leather belt, buckled into a loop (3/4 top view), minimal gold buckle."""
    W, H = 400, 200
    ring = _ell(200, 104, 172, 72) + ' ' + _ell(200, 92, 160, 54, rev=True)
    tail = 'M150 150 L240 150 Q252 150 262 162 Q252 174 240 174 L150 174 Z'
    # inner face of the back half of the ring shows as a darker band
    shade = (f'<path d="M40 92 A160 54 0 0 1 360 92 L372 104 A172 72 0 0 0 28 104 Z" fill="#000" opacity=".28"/>'
             + K('M40 112 Q200 150 360 112', .25, 6, 6) + K('M30 104 Q34 140 80 160', .3, 6, 10) + K('M370 104 Q366 140 320 160', .35, 6, 10))
    light = (Wt('M60 140 Q130 170 200 172', .4, 3, 4) + Wt('M90 152 Q140 166 180 168', .7, 1, 1.5)
             + Wt('M300 50 Q340 60 356 84', .3, 3, 3))
    edge = stitch(_ell(200, 104, 167, 67), '#fff', .3, 1, '3 3')
    hole = lambda x: f'<ellipse cx="{x}" cy="162" rx="2.6" ry="3" fill="#1a1716"/><ellipse cx="{x}" cy="161" rx="2.6" ry="1.4" fill="#fff" opacity=".15"/>'
    buckle = (f'<rect x="126" y="140" width="34" height="44" rx="9" fill="none" stroke="#000" stroke-opacity=".35" stroke-width="7" transform="translate(1 3)" filter="url(#b1)"/>'
              f'<rect x="126" y="140" width="34" height="44" rx="9" fill="none" stroke="{metal}" stroke-width="6"/>'
              f'<rect x="126" y="140" width="34" height="44" rx="9" fill="none" stroke="#7a5f28" stroke-opacity=".35" stroke-width="1"/>'
              f'<path d="M130 146 Q132 141 140 141" fill="none" stroke="#FFF3C8" stroke-width="2" stroke-linecap="round"/>'
              f'<rect x="141" y="140" width="4" height="44" rx="2" fill="{metal}"/><rect x="141" y="140" width="1.5" height="44" fill="#FFF3C8" opacity=".6"/>')
    tail_g = (raised(tail, color, .4, .14) + f'<clipPath id="bt"><path d="{tail}"/></clipPath><g clip-path="url(#bt)">'
              + f'<rect width="{W}" height="{H}" filter="url(#grainW)"/>' + Wt('M150 154 L240 154', .4, 1, 1.5) + K('M150 172 L250 172', .35, 1, 3) + '</g>'
              + stitch('M152 154 L240 154 Q248 155 255 162 Q248 169 240 170 L152 170', '#fff', .35, 1, '3 3')
              + hole(186) + hole(204) + hole(222))
    keeper = (f'<rect x="166" y="144" width="10" height="36" rx="2" fill="#000" opacity=".35" transform="translate(1 2)" filter="url(#b1)"/>'
              f'<rect x="166" y="144" width="10" height="36" rx="2" fill="{color}"/><rect x="166" y="144" width="10" height="36" rx="2" fill="#000" opacity=".12"/>'
              + line('M168 146 L168 178', '#fff', .25, 1.2))
    return piece(W, H, ring, color, (.18, .3), shade, light, '', edge + tail_g + buckle + keeper, grain=(1.8, .15), name='belt')
