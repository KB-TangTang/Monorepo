import test from 'node:test';
import assert from 'node:assert/strict';
import { isBackendUnreachable } from '../src/utils/auth.js';

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
