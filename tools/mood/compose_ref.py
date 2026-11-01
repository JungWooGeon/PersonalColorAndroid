"""A "햇살 침대" mood scene: asset builder + Python reference composer (the Kotlin runtime must match compose_A).

  python3 compose_ref.py          # rebuild A_* assets, grain.webp, A_layout.json and reference/A_*.png
Runtime order (compose_A): base -> contact shadows -> garments (graded, dark rim) -> shade -> warm light (screen)
-> grain (overlay) -> vignette -> text.  All rects are base px (760x1088); `mini` rects are local to `mini.crop`.
Dark garments (color luminance < 0.12) get a highlight rim: the garment alpha minus itself shifted (+RIM_D, +RIM_D),
filled with RIM_COLOR at RIM_ALPHA (a top-left edge catching the window light), so black shoes never become a blob.
"""
import json, math, os, sys
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = Path(__file__).resolve().parents[2] / 'feature/widget/src/main/assets/mood'  # the assets
REF = Path(__file__).resolve().parent / 'reference'
REPO = HERE.parents[5]
GARMENTS = REPO / 'core/ui/src/main/assets/garments'
MYEONG = REPO / 'core/designsystem/src/main/res/font/nanum_myeongjo.ttf'
SERIF_IT = ('/System/Library/Fonts/Supplemental/PTSerif.ttc', 1)  # stand-in for Noto Serif Italic
W, H, Q = 760, 1088, 4
WEATHERS = ('sunny', 'cloudy', 'rain', 'night')
PHASES = (0.0, 0.5, 1.0)
RIM_D, RIM_COLOR, RIM_ALPHA = 4, (176, 168, 158), 0.75

# ---------------------------------------------------------------- layout (the JSON contract) ----
def slot(cx, cy, h, aspect, rot, z):
    w = h * aspect
    return {'rect': [round(cx * W - w / 2), round(cy * H - h / 2), round(w), round(h)], 'rot': -rot, 'z': z, 'hanger': None, 'hook': None}

def mslot(cx, cy, h, aspect, rot, z, cw=480, ch=696):
    w = h * aspect
    return {'rect': [round(cx * cw - w / 2), round(cy * ch - h / 2), round(w), round(h)], 'rot': -rot, 'z': z, 'hanger': None, 'hook': None}

