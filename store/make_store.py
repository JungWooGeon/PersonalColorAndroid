"""Builds Play Store screenshots (1080x1920) and the feature graphic (1024x500) from raw emulator captures."""
import math
import os
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.dirname(os.path.abspath(__file__))
RAW, OUT, FONTS = f"{ROOT}/raw", f"{ROOT}/out", f"{ROOT}/fonts"
os.makedirs(OUT, exist_ok=True)
ICON = "/Users/jwg/Desktop/내 문서/JWG/repos/PersonalColorAndroid/app/src/main/ic_launcher-playstore.png"

INK = (29, 27, 26)
MUTED = (102, 96, 91)
ACCENT = (91, 75, 138)
W, H = 1080, 1920


def font(weight, size):
    return ImageFont.truetype(f"{FONTS}/Pretendard-{weight}.otf", size)


def hex_rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def gradient(w, h, stops, angle=35):
    """Smooth multi-stop linear gradient (stops: list of RGB), angle in degrees."""
    img = Image.new("RGB", (w, h))
    px = img.load()
    a = math.radians(angle)
    dx, dy = math.cos(a), math.sin(a)
    proj = [x * dx + y * dy for x, y in ((0, 0), (w, 0), (0, h), (w, h))]
    lo, hi = min(proj), max(proj)
    n = len(stops) - 1
    for y in range(h):
        for x in range(w):
            t = ((x * dx + y * dy) - lo) / (hi - lo) * n
            i = min(int(t), n - 1)
            f = t - i
            c0, c1 = stops[i], stops[i + 1]
            px[x, y] = tuple(int(c0[k] + (c1[k] - c0[k]) * f) for k in range(3))
    return img


def soft_background(stops):
    # Render small and upscale: fast and naturally smooth.
    return gradient(W // 6, H // 6, stops).resize((W, H), Image.BICUBIC).filter(ImageFilter.GaussianBlur(6))


def rounded_mask(size, r):
    m = Image.new("L", size, 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, size[0] - 1, size[1] - 1], r, fill=255)
    return m


