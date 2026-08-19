import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync, readdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join, relative } from 'node:path';
import { maskEmail } from '../src/utils/user.js';

/*
 * 이슈 #328 - 화면에 이메일 원본을 그리지 않는다.
 *
 * 마이페이지는 본인만 보는 화면이지만 시연영상·발표자료·스크린샷에 계속 등장한다.
 * 2026-08-19 배포본 점검에서 `google · twinsjh01@gmail.com` 이 그대로 찍히는 것을 확인했다.
 */

test('아이디 앞 3자만 남기고 도메인은 그대로 둔다', () => {
    assert.equal(maskEmail('twinsjh01@gmail.com'), 'twi******@gmail.com');
    assert.equal(maskEmail('hong@naver.com'), 'hon*@naver.com');
});

/* 도메인을 남기는 이유는 소셜 계정을 여러 개 쓸 때 어느 계정인지 구분해야 해서다. */
test('도메인은 가리지 않는다 - 어느 소셜 계정인지는 알아볼 수 있어야 한다', () => {
    assert.match(maskEmail('twinsjh01@gmail.com'), /@gmail\.com$/);
    assert.match(maskEmail('someone@kakao.com'), /@kakao\.com$/);
});

/*
 * 남기는 글자 수가 아이디 길이를 넘으면 아무것도 가려지지 않는다.
 * 짧은 아이디에서 원본이 그대로 새는 구멍이라 최소 1자는 반드시 가린다.
 */
test('짧은 아이디도 최소 1자는 가린다', () => {
    assert.equal(maskEmail('ab@naver.com'), 'a*@naver.com');
    assert.equal(maskEmail('abc@naver.com'), 'ab*@naver.com');
    assert.equal(maskEmail('abcd@naver.com'), 'abc*@naver.com');
});

test('가린 결과에 원본 아이디가 통째로 남아 있지 않다', () => {
    for (const email of ['twinsjh01@gmail.com', 'ab@naver.com', 'abc@daum.net']) {
        const local = email.slice(0, email.indexOf('@'));
        assert.ok(!maskEmail(email).startsWith(`${local}@`), `${email} 의 아이디가 그대로 남았다`);
    }
});

/* 이메일이 아닌 값을 화면에 흘리지 않는다. 빈 문자열이면 호출부의 filter(Boolean) 가 걸러낸다. */
test('값이 없거나 이메일 꼴이 아니면 빈 문자열이다', () => {
    assert.equal(maskEmail(null), '');
    assert.equal(maskEmail(undefined), '');
    assert.equal(maskEmail(''), '');
    assert.equal(maskEmail('   '), '');
    assert.equal(maskEmail('그냥문자열'), '');
    assert.equal(maskEmail('@gmail.com'), '', '아이디가 없으면 가릴 것도 없다');
    assert.equal(maskEmail('twinsjh01@'), '', '도메인이 없으면 이메일이 아니다');
});

test('@ 가 여러 개면 마지막 것을 도메인 경계로 본다', () => {
    /* 앞의 @ 는 아이디의 일부로 취급된다. 검증할 것은 도메인을 어디서 끊느냐다. */
    assert.match(maskEmail('a@b@gmail.com'), /@gmail\.com$/);
    assert.ok(maskEmail('a@b@gmail.com').includes('*'), '아이디 일부는 가려져야 한다');
});

/* ── 다른 화면이 생겨도 원본이 새지 않게 ──────────────── */

const SRC = fileURLToPath(new URL('../src/', import.meta.url));

function walk(dir) {
    const found = [];
    for (const entry of readdirSync(dir, { withFileTypes: true })) {
        const full = join(dir, entry.name);
        if (entry.isDirectory()) found.push(...walk(full));
        else if (entry.name.endsWith('.vue')) found.push(full);
    }
    return found;
}

/*
 * 지금은 MyProfileCard 한 곳뿐이지만, 나중에 다른 화면이 이메일을 그릴 때
 * maskEmail 을 거치지 않으면 이 테스트가 잡는다.
 */
test('컴포넌트가 이메일 원본을 화면에 그리지 않는다', () => {
    const offenders = [];

    for (const file of walk(SRC)) {
        const source = readFileSync(file, 'utf8');
        if (!/\.email\b/.test(source)) continue;
        if (!source.includes('maskEmail')) {
            offenders.push(relative(SRC, file));
        }
    }

    assert.deepEqual(
        offenders,
        [],
        `이메일을 그리려면 utils/user.js 의 maskEmail 을 거친다:\n${offenders.join('\n')}`,
    );
});
