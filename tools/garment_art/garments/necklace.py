from kit.art_kit import *
import math

def necklace(color):
    """Glossy colour bead strand with freshwater pearl accents and a tiny gold heart charm, laid flat."""
    W, H = 300, 270
    cx, cy, rx, ry, r = 150, 120, 112, 100, 10
    beads, pearls = [], []
    n = 33
    for i in range(n):
        t = math.radians(16 + (360 - 32) * i / (n - 1))
        x, y = cx + rx * math.sin(t), cy - ry * math.cos(t)
        (pearls if i % 6 == 3 else beads).append((x, y))
    circ = lambda x, y, rr: f'M{x-rr:.1f} {y:.1f} A{rr} {rr} 0 1 1 {x+rr:.1f} {y:.1f} A{rr} {rr} 0 1 1 {x-rr:.1f} {y:.1f} Z'
    base = ' '.join(circ(x, y, r) for x, y in beads)
    g, gd, gl = '#C9A85C', '#7a5f28', '#FFF3C8'
    ends = [(cx + rx * math.sin(math.radians(a)), cy - ry * math.cos(math.radians(a))) for a in (16, 344)]
    # thread peeking between beads + gold clasp at the top
    under = f'<path d="M{ends[0][0]:.1f} {ends[0][1]:.1f} A{rx} {ry} 0 1 1 {ends[1][0]:.1f} {ends[1][1]:.1f}" fill="none" stroke="#000" stroke-opacity=".35" stroke-width="1.6" transform="scale(1)"/>'
    (x0, y0), (x1, y1) = ends
    clasp = (f'<path d="M{x1+6:.1f} {y1-4:.1f} Q{cx} {y0-16:.1f} {x0-6:.1f} {y0-4:.1f}" fill="none" stroke="{g}" stroke-width="1.6" stroke-dasharray="3 1.4"/>'
             f'<ellipse cx="{cx+6}" cy="{y0-12:.1f}" rx="6" ry="4" fill="none" stroke="{g}" stroke-width="2.6"/>'
             f'<circle cx="{cx-8}" cy="{y0-12:.1f}" r="3" fill="none" stroke="{g}" stroke-width="2"/>')
    hb = cy + ry + 8
    heart = (f'M{cx} {hb+10} C{cx-4} {hb+6} {cx-12} {hb+10} {cx-12} {hb+17} C{cx-12} {hb+24} {cx-4} {hb+28} {cx} {hb+33} '
             f'C{cx+4} {hb+28} {cx+12} {hb+24} {cx+12} {hb+17} C{cx+12} {hb+10} {cx+4} {hb+6} {cx} {hb+10} Z')
    charm = (f'<ellipse cx="{cx}" cy="{hb+3}" rx="2.6" ry="4" fill="none" stroke="{g}" stroke-width="2"/>'
             f'<path d="{heart}" fill="#000" opacity=".3" transform="translate(1 2)" filter="url(#b1)"/><path d="{heart}" fill="{g}"/>'
             f'<path d="{heart}" fill="none" stroke="{gd}" stroke-opacity=".5" stroke-width="1"/>'
             f'<path d="M{cx-8} {hb+15} Q{cx-7} {hb+11} {cx-3} {hb+12}" fill="none" stroke="{gl}" stroke-width="2" stroke-linecap="round"/>')
    detail = clasp + ''.join(pearl(x, y, r * .9) for x, y in pearls) + charm
    shade = ''.join(f'<circle cx="{x+2.4:.1f}" cy="{y+2.8:.1f}" r="{r*.9}" fill="none" stroke="#000" stroke-width="3" opacity=".3" filter="url(#b1)"/>' for x, y in beads)
    light = ''.join(f'<ellipse cx="{x-2.8:.1f}" cy="{y-3.2:.1f}" rx="2.8" ry="1.8" fill="#fff" opacity=".9" transform="rotate(-35 {x-2.8:.1f} {y-3.2:.1f})"/>'
                    f'<circle cx="{x-1.5:.1f}" cy="{y-1.5:.1f}" r="{r*.6}" fill="#fff" opacity=".18" filter="url(#b1)"/>' for x, y in beads)
    # thin piece: the kit's 16px edge-darkening would grey out the whole part, so narrow it
    return piece(W, H, base, color, (.1, .12), shade, light, '', detail, under=under, grain=(2, .06), name='necklace').replace('stroke-width="16" filter="url(#b6)"', 'stroke-width="5" filter="url(#b3)"')