ACC = lambda: slot(.86, .56, 520, .30, -18, 0)
BAG = lambda cx=.84, cy=.89, r=10: slot(cx, cy, 210, 1.1, r, 6)
SHOES = lambda cx=.40, cy=.93, r=-5: slot(cx, cy, 78, 3.6, r, 5)
LAYOUT = {
    'size': [W, H], 'lightScale': Q,
    'shadeMax': {'sunny': 0.38, 'cloudy': 0.22, 'rain': 0.26, 'night': 0.30},
    'lightMax': {'sunny': 0.24, 'cloudy': 0.10, 'rain': 0.22, 'night': 0.28},
    'grainAlpha': 0.22, 'vignette': 0.30,
    'garmentGrade': {'sunny': {'multiply': '#FFEFDC', 'saturation': 0.90},
                     'cloudy': {'multiply': '#F2EEE8', 'saturation': 0.84},
                     'rain': {'multiply': '#EDEBEA', 'saturation': 0.84},
                     'night': {'multiply': '#D2BEA8', 'saturation': 0.86}},
    'contactShadow': {'dx': 6, 'dy': 10, 'blur': 18, 'alpha': 0.35},
    'layouts': {
        'outer_top_bottom': {'acc': ACC(), 'outer': slot(.30, .63, 545, .62, 6, 1), 'bottom': slot(.64, .69, 500, .52, -4, 2),
                             'top': slot(.51, .52, 315, 1.06, -11, 3), 'shoes': SHOES(.42, .935), 'bag': BAG()},
        'top_bottom': {'acc': ACC(), 'bottom': slot(.60, .66, 545, .52, -5, 1), 'top': slot(.42, .50, 330, 1.05, 9, 2),
                       'shoes': SHOES(.40, .93, 6), 'bag': BAG(.84, .87, -8)},
        'dress': {'acc': ACC(), 'dress': slot(.50, .63, 620, .62, -4, 1), 'shoes': SHOES(.36, .93, 6), 'bag': BAG(.82, .87, -8)},
        'outer_dress': {'acc': ACC(), 'outer': slot(.32, .62, 560, .62, 6, 1), 'dress': slot(.62, .66, 560, .62, -4, 2),
                        'shoes': SHOES(.40, .935), 'bag': BAG(.84, .89)},
    },
    'text': {'line': {'rect': [63, 112, 600, 160], 'size': 56, 'lineSpacing': 1.25,
                      'color': {'sunny': '#4A3A2E', 'cloudy': '#45403B', 'rain': '#33323A', 'night': '#EDE6DA'}},
             'caption': {'rect': [63, 60, 600, 46], 'size': 36,
                         'color': {'sunny': '#6E5848', 'cloudy': '#625A53', 'rain': '#4E4D57', 'night': '#CFC6B8'}}},
    'mini': {'crop': [150, 250, 480, 696],
             'layouts': {'top_bottom': {'bottom': mslot(.62, .56, 370, .52, -4, 1), 'top': mslot(.42, .38, 245, 1.06, -12, 2),
                                        'shoes': mslot(.66, .90, 52, 3.6, -6, 3)},
                         'dress': {'dress': mslot(.52, .52, 450, .62, -4, 1), 'shoes': mslot(.66, .90, 52, 3.6, -6, 2)}},
             'text': {'caption': {'rect': [36, 28, 400, 56], 'size': 40,
                                  'color': {'sunny': '#6E5848', 'cloudy': '#625A53', 'rain': '#4E4D57', 'night': '#CFC6B8'}}}},
}

def _hero(k=.87, crop=(0, 0, 760, 950)):
    """4:5 hero: the 2x2 layouts scaled by k about the top centre, so every piece fits inside the crop."""
    ox = (crop[2] - W * k) / 2
    sc = lambda s: dict(s, rect=[round(s['rect'][0] * k + ox), round(s['rect'][1] * k), round(s['rect'][2] * k), round(s['rect'][3] * k)])
    lays = {n: {r: sc(s) for r, s in lay.items()} for n, lay in LAYOUT['layouts'].items()}
    for lay in lays.values():
        for s in lay.values():
            x, y, w, h = s['rect']; assert x >= 0 and y >= 0 and x + w <= crop[2] and y + h <= crop[3], s
    return {'crop': list(crop), 'layouts': lays, 'text': json.loads(json.dumps(LAYOUT['text']))}
LAYOUT['hero'] = _hero()

# ---------------------------------------------------------------- procedural helpers (from vA/mood.py) ----
def blur(a, s):
    if s <= 0: return a
    p = int(min(3 * s, min(a.shape) - 1)); b = np.pad(a.astype(np.float32), p, mode='reflect')
    fy, fx = np.fft.fftfreq(b.shape[0])[:, None], np.fft.rfftfreq(b.shape[1])[None, :]
    g = np.exp(-2 * (np.pi * s) ** 2 * (fx ** 2 + fy ** 2))
    return np.fft.irfft2(np.fft.rfft2(b) * g, b.shape)[p:p + a.shape[0], p:p + a.shape[1]].astype(np.float32)

def noise(h, w, s, rng):
    n = blur(rng.standard_normal((h, w)).astype(np.float32), s); return n / (n.std() + 1e-6)

