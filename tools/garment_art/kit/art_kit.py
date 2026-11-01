"""Garment SVGs: base fill (recolorable) + shade / light / texture overlays clipped to the base.
Every overlay is black or white at an alpha, so one SVG works for any palette color.
Layer ids: base, form (soft volume), shade, light, texture, detail (seams/hardware)."""
import os, subprocess, hashlib, colorsys
S = os.path.dirname(os.path.abspath(__file__))
CHROME = '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'

def smooth(pts, closed=True):
    """Catmull-Rom through points; (x,y,'c') = sharp corner."""
    P = [(p[0], p[1]) for p in pts]; C = [len(p) > 2 for p in pts]; n = len(P)
    d = f'M{P[0][0]:.1f} {P[0][1]:.1f}'
    rng = range(n if closed else n - 1)
    for i in rng:
        a, b = P[i], P[(i + 1) % n]
        pa, pb = P[(i - 1) % n], P[(i + 2) % n]
        if not closed and i == 0: pa = a
        if not closed and i + 2 >= n: pb = b
        c1 = a if C[i] else (a[0] + (b[0] - pa[0]) / 6, a[1] + (b[1] - pa[1]) / 6)
        c2 = b if C[(i + 1) % n] else (b[0] - (pb[0] - a[0]) / 6, b[1] - (pb[1] - a[1]) / 6)
        d += f' C{c1[0]:.1f} {c1[1]:.1f} {c2[0]:.1f} {c2[1]:.1f} {b[0]:.1f} {b[1]:.1f}'
    return d + (' Z' if closed else '')

def sym(left, W):
    """left: points from top-center down the left side to bottom-center. Mirrors to a closed outline."""
    right = [((W - p[0]), p[1]) + tuple(p[2:]) for p in reversed(left[1:-1])]
    return left + right

def mir(pts, W): return [((W - p[0]), p[1]) + tuple(p[2:]) for p in pts]

def lum(hexc):
    r, g, b = (int(hexc[i:i + 2], 16) / 255 for i in (1, 3, 5))
    return .2126 * r + .7152 * g + .0722 * b

DEFS = '''
<filter id="b1" filterUnits="userSpaceOnUse" x="-200" y="-200" width="1000" height="1000"><feGaussianBlur stdDeviation="1.2"/></filter>
<filter id="b3" filterUnits="userSpaceOnUse" x="-200" y="-200" width="1000" height="1000"><feGaussianBlur stdDeviation="3"/></filter>
<filter id="b6" filterUnits="userSpaceOnUse" x="-200" y="-200" width="1000" height="1000"><feGaussianBlur stdDeviation="6"/></filter>
<filter id="b12" filterUnits="userSpaceOnUse" x="-200" y="-200" width="1000" height="1000"><feGaussianBlur stdDeviation="12"/></filter>
<filter id="b22" filterUnits="userSpaceOnUse" x="-200" y="-200" width="1000" height="1000"><feGaussianBlur stdDeviation="22"/></filter>
<filter id="grainW"><feTurbulence type="fractalNoise" baseFrequency="{gf}" numOctaves="2" seed="3"/><feColorMatrix values="0 0 0 0 1 0 0 0 0 1 0 0 0 0 1 {gk} 0 0 0 {gb}"/></filter>
<filter id="grainK"><feTurbulence type="fractalNoise" baseFrequency="{gf}" numOctaves="2" seed="3"/><feColorMatrix values="0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 -{gk} 0 0 0 {gc}"/></filter>
<filter id="boucleW"><feTurbulence type="turbulence" baseFrequency=".22" numOctaves="2" seed="7"/><feColorMatrix values="0 0 0 0 1 0 0 0 0 1 0 0 0 0 1 2.4 0 0 0 -.6"/></filter>
<filter id="boucleK"><feTurbulence type="turbulence" baseFrequency=".26" numOctaves="2" seed="11"/><feColorMatrix values="0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 2.2 0 0 0 -.45"/></filter>
<pattern id="rib" width="5" height="10" patternUnits="userSpaceOnUse"><rect width="2" height="10" fill="#fff" opacity=".11"/><rect x="2.5" width="2" height="10" fill="#000" opacity=".17"/></pattern>
<pattern id="ribH" width="10" height="5" patternUnits="userSpaceOnUse"><rect width="10" height="2" fill="#fff" opacity=".22"/><rect y="3" width="10" height="2" fill="#000" opacity=".3"/></pattern>
<pattern id="stock" width="9" height="8" patternUnits="userSpaceOnUse"><path d="M0.5 0 L4.5 7 M8.5 0 L4.5 7" stroke="#fff" stroke-width="1.6" opacity=".12" fill="none"/><path d="M0 4 L4.5 11" stroke="#000" stroke-width="1" opacity=".1"/></pattern>
<pattern id="twill" width="5" height="5" patternUnits="userSpaceOnUse" patternTransform="rotate(-35)"><rect width="5" height="2" fill="#fff" opacity=".13"/><rect y="2.5" width="5" height="1.2" fill="#000" opacity=".12"/></pattern>
'''

