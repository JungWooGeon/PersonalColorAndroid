"""Play Store graphics for 나의 컬러는? v2.0.

Builds, from the real emulator captures in store/raw/:
  store/screenshots/screenshot_01..08.png  1080x1920, RGB
  store/feature_graphic.png                1024x500, RGB
  store/preview_sheet.png                  contact sheet for review

Look: the app's mood-lookbook feel. Warm linen paper with film grain and soft window light,
a quiet Nanum Myeongjo eyebrow, a Pretendard headline, the real screen in a slim device frame.
Captures: Pixel 8 emulator (1080x2400), app locale ko-KR, SystemUI demo mode (9:41, full battery).
Tinty art comes from the brand folder (TINTY below); change the path if it moves.

    python3 store/make_store.py
"""
import os
from PIL import Image, ImageDraw, ImageFilter, ImageFont

STORE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(STORE)
RAW = f"{STORE}/raw"
FONT_DIR = f"{REPO}/core/designsystem/src/main/res/font"
TINTY = "/Users/jwg/Downloads/나의컬러_캐릭터_시안/hires"

INK = (36, 31, 39)          # tinty-ink, a touch warmer than black
INK2 = (102, 96, 91)
BROWN = (138, 108, 86)      # eyebrow, like the mood hero's caption
ACCENT = (91, 75, 138)
LINEN = (246, 240, 232)

W, H = 1080, 1920


def pretendard(size, weight=700):
    f = ImageFont.truetype(f"{FONT_DIR}/pretendard.ttf", size)
    f.set_variation_by_axes([weight])
    return f


def myeongjo(size):
    return ImageFont.truetype(f"{FONT_DIR}/nanum_myeongjo.ttf", size)


def hex_rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def blob(canvas, xy, r, color, alpha, blur):
    """Soft colored light, composited on an RGBA canvas."""
    layer = Image.new("RGBA", canvas.size, color + (0,))   # same color at 0 alpha: blur won't fade to grey
    x, y = xy
    ImageDraw.Draw(layer).ellipse([x - r, y - r, x + r, y + r], fill=color + (alpha,))
    canvas.alpha_composite(layer.filter(ImageFilter.GaussianBlur(blur)))


def film_grain(img, strength=10):
    """Fine monochrome grain, the lookbook's paper texture."""
    from PIL import ImageChops
    n = Image.effect_noise(img.size, 30).convert("RGB")
    return \
        Image.blend(img.convert("RGB"), ImageChops.overlay(img.convert("RGB"), n), strength / 100)


