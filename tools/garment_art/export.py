"""Export garments/<key>.py into recolorable app assets.

Two renders per luminance bucket (base white, base black) make every pixel an affine function of the base color:
  premul_out = color * M + G + D
M (grey) and G (grey) and alpha are packed into one RGBA PNG (R=M, G=G, A=alpha); D (colored hardware) goes into a
second RGBA PNG only when the piece has colored details. Buckets: 'd' (very dark colors, L<.08) and 'n' (the rest).
Usage: python3 export.py [out_dir] [keys...]
"""
import importlib, os, re, subprocess, sys, tempfile, hashlib
from concurrent.futures import ThreadPoolExecutor
import numpy as np
from PIL import Image

S = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, S)
import kit.art_kit as kit

CHROME = kit.CHROME
MAX_SIDE = 420
BUCKET_LUM = {'d': 0.03, 'n': 0.35}


def svg_for(key, color, bucket):
    mod = importlib.import_module(f'garments.{key}')
    fake = lambda _c: BUCKET_LUM[bucket]
    kit.lum = fake
    if hasattr(mod, 'lum'): mod.lum = fake
    return getattr(mod, key)(color)


def chrome(svg, scale, tmp):
    W, H = map(float, re.search(r'width="([\d.]+)" height="([\d.]+)"', svg).groups())
    h = hashlib.md5(svg.encode()).hexdigest()[:12]
    f, png = f'{tmp}/{h}.svg', f'{tmp}/{h}.png'
    open(f, 'w').write(svg)
    subprocess.run([CHROME, '--headless=new', '--disable-gpu', '--hide-scrollbars', '--default-background-color=00000000', f'--user-data-dir={tmp}/p_{h}',
                    f'--force-device-scale-factor={scale}', f'--window-size={int(W)},{int(H)}', f'--screenshot={png}', 'file://' + f],
                   capture_output=True, timeout=120)
    return np.asarray(Image.open(png).convert('RGBA')).astype(np.float32) / 255


def export(key, out, tmp):
    res = []
    for b in BUCKET_LUM:
        sw, sb = svg_for(key, '#FFFFFF', b), svg_for(key, '#000000', b)
        W, H = map(float, re.search(r'width="([\d.]+)" height="([\d.]+)"', sw).groups())
        scale = MAX_SIDE / max(W, H)
        iw, ib = chrome(sw, scale, tmp), chrome(sb, scale, tmp)
        a = np.maximum(iw[..., 3], ib[..., 3])
        pw, pb = iw[..., :3] * iw[..., 3:4], ib[..., :3] * ib[..., 3:4]
        m = np.clip((pw - pb).mean(axis=2), 0, 1)
        g = pb.min(axis=2)
        d = np.clip(pb - g[..., None], 0, 1)
        # trim transparent border (keep 2px)
        ys, xs = np.where(a > 0.004)
        y0, y1, x0, x1 = max(ys.min() - 2, 0), ys.max() + 3, max(xs.min() - 2, 0), xs.max() + 3
        sl = (slice(y0, y1), slice(x0, x1))
        packed = np.dstack([m[sl], g[sl], np.zeros_like(m[sl]), a[sl]])
        Image.fromarray((packed * 255 + .5).astype(np.uint8), 'RGBA').save(f'{out}/{key}_{b}.webp', 'WEBP', quality=85, alpha_quality=100, method=6)
        if d[sl].max() > 0.02:
            da = np.clip(d[sl].max(axis=2), 0, 1)
            straight = np.where(da[..., None] > 0, d[sl] / np.maximum(da[..., None], 1e-6), 0)
            Image.fromarray((np.dstack([straight, da]) * 255 + .5).astype(np.uint8), 'RGBA').save(f'{out}/{key}_{b}_d.webp', 'WEBP', lossless=True, method=6)
        elif os.path.exists(f'{out}/{key}_{b}_d.webp'):
            os.remove(f'{out}/{key}_{b}_d.webp')
        res.append((b, x1 - x0, y1 - y0))
    return key, res


def compose(key, color, b, src):
    """Python twin of the Kotlin runtime, for previews: straight-alpha RGBA image."""
    p = np.asarray(Image.open(f'{src}/{key}_{b}.webp').convert('RGBA')).astype(np.float32) / 255
    c = np.array([int(color[i:i + 2], 16) for i in (1, 3, 5)], np.float32) / 255
    pre = c * p[..., 0:1] + p[..., 1:2]
    dp = f'{src}/{key}_{b}_d.webp'
    if os.path.exists(dp):
        d = np.asarray(Image.open(dp).convert('RGBA')).astype(np.float32) / 255
        pre = pre + d[..., :3] * d[..., 3:4]
    a = p[..., 3:4]
    rgb = np.where(a > 0, np.clip(pre / np.maximum(a, 1e-6), 0, 1), 0)
    return Image.fromarray((np.dstack([rgb, a]) * 255 + .5).astype(np.uint8), 'RGBA')


if __name__ == '__main__':
    out = sys.argv[1] if len(sys.argv) > 1 else f'{S}/assets'
    os.makedirs(out, exist_ok=True)
    keys = sys.argv[2:] or sorted(f[:-3] for f in os.listdir(f'{S}/garments') if f.endswith('.py') and not f.startswith('_'))
    tmp = tempfile.mkdtemp()
    # import sequentially (monkeypatching lum is global), render in parallel per key
    if len(keys) == 1:
        print(*export(keys[0], out, tmp))
    else:
        with ThreadPoolExecutor(6) as ex:
            list(ex.map(lambda k: subprocess.run([sys.executable, __file__, out, k]), keys))
