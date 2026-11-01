from kit.art_kit import *
import math

def _chain(p0, c, p1, n=34):
    """Gold curb chain along a quadratic curve: alternating flat / edge-on links."""
    out = ''
    for i in range(n + 1):
        t = i / n
        x = (1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * c[0] + t * t * p1[0]
        y = (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * c[1] + t * t * p1[1]
        dx = 2 * (1 - t) * (c[0] - p0[0]) + 2 * t * (p1[0] - c[0]); dy = 2 * (1 - t) * (c[1] - p0[1]) + 2 * t * (p1[1] - c[1])
        a = math.degrees(math.atan2(dy, dx))
        ry = 2.6 if i % 2 else 1.3
        out += (f'<g transform="translate({x:.1f} {y:.1f}) rotate({a:.0f})"><ellipse rx="4.6" ry="{ry}" fill="none" stroke="#8a6a2c" stroke-width="2.2"/>'
                f'<ellipse rx="4.6" ry="{ry}" fill="none" stroke="#D9BC72" stroke-width="1.3"/></g>')
    return out

def crossbody(color):
    """Mini quilted flap crossbody: quilted lambskin, rounded flap, gold turn-lock, gold chain strap."""
    W, H = 300, 300
    body = smooth([(84, 168, 'c'), (216, 168, 'c'), (222, 176), (224, 262), (214, 274), (150, 276), (86, 274), (76, 262), (78, 176)])
    flap = smooth([(80, 168, 'c'), (220, 168, 'c'), (222, 218), (210, 236), (150, 240), (90, 236), (78, 218)])
    under = _chain((86, 176), (150, -120), (214, 176))
    q = ''   # diamond quilt lines
    for k in range(-6, 8):
        q += f'M{40 + k * 26} 160 L{160 + k * 26} 290 M{260 - k * 26} 160 L{140 - k * 26} 290 '
    texture = (f'<path d="{q}" fill="none" stroke="#000" stroke-opacity=".16" stroke-width="3" filter="url(#b1)"/>'
               + f'<path d="{q}" fill="none" stroke="#fff" stroke-opacity=".12" stroke-width="1.2" transform="translate(1.5 0)"/>'
               + stitch(q, '#000', .18, .8, '2 2'))
    lock = (f'<rect x="136" y="224" width="28" height="22" rx="7" fill="#000" opacity=".35" filter="url(#b1)" transform="translate(0 2)"/>'
            f'<rect x="136" y="224" width="28" height="22" rx="7" fill="#C9A85C"/><rect x="139" y="227" width="22" height="16" rx="5" fill="none" stroke="#7a5f28" stroke-opacity=".6"/>'
            f'<ellipse cx="150" cy="235" rx="4" ry="7" fill="#B89650" stroke="#7a5f28" stroke-opacity=".6"/><ellipse cx="146" cy="229" rx="5" ry="2" fill="#FFF3C8" opacity=".8" filter="url(#b1)"/>')
    rings = ''.join(f'<circle cx="{x}" cy="174" r="5" fill="none" stroke="#8a6a2c" stroke-width="3"/><circle cx="{x}" cy="174" r="5" fill="none" stroke="#D9BC72" stroke-width="1.6"/>' for x in (86, 214))
    flap_q = f'<g clip-path="url(#fl)"><path d="{q}" fill="none" stroke="#000" stroke-opacity=".18" stroke-width="3" filter="url(#b1)"/>' + stitch(q, '#000', .18, .8, '2 2') + '</g>'
    detail = (f'<defs><clipPath id="fl"><path d="{flap}"/></clipPath></defs>' + raised(flap, color, .45, .2) + flap_q
              + f'<path d="{flap}" fill="none" stroke="#000" stroke-opacity=".1" stroke-width="8" filter="url(#b3)"/>'
              + Wt('M96 176 Q150 170 204 176', .35, 3, 5) + K('M86 226 Q150 246 214 226', .25, 3, 4)
              + rings + lock)
    shade = K('M82 266 Q150 284 218 266', .45, 6, 14) + K('M220 180 L222 266', .4, 6, 10) + K('M80 240 L84 270', .2, 6, 8)
    light = Wt('M92 250 Q150 258 208 250', .15, 3, 6)
    return piece(W, H, body, color, (.2, .3), shade, light, texture, detail, under=under, grain=(1.5, .2), name='crossbody')
