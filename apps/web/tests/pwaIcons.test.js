import test from 'node:test';
import assert from 'node:assert/strict';
import { existsSync, readFileSync, readdirSync } from 'node:fs';
import { inflateSync } from 'node:zlib';

/*
 * PWA 아이콘 회귀 방지 (2026-08-21, 팀 로고 v4 로 교체하면서 추가).
 *
 * 아이콘은 폰에 설치해 봐야 아는 자산이다. 경로가 어긋나거나 흰 테두리가 남아도
 * 빌드는 통과하고 배포도 초록불로 끝난다. 그래서 아래를 검사한다.
 *   1. manifest·index.html 이 가리키는 파일이 실제로 있는가
 *   2. 버전 접미사가 한 가지로 통일돼 있고 옛 파일이 남아 있지 않은가
 *   3. maskable 이 선언돼 있는가
 *   4. 네 모서리에 흰색이 남아 있지 않은가  ← v1 에서 실제로 났던 문제 (PR #384)
 */

function source(path) {
    return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

const manifest = JSON.parse(source('public/manifest.webmanifest'));
const indexHtml = source('index.html');

/** manifest 와 index.html 이 가리키는 모든 아이콘 경로 */
function referencedIcons() {
    const fromManifest = manifest.icons.map((i) => i.src);
    const fromHtml = [...indexHtml.matchAll(/href="(\/icons\/[^"]+)"/g)].map((m) => m[1]);
    return [...new Set([...fromManifest, ...fromHtml])];
}

/**
 * PNG 의 좌상단 픽셀 하나를 읽는다.
 *
 * 첫 행의 첫 픽셀은 필터 종류와 무관하게 원본 바이트 그대로다.
 * (왼쪽·위쪽 이웃이 전부 없어 Sub·Up·Average·Paeth 예측값이 모두 0이다)
 * 그래서 디코더 없이 zlib 만으로 안전하게 꺼낼 수 있다.
 */
function topLeftPixel(fileUrl) {
    const buf = readFileSync(fileUrl);
    assert.equal(buf.readUInt32BE(0), 0x89504e47, 'PNG 시그니처가 아니다');

    let offset = 8;
    let header = null;
    const dataChunks = [];
    while (offset < buf.length) {
        const length = buf.readUInt32BE(offset);
        const type = buf.toString('ascii', offset + 4, offset + 8);
        const body = buf.subarray(offset + 8, offset + 8 + length);
        if (type === 'IHDR') {
            header = { bitDepth: body[8], colorType: body[9], interlace: body[12] };
        } else if (type === 'IDAT') {
            dataChunks.push(body);
        } else if (type === 'IEND') {
            break;
        }
        offset += 12 + length;
    }
    assert.ok(header, 'IHDR 를 못 찾았다');
    assert.equal(header.bitDepth, 8, '8비트 PNG 만 읽는다');
    assert.equal(header.interlace, 0, '인터레이스 PNG 는 읽지 않는다');
    assert.ok([2, 6].includes(header.colorType), 'RGB 또는 RGBA 만 읽는다');

    // Buffer 전역은 eslint 설정에 없다. Uint8Array 로 이어 붙여도 inflateSync 가 받는다.
    const total = dataChunks.reduce((sum, chunk) => sum + chunk.length, 0);
    const compressed = new Uint8Array(total);
    let cursor = 0;
    for (const chunk of dataChunks) {
        compressed.set(chunk, cursor);
        cursor += chunk.length;
    }

    const raw = inflateSync(compressed);
    return { r: raw[1], g: raw[2], b: raw[3] };
}

test('가리키는 아이콘 파일이 전부 실제로 있다', () => {
    for (const src of referencedIcons()) {
        assert.ok(
            existsSync(new URL(`../public${src}`, import.meta.url)),
            `${src} 가 public/ 에 없다`,
        );
    }
});

test('버전 접미사가 한 가지로 통일돼 있다', () => {
    const versions = new Set(
        referencedIcons().map((src) => {
            const m = src.match(/-(v\d+)\.png$/);
            assert.ok(m, `${src} 에 버전 접미사가 없다. 캐시 때문에 옛 아이콘이 계속 뜬다`);
            return m[1];
        }),
    );
    assert.equal(versions.size, 1, `버전이 섞였다: ${[...versions].join(', ')}`);
});

test('옛 버전 아이콘이 남아 있지 않다', () => {
    const current = referencedIcons()[0].match(/-(v\d+)\.png$/)[1];
    const stale = readdirSync(new URL('../public/icons/', import.meta.url)).filter(
        (name) => name.endsWith('.png') && !name.includes(`-${current}.`),
    );
    assert.deepEqual(stale, [], `쓰이지 않는 옛 아이콘이 남아 있다: ${stale.join(', ')}`);
});

test('maskable 아이콘이 하나 이상 선언돼 있다', () => {
    /*
     * 안드로이드 런처는 maskable 이 없으면 any 아이콘을 흰 원 안에 축소해 넣는다.
     * v1 때 이것 때문에 아이콘 둘레에 흰 후광이 생겼다 (PR #384).
     */
    assert.ok(
        manifest.icons.some((i) => i.purpose === 'maskable'),
        'purpose: maskable 이 필요하다',
    );
});

test('아이콘 모서리에 흰색이 남아 있지 않다', () => {
    /*
     * 로고 원본은 둥근 사각형 **바깥이 흰색**이다. 그대로 넣으면 런처가 제 모양대로
     * 오려낼 때 흰 테두리가 후광처럼 남는다. build-icons.py 가 바탕을 캔버스 끝까지
     * 칠하는데, 그 단계를 건너뛰고 PNG 를 손으로 만들면 이 검사가 잡는다.
     */
    for (const src of referencedIcons()) {
        const { r, g, b } = topLeftPixel(new URL(`../public${src}`, import.meta.url));
        const brightness = (r + g + b) / 3;
        assert.ok(brightness < 120, `${src} 좌상단이 밝다 (rgb ${r},${g},${b}) — 흰 여백이 남았다`);
    }
});

test('아이콘 원본과 빌드 스크립트가 함께 있다', () => {
    /*
     * PNG 를 손으로 만드는 것을 막는 장치다. 원본이 사라지면 다음 사람이 PNG 를 직접 만진다.
     * 원본은 팀 산출물 doc/개발산출물/PWA로고/LOGO_TANGTANG.png 을 복사한 것이다.
     */
    assert.ok(existsSync(new URL('../src/assets/images/logo-tangtang.png', import.meta.url)));
    assert.ok(existsSync(new URL('../scripts/build-icons.py', import.meta.url)));
});