def piece(W, H, base, color, form=(.16, .30), shade='', light='', texture='', detail='', under='', fabric='', grain=(.9, .35), name=''):
    """form: (light alpha, shade alpha) of the global volume. Dark colors get more light, light colors more shade."""
    L = lum(color)
    la, sa = form
    if L < .08: la, sa = la * 1.9, sa * .75          # black / navy: form comes from highlights
    elif L > .7: la, sa = la * .35, sa * .8          # white / cream: form comes from shade
    gk = grain[1] * (1.8 if L < .08 else 1)
    defs = DEFS.format(gf=grain[0], gk=gk, gb=-gk * .5, gc=gk * .5)
    k = 1 if L > .08 else .8   # shade layer alpha scale
    w = 1 if L > .7 else (2.1 if L < .08 else 1.3)  # light layer alpha scale
    return f'''<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="{W}" height="{H}" viewBox="0 0 {W} {H}" data-name="{name}">
<defs>{defs}<clipPath id="c"><path d="{base}"/></clipPath>
<linearGradient id="fg" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#fff" stop-opacity="{la}"/><stop offset=".45" stop-color="#fff" stop-opacity="0"/><stop offset=".6" stop-color="#000" stop-opacity="0"/><stop offset="1" stop-color="#000" stop-opacity="{sa}"/></linearGradient></defs>
<g id="under">{under}</g>
<g id="base"><path d="{base}" fill="{color}"/></g>
<g clip-path="url(#c)">
 <g id="fabric">{fabric}</g>
 <g id="texture">{texture}<rect width="{W}" height="{H}" filter="url(#grainW)"/><rect width="{W}" height="{H}" filter="url(#grainK)"/></g>
 <g id="form"><rect width="{W}" height="{H}" fill="url(#fg)"/><path d="{base}" fill="none" stroke="#000" stroke-opacity="{.32 * k}" stroke-width="16" filter="url(#b6)"/></g>
 <g id="shade" opacity="{k}">{shade}</g>
 <g id="light" opacity="{min(1, w)}">{light}</g>
</g>
<g id="detail">{detail}</g>
<path d="{base}" fill="none" stroke="#000" stroke-opacity="{.16 if L > .5 else .1}" stroke-width="1"/>
</svg>'''

def K(d, a, blur=6, w=None):   # soft dark shape (or stroke if w)
    return f'<path d="{d}" fill="{"none" if w else "#000"}" stroke="{"#000" if w else "none"}" stroke-width="{w or 0}" stroke-linecap="round" opacity="{a}" filter="url(#b{blur})"/>'
def Wt(d, a, blur=6, w=None):
    return f'<path d="{d}" fill="{"none" if w else "#fff"}" stroke="{"#fff" if w else "none"}" stroke-width="{w or 0}" stroke-linecap="round" opacity="{a}" filter="url(#b{blur})"/>'
def stitch(d, col='#000', a=.35, w=1.2, dash='4 3'):
    return f'<path d="{d}" fill="none" stroke="{col}" stroke-opacity="{a}" stroke-width="{w}" stroke-dasharray="{dash}" stroke-linecap="round"/>'
def line(d, col='#000', a=.3, w=1.5):
    return f'<path d="{d}" fill="none" stroke="{col}" stroke-opacity="{a}" stroke-width="{w}" stroke-linecap="round"/>'
def shape(d, fill, a=1, extra=''):
    return f'<path d="{d}" fill="{fill}" opacity="{a}" {extra}/>'
def button(x, y, r, col='#1a1a1a', metal=False):
    if metal:
        return (f'<circle cx="{x}" cy="{y+1.5}" r="{r}" fill="#000" opacity=".35" filter="url(#b1)"/>'
                f'<circle cx="{x}" cy="{y}" r="{r}" fill="#B89650"/><circle cx="{x}" cy="{y}" r="{r*.72}" fill="none" stroke="#7a5f28" stroke-width="{r*.18}" opacity=".7"/>'
                f'<circle cx="{x-r*.3}" cy="{y-r*.35}" r="{r*.32}" fill="#FFF3C8" opacity=".85" filter="url(#b1)"/>')
    return (f'<circle cx="{x}" cy="{y+1.5}" r="{r}" fill="#000" opacity=".4" filter="url(#b1)"/>'
            f'<circle cx="{x}" cy="{y}" r="{r}" fill="{col}"/><circle cx="{x-r*.25}" cy="{y-r*.3}" r="{r*.55}" fill="#fff" opacity=".18" filter="url(#b1)"/>'
            f'<circle cx="{x-r*.25}" cy="{y}" r="{r*.12}" fill="#000" opacity=".5"/><circle cx="{x+r*.25}" cy="{y}" r="{r*.12}" fill="#000" opacity=".5"/>')
def pearl(x, y, r):
    return (f'<circle cx="{x}" cy="{y+1.2}" r="{r}" fill="#000" opacity=".3" filter="url(#b1)"/><circle cx="{x}" cy="{y}" r="{r}" fill="#F3EEE6"/>'
            f'<circle cx="{x+r*.2}" cy="{y+r*.25}" r="{r*.8}" fill="#B9AFA2" opacity=".5" filter="url(#b1)"/><circle cx="{x-r*.3}" cy="{y-r*.35}" r="{r*.35}" fill="#fff"/>')
def raised(d, color, sh=.35, hi=.12):
    """A layer of the same fabric sitting on top (lapel, flap, pocket): drop shadow + fill + top light."""
    return (f'<path d="{d}" fill="#000" opacity="{sh}" transform="translate(1.5 3)" filter="url(#b3)"/>'
            f'<path d="{d}" fill="{color}"/><path d="{d}" fill="none" stroke="#fff" stroke-opacity="{hi}" stroke-width="2"/>')

