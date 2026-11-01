from kit.art_kit import *

def watch(color):
    """Vintage small rectangular gold watch (tank mood) on a slim leather strap, laid flat."""
    W, H = 160, 310
    cx, sw = 80, 17          # strap half-width
    strap = (f'M{cx-sw+2} 22 Q{cx-sw+2} 12 {cx} 12 Q{cx+sw-2} 12 {cx+sw-2} 22 L{cx+sw} 120 L{cx+sw} 196 '
             f'L{cx+sw-2} 278 Q{cx+sw-3} 296 {cx} 300 Q{cx-sw+3} 296 {cx-sw+2} 278 L{cx-sw} 196 L{cx-sw} 120 Z')
    g, gd, gl = '#C9A85C', '#7a5f28', '#FFF3C8'
    edge = stitch(f'M{cx-sw+5} 24 L{cx-sw+4} 112 M{cx+sw-5} 24 L{cx+sw-4} 112 M{cx-sw+4} 204 L{cx-sw+5} 278 Q{cx} 296 {cx+sw-5} 278 L{cx+sw-4} 204', '#fff', .35, .9, '2.5 2')
    holes = ''.join(f'<ellipse cx="{cx}" cy="{y}" rx="2.2" ry="2.5" fill="#1a1716"/><ellipse cx="{cx}" cy="{y-.8}" rx="2" ry="1" fill="#fff" opacity=".18"/>' for y in (232, 246, 260))
    buckle = (f'<rect x="{cx-sw-3}" y="20" width="{2*sw+6}" height="22" rx="6" fill="none" stroke="#000" stroke-opacity=".35" stroke-width="4" transform="translate(1 2)" filter="url(#b1)"/>'
              f'<rect x="{cx-sw-3}" y="20" width="{2*sw+6}" height="22" rx="6" fill="none" stroke="{g}" stroke-width="3.6"/>'
              f'<path d="M{cx-sw} 25 Q{cx-sw+2} 21 {cx-6} 21" fill="none" stroke="{gl}" stroke-width="1.4" stroke-linecap="round"/>'
              f'<rect x="{cx-1.4}" y="22" width="2.8" height="24" rx="1.4" fill="{g}"/>')
    keeper = (f'<rect x="{cx-sw-1}" y="208" width="{2*sw+2}" height="7" rx="2" fill="#000" opacity=".35" transform="translate(0 1.5)" filter="url(#b1)"/>'
              f'<rect x="{cx-sw-1}" y="208" width="{2*sw+2}" height="7" rx="2" fill="{color}"/><rect x="{cx-sw-1}" y="208" width="{2*sw+2}" height="7" rx="2" fill="#000" opacity=".15"/>'
              + line(f'M{cx-sw} 209.5 L{cx+sw} 209.5', '#fff', .25, 1))
    # case
    x0, y0, cw, ch = cx - 27, 118, 54, 78
    case = (f'<rect x="{x0}" y="{y0}" width="{cw}" height="{ch}" rx="7" fill="#000" opacity=".4" transform="translate(2 4)" filter="url(#b3)"/>'
            f'<rect x="{x0}" y="{y0}" width="{cw}" height="{ch}" rx="7" fill="{g}"/>'
            f'<rect x="{x0}" y="{y0}" width="{cw}" height="{ch}" rx="7" fill="none" stroke="{gd}" stroke-opacity=".55" stroke-width="1"/>'
            f'<path d="M{x0+3} {y0+ch-10} L{x0+3} {y0+10} Q{x0+3} {y0+3} {x0+10} {y0+3} L{x0+cw-14} {y0+3}" fill="none" stroke="{gl}" stroke-width="2.2" stroke-linecap="round" opacity=".9"/>'
            f'<path d="M{x0+cw-3} {y0+12} L{x0+cw-3} {y0+ch-8}" stroke="{gd}" stroke-width="2" opacity=".5"/>'
            f'<rect x="{x0+cw}" y="{y0+ch/2-6}" width="6" height="12" rx="2" fill="{g}"/><rect x="{x0+cw+1}" y="{y0+ch/2-5}" width="1.4" height="10" fill="{gl}" opacity=".8"/>'
            f'<rect x="{x0+7}" y="{y0+7}" width="{cw-14}" height="{ch-14}" rx="3" fill="#F6F1E6"/>'
            f'<rect x="{x0+7}" y="{y0+7}" width="{cw-14}" height="{ch-14}" rx="3" fill="none" stroke="#000" stroke-opacity=".18" stroke-width="2"/>')
    dcx, dcy = cx, y0 + ch / 2
    ticks = ''
    import math
    for i in range(12):
        a = math.radians(i * 30); sx, sy = math.sin(a), -math.cos(a)
        k = min((cw / 2 - 11) / max(abs(sx), 1e-6), (ch / 2 - 11) / max(abs(sy), 1e-6))
        L = 3.2 if i % 3 == 0 else 1.8
        ticks += f'<path d="M{dcx+sx*k:.1f} {dcy+sy*k:.1f} L{dcx+sx*(k-L):.1f} {dcy+sy*(k-L):.1f}" stroke="#2a2622" stroke-width="{1.4 if i % 3 == 0 else .9}" stroke-linecap="round"/>'
    hands = (f'<path d="M{dcx} {dcy} L{dcx-7} {dcy-12}" stroke="#2a2622" stroke-width="1.8" stroke-linecap="round"/>'
             f'<path d="M{dcx} {dcy} L{dcx+8} {dcy-20}" stroke="#2a2622" stroke-width="1.2" stroke-linecap="round"/>'
             f'<circle cx="{dcx}" cy="{dcy}" r="1.6" fill="#2a2622"/>'
             f'<path d="M{x0+9} {y0+10} L{x0+cw-20} {y0+10} L{x0+9} {y0+36} Z" fill="#fff" opacity=".35"/>')
    detail = edge + holes + keeper + buckle + case + ticks + hands
    shade = (K(f'M{cx+sw-3} 20 L{cx+sw-2} 296', .4, 3, 5) + K(f'M{cx-sw} 112 L{cx+sw} 112', .4, 3, 6) + K(f'M{cx-sw} 200 L{cx+sw} 200', .35, 3, 6)
             + K(f'M{cx-sw} 268 Q{cx} 300 {cx+sw} 268', .3, 3, 5))
    light = (Wt(f'M{cx-sw+5} 30 L{cx-sw+5} 108', .45, 3, 4) + Wt(f'M{cx-sw+5} 206 L{cx-sw+5} 280', .45, 3, 4)
             + Wt(f'M{cx-sw+6} 60 L{cx-sw+6} 92', .7, 1, 1.4) + Wt(f'M{cx-sw+6} 220 L{cx-sw+6} 250', .7, 1, 1.4))
    # thin piece: the kit's 16px edge-darkening would grey out the whole part, so narrow it
    return piece(W, H, strap, color, (.16, .28), shade, light, '', detail, grain=(1.6, .3), name='watch').replace('stroke-width="16" filter="url(#b6)"', 'stroke-width="9" filter="url(#b3)"')
