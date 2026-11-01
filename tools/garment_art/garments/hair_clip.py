from kit.art_kit import *

def _tooth(x, top, w, L, lean):
    """Short rounded tooth, clockwise, tip leaning by `lean`."""
    r = w / 2
    return (f'M{x-r:.1f} {top} L{x+r:.1f} {top} L{x+r*.5+lean:.1f} {top+L-r:.1f} '
            f'A{r*.5:.1f} {r*.5:.1f} 0 0 1 {x-r*.5+lean:.1f} {top+L-r:.1f} Z')

def hair_clip(color):
    """Acetate claw clip (집게핀), classic shop cut-out: chunky bean-shaped glossy front jaw,
    short interlocking teeth with the back jaw, spring hinge bump on top."""
    W, H = 260, 210
    jaw = smooth([(40, 76), (62, 54), (130, 48), (198, 54), (220, 76), (222, 108), (208, 134), (130, 142), (52, 134), (38, 108)])
    back = smooth([(54, 60), (130, 40), (206, 60), (214, 90), (130, 84), (46, 90)])       # back jaw rim peeking above
    hinge = 'M108 52 Q108 34 130 32 Q152 34 152 52 Z'
    fx = [62, 89, 116, 144, 171, 198]
    bx = [75, 102, 130, 158, 185]
    front_t = ''.join(_tooth(x, 128, 19, 40 - abs(x - 130) * .06, (x - 130) * .05) for x in fx)
    back_t = ''.join(_tooth(x, 124, 14, 36 - abs(x - 130) * .05, -(x - 130) * .03) for x in bx)
    base = ' '.join([back, hinge, back_t, jaw, front_t])
    # back jaw sits behind: darken it everywhere the front jaw doesn't cover, plus the front jaw's cast shadow
    shade = (f'<mask id="hcm"><rect width="{W}" height="{H}" fill="#fff"/><path d="{jaw} {front_t}" fill="#000"/></mask>'
             f'<g mask="url(#hcm)"><path d="{back} {back_t}" fill="#000" opacity=".4"/>'
             f'<path d="{jaw} {front_t}" fill="#000" opacity=".35" transform="translate(3 3)" filter="url(#b3)"/></g>'
             + K('M52 128 Q130 146 208 128', .5, 3, 8)                    # jaw underside where teeth start
             + K('M214 74 Q228 104 204 134', .45, 6, 10) + K('M110 50 Q130 58 150 50', .4, 3, 4)
             + ''.join(K(f'M{x+5:.1f} 136 L{x+4+(x-130)*.05:.1f} {158-abs(x-130)*.06:.1f}', .35, 1, 3.5) for x in fx))
    light = (Wt('M62 66 Q130 54 198 66', .8, 1, 3)                  # crisp gloss along the top curve
             + Wt('M60 74 Q130 62 200 74', .3, 6, 14)
             + Wt('M48 86 Q44 104 56 122', .5, 1, 2.4) + Wt('M64 96 Q130 92 196 96', .12, 6, 22)
             + Wt('M118 38 Q130 34 142 38', .7, 1, 2)
             + ''.join(Wt(f'M{x-4:.1f} 134 L{x-4+(x-130)*.05:.1f} {154-abs(x-130)*.06:.1f}', .65, 1, 2) for x in fx))
    detail = (f'<rect x="114" y="44" width="32" height="7" rx="3.5" fill="#000" opacity=".3" filter="url(#b1)"/>'
              f'<rect x="114" y="42" width="32" height="6" rx="3" fill="#BFC3C7"/>'
              + ''.join(f'<rect x="{116+i*6}" y="42" width="2" height="6" fill="#7d8287"/>' for i in range(5))
              + '<rect x="115" y="42.5" width="30" height="1.6" rx=".8" fill="#fff" opacity=".7"/>')
    return piece(W, H, base, color, (.22, .3), shade, light, '', detail, grain=(2, .05), name='hair_clip')