# ---------------------------------------------------------------- outer
def short_coat(color):
    W, H = 400, 430
    left = [(200, 34, 'c'), (166, 28, 'c'), (146, 38), (100, 54, 'c'), (80, 72), (66, 120), (54, 200), (42, 290), (32, 380, 'c'), (84, 390, 'c'),
            (92, 300), (100, 230), (110, 176, 'c'), (112, 260), (110, 336), (106, 404, 'c'), (150, 408), (200, 406, 'c')]
    base = smooth(sym(left, W))
    vopen = smooth([(170, 32, 'c'), (200, 228, 'c'), (230, 32, 'c'), (200, 40)])
    lapL = [(168, 30, 'c'), (146, 42), (138, 112, 'c'), (124, 120, 'c'), (138, 140, 'c'), (198, 238, 'c'), (176, 40, 'c')]
    lapR = mir(lapL, W)
    collar_back = smooth([(166, 28, 'c'), (200, 22), (234, 28, 'c'), (226, 40), (200, 36), (174, 40, 'c')])
    pk = lambda x: f'M{x} 316 L{x+52} 312 L{x+53} 330 Q{x+27} 334 {x+1} 333 Z'
    fabric = (raised(collar_back, color, .2) + shape(vopen, '#000', .45) + K(vopen, .35, 6)
              + raised(smooth(lapR), color, .4, .1) + raised(smooth(lapL), color, .45, .14)
              + line('M200 238 L200 405', '#000', .35, 2) + line('M202 238 L202 405', '#fff', .08, 1)
              + raised(pk(124), color, .3, .1) + raised(pk(224), color, .3, .1))
    detail = (button(212, 268, 9) + button(212, 336, 9)
              + stitch('M40 360 Q58 364 86 368', '#000', .22) + stitch(f'M{W-40} 360 Q{W-58} 364 {W-86} 368', '#000', .22))
    shade = (K('M108 180 Q114 280 106 400', .5, 12, 14) + K(f'M{W-108} 180 Q{W-114} 280 {W-106} 400', .55, 12, 14)
             + K('M84 120 Q78 250 70 380', .25, 12, 10) + K(f'M{W-84} 120 Q{W-78} 250 {W-70} 380', .3, 12, 10)
             + K('M120 395 Q200 385 280 396', .25, 6, 10) + K('M160 160 Q170 260 150 390', .18, 22, 30))
    light = (Wt('M140 120 Q150 220 140 330', .22, 22, 40) + Wt('M70 90 Q58 200 48 300', .2, 12, 12)
             + Wt(f'M{W-150} 150 Q{W-150} 240 {W-160} 330', .1, 22, 30) + Wt('M240 260 Q270 270 300 250', .08, 12, 20))
    return piece(W, H, base, color, (.18, .32), shade, light, '', detail, fabric=fabric, grain=(1.1, .5), name='short_coat')

def boucle_jacket(color):
    W, H = 400, 340
    left = [(200, 30, 'c'), (162, 28, 'c'), (108, 46, 'c'), (90, 60), (78, 100), (68, 170), (58, 250), (50, 316, 'c'), (96, 324, 'c'), (102, 250),
            (110, 170), (118, 124, 'c'), (120, 200), (118, 268, 'c'), (200, 272, 'c')]
    base = smooth(sym(left, W))
    neck = 'M162 28 Q166 84 200 88 Q234 84 238 28 Q200 38 162 28 Z'
    trim = lambda d: (line(d, '#000', .25, 9) + stitch(d, '#fff', .35, 3, '3 3') + stitch(d, '#000', .3, 2, '2 4'))
    tex = (f'<rect width="{W}" height="{H}" filter="url(#boucleW)" opacity=".55"/><rect width="{W}" height="{H}" filter="url(#boucleK)" opacity=".7"/>')
    pocket = lambda x, y, w, h: f'M{x} {y} L{x+w} {y} L{x+w} {y+h-8} Q{x+w} {y+h} {x+w-8} {y+h} L{x+8} {y+h} Q{x} {y+h} {x} {y+h-8} Z'
    detail = (shape(neck, '#000', .5) + K(neck, .3, 6)
              + trim('M162 30 Q166 84 200 88 Q234 84 238 30') + trim('M200 88 L200 270') + trim('M120 266 L280 266')
              + trim('M52 306 Q74 311 96 314') + trim(f'M{W-52} 306 Q{W-74} 311 {W-96} 314')
              + trim('M132 198 L182 198') + trim('M218 198 L268 198')
              + ''.join(button(212, y, 7, metal=True) for y in (118, 168, 218)) + button(157, 196, 5.5, metal=True) + button(243, 196, 5.5, metal=True))
    shade = (K('M118 128 Q122 200 118 266', .45, 12, 12) + K(f'M{W-118} 128 Q{W-122} 200 {W-118} 266', .5, 12, 12)
             + K('M90 110 Q80 210 72 310', .25, 12, 10) + K(f'M{W-90} 110 Q{W-80} 210 {W-72} 310', .3, 12, 10))
    light = Wt('M150 100 Q160 160 150 230', .2, 22, 40) + Wt('M76 90 Q66 180 58 270', .18, 12, 12)
    fabric = raised(pocket(132, 196, 50, 46), color, .25, .05) + raised(pocket(218, 196, 50, 46), color, .25, .05)
    return piece(W, H, base, color, (.14, .28), shade, light, tex, detail, fabric=fabric, grain=(1.3, .45), name='boucle_jacket')