def linen(seed=5, tex=1.0, fold_k=1.0):
    rng = np.random.default_rng(seed); S = H / 680
    def stretched(sig, ang, k=3.0):
        n = rng.standard_normal((int(H / 4) + 8, int(W / 4) + 8)).astype(np.float32)
        im = Image.fromarray(((n - n.min()) / (n.max() - n.min()) * 255).astype(np.uint8))
        im = im.resize((im.width, int(im.height * k)), Image.BICUBIC).rotate(ang, Image.BICUBIC, expand=False)
        a = blur(np.asarray(im.resize((W, H), Image.BICUBIC)).astype(np.float32), sig * S); return (a - a.mean()) / (a.std() + 1e-6)
    h = stretched(40, 35, 4) + .4 * stretched(22, -25, 3) + .6 * noise(H, W, 130 * S, rng)
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    for _ in range(6):
        px, py, th = rng.random() * W, rng.random() * H, rng.random() * math.pi
        along = (xx - px) * math.cos(th) + (yy - py) * math.sin(th)
        perp = -(xx - px) * math.sin(th) + (yy - py) * math.cos(th) + 18 * S * np.sin(along / (90 * S) + rng.random() * 6)
        L = (rng.random() * .4 + .35) * H
        h += (rng.random() * .6 + .7) * np.exp(-(perp / (22 * S)) ** 2) * np.exp(-(along / L) ** 2)
    h = blur(h, 4 * S); gy, gx = np.gradient(h)
    x = (26 / S) * (-gx * .7 - gy * .7)
    shade = 1 + .13 * fold_k * np.tanh(x / .13) - .05 * fold_k * np.clip(-h, 0, 2)
    rows = blur(rng.standard_normal((H, 1)).astype(np.float32), 1.2) * .016 * tex
    cols = blur(rng.standard_normal((1, W)).astype(np.float32), 1.2) * .012 * tex
    fine = .012 * tex * blur(rng.standard_normal((H, W)).astype(np.float32), .5)
    return (shade + rows + cols + fine)[..., None]

def light_field(t, rain, soft, seed=7):
    """Window + leaf light at 1/4 res (h, w), 0..~1.3. t drifts the light gently (phase)."""
    rng = np.random.default_rng(seed); w, h = W // Q, H // Q
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32); u, v = xx * Q / H, yy * Q / H
    ang = math.radians(-32 + 2.5 * t)
    a = u * math.cos(ang) - v * math.sin(ang) + t * .12
    b = u * math.sin(ang) + v * math.cos(ang) + t * .03
    lit = ((a > -.05) & (a < .78) & (b > -.45) & (b < 1.25)).astype(np.float32)
    lit *= (np.abs(a - .36) > .02) * (np.abs(b - .40) > .015)
    leaf = Image.new('L', (w * 2, h * 2), 0); dr = ImageDraw.Draw(leaf)
    bx, by = w * 2 * (1.05 + t * .04), h * 2 * -.05
    for i in range(260):
        s_ = rng.random()
        px, py = bx - s_ * w * 1.3 + rng.normal(0, w * .16), by + s_ * h * 1.0 + rng.normal(0, h * .14)
        sway = t * 14 * s_  # leaves farther from the branch sway a bit more
        px, py = px + sway, py + sway * .5
        L_ = (rng.random() * .5 + .6) * h * .042; th = rng.random() * math.pi + t * .9
        pts = [(px + math.cos(th + k * math.pi / 8) * L_ * (1 if k % 8 == 0 else .5) * abs(math.cos(k * math.pi / 16)) ** .3,
                py + math.sin(th + k * math.pi / 8) * L_ * .45 * abs(math.cos(k * math.pi / 16)) ** .3) for k in range(16)]
        dr.ellipse((px - L_, py - L_ * .38, px + L_, py + L_ * .38), fill=255) if i % 2 else dr.polygon(pts, fill=255)
    lf = np.asarray(leaf.rotate(rng.random() * 20 + t * 5, Image.BICUBIC).resize((w, h), Image.LANCZOS)
                    .filter(ImageFilter.GaussianBlur(1.6 * soft if not rain else 3 * soft))) / 255
    lit = blur(lit, h * .010 * soft) * (1 - .55 * lf)
    if rain:
        drops = np.zeros((h, w), np.float32); core = np.zeros_like(drops)
        for _ in range(int(w * h / 260)):
            x, y = rng.random() * w, (rng.random() + t * .6 * (1 + rng.random())) % 1 * h
            r = rng.random() ** 2.5 * 3.6 + 1.0; y0i, x0i = int(y), int(x)
            y0, y1, x0, x1 = max(y0i - 8, 0), min(y0i + 9, h), max(x0i - 8, 0), min(x0i + 9, w)
            Y, X = np.mgrid[y0:y1, x0:x1]; dd = ((X - x) ** 2 + ((Y - y) * .85) ** 2) ** .5
            drops[y0:y1, x0:x1] = np.maximum(drops[y0:y1, x0:x1], np.clip(1.2 - dd / r, 0, 1))
            core[y0:y1, x0:x1] = np.maximum(core[y0:y1, x0:x1], np.exp(-((X - x + r * .25) ** 2 + (Y - y + r * .3) ** 2) / (r * .3) ** 2))
        lit = lit * (1 - .55 * blur(drops, .8)) + .35 * blur(core, .6) * lit
    return np.clip(lit, 0, 1.3)

