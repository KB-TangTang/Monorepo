import test from 'node:test';
import assert from 'node:assert/strict';
import {
    NICKNAME_MAX_LENGTH,
    needsNicknameSetup,
    resolveDisplayName,
    validateNickname,
} from '../src/utils/user.js';

/* ── 표시명 규칙 ─────────────────────────────────
 * `nickname ?? socialName` 이고 서버가 displayName 으로 계산해 내려준다.
 * (DECISIONS.md 2026-08-11 「닉네임 온보딩 신설」)
 */
test('서버가 계산한 displayName 을 가장 먼저 쓴다', () => {
    assert.equal(
        resolveDisplayName({ displayName: '탕탕이', nickname: '별명', socialName: '장재한' }),
        '탕탕이',
    );
});

test('displayName 이 없으면 nickname 을 쓴다 — 부분 갱신 응답에는 그 필드가 없을 수 있다', () => {
    assert.equal(resolveDisplayName({ nickname: '탕탕이', socialName: '장재한' }), '탕탕이');
});

test('닉네임을 아직 정하지 않았으면 구글 이름(socialName)을 쓴다', () => {
    assert.equal(resolveDisplayName({ nickname: null, socialName: '장재한' }), '장재한');
});

test('셋 다 없으면 빈 문자열이다 — 대체 문구는 화면이 정한다', () => {
    assert.equal(resolveDisplayName({}), '');
    assert.equal(resolveDisplayName(null), '');
    assert.equal(resolveDisplayName(undefined), '');
    /* 빈 문자열·공백만 있는 값은 이름이 아니다. 그대로 그리면 카드가 비어 보인다. */
    assert.equal(resolveDisplayName({ displayName: '   ', nickname: '', socialName: null }), '');
});

/* ── 닉네임 검증 ───────────────────────────────── */
test('빈 값과 공백만 있는 입력은 저장할 수 없다', () => {
    assert.equal(validateNickname('').valid, false);
    assert.equal(validateNickname('   ').valid, false);
    assert.equal(validateNickname(null).valid, false);
    assert.equal(validateNickname(undefined).valid, false);
});

test('1자부터 50자까지 통과한다 — 서버 검증(1~50자)과 같은 규칙이라야 어긋나지 않는다', () => {
    assert.equal(validateNickname('탕').valid, true);
    assert.equal(validateNickname('탕'.repeat(NICKNAME_MAX_LENGTH)).valid, true);
});

test('51자는 막는다', () => {
    const result = validateNickname('탕'.repeat(NICKNAME_MAX_LENGTH + 1));
    assert.equal(result.valid, false);
    assert.notEqual(result.error, '');
});

test('서버로 보낼 값은 앞뒤 공백을 지운 것이다', () => {
    assert.deepEqual(validateNickname('  탕탕이  '), { valid: true, value: '탕탕이', error: '' });
});

/* ── 온보딩 게이트 판정 ─────────────────────────
 * 라우터를 통째로 띄우지 않고 조건만 검증한다.
 */
const HOME = { name: 'home', meta: {} };

test('닉네임이 비어 있으면 온보딩 화면으로 보낸다', () => {
    assert.equal(needsNicknameSetup({ nickname: null }, HOME), true);
    assert.equal(needsNicknameSetup({ nickname: '' }, HOME), true);
    assert.equal(needsNicknameSetup({}, HOME), true);
});

test('닉네임이 있으면 통과시킨다', () => {
    assert.equal(needsNicknameSetup({ nickname: '탕탕이' }, HOME), false);
});

test('사용자 정보가 아직 없으면 통과시킨다 — 없는 값으로 판단하면 부팅 직후 엉뚱하게 튕긴다', () => {
    assert.equal(needsNicknameSetup(null, HOME), false);
    assert.equal(needsNicknameSetup(undefined, HOME), false);
});

test('닉네임 화면 자신은 면제한다 — 아니면 무한 리다이렉트가 난다', () => {
    assert.equal(
        needsNicknameSetup({ nickname: null }, { name: 'nicknameSetup', meta: {} }),
        false,
    );
});

test('닉네임보다 앞 단계인 동의 화면은 면제한다', () => {
    assert.equal(needsNicknameSetup({ nickname: null }, { name: 'consent', meta: {} }), false);
    assert.equal(
        needsNicknameSetup({ nickname: null }, { name: 'financialConsent', meta: {} }),
        false,
    );
});

test('계좌 연동 플로우는 면제한다 — 연동 중인 사용자가 튕겨나가면 연동을 끝낼 수 없다', () => {
    assert.equal(
        needsNicknameSetup({ nickname: null }, { name: 'accountLinkInstitutions', meta: {} }),
        false,
    );
    /* 2~5단계는 meta.linkStep 으로 판별한다. 연결 완료 화면(done)도 여기에 든다. */
    assert.equal(
        needsNicknameSetup(
            { nickname: null },
            { name: 'accountLinkAuth', meta: { linkStep: 'auth' } },
        ),
        false,
    );
    assert.equal(
        needsNicknameSetup(
            { nickname: null },
            { name: 'accountLinkDone', meta: { linkStep: 'done' } },
        ),
        false,
    );
});