def phone(screen, width=820):
    """Dark phone body with the screenshot inside; returned as RGBA (taller than the canvas is fine — it's cropped)."""
    bezel = 22
    sw = width - bezel * 2
    scale = sw / screen.width
    sh = int(screen.height * scale)
    scr = screen.resize((sw, sh), Image.LANCZOS)
    body = Image.new("RGBA", (width, sh + bezel * 2), (0, 0, 0, 0))
    d = ImageDraw.Draw(body)
    d.rounded_rectangle([0, 0, width - 1, sh + bezel * 2 - 1], 92, fill=(24, 22, 22, 255))
    body.paste(scr, (bezel, bezel), rounded_mask(scr.size, 72))
    # camera dot
    d.ellipse([width // 2 - 11, bezel + 18, width // 2 + 11, bezel + 40], fill=(10, 10, 10, 255))
    return body


def shadow(img, radius=40, offset=(0, 24), alpha=70):
    a = img.split()[-1].point(lambda v: alpha if v else 0)
    sh = Image.new("RGBA", img.size, (0, 0, 0, 0))
    sh.putalpha(a)
    pad = radius * 3
    canvas = Image.new("RGBA", (img.width + pad * 2, img.height + pad * 2), (0, 0, 0, 0))
    canvas.paste(sh, (pad + offset[0], pad + offset[1]), sh)
    return canvas.filter(ImageFilter.GaussianBlur(radius)), pad


def text_center(d, y, text, f, fill):
    w = d.textlength(text, font=f)
    d.text(((W - w) / 2, y), text, font=f, fill=fill)


def screenshot(n, raw, headline, sub, stops, patch=None):
    bg = soft_background([hex_rgb(s) for s in stops]).convert("RGBA")
    d = ImageDraw.Draw(bg)
    lines = headline.split(" / ")
    hf = font("ExtraBold", 82)
    y = 150
    for line in lines:
        text_center(d, y, line, hf, INK)
        y += 106
    text_center(d, y + 18, sub, font("Medium", 40), MUTED)

    screen = Image.open(f"{RAW}/{raw}").convert("RGB")
    clean_status(screen)
    if patch:
        patch(screen)
    ph = phone(screen)
    top = y + 120
    sh, pad = shadow(ph)
    bg.alpha_composite(sh.crop((0, 0, sh.width, min(sh.height, H - top + pad))), ((W - ph.width) // 2 - pad, top - pad))
    bg.alpha_composite(ph.crop((0, 0, ph.width, H - top)), ((W - ph.width) // 2, top))
    out = f"{OUT}/screenshot_{n:02d}.png"
    bg.convert("RGB").save(out, optimize=True)
    return out


def clean_status(screen):
    """Redraw the emulator status bar: just 9:41, signal, wifi and a full battery (no '3G', no debug icons)."""
    if screen.height < 2200:   # not a phone capture (e.g. the report image)
        return
    d = ImageDraw.Draw(screen)
    bg = screen.getpixel((540, 40))
    d.rectangle([0, 0, screen.width, 70], fill=bg)
    ink = INK if sum(bg) > 380 else (240, 240, 240)
    d.text((48, 12), "9:41", font=font("SemiBold", 38), fill=ink)
    x = 900
    for i in range(4):  # signal bars
        bh = 10 + i * 7
        d.rounded_rectangle([x + i * 11, 48 - bh, x + i * 11 + 7, 48], 2, fill=ink)
    d.rounded_rectangle([956, 22, 1010, 48], 6, outline=ink, width=3)   # battery
    d.rounded_rectangle([961, 27, 1005, 43], 3, fill=ink)
    d.rectangle([1012, 30, 1016, 40], fill=ink)


def patch_place(screen):
    """Emulator location stays at its default (Mountain View); show what a Korean user sees."""
    d = ImageDraw.Draw(screen)
    bg = screen.getpixel((560, 300))
    d.rectangle([284, 318, 640, 380], fill=bg)
    d.text((287, 322), "마포구 망원동", font=ImageFont.truetype("/System/Library/Fonts/AppleSDGothicNeo.ttc", 46, index=6), fill=INK)


SHOTS = [
    ("home.png", "오늘의 내 컬러, / 한눈에", "진단·코디·컬러를 한 곳에", ["#F7D6E0", "#E9DDF7", "#D8E6FA"], None),
    ("result1.png", "사진 한 장으로 / 찾는 내 톤", "12톤 일치도와 분석 근거", ["#F9DCE3", "#EADCF6", "#DCE3F8"], None),
    ("tone.png", "내 톤 베스트 컬러 / 총정리", "메이크업·헤어·코디까지", ["#E4ECFB", "#EDE2F7", "#FBE1EA"], None),
    ("colorcheck2.png", "살까 말까? / 찍어서 확인", "옷·립 색이 내 톤에 맞는지", ["#E8E0F8", "#F3E3F0", "#FCEBDD"], None),
    ("coordi.png", "날씨까지 맞춘 / 오늘의 코디", "기온·강수·내 톤으로 추천", ["#DDEBFA", "#E6E3F8", "#F6E4EE"], patch_place),
    ("closet.png", "내 옷장 컬러, / 톤으로 정리", "어울리는 옷이 한눈에", ["#DFF1EA", "#E5E7F8", "#F7E3EA"], None),
    ("report.png", "간직하고 싶은 / 내 컬러 카드", "이미지 저장·카톡 공유", ["#F6E5D9", "#F4DCE6", "#E6DDF6"], None),
    ("guide.png", "웜? 쿨? / 헷갈릴 땐 가이드", "알기 쉬운 가이드 14편", ["#FBE6D8", "#F5E0EA", "#E3E2F8"], None),
]


def feature_graphic():
    w, h = 1024, 500
    bg = gradient(w // 4, h // 4, [hex_rgb(c) for c in ["#FFCBB0", "#F6C6D6", "#C9D3F5", "#B9A8E6"]], angle=20) \
        .resize((w, h), Image.BICUBIC).filter(ImageFilter.GaussianBlur(3)).convert("RGBA")
    veil = Image.new("RGBA", (w, h), (255, 255, 255, 60))
    bg.alpha_composite(veil)
    d = ImageDraw.Draw(bg)
    # 12-tone colour ring on the right
    ring = ["#F4C2C2", "#FF8C69", "#FF5A5F", "#F7CAC9", "#C8A2C8", "#9DB4E0", "#B0A8B9", "#C9A66B",
            "#B5651D", "#5B3A29", "#7B1E3A", "#3B4A8C"]
    cx, cy, r, sr = 770, 250, 140, 38
    for i, c in enumerate(ring):
        a = -math.pi / 2 + i * 2 * math.pi / len(ring)
        x, y = cx + r * math.cos(a), cy + r * math.sin(a)
        d.ellipse([x - sr, y - sr, x + sr, y + sr], fill=hex_rgb(c) + (255,), outline=(255, 255, 255, 255), width=5)
    icon = Image.open(ICON).convert("RGBA").resize((150, 150), Image.LANCZOS)
    icon_mask = Image.new("L", icon.size, 0)
    ImageDraw.Draw(icon_mask).ellipse([0, 0, 149, 149], fill=255)
    bg.paste(icon, (cx - 75, cy - 75), icon_mask)
    # Copy (left, inside the safe area)
    d.text((70, 150), "사진 한 장으로 찾는", font=font("ExtraBold", 54), fill=INK)
    d.text((70, 220), "나의 컬러", font=font("ExtraBold", 54), fill=ACCENT)
    d.text((70, 305), "AI 12톤 퍼스널컬러 진단", font=font("SemiBold", 30), fill=MUTED)
    out = f"{OUT}/feature_graphic.png"
    bg.convert("RGB").save(out, optimize=True)
    return out


def app_icon_512():
    out = f"{OUT}/app_icon_512.png"
    Image.open(ICON).convert("RGBA").resize((512, 512), Image.LANCZOS).save(out)
    return out


if __name__ == "__main__":
    for i, (raw, head, sub, stops, patch) in enumerate(SHOTS, 1):
        print(screenshot(i, raw, head, sub, stops, patch))
    print(feature_graphic())
    print(app_icon_512())
