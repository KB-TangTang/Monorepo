# -*- coding: utf-8 -*-
"""PWA 아이콘 PNG 를 팀 로고 원본에서 다시 만든다.

    python apps/web/scripts/build-icons.py

원본은 src/assets/images/logo-tangtang.png 하나뿐이다.
(팀 산출물 doc/개발산출물/PWA로고/LOGO_TANGTANG.png 을 그대로 복사한 것)
public/icons/*.png 를 손으로 고치면 다음 실행 때 덮어써진다.

필요한 것: Python 3 + Pillow  (pip install pillow)

손으로 자르지 않고 스크립트를 두는 이유가 둘 있다.

1. **바탕을 캔버스 끝까지 채워야 한다.**
   원본은 둥근 사각형 바깥이 흰색이다. 그대로 넣으면 런처가 제 모양으로 오려낼 때
   흰 테두리가 후광처럼 남는다. v1 에서 실제로 났던 문제다 (PR #384).

2. **maskable 은 글자가 잘리면 안 된다.**
   런처는 아이콘을 제 모양대로 오려낸다(삼성 둥근사각, 픽셀 원). 규격이 보장하는
   안전영역은 지름 80% 원뿐이다. 도형이면 조금 잘려도 되지만 글자는 안 된다.
   그래서 글자 획의 실제 최대 반지름을 재서 그 원 안에 들어가게 줄인다.
"""

import math
import pathlib
import sys
from collections import Counter

try:
    from PIL import Image
except ImportError:
    sys.exit('Pillow 가 필요하다:  pip install pillow')

WEB = pathlib.Path(__file__).resolve().parent.parent
SOURCE = WEB / 'src' / 'assets' / 'images' / 'logo-tangtang.png'
ICONS = WEB / 'public' / 'icons'

VERSION = 'v4'
CANVAS = 512

# any 는 마스크가 없다. 로고 원본의 글자 비율(바탕 폭의 78%)에 맞춘다.
ANY_TEXT_RATIO = 0.80
# maskable 은 지름 80% 원(반지름 0.40 * 512) 안에 글자가 다 들어가야 한다.
MASK_SAFE_RATIO = 0.40

# 파일명은 v3 때 쓰던 규칙 그대로다. 이름 뒤에 버전을 붙여 캐시를 우회한다.
OUTPUTS = [
    (f'icon-512-{VERSION}.png', 512, 'any'),
    (f'icon-192-{VERSION}.png', 192, 'any'),
    (f'apple-touch-icon-180-{VERSION}.png', 180, 'any'),
    (f'icon-maskable-512-{VERSION}.png', 512, 'maskable'),
]


def analyse(src):
    """원본에서 바탕색·글자 bbox·글자 최대 반지름을 잰다.

    좌표를 박아 두지 않는 이유는 로고가 교체돼도 스크립트가 그대로 돌게 하기 위해서다.

    ⚠ 둥근 모서리 주의. 바탕 bbox 를 그대로 쓰면 **모서리의 흰 여백이 글자로 잡힌다**
      (흰색 #FDFDFE 와 크림 글자 #FFF8E7 은 색이 가까워 밝기만으로는 못 가른다).
      그래서 행·열마다 바탕이 실제로 시작하고 끝나는 지점을 따로 구해 그 안쪽만 본다.
    """
    px = src.load()
    width, height = src.size

    counts = Counter()
    for y in range(0, height, 3):
        for x in range(0, width, 3):
            counts[px[x, y]] += 1
    background = counts.most_common(1)[0][0]

    def near_background(pixel):
        return sum(abs(a - b) for a, b in zip(pixel, background)) < 60

    inset = 5
    row_span, col_span = {}, {}
    for y in range(height):
        xs = [x for x in range(width) if near_background(px[x, y])]
        if xs:
            row_span[y] = (min(xs) + inset, max(xs) - inset)
    for x in range(width):
        ys = [y for y in range(height) if near_background(px[x, y])]
        if ys:
            col_span[x] = (min(ys) + inset, max(ys) - inset)
    if not row_span:
        sys.exit('바탕색 영역을 못 찾았다. 원본을 확인하라.')

    glyph = []
    for y, (lo, hi) in row_span.items():
        for x in range(lo, hi + 1):
            span = col_span.get(x)
            if not span or not (span[0] <= y <= span[1]):
                continue          # 모서리 흰 여백
            if not near_background(px[x, y]):
                glyph.append((x, y))
    if not glyph:
        sys.exit('글자를 못 찾았다. 원본을 확인하라.')

    xs = [p[0] for p in glyph]
    ys = [p[1] for p in glyph]
    box = (min(xs), min(ys), max(xs), max(ys))
    cx, cy = (box[0] + box[2]) / 2, (box[1] + box[3]) / 2
    # bbox 모서리가 아니라 실제 획 기준. 글자는 모서리가 비어 있어 차이가 크다.
    radius = max(math.hypot(x - cx, y - cy) for x, y in glyph)
    return background, box, radius


def render(src, background, box, text_width, size):
    """글자를 target 폭으로 맞춰 바탕이 꽉 찬 캔버스 한가운데에 얹는다."""
    pad = 8   # 안티앨리어싱 픽셀이 잘리지 않게. 이 영역도 바탕색이라 이음매가 없다
    crop = src.crop((box[0] - pad, box[1] - pad, box[2] + pad, box[3] + pad))
    scale = text_width / (box[2] - box[0])
    crop = crop.resize((round(crop.width * scale), round(crop.height * scale)), Image.LANCZOS)

    out = Image.new('RGB', (CANVAS, CANVAS), background)
    out.paste(crop, ((CANVAS - crop.width) // 2, (CANVAS - crop.height) // 2))
    if size != CANVAS:
        out = out.resize((size, size), Image.LANCZOS)
    return out


def main():
    if not SOURCE.exists():
        sys.exit(f'원본이 없다: {SOURCE}')
    src = Image.open(SOURCE).convert('RGB')
    background, box, radius = analyse(src)
    text_w = box[2] - box[0]

    widths = {
        'any': round(CANVAS * ANY_TEXT_RATIO),
        # 글자 폭 : 반지름 = text_w : radius 이므로, 허용 반지름에서 폭을 역산한다
        'maskable': round(text_w * (CANVAS * MASK_SAFE_RATIO) / radius),
    }

    print('바탕색 #%02X%02X%02X' % background)
    print('글자 bbox %dx%d · 획 최대 반지름 %.0f' % (text_w, box[3] - box[1], radius))
    print('512 캔버스 글자 폭 : any %d px · maskable %d px' % (widths['any'], widths['maskable']))

    ICONS.mkdir(parents=True, exist_ok=True)
    for name, size, purpose in OUTPUTS:
        render(src, background, box, widths[purpose], size).save(ICONS / name)
        print(f'  {name}  {size}x{size}  ({purpose})')

    print('완료. manifest.webmanifest 와 index.html 의 경로도 맞는지 확인하라.')


if __name__ == '__main__':
    main()
