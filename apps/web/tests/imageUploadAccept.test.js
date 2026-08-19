import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync, readdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join, relative } from 'node:path';

/*
 * 업로드 파일 형식 제한.
 *
 * 서버(`ImageProcessor`)는 ImageIO 로 다시 디코딩해 JPEG 로 재인코딩한다.
 * 즉 SVG 는 애초에 디코딩되지 않아 저장되지 않는다 — 보안 구멍은 아니다.
 * 문제는 UX 다. `accept="image/*"` 는 SVG · HEIC · WEBP 를 **고를 수 있게** 해놓고
 * 올린 뒤에야 INVALID_IMAGE 로 튕긴다. 특히 HEIC 는 아이폰 기본 포맷이라 흔하다.
 * 그래서 선택창 단계에서 JPG · PNG 로 좁힌다.
 *
 * 화면마다 손으로 맞추면 새 업로드 화면이 생길 때 또 `image/*` 로 돌아간다.
 * 개별 화면을 단언하지 않고 **소스 전체를 훑는 스캐너**로 둔다.
 */

const SRC = fileURLToPath(new URL('../src/', import.meta.url));

function walk(dir, extensions) {
    const found = [];
    for (const entry of readdirSync(dir, { withFileTypes: true })) {
        const full = join(dir, entry.name);
        if (entry.isDirectory()) {
            found.push(...walk(full, extensions));
        } else if (extensions.some((extension) => entry.name.endsWith(extension))) {
            found.push(full);
        }
    }
    return found;
}

/** `<input ... type="file" ...>` 한 덩어리씩. 속성이 여러 줄에 걸쳐 있어 줄 단위로는 못 잡는다. */
function fileInputs(source) {
    return [...source.matchAll(/<input\b[^>]*?>/gs)]
        .map((match) => match[0])
        .filter((tag) => /type="file"/.test(tag));
}

test('파일 선택창을 image/* 로 열어두지 않는다', () => {
    const offenders = [];

    for (const file of walk(SRC, ['.vue'])) {
        if (readFileSync(file, 'utf8').includes('accept="image/*"')) {
            offenders.push(relative(SRC, file));
        }
    }

    assert.deepEqual(
        offenders,
        [],
        `accept="image/*" 는 SVG · HEIC 까지 고를 수 있게 한다. 서버가 읽을 수 있는 형식만 남긴다:\n${offenders.join('\n')}`,
    );
});

test('모든 파일 입력에 accept 가 붙어 있다', () => {
    const offenders = [];

    for (const file of walk(SRC, ['.vue'])) {
        for (const tag of fileInputs(readFileSync(file, 'utf8'))) {
            if (!/(^|\s):?accept=/.test(tag)) {
                offenders.push(`${relative(SRC, file)}  ${tag.replace(/\s+/g, ' ').slice(0, 80)}`);
            }
        }
    }

    assert.deepEqual(
        offenders,
        [],
        `accept 가 없으면 모든 파일이 선택된다:\n${offenders.join('\n')}`,
    );
});

test('업로드 화면 3곳은 JPG · PNG 만 받는다', () => {
    /* 서버가 ImageIO 로 읽을 수 있는 형식 중 실제로 쓰이는 둘. 늘리려면 서버 확인이 먼저다. */
    const EXPECTED = 'image/jpeg,image/png';
    const VIEWS = [
        'views/MyPageView.vue',
        'views/onboarding/NicknameSetupView.vue',
        'views/challenge/group/DefenseWriteView.vue',
    ];

    for (const view of VIEWS) {
        const source = readFileSync(join(SRC, view), 'utf8');
        const tags = fileInputs(source);
        assert.equal(tags.length, 1, `${view} 의 파일 입력이 1개가 아니다`);

        /* 정적 accept 이거나, DefenseWriteView 처럼 상수를 join 해 바인딩하거나 둘 중 하나다. */
        const staticAccept = tags[0].includes(`accept="${EXPECTED}"`);
        const boundAccept =
            /:accept="ALLOWED_IMAGE_TYPES\.join\(','\)"/.test(tags[0]) &&
            source.includes(`const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png']`);

        assert.ok(staticAccept || boundAccept, `${view} 가 ${EXPECTED} 로 제한하지 않는다`);
    }
});
