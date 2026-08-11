import test from 'node:test';
import assert from 'node:assert/strict';
import { createPinia, setActivePinia } from 'pinia';
import { isBackendUnreachable } from '../src/utils/auth.js';
import { useAuthStore } from '../src/stores/auth.js';

test('네트워크 오류(status 0)는 백엔드 미도달로 본다', () => {
    assert.equal(isBackendUnreachable({ code: 'NETWORK_ERROR', status: 0 }), true);
});

test('5xx 는 백엔드 미도달로 본다 — 배포 중 Vercel 이 502 를 돌려준다', () => {
    assert.equal(isBackendUnreachable({ code: 'NETWORK_ERROR', status: 502 }), true);
    assert.equal(isBackendUnreachable({ status: 500 }), true);
    assert.equal(isBackendUnreachable({ status: 503 }), true);
});

test('401 은 정상적인 비로그인이므로 미도달이 아니다', () => {
    assert.equal(isBackendUnreachable({ code: 'UNAUTHORIZED', status: 401 }), false);
});

test('4xx 업무 오류도 미도달이 아니다', () => {
    assert.equal(isBackendUnreachable({ code: 'REFRESH_TOKEN_REUSED', status: 400 }), false);
    assert.equal(isBackendUnreachable({ status: 404 }), false);
});

test('status 가 없는 값에는 반응하지 않는다', () => {
    assert.equal(isBackendUnreachable(undefined), false);
    assert.equal(isBackendUnreachable(null), false);
    assert.equal(isBackendUnreachable({}), false);
    assert.equal(isBackendUnreachable(new Error('boom')), false);
});

/*
 * mergeUser 는 개별 API 응답(예: PATCH /users/me/name)을 세션 전체를 다시 받지 않고 반영한다.
 * 이게 없으면 저장은 됐는데 화면은 옛 이름을 계속 보여준다. (DECISIONS.md 2026-08-11)
 */
test('mergeUser 는 넘긴 필드만 갈아끼운다', () => {
    setActivePinia(createPinia());
    const auth = useAuthStore();
    auth.setSession({
        accessToken: 'token',
        user: { id: 1, nickname: 'jaehan', name: '장재한', email: 'a@b.com' },
    });

    auth.mergeUser({ name: '홍길동' });
    assert.deepEqual(auth.user, {
        id: 1,
        nickname: 'jaehan',
        name: '홍길동',
        email: 'a@b.com',
    });
    /* 토큰·동의 여부는 사용자 정보가 아니므로 건드리지 않는다. */
    assert.equal(auth.accessToken, 'token');
});

test('사용자 정보가 없을 때 mergeUser 는 그 값으로 채운다', () => {
    setActivePinia(createPinia());
    const auth = useAuthStore();
    auth.mergeUser({ name: '홍길동' });
    assert.deepEqual(auth.user, { name: '홍길동' });
});

/*
 * 온보딩 게이트 플래그 3종은 완전히 같은 취급이다 — 재발급 응답의 같은 자리에 실려 온다.
 * (DECISIONS.md 2026-08-11 (7))
 */
function newAuthStore() {
    setActivePinia(createPinia());
    return useAuthStore();
}

test('setSession 은 게이트 플래그 3종을 함께 채운다', () => {
    const auth = newAuthStore();
    auth.setSession({
        accessToken: 'token',
        user: { id: 1 },
        needsConsent: true,
        needsFinancialConsent: true,
        needsAccountLink: true,
    });

    assert.equal(auth.needsConsent, true);
    assert.equal(auth.needsFinancialConsent, true);
    assert.equal(auth.needsAccountLink, true);
});

test('응답에 플래그가 없으면 미완료가 아니라 false 로 본다 — 서버가 말하지 않은 것을 막지 않는다', () => {
    const auth = newAuthStore();
    auth.setSession({ accessToken: 'token', user: { id: 1 } });

    assert.equal(auth.needsConsent, false);
    assert.equal(auth.needsFinancialConsent, false);
    assert.equal(auth.needsAccountLink, false);
});

test('clear 는 게이트 플래그도 되돌린다 — 남으면 다음 로그인 사용자에게 새어나간다', () => {
    const auth = newAuthStore();
    auth.setSession({
        accessToken: 'token',
        user: { id: 1 },
        needsConsent: true,
        needsFinancialConsent: true,
        needsAccountLink: true,
    });

    auth.clear();
    assert.equal(auth.needsConsent, false);
    assert.equal(auth.needsFinancialConsent, false);
    assert.equal(auth.needsAccountLink, false);
});

test('applyOnboardingFlags 는 응답에 실려 온 플래그만 갈아끼운다', () => {
    const auth = newAuthStore();
    auth.setSession({
        accessToken: 'token',
        user: { id: 1 },
        needsConsent: true,
        needsFinancialConsent: true,
        needsAccountLink: true,
    });

    /* FINANCIAL 저장 응답: 동의 두 개는 끝났고 계좌 연동은 아직이다. */
    auth.applyOnboardingFlags({ needsConsent: false, needsFinancialConsent: false });
    assert.equal(auth.needsConsent, false);
    assert.equal(auth.needsFinancialConsent, false);
    assert.equal(auth.needsAccountLink, true, '응답에 없는 플래그는 건드리지 않는다');

    /* 계좌 연결 저장 성공: 이 플래그만 내린다. 안 내리면 가드가 연동 화면으로 되돌려보낸다. */
    auth.applyOnboardingFlags({ needsAccountLink: false });
    assert.equal(auth.needsAccountLink, false);
});

test('applyOnboardingFlags 는 철회 응답처럼 일부만 오는 값도 안전하게 다룬다', () => {
    const auth = newAuthStore();
    auth.setSession({ accessToken: 'token', user: { id: 1 }, needsFinancialConsent: true });

    /* 철회 응답에는 needsConsent 만 있다. undefined 를 false 로 읽으면 게이트가 뚫린다. */
    auth.applyOnboardingFlags({ needsConsent: true });
    assert.equal(auth.needsConsent, true);
    assert.equal(auth.needsFinancialConsent, true);

    auth.applyOnboardingFlags(undefined);
    assert.equal(auth.needsConsent, true);
    assert.equal(auth.needsFinancialConsent, true);
});