def window_light(size, strength=9):
    """Diagonal soft shadow bars, like late light through a window frame (mood hero)."""
    w, h = size
    m = Image.new("L", (w, h), 0)
    d = ImageDraw.Draw(m)
    s = max(w, h)
    for off in (-0.15, 0.32):
        x0 = int(w * off)
        d.polygon([(x0, -s), (x0 + int(s * .09), -s), (x0 + int(s * .09) + s, h + s // 3), (x0 + s, h + s // 3)], fill=strength)
    d.polygon([(-s, int(h * .62)), (w + s, int(h * .40)), (w + s, int(h * .43)), (-s, int(h * .65))], fill=strength)
    return m.filter(ImageFilter.GaussianBlur(s // 30))


def linen(size, tint, light_xy):
    """Warm paper base + one tone-colored glow + window shadow + grain."""
    w, h = size
    bg = Image.new("RGBA", size, LINEN + (255,))
    blob(bg, light_xy, int(max(w, h) * .55), (255, 250, 240), 255, max(w, h) // 8)
    blob(bg, (int(w * .82), int(h * .78)), int(max(w, h) * .38), tint, 120, max(w, h) // 7)
    blob(bg, (int(w * .1), int(h * .55)), int(max(w, h) * .28), (234, 228, 248), 90, max(w, h) // 7)
    shade = Image.new("RGBA", size, (176, 132, 96, 255))   # warm, never grey
    bg = Image.composite(shade, bg, window_light(size))
    return film_grain(bg)


def rounded_mask(size, r):
    m = Image.new("L", size, 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, size[0] - 1, size[1] - 1], r, fill=255)
    return m


def device(screen, width):
    """Slim dark device frame around the real capture (RGBA)."""
    bezel = 13
    sw = width - bezel * 2
    scr = screen.resize((sw, round(screen.height * sw / screen.width)), Image.LANCZOS)
    h = scr.height + bezel * 2
    body = Image.new("RGBA", (width, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(body)
    d.rounded_rectangle([0, 0, width - 1, h - 1], 84, fill=(40, 35, 44, 255))
    d.rounded_rectangle([2, 2, width - 3, h - 3], 82, outline=(88, 80, 94, 255), width=2)   # metal edge highlight
    body.paste(scr, (bezel, bezel), rounded_mask(scr.size, 72))
    d.ellipse([width // 2 - 10, bezel + 22, width // 2 + 10, bezel + 42], fill=(16, 14, 18, 255))  # punch-hole camera
    return body


def drop_shadow(canvas, img, xy):
    """Low violet shadow (brand e2), under the device."""
    a = img.split()[-1]
    sh = Image.new("RGBA", img.size, (60, 44, 90, 0))
    sh.putalpha(a.point(lambda v: 70 if v else 0))
    layer = Image.new("RGBA", canvas.size, (60, 44, 90, 0))
    layer.alpha_composite(sh, (xy[0], xy[1] + 30))
    canvas.alpha_composite(layer.filter(ImageFilter.GaussianBlur(36)))
    canvas.alpha_composite(img, xy)


def draw_tracked(d, xy, text, font, fill, tracking=0):
    x, y = xy
    if not tracking:
        d.text((x, y), text, font=font, fill=fill)
        return
    for ch in text:
        d.text((x, y), ch, font=font, fill=fill)
        x += d.textlength(ch, font=font) + tracking


# (raw capture, eyebrow, headline lines, glow tint behind the device)
SHOTS = [
    ("diagnose.png", "01 — 퍼스널컬러 진단", ["사진 한 장이면", "내 컬러를 찾아요"], "#FFD9C4"),
    ("result.png", "02 — 진단 결과", ["내 톤을 찾으면", "틴티도 그 색으로 물들어요"], "#FFCBB0"),
    ("tone.png", "03 — 12톤 컬러 도감", ["BEST부터 WORST까지", "내 톤 컬러 총정리"], "#F6C9CF"),
    ("colorcheck.png", "04 — 컬러 체크", ["이 옷, 나한테", "어울릴까?"], "#CFE7DC"),
    ("closet.png", "05 — 내 옷장", ["내 옷장도", "내 톤 기준으로"], "#E6DFFB"),
    ("coordi.png", "06 — 오늘의 코디", ["날씨까지 맞춘", "오늘의 코디"], "#F3DDC0"),
    ("widgets.png", "07 — 위젯", ["홈 화면에서 만나는", "오늘의 룩"], "#E6DFFB"),
    ("wallpaper.png", "08 — 움직이는 배경화면", ["내 톤으로 물든 화면에", "틴티가 함께해요"], "#FFD9C4"),
]


def screenshot(n, raw, eyebrow, lines, tint):
    bg = linen((W, H), hex_rgb(tint), (int(W * .85), int(H * .05))).convert("RGBA")
    d = ImageDraw.Draw(bg)
    x = 92
    d.text((x, 132), eyebrow, font=myeongjo(34), fill=BROWN)
    hf = pretendard(78, 700)
    y = 196
    for line in lines:
        draw_tracked(d, (x, y), line, hf, INK, -2)
        y += 104
    top = y + 64
    screen = Image.open(f"{RAW}/{raw}").convert("RGB")
    dev = device(screen, 800)
    drop_shadow(bg, dev, ((W - dev.width) // 2, top))
    out = f"{STORE}/screenshots/screenshot_{n:02d}.png"
    bg.convert("RGB").save(out, optimize=True)
    return out


def feature_graphic():
    w, h = 1024, 500
    bg = linen((w, h), hex_rgb("#FFCBB0"), (int(w * .75), int(h * .1))).convert("RGBA")
    d = ImageDraw.Draw(bg)
    # Tinty on the right, standing in the light, with the 12 tones as a soft arc of drops.
    tinty = Image.open(f"{TINTY}/tinty_main_4096.png").convert("RGBA")
    tinty = tinty.crop(tinty.getbbox())
    th = 300
    tinty = tinty.resize((round(tinty.width * th / tinty.height), th), Image.LANCZOS)
    cx = 770
    blob(bg, (cx, 260), 190, (234, 228, 248), 200, 60)
    tones = ["#FFCBA4", "#FF7F50", "#FF4F6D", "#F4B6C2", "#8FB8DE", "#9E9AC8",
             "#C9A66B", "#C8641E", "#7A2E2E", "#E6007E", "#1F3A93", "#4B2C5E"]
    import math
    for i, c in enumerate(tones):
        a = math.radians(200 + i * (140 / (len(tones) - 1)))
        px, py = cx + 190 * math.cos(a), 285 + 165 * math.sin(a)
        r = 13
        d.ellipse([px - r, py - r, px + r, py + r], fill=hex_rgb(c) + (255,))
    bg.alpha_composite(tinty, (cx - tinty.width // 2, 445 - th))
    # Copy, inside the safe area.
    x = 84
    icon = Image.open(f"{STORE}/app_icon_512.png").convert("RGBA").resize((64, 64), Image.LANCZOS)
    bg.paste(icon, (x, 104), rounded_mask((64, 64), 16))
    d.text((x + 82, 120), "나의 컬러는?", font=pretendard(30, 700), fill=INK)
    hf = pretendard(56, 760)
    draw_tracked(d, (x, 206), "내 퍼스널컬러,", hf, INK, -2)
    draw_tracked(d, (x, 278), "사진 한 장으로", hf, ACCENT, -2)
    d.text((x, 368), "12톤 진단부터 오늘의 코디까지", font=pretendard(26, 500), fill=INK2)
    out = f"{STORE}/feature_graphic.png"
    bg.convert("RGB").save(out, optimize=True)
    return out


def preview_sheet(shots, fg):
    cols, tw = 4, 380
    th = round(tw * H / W)
    gap = 36
    fw = cols * tw + (cols - 1) * gap
    fh = round(fw * 500 / 1024)
    sheet = Image.new("RGB", (fw + gap * 2, gap + fh + gap + 2 * (th + gap)), (233, 227, 219))
    sheet.paste(Image.open(fg).resize((fw, fh), Image.LANCZOS), (gap, gap))
    for i, p in enumerate(shots):
        r, c = divmod(i, cols)
        sheet.paste(Image.open(p).resize((tw, th), Image.LANCZOS), (gap + c * (tw + gap), gap * 2 + fh + r * (th + gap)))
    out = f"{STORE}/preview_sheet.png"
    sheet.save(out, optimize=True)
    return out


if __name__ == "__main__":
    os.makedirs(f"{STORE}/screenshots", exist_ok=True)
    shots = [screenshot(i, *s) for i, s in enumerate(SHOTS, 1) if os.path.exists(f"{RAW}/{s[0]}")]
    fg = feature_graphic()
    print("\n".join(shots + [fg, preview_sheet(shots, fg)]))
    for p in shots + [fg]:
        im = Image.open(p)
        assert im.mode == "RGB", p
    assert Image.open(fg).size == (1024, 500)