def cropped_cardigan(color):
    W, H = 380, 300
    left = [(190, 30, 'c'), (156, 28, 'c'), (104, 44, 'c'), (88, 58), (78, 100), (68, 170), (58, 240), (52, 282, 'c'), (96, 290, 'c'), (100, 240),
            (106, 170), (114, 118, 'c'), (116, 190), (118, 252, 'c'), (190, 256, 'c')]
    base = smooth(sym(left, W))
    v = 'M156 28 L190 150 L224 28 Q190 40 156 28 Z'
    tex = f'<rect width="{W}" height="{H}" fill="url(#stock)"/>'
    detail = (shape(v, '#000', .45) + K(v, .3, 6)
              + f'<path d="M156 28 L190 150 L190 255 M224 28 L190 150" fill="none" stroke="url(#ribH)" stroke-width="0"/>'
              + line('M154 30 L186 148', '#000', .25, 10) + line(f'M{W-154} 30 L{W-186} 148', '#000', .25, 10)
              + stitch('M156 30 L188 146', '#fff', .25, 2, '1.5 2.5') + stitch(f'M{W-156} 30 L{W-188} 146', '#fff', .25, 2, '1.5 2.5')
              + line('M190 150 L190 254', '#000', .3, 1.5)
              + f'<g clip-path="url(#c)"><rect x="100" y="232" width="180" height="26" fill="url(#ribH)" transform="rotate(90 190 245)" opacity="0"/></g>'
              + ''.join(pearl(196, y, 5.5) for y in (164, 190, 216, 240)))
    rib = (f'<rect x="0" y="230" width="{W}" height="30" fill="url(#rib)"/>' + line('M116 231 L264 231', '#000', .25, 2)
           + f'<rect x="0" y="262" width="{W}" height="40" fill="url(#rib)"/>')
    shade = (K('M114 122 Q118 190 118 250', .4, 12, 12) + K(f'M{W-114} 122 Q{W-118} 190 {W-118} 250', .45, 12, 12)
             + K('M88 100 Q78 180 70 280', .22, 12, 10) + K(f'M{W-88} 100 Q{W-78} 180 {W-70} 280', .26, 12, 10) + rib)
    light = Wt('M140 80 Q150 140 140 210', .2, 22, 36) + Wt('M78 90 Q68 170 60 250', .18, 12, 12)
    return piece(W, H, base, color, (.14, .26), shade, light, tex, detail, grain=(1.4, .35), name='cropped_cardigan')

# ---------------------------------------------------------------- tops
def turtleneck(color):
    W, H = 360, 450
    left = [(180, 62, 'c'), (146, 58, 'c'), (104, 74, 'c'), (90, 90), (78, 150), (64, 250), (52, 350), (44, 418, 'c'), (84, 424, 'c'),
            (90, 350), (100, 250), (112, 160, 'c'), (114, 260), (114, 340), (112, 424, 'c'), (180, 428, 'c')]
    base = smooth(sym(left, W))
    collar = smooth([(140, 14, 'c'), (180, 10), (220, 14, 'c'), (224, 72, 'c'), (180, 78), (136, 72, 'c')])
    tex = f'<rect width="{W}" height="{H}" fill="url(#rib)"/>'
    collar_g = (f'<path d="{collar}" fill="#000" opacity=".35" transform="translate(0 4)" filter="url(#b3)"/><path d="{collar}" fill="{color}"/>'
                f'<clipPath id="cc"><path d="{collar}"/></clipPath><g clip-path="url(#cc)"><rect width="{W}" height="100" fill="url(#rib)"/>'
                f'{K("M136 46 Q180 54 224 46", .35, 3, 6)}{Wt("M136 52 Q180 60 224 52", .2, 3, 4)}{K("M140 14 Q180 24 220 14", .5, 3, 10)}'
                f'{Wt("M150 30 L150 70", .2, 6, 14)}{K("M216 20 L216 74", .3, 6, 14)}</g>')
    detail = (collar_g + line('M114 404 L246 404', '#000', .22, 2) + line('M48 396 Q66 398 86 402', '#000', .22, 2)
              + line(f'M{W-48} 396 Q{W-66} 398 {W-86} 402', '#000', .22, 2))
    shade = (K('M112 164 Q118 280 112 420', .45, 12, 12) + K(f'M{W-112} 164 Q{W-118} 280 {W-112} 420', .5, 12, 12)
             + K('M86 120 Q76 260 64 410', .2, 12, 10) + K(f'M{W-86} 120 Q{W-76} 260 {W-64} 410', .25, 12, 10)
             + K('M150 240 Q180 300 170 400', .14, 22, 30))
    light = Wt('M140 110 Q150 200 140 300', .2, 22, 40) + Wt('M82 110 Q70 230 56 360', .2, 12, 10) + Wt('M216 300 Q230 340 220 400', .08, 12, 20)
    return piece(W, H, base, color, (.16, .3), shade, light, tex, detail, grain=(1.2, .3), name='turtleneck')

def oversized_knit(color):
    W, H = 420, 400
    left = [(210, 52, 'c'), (172, 44, 'c'), (90, 64, 'c'), (66, 118), (46, 210), (32, 300), (24, 352, 'c'), (84, 368, 'c'), (90, 300),
            (100, 220), (110, 176, 'c'), (104, 250), (102, 330), (110, 356), (114, 386, 'c'), (210, 390, 'c')]
    base = smooth(sym(left, W))
    neck_in = 'M174 46 Q182 84 210 88 Q238 84 246 46 Q210 56 174 46 Z'
    tex = f'<rect width="{W}" height="{H}" fill="url(#stock)"/><rect width="{W}" height="{H}" fill="url(#stock)" transform="translate(4 4)" opacity=".5"/>'
    rib = (f'<rect x="100" y="352" width="220" height="40" fill="url(#rib)"/>' + line('M104 352 Q210 345 316 352', '#000', .3, 2.5)
           + f'<g transform="rotate(8 54 360)"><rect x="20" y="336" width="70" height="40" fill="url(#rib)"/></g>'
           + f'<g transform="rotate(-8 {W-54} 360)"><rect x="{W-90}" y="336" width="70" height="40" fill="url(#rib)"/></g>'
           + line('M28 330 Q56 336 88 344', '#000', .25, 2) + line(f'M{W-28} 330 Q{W-56} 336 {W-88} 344', '#000', .25, 2))
    detail = (shape(neck_in, '#000', .45) + K(neck_in, .3, 6)
              + f'<path d="M172 44 Q182 92 210 96 Q238 92 248 44" fill="none" stroke="{color}" stroke-width="13"/>'
              + f'<path d="M172 44 Q182 92 210 96 Q238 92 248 44" fill="none" stroke="url(#rib)" stroke-width="13"/>'
              + line('M166 46 Q178 102 210 104 Q242 102 254 46', '#000', .18, 2)
              + stitch('M90 66 Q96 120 110 176', '#000', .3, 2.5, '1 0') + stitch(f'M{W-90} 66 Q{W-96} 120 {W-110} 176', '#000', .3, 2.5, '1 0'))
    shade = (rib + K('M108 180 Q100 270 108 380', .4, 12, 14) + K(f'M{W-108} 180 Q{W-100} 270 {W-108} 380', .45, 12, 14)
             + K('M70 130 Q54 240 44 340', .2, 12, 12) + K(f'M{W-70} 130 Q{W-54} 240 {W-44} 340', .25, 12, 12)
             + K('M150 200 Q200 260 170 350', .16, 22, 30) + K('M260 150 Q280 240 270 340', .14, 22, 26))
    light = Wt('M150 110 Q170 180 150 260', .2, 22, 46) + Wt('M70 100 Q52 200 38 300', .2, 12, 14) + Wt('M230 220 Q250 280 236 330', .1, 22, 30)
    return piece(W, H, base, color, (.16, .28), shade, light, tex, detail, grain=(1.2, .3), name='oversized_knit')

