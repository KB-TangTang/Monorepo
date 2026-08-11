import test from 'node:test';
import assert from 'node:assert/strict';
import {
    NICKNAME_MAX_LENGTH,
    needsNicknameSetup,
    resolveDisplayName,
    resolveOnboardingRedirect,
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

/* ── 온보딩 체인 (DECISIONS.md 2026-08-11 (7)) ────
 * `로그인 → 서비스동의 → 금융동의 → 계좌연동 → 닉네임 설정 → 홈`.
 * 라우터를 띄우지 않고 순수 함수로만 검증한다.
 */
const route = (name, meta = {}) => ({ name, meta });

/** 아무것도 안 끝낸 신규 사용자. */
const NEW_USER = {
    user: { nickname: null },
    needsConsent: true,
    needsFinancialConsent: true,
    needsAccountLink: true,
};

test('온보딩은 동의 → 금융동의 → 계좌연동 → 닉네임 순서로 나온다', () => {
    /* 홈에 가려 해도 남은 첫 단계로 되돌린다. 앞 단계가 끝날 때마다 다음 단계가 나온다. */
    assert.equal(resolveOnboardingRedirect(NEW_USER, HOME), 'consent');
    assert.equal(
        resolveOnboardingRedirect({ ...NEW_USER, needsConsent: false }, HOME),
        'financialConsent',
    );
    assert.equal(
        resolveOnboardingRedirect(
            { ...NEW_USER, needsConsent: false, needsFinancialConsent: false },
            HOME,
        ),
        'accountLinkInstitutions',
    );
    assert.equal(
        resolveOnboardingRedirect(
            {
                ...NEW_USER,
                needsConsent: false,
                needsFinancialConsent: false,
                needsAccountLink: false,
            },
            HOME,
        ),
        'nicknameSetup',
    );
});

test('온보딩을 마친 사용자는 아무 데도 보내지 않는다', () => {
    assert.equal(
        resolveOnboardingRedirect(
            {
                user: { nickname: '탕탕이' },
                needsConsent: false,
                needsFinancialConsent: false,
                needsAccountLink: false,
            },
            HOME,
        ),
        null,
    );
});

test('사용자 정보가 아직 없으면 모든 단계를 통과시킨다 — 부팅 직후 엉뚱하게 튕기지 않게', () => {
    assert.equal(resolveOnboardingRedirect({ ...NEW_USER, user: null }, HOME), null);
    assert.equal(resolveOnboardingRedirect({}, HOME), null);
    assert.equal(resolveOnboardingRedirect(null, HOME), null);
});

test('각 단계는 자기 화면에서 발동하지 않는다 — 아니면 무한 리다이렉트가 난다', () => {
    assert.equal(resolveOnboardingRedirect(NEW_USER, route('consent')), null);
    assert.equal(
        resolveOnboardingRedirect({ ...NEW_USER, needsConsent: false }, route('financialConsent')),
        null,
    );
    assert.equal(
        resolveOnboardingRedirect(
            { ...NEW_USER, needsConsent: false, needsFinancialConsent: false },
            route('accountLinkInstitutions'),
        ),
        null,
    );
    assert.equal(
        resolveOnboardingRedirect(
            {
                ...NEW_USER,
                needsConsent: false,
                needsFinancialConsent: false,
                needsAccountLink: false,
            },
            route('nicknameSetup'),
        ),
        null,
    );
});

test('앞 단계 화면에 있으면 뒤 단계는 발동하지 않는다 — 동의 중인 사용자를 계좌연동으로 끌어내지 않는다', () => {
    /* 셋 다 남은 사용자가 동의 화면에 있으면 그대로 둔다(자기 단계라 면제). */
    assert.equal(resolveOnboardingRedirect(NEW_USER, route('consent')), null);
    /* 금융동의 화면에 있는 사용자에게 계좌연동·닉네임 게이트가 끼어들지 않는다. */
    assert.equal(
        resolveOnboardingRedirect({ ...NEW_USER, needsConsent: false }, route('financialConsent')),
        null,
    );
});

test('뒤 단계 화면으로는 건너뛸 수 없다 — 앞 단계 게이트가 그대로 발동한다', () => {
    assert.equal(resolveOnboardingRedirect(NEW_USER, route('financialConsent')), 'consent');
    assert.equal(resolveOnboardingRedirect(NEW_USER, route('accountLinkInstitutions')), 'consent');
    assert.equal(resolveOnboardingRedirect(NEW_USER, route('nicknameSetup')), 'consent');
    /* 닉네임 화면에 있어도 계좌 연동이 남았으면 연동이 먼저다. */
    assert.equal(
        resolveOnboardingRedirect(
            { ...NEW_USER, needsConsent: false, needsFinancialConsent: false },
            route('nicknameSetup'),
        ),
        'accountLinkInstitutions',
    );
});

test('계좌 연동 게이트는 연동 플로우(meta.linkStep) 전체에서 면제된다', () => {
    const linking = { ...NEW_USER, needsConsent: false, needsFinancialConsent: false };
    /* 1단계(기관 선택)에는 linkStep 이 없고, 2~5단계는 linkStep 으로 판별한다. */
    assert.equal(resolveOnboardingRedirect(linking, route('accountLinkInstitutions')), null);
    assert.equal(
        resolveOnboardingRedirect(linking, route('accountLinkAuth', { linkStep: 'auth' })),
        null,
    );
    assert.equal(
        resolveOnboardingRedirect(linking, route('accountLinkProgress', { linkStep: 'progress' })),
        null,
    );
    assert.equal(
        resolveOnboardingRedirect(linking, route('accountLinkSelect', { linkStep: 'select' })),
        null,
    );
    assert.equal(
        resolveOnboardingRedirect(linking, route('accountLinkDone', { linkStep: 'done' })),
        null,
    );
});

test('닉네임 게이트도 연동 플로우에서 면제된다 — 연동을 끝내기 전에 튕겨나가면 안 된다', () => {
    const afterLink = {
        user: { nickname: null },
        needsConsent: false,
        needsFinancialConsent: false,
        needsAccountLink: false,
    };
    assert.equal(
        resolveOnboardingRedirect(afterLink, route('accountLinkDone', { linkStep: 'done' })),
        null,
    );
    assert.equal(resolveOnboardingRedirect(afterLink, HOME), 'nicknameSetup');
});