def radial(cx, cy, rx, ry):
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    return np.exp(-(((xx / W - cx) / rx) ** 2 + ((yy / H - cy) / ry) ** 2))[..., None]

def screen(img, col, amt): return 1 - (1 - img) * (1 - amt * np.array(col, np.float32))

# weather look: sheet color, fold strength, texture, desat, tint, leak, lamp
WX = {
    'sunny': dict(sheet=(246, 234, 218), sun=(1.08, 1.0, .88), fold=1.0, tex=1.0, sat=.86, mul=(1.02, .99, .95), add=(.018, .006, .004),
                  leak=((1, .55, .25), .22), lamp=None, soft=1.0, lightcol=(1, .80, .48), bounce=(.55, .30, .30)),
    'cloudy': dict(sheet=(232, 228, 221), sun=(1.0, .99, .97), fold=1.15, tex=1.3, sat=.82, mul=(1.0, 1.0, .98), add=(.01, .01, .01),
                   leak=((1, .8, .6), .08), lamp=None, soft=3.2, lightcol=(1, .95, .88), bounce=(.20, .14, .12)),
    'rain': dict(sheet=(234, 227, 218), sun=(1.0, 1.0, 1.0), fold=1.35, tex=1.9, sat=.82, mul=(.985, .99, 1.0), add=(.012, .010, .012),
                 leak=((.55, .7, .95), .08), lamp=((1, .62, .30), .62, (.98, .16, .70, .60)), soft=1.0, lightcol=(.85, .92, 1.0), bounce=(.35, .22, .16)),
    'night': dict(sheet=(150, 128, 110), sun=(.56, .50, .48), fold=1.4, tex=1.6, sat=.86, mul=(1.0, .97, .95), add=(.016, .012, .022),
                  leak=((.45, .58, .95), .10), lamp=((1, .68, .34), .72, (.80, .62, .55, .45)), soft=1.4, lightcol=(1, .78, .48), bounce=(.26, .14, .08)),
}

def build_base(wx):
    p = WX[wx]
    img = np.array(p['sheet'], np.float32) / 255 * linen(5, p['tex'], p['fold']) * np.array(p['sun'], np.float32)
    lum = img @ np.array([.299, .587, .114], np.float32)
    img = lum[..., None] + (img - lum[..., None]) * p['sat']
    img = .045 + img * .93
    img = img * np.array(p['mul']) + np.array(p['add'])
    if p['leak']: img = screen(img, p['leak'][0], p['leak'][1] * radial(-.1, -.05, .42, .42))
    if p['lamp']:  # warm lamp pool, top right: keeps the rainy/night sheet from going grey
        col, amt, (cx, cy, rx, ry) = p['lamp']
        img = screen(img, col, amt * radial(cx, cy, rx, ry))
        img = img * (1 + .06 * radial(cx, cy, rx * 1.6, ry * 1.6) * (np.array(col) - .6))
    return Image.fromarray((np.clip(img, 0, 1) * 255 + .5).astype(np.uint8))