# ---------------------------------------------------------------- bottoms
def wide_denim(color):
    W, H = 300, 520
    pts = [(58, 20, 'c'), (242, 20, 'c'), (248, 70), (256, 160), (262, 300), (270, 504, 'c'), (164, 510, 'c'), (158, 320), (150, 196, 'c'),
           (142, 320), (136, 510, 'c'), (30, 504, 'c'), (38, 300), (44, 160), (52, 70)]
    base = smooth(pts)
    thread = '#C99A52'
    tex = f'<rect width="{W}" height="{H}" fill="url(#twill)"/>'
    detail = (line('M57 44 L243 44', '#000', .3, 2) + stitch('M58 24 L242 24', thread, .8, 1.2) + stitch('M58 40 L242 40', thread, .8, 1.2)
              + stitch('M162 46 L162 128 Q162 150 148 160', thread, .85, 1.3) + line('M150 46 L150 170', '#000', .28, 2)
              + stitch('M60 48 Q92 52 100 50 Q90 92 54 108', thread, .8, 1.2) + line('M62 46 Q96 50 102 48 Q92 94 52 112', '#000', .25, 1.5)
              + stitch(f'M{W-60} 48 Q{W-92} 52 {W-100} 50 Q{W-90} 92 {W-54} 108', thread, .8, 1.2) + line(f'M{W-62} 46 Q{W-96} 50 {W-102} 48 Q{W-92} 94 {W-52} 112', '#000', .25, 1.5)
              + ''.join(f'<rect x="{x-4}" y="17" width="8" height="30" rx="2" fill="#000" opacity=".25" filter="url(#b1)"/><rect x="{x-4}" y="16" width="8" height="28" rx="2" fill="{color}"/>' + stitch(f'M{x} 18 L{x} 42', thread, .7, 1, '2 2') for x in (78, 120, 180, 222))
              + button(150, 32, 7, metal=True) + button(100, 52, 3, metal=True) + button(200, 52, 3, metal=True)
              + stitch('M33 492 L138 497', thread, .7, 1.2) + stitch(f'M{W-33} 492 L{W-138} 497', thread, .7, 1.2))
    shade = (K('M150 196 L148 500', .5, 6, 10) + K('M150 196 Q152 330 156 505', .5, 6, 8)
             + K('M40 160 Q34 330 32 500', .3, 12, 14) + K(f'M{W-40} 160 Q{W-34} 330 {W-32} 500', .35, 12, 14)
             + K('M40 470 Q90 460 136 474', .2, 6, 6) + K(f'M{W-40} 470 Q{W-90} 460 {W-136} 474', .2, 6, 6))
    light = (Wt('M92 120 Q88 300 86 480', .35, 22, 34) + Wt(f'M{W-92} 120 Q{W-88} 300 {W-86} 480', .25, 22, 30)
             + ''.join(Wt(f'M{140-i*7} {168+i*4} Q{118-i*6} {178+i*6} {96-i*4} {176+i*12}', .35, 3, 3) for i in range(4))
             + ''.join(Wt(f'M{160+i*7} {168+i*4} Q{182+i*6} {178+i*6} {204+i*4} {176+i*12}', .3, 3, 3) for i in range(4))
             + Wt('M60 300 Q90 296 120 302', .2, 6, 10) + Wt(f'M{W-60} 300 Q{W-90} 296 {W-120} 302', .16, 6, 10))
    return piece(W, H, base, color, (.12, .3), shade, light, tex, detail, grain=(1.6, .5), name='wide_denim')

def pleated_mini(color):
    W, H = 320, 250
    n = 16
    hem = []
    for i in range(n + 1):
        t = i / n; x = 20 + 280 * t; y = 226 + 14 * (1 - (2 * t - 1) ** 2) * .5 + (6 if i % 2 else 0)
        hem.append((x, y, 'c'))
    base = smooth([(78, 14, 'c'), (242, 14, 'c'), (244, 40, 'c')] + list(reversed(hem)) + [(76, 40, 'c')])
    pleats_s, pleats_l = '', ''
    for i in range(n):
        t0, t1 = i / n, (i + 1) / n
        x0w, x1w = 78 + 164 * t0, 78 + 164 * t1
        x0, x1 = hem[i][0], hem[i + 1][0]
        poly = f'M{x0w} 40 L{x1w} 40 L{x1} {hem[i+1][1]} L{x0} {hem[i][1]} Z'
        if i % 2: pleats_s += f'<path d="{poly}" fill="#000" opacity=".2"/>'
        else: pleats_l += f'<path d="{poly}" fill="#fff" opacity=".07"/>'
        pleats_s += line(f'M{x0w} 42 L{x0} {hem[i][1]}', '#000', .25, 1.2)
    detail = (line('M77 40 L243 40', '#000', .3, 2) + stitch('M80 18 L240 18', '#000', .2, 1) + stitch('M80 36 L240 36', '#000', .2, 1))
    shade = pleats_s + K('M60 120 L30 230', .25, 12, 20) + K('M260 120 L292 230', .3, 12, 20) + K('M80 40 L242 40', .25, 6, 8)
    light = pleats_l + Wt('M120 60 Q110 150 100 220', .18, 22, 40)
    return piece(W, H, base, color, (.14, .26), shade, light, '', detail, grain=(1.3, .3), name='pleated_mini')

