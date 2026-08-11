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