def build_light(wx, t):
    p = WX[wx]; lf = light_field(t, wx == 'rain', p['soft'])
    if wx == 'cloudy': lf = .55 + .45 * lf          # overcast: only a ghost of the window
    shade = np.clip(1 - lf, 0, 1)
    glow = .3 * np.clip(lf, 0, 1) ** 2 + .7 * blur(np.clip(lf - .6, 0, 1), 5)
    # shaded areas get a faint rosy bounce so a black shade never reads olive on the warm sheet
    rgb = glow[..., None] * np.array(p['lightcol']) + shade[..., None] * np.array(p['bounce'])
    if p['lamp']:  # the lamp pool breathes across the phases
        col, _, (cx, cy, rx, ry) = p['lamp']; yy, xx = np.mgrid[0:H // Q, 0:W // Q].astype(np.float32)
        pool = np.exp(-(((xx * Q / W - cx) / rx) ** 2 + ((yy * Q / H - cy) / ry) ** 2))
        rgb = rgb + (.25 + .75 * t) * pool[..., None] * np.array(col)
    rgb = np.clip(rgb, 0, 1)
    return Image.fromarray((np.dstack([rgb, shade[..., None]]) * 255 + .5).astype(np.uint8), 'RGBA')

def build_grain(seed=1):
    rng = np.random.default_rng(seed)
    n = rng.standard_normal((256, 256)).astype(np.float32)
    f = np.fft.fft2(n); fy, fx = np.fft.fftfreq(256)[:, None], np.fft.fftfreq(256)[None, :]
    n = np.real(np.fft.ifft2(f * np.exp(-2 * (np.pi * .6) ** 2 * (fx ** 2 + fy ** 2))))  # periodic blur -> tileable
    n = n / n.std() * .14
    return Image.fromarray((np.clip(.5 + n, 0, 1) * 255 + .5).astype(np.uint8))

# ---------------------------------------------------------------- reference composer ----
def garment(key, color):
    b = 'd' if lum_of(color) < .08 else 'n'
    p = np.asarray(Image.open(GARMENTS / f'{key}_{b}.webp').convert('RGBA')).astype(np.float32) / 255
    c = hexrgb(color)
    pre = c * p[..., 0:1] + p[..., 1:2]
    dp = GARMENTS / f'{key}_{b}_d.webp'
    if dp.exists():
        d = np.asarray(Image.open(dp).convert('RGBA')).astype(np.float32) / 255; pre = pre + d[..., :3] * d[..., 3:4]
    a = p[..., 3:4]; rgb = np.where(a > 0, np.clip(pre / np.maximum(a, 1e-6), 0, 1), 0)
    im = Image.fromarray((np.dstack([rgb, a]) * 255 + .5).astype(np.uint8), 'RGBA')
    return im.crop(im.getbbox())

def hexrgb(c): return np.array([int(c[i:i + 2], 16) for i in (1, 3, 5)], np.float32) / 255
def lum_of(c): return float(hexrgb(c) @ np.array([.2126, .7152, .0722]))

def grade_garment(im, g):
    a = np.asarray(im).astype(np.float32) / 255; rgb = a[..., :3]
    lum = rgb @ np.array([.213, .715, .072], np.float32)          # android ColorMatrix.setSaturation weights
    rgb = lum[..., None] + (rgb - lum[..., None]) * g['saturation']
    rgb = rgb * hexrgb(g['multiply'])
    return Image.fromarray((np.dstack([np.clip(rgb, 0, 1), a[..., 3:]]) * 255 + .5).astype(np.uint8), 'RGBA')

def place(im, s, shoes):
    x, y, w, h = s['rect']; k = min(w / im.width, h / im.height)
    im = im.resize((max(1, round(im.width * k)), max(1, round(im.height * k))), Image.LANCZOS)
    ox, oy = x + (w - im.width) / 2, (y + h - im.height) if shoes else y + (h - im.height) / 2
    big = Image.new('RGBA', (w * 3, h * 3)); big.paste(im, (round(ox - x + w), round(oy - y + h)))
    big = big.rotate(-s['rot'], Image.BICUBIC)  # positive rot = clockwise on screen (Canvas.rotate)
    return big, (x - w, y - h)

def text_draw(img, spec, txt, font, wx, spacing=1.25):
    dr = ImageDraw.Draw(img); x, y, w, h = spec['rect']
    col = tuple(int(spec['color'][wx][i:i + 2], 16) for i in (1, 3, 5))
    for i, ln in enumerate(txt.split('\n')):
        dr.text((x, y + i * spec['size'] * spacing), ln, font=font, fill=col, anchor='la')

def compose_A(look, wx, phase=0, line='', caption='', mini=False, L=None, view=None):
    """view: None (2x2), 'mini' (1x1) or 'hero' (4:5 in-app hero / share print); crop views use crop-local rects."""
    view = view or ('mini' if mini else None)
    """look: {role: (garment_key, '#RRGGBB')} with roles outer/top/bottom/dress/shoes/bag/acc."""
    L = L or json.loads((HERE / 'A_layout.json').read_text())
    roles = set(look)
    if view == 'mini':
        lay = L['mini']['layouts']['dress' if 'dress' in roles else 'top_bottom']
    else:
        lay = (L[view] if view else L)['layouts'][('outer_' if 'outer' in roles else '') + ('dress' if 'dress' in roles else 'top_bottom')]
        if 'outer' in roles and 'dress' not in roles and 'top' not in roles:
            lay = (L[view] if view else L)['layouts']['top_bottom']; look = {('top' if r == 'outer' else r): v for r, v in look.items()}
    canvas = Image.open(HERE / f'A_{wx}_base.webp').convert('RGBA')
    # light is NOT premultiplied: upscale each channel on its own (Android: decode with inPremultiplied = false)
    light = Image.merge('RGBA', [c.resize(canvas.size, Image.BILINEAR) for c in Image.open(HERE / f'A_{wx}_light{phase}.webp').convert('RGBA').split()])
    if view:
        cx, cy, cw, ch = L[view]['crop']; canvas = canvas.crop((cx, cy, cx + cw, cy + ch)); light = light.crop((cx, cy, cx + cw, cy + ch))
    cs, gg = L['contactShadow'], L['garmentGrade'][wx]
    for role, s in sorted(lay.items(), key=lambda kv: kv[1]['z']):
        if role not in look: continue
        key, color = look[role]
        im = grade_garment(garment(key, color), gg)
        if lum_of(color) < .12:  # highlight rim for dark garments
            a = im.split()[3]; sh = Image.new('L', a.size); sh.paste(a, (RIM_D, RIM_D))
            rim = Image.fromarray(np.clip(np.asarray(a).astype(int) - np.asarray(sh), 0, 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(1))
            hl = Image.new('RGBA', im.size, RIM_COLOR + (0,)); hl.putalpha(rim.point(lambda v: int(v * RIM_ALPHA)))
            im.alpha_composite(hl); im.putalpha(a)
        big, (bx, by) = place(im, s, role == 'shoes')
        m = big.split()[3].filter(ImageFilter.GaussianBlur(cs['blur'] / 2)).point(lambda v: int(v * cs['alpha']))
        shadow = Image.new('RGBA', big.size, (0, 0, 0, 0)); shadow.putalpha(m)
        canvas.alpha_composite(shadow, (bx + cs['dx'], by + cs['dy']))
        canvas.alpha_composite(big, (bx, by))
    img = np.asarray(canvas.convert('RGB')).astype(np.float32) / 255
    lt = np.asarray(light).astype(np.float32) / 255
    img = img * (1 - L['shadeMax'][wx] * lt[..., 3:4])                                  # shade
    img = 1 - (1 - img) * (1 - L['lightMax'][wx] * lt[..., :3])                         # warm light, screen
    hh, ww = img.shape[:2]
    gr = np.asarray(Image.open(HERE / 'grain.webp').convert('L')).astype(np.float32) / 255
    g = np.tile(gr, (hh // 256 + 1, ww // 256 + 1))[:hh, :ww, None]
    ov = np.where(img < .5, 2 * img * g, 1 - 2 * (1 - img) * (1 - g))                  # overlay
    img = img + (ov - img) * L['grainAlpha']
    yy, xx = np.mgrid[0:hh, 0:ww]
    d = np.hypot(xx - ww / 2, yy - hh / 2) / np.hypot(ww / 2, hh / 2)                    # radial gradient, stops .45 -> 1
    img = img * (1 - L['vignette'] * np.clip((d - .45) / .55, 0, 1))[..., None]
    out = Image.fromarray((np.clip(img, 0, 1) * 255 + .5).astype(np.uint8))
    T = L[view]['text'] if view else L['text']
    if caption: text_draw(out, T['caption'], caption, ImageFont.truetype(SERIF_IT[0], T['caption']['size'], index=SERIF_IT[1]), wx)
    if line and view != 'mini': text_draw(out, T['line'], line, ImageFont.truetype(str(MYEONG), T['line']['size']), wx, T['line']['lineSpacing'])
    return out

TODAY = {'outer': ('coat', '#1C2541'), 'top': ('knit', '#C41E3A'), 'bottom': ('long_skirt', '#9B9FA7'),
         'shoes': ('loafers', '#1E1E20'), 'bag': ('hobo', '#F4F3EF'), 'acc': ('umbrella', '#F4F3EF')}
SPRING = {'top': ('cardigan', '#F7C6B1'), 'bottom': ('jeans', '#A9C1DB'), 'shoes': ('mary_jane', '#F2EEE5'), 'bag': ('shoulder_bag', '#C8A27A')}
COPY = {'sunny': ('햇살 아래,\n와인 한 조각', 'Nov 1 · 12° · sunny day look'), 'cloudy': ('흐린 날엔\n와인으로', 'Nov 1 · 9° · cloudy day look'),
        'rain': ('비 오는 날,\n와인 한 방울', 'Nov 1 · 9° · rainy day look'), 'night': ('내일은\n와인', 'Nov 2 · 7° · tomorrow')}

if __name__ == '__main__':
    q = dict(quality=82, method=6)
    (HERE / 'A_layout.json').write_text(json.dumps(LAYOUT, indent=1, ensure_ascii=False))
    build_grain().save(HERE / 'grain.webp', quality=90, method=6)
    for wx in WEATHERS:
        build_base(wx).save(HERE / f'A_{wx}_base.webp', **q)
        for i, t in enumerate(PHASES): build_light(wx, t).save(HERE / f'A_{wx}_light{i}.webp', alpha_quality=90, exact=True, **q)
    (REF).mkdir(exist_ok=True)
    for wx in WEATHERS:
        line, cap = COPY[wx]
        compose_A(TODAY, wx, 0, line, cap).save(REF / f'A_{wx}.png')
    for wx in ('rain', 'sunny'):
        compose_A(TODAY, wx, 0, *COPY[wx], view='hero').save(REF / f'A_hero_{wx}.png')
    compose_A(TODAY, 'rain', 0, '', '9° · rain', mini=True).save(REF / 'A_rain_mini.png')
    compose_A(SPRING, 'sunny', 0, '햇살 아래,\n복숭아 한 조각', 'Apr 6 · 17° · sunny day look').save(REF / 'A_sunny_spring.png')
    print('ok')