def long_skirt(color):
    W, H = 280, 500
    hem = [(24, 476, 'c'), (70, 484), (120, 478), (160, 486), (210, 478), (256, 476, 'c')]
    base = smooth([(84, 14, 'c'), (196, 14, 'c'), (198, 38, 'c'), (220, 200), (256, 476, 'c')] + list(reversed(hem[1:-1])) + [(24, 476, 'c'), (60, 200), (82, 38, 'c')])
    detail = line('M82 38 L198 38', '#000', .3, 2) + stitch('M86 20 L194 20', '#000', .18, 1)
    shade = (K('M110 60 Q96 280 80 480', .35, 12, 12) + K('M170 60 Q186 280 200 480', .4, 12, 12) + K('M140 100 Q142 300 146 480', .2, 12, 8)
             + K('M40 300 Q30 400 26 476', .3, 12, 16) + K('M240 300 Q250 400 254 476', .35, 12, 16) + K('M90 38 L192 38', .3, 6, 8))
    light = (Wt('M122 70 Q112 280 104 470', .2, 12, 16) + Wt('M156 80 Q166 280 176 470', .14, 12, 14)
             + Wt('M64 200 Q54 340 46 460', .14, 12, 12) + Wt('M220 220 Q232 340 238 460', .08, 12, 10))
    return piece(W, H, base, color, (.18, .3), shade, light, '', detail, grain=(1.5, .18), name='long_skirt')

# ---------------------------------------------------------------- shoes (side view, pair: back shoe offset & darker)
def pair(W, H, one_svg_inner, dx, dy):
    return (f'<g transform="translate({dx} {dy})">{one_svg_inner}<g opacity=".28">{one_svg_inner.replace("fill=", "data-f=")}</g></g>'
            f'<g>{one_svg_inner}</g>')

def shoe_svg(W, H, name, color, outline, sole, detail, shade, light, back=(36, -26), sole_col='#1b1918', gloss=1.0, grain=(1.2, .2)):
    """Two-shoe side view. The back shoe gets an extra shade wash."""
    L = lum(color)
    defs = DEFS.format(gf=grain[0], gk=grain[1], gb=-grain[1] * .5, gc=grain[1] * .5)
    wl = 1 if L > .7 else (2.0 if L < .08 else 1.3)
    one = (f'<path d="{sole}" fill="{sole_col}"/><path d="{sole}" fill="#fff" opacity=".06"/>'
           f'<path d="{outline}" fill="{color}"/>'
           f'<g clip-path="url(#c)"><rect width="{W}" height="{H}" filter="url(#grainW)"/><rect width="{W}" height="{H}" filter="url(#grainK)"/>'
           f'<rect width="{W}" height="{H}" fill="url(#fg)"/><path d="{outline}" fill="none" stroke="#000" stroke-opacity=".3" stroke-width="14" filter="url(#b6)"/>'
           f'<g id="shade">{shade}</g><g id="light" opacity="{min(1, wl * gloss)}">{light}</g></g>{detail}'
           f'<path d="{outline}" fill="none" stroke="#000" stroke-opacity=".14" stroke-width="1"/>')
    dx, dy = back
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}" data-name="{name}">
<defs>{defs}<clipPath id="c"><path d="{outline}"/></clipPath>
<linearGradient id="fg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#fff" stop-opacity=".1"/><stop offset=".5" stop-color="#fff" stop-opacity="0"/><stop offset="1" stop-color="#000" stop-opacity=".3"/></linearGradient></defs>
<g transform="translate({dx} {dy})">{one}<path d="{outline}" fill="#000" opacity=".22"/><path d="{sole}" fill="#000" opacity=".2"/></g>
{one}
</svg>'''

def loafers(color):
    W, H = 380, 200
    o = smooth([(10, 160), (20, 142), (56, 128), (110, 118), (150, 114, 'c'), (200, 126), (252, 124), (294, 110, 'c'), (302, 134), (300, 162, 'c'), (14, 166, 'c')])
    sole = smooth([(8, 158), (12, 174, 'c'), (252, 176, 'c'), (256, 186, 'c'), (298, 186, 'c'), (302, 160, 'c'), (14, 162, 'c')])
    strap = 'M104 122 Q140 112 176 120 L178 134 Q140 126 104 136 Z'
    detail = (raised(strap, color, .35, .15) + '<path d="M128 124 Q142 120 156 123 L156 127 Q142 124 128 128 Z" fill="#000" opacity=".6"/>'
              + stitch('M22 152 Q40 130 104 122', '#000', .3, 1.2, '3 2') + line('M24 154 Q44 133 106 125', '#fff', .16, 1)
              + line('M10 162 L300 162', '#fff', .22, 1.2) + line('M150 115 Q200 130 294 111', '#000', .55, 3))
    shade = K('M30 158 Q150 166 292 156', .3, 6, 12) + K('M150 118 Q200 134 292 114', .45, 6, 8)
    light = (Wt('M24 146 Q50 128 100 122', .55, 3, 5) + Wt('M276 120 Q292 132 294 156', .45, 3, 5) + Wt('M190 136 Q220 140 250 134', .22, 6, 8)
             + Wt('M44 136 Q56 130 70 128', .85, 1, 2.5))
    return shoe_svg(W, H, 'loafers', color, o, sole, detail, shade, light)

def mary_janes(color):
    W, H = 380, 190
    o = smooth([(10, 154), (20, 136), (52, 124), (100, 122, 'c'), (170, 140), (244, 134), (292, 112, 'c'), (302, 136), (300, 158, 'c'), (14, 162, 'c')])
    sole = smooth([(8, 152), (12, 168, 'c'), (250, 172, 'c'), (254, 182, 'c'), (298, 182, 'c'), (302, 156, 'c'), (14, 158, 'c')])
    strap = 'M168 140 Q156 118 140 104 L150 99 Q168 114 182 138 Z'
    detail = (raised(strap, color, .35, .18) + '<rect x="170" y="126" width="13" height="11" rx="2.5" fill="none" stroke="#C8B48A" stroke-width="2.4" transform="rotate(-55 176 131)"/>'
              + line('M100 123 Q170 146 292 113', '#000', .55, 3) + line('M10 158 L300 158', '#fff', .22, 1.2))
    shade = K('M30 152 Q150 160 292 152', .3, 6, 12) + K('M104 126 Q180 148 288 116', .4, 6, 8)
    light = Wt('M24 140 Q46 126 92 124', .6, 3, 5) + Wt('M276 124 Q292 136 294 152', .45, 3, 5) + Wt('M40 132 Q52 127 66 125', .85, 1, 2.5)
    return shoe_svg(W, H, 'mary_janes', color, o, sole, detail, shade, light, back=(40, -22))

def long_boots(color):
    W, H = 300, 490
    o = smooth([(64, 12, 'c'), (156, 12, 'c'), (162, 120), (152, 250), (148, 330), (158, 400), (160, 446, 'c'), (20, 452, 'c'), (8, 430), (22, 408), (66, 392), (88, 356), (82, 250), (72, 120)])
    sole = smooth([(6, 432), (10, 460, 'c'), (118, 462, 'c'), (122, 478, 'c'), (160, 478, 'c'), (162, 444, 'c'), (20, 446, 'c')])
    detail = (line('M64 14 L156 14', '#000', .4, 3) + line('M8 446 L160 446', '#fff', .2, 1.2)
              + line('M150 40 Q156 160 146 300 Q146 360 156 420', '#000', .25, 1.5) + stitch('M154 44 Q160 160 150 300', '#000', .2, 1))
    shade = (K('M150 20 Q160 160 146 330 Q150 400 158 444', .45, 12, 20) + K('M30 430 Q90 440 150 436', .3, 6, 12)
             + K('M70 20 L150 20', .4, 6, 10))
    light = (Wt('M84 30 Q92 160 96 260 Q100 330 92 370', .55, 6, 10) + Wt('M90 60 Q96 160 98 240', .7, 1, 2.5)
             + Wt('M22 418 Q50 400 80 396', .6, 3, 6) + Wt('M128 300 Q136 360 140 420', .18, 12, 16))
    return shoe_svg(W, H, 'long_boots', color, o, sole, detail, shade, light, back=(70, -6))

def rain_boots(color):
    W, H = 330, 230
    o = smooth([(118, 18, 'c'), (222, 18, 'c'), (228, 80), (234, 150), (238, 196, 'c'), (24, 200, 'c'), (8, 176), (26, 146), (76, 124), (108, 100), (116, 60)])
    sole = smooth([(6, 178), (8, 206, 'c'), (238, 210, 'c'), (240, 192, 'c'), (24, 194, 'c')])
    gore = 'M150 18 L196 18 Q186 70 174 110 Q162 70 150 18 Z'
    detail = (f'<path d="{gore}" fill="#000" opacity=".5"/><clipPath id="g"><path d="{gore}"/></clipPath><g clip-path="url(#g)"><rect x="140" width="70" height="120" fill="url(#rib)"/></g>'
              + line('M118 20 L222 20', '#000', .4, 3) + f'<path d="M216 6 Q232 8 230 34 L220 34 Q222 16 214 14 Z" fill="{color}"/>'
              + line('M6 194 L238 194', '#fff', .3, 1.5) + ''.join(line(f'M{x} 198 L{x+4} 208', '#fff', .18, 2) for x in range(20, 236, 12)))
    shade = K('M222 30 Q232 120 236 190', .45, 12, 16) + K('M30 184 Q120 192 230 186', .3, 6, 12)
    light = (Wt('M128 30 Q130 80 120 104 Q80 130 40 150', .7, 3, 7) + Wt('M134 40 Q134 80 126 100', .9, 1, 2.5)
             + Wt('M210 60 Q218 120 220 170', .3, 6, 8) + Wt('M30 158 Q50 146 70 140', .8, 1, 3))
    return shoe_svg(W, H, 'rain_boots', color, o, sole, detail, shade, light, back=(46, -18), gloss=1.25)

# ---------------------------------------------------------------- bags / extras
def shoulder_bag(color, hardware='#C8B48A'):
    W, H = 300, 250
    body = smooth([(46, 110, 'c'), (254, 110, 'c'), (262, 196), (240, 226), (150, 232), (60, 226), (38, 196)])
    flap = smooth([(46, 110, 'c'), (254, 110, 'c'), (250, 150), (230, 172), (150, 178), (70, 172), (50, 150)])
    strap_d = 'M64 114 Q70 30 150 26 Q230 30 236 114'
    under = (f'<path d="{strap_d}" fill="none" stroke="#000" stroke-opacity=".25" stroke-width="16" transform="translate(1 3)" filter="url(#b3)"/>'
             f'<path d="{strap_d}" fill="none" stroke="{color}" stroke-width="14"/><path d="{strap_d}" fill="none" stroke="#000" stroke-opacity=".25" stroke-width="14"/>'
             + stitch('M58 112 Q64 24 150 20 Q236 24 242 112', '#fff', .2, 1, '3 3') + Wt('M74 70 Q100 34 150 30', .35, 1, 3))
    detail = (raised(flap, color, .4, .2) + f'<path d="{flap}" fill="none" stroke="#000" stroke-opacity=".08" stroke-width="10" filter="url(#b3)"/>'
              + stitch('M54 116 L246 116 L244 148 Q228 168 150 172 Q72 168 56 148 Z', '#000', .25, 1, '3 2')
              + Wt('M70 124 Q150 116 230 124', .35, 3, 6) + Wt('M90 128 Q150 122 200 128', .6, 1, 2)
              + f'<rect x="136" y="164" width="28" height="18" rx="4" fill="#000" opacity=".3" filter="url(#b1)"/><rect x="136" y="162" width="28" height="17" rx="4" fill="{hardware}"/><rect x="140" y="165" width="20" height="5" rx="2" fill="#fff" opacity=".5"/>')
    shade = K('M50 200 Q150 236 250 200', .4, 12, 20) + K('M60 182 Q150 190 240 182', .3, 6, 8)
    light = Wt('M70 196 Q150 214 230 196', .15, 6, 8)
    return piece(W, H, body, color, (.18, .3), shade, light, '', detail, under=under, grain=(1.4, .2), name='shoulder_bag')

def umbrella(color):
    W, H = 110, 440
    can = smooth([(55, 22, 'c'), (66, 60), (80, 160), (84, 260), (74, 312), (60, 330, 'c'), (50, 330, 'c'), (36, 312), (28, 250), (34, 150), (46, 60)])
    under = (line('M55 4 L55 26', '#888', 1, 4) + line('M55 320 L55 386', '#2a2522', 1, 5)
             + '<path d="M55 384 L55 404 Q55 428 76 428 Q94 428 94 410" fill="none" stroke="#3a2a20" stroke-width="10" stroke-linecap="round"/>'
             + Wt('M53 392 Q53 420 72 424', .4, 1, 2))
    detail = (f'<path d="M30 236 Q56 230 84 240 L83 258 Q56 248 30 254 Z" fill="{color}"/>' + K('M30 254 Q56 248 83 258', .5, 1, 2)
              + button(56, 246, 4, metal=True))
    shade = K('M60 40 Q76 160 70 320', .4, 6, 8) + K('M48 40 Q36 150 42 320', .25, 6, 6) + K('M80 150 Q86 240 76 310', .35, 6, 8)
    light = Wt('M54 40 Q58 160 56 320', .45, 3, 4) + Wt('M40 120 Q34 200 38 300', .3, 3, 4)
    return piece(W, H, can, color, (.1, .2), shade, light, '', detail, under=under, grain=(1.6, .2), name='umbrella')

ART = dict(short_coat=short_coat, boucle_jacket=boucle_jacket, cropped_cardigan=cropped_cardigan, turtleneck=turtleneck,
           oversized_knit=oversized_knit, wide_denim=wide_denim, pleated_mini=pleated_mini, long_skirt=long_skirt,
           loafers=loafers, mary_janes=mary_janes, long_boots=long_boots, rain_boots=rain_boots, shoulder_bag=shoulder_bag, umbrella=umbrella)

def render(kind, color, scale=2.5, outdir=None):
    """SVG -> transparent PNG via headless Chrome (cached)."""
    outdir = outdir or f'{S}/r'; os.makedirs(outdir, exist_ok=True)
    svg = ART[kind](color)
    key = hashlib.md5((svg + str(scale)).encode()).hexdigest()[:10]
    png = f'{outdir}/{kind}_{color[1:]}_{key}.png'
    if os.path.exists(png): return png
    import re
    W, H = map(int, re.search(r'width="(\d+)" height="(\d+)"', svg).groups())
    f = f'{outdir}/{kind}_{color[1:]}_{key}.svg'; open(f, 'w').write(svg)
    subprocess.run([CHROME, '--headless=new', '--disable-gpu', '--hide-scrollbars', '--default-background-color=00000000',
                    f'--force-device-scale-factor={scale}', f'--window-size={W},{H}', f'--screenshot={png}', 'file://' + f],
                   capture_output=True)
    return png

if __name__ == '__main__':
    import sys
    from concurrent.futures import ThreadPoolExecutor
    from PIL import Image
    jobs = [(k, c) for k, c in [('short_coat', '#2F3136'), ('boucle_jacket', '#F1EDE6'), ('cropped_cardigan', '#FFCBA4'), ('turtleneck', '#5E1224'),
            ('oversized_knit', '#0F1A3A'), ('wide_denim', '#2B3A55'), ('pleated_mini', '#151515'), ('long_skirt', '#151515'), ('loafers', '#141414'),
            ('mary_janes', '#141414'), ('long_boots', '#141414'), ('rain_boots', '#141414'), ('shoulder_bag', '#141414'), ('umbrella', '#141414')]]
    with ThreadPoolExecutor(6) as ex: pngs = list(ex.map(lambda j: render(*j), jobs))
    sheet = Image.new('RGBA', (2400, 1500), (244, 241, 236, 255))
    for i, p in enumerate(pngs):
        im = Image.open(p); im.thumbnail((440, 440))
        sheet.alpha_composite(im, (20 + (i % 5) * 476, 20 + (i // 5) * 490))
    sheet.convert('RGB').save(f'{S}/sheet.png')
