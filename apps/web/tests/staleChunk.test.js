import test from 'node:test';
import assert from 'node:assert/strict';
import {
    clearStaleChunkFlag,
    installStaleChunkRecovery,
    isStaleChunkError,
    recoverFromStaleChunk,
} from '../src/utils/staleChunk.js';

/*
 * 이슈 #320 - 재배포로 사라진 청크에서 복구한다.
 *
 * 실제 증상은 배포본에서 났다. 탭을 열어 둔 사이 재배포가 되자 그룹 챌린지 상세는
 * 멀쩡히 보이는데 채팅 아이콘을 눌러도 아무 반응이 없었다. 콘솔에만
 * 「Failed to fetch dynamically imported module」 가 남았다.
 */

function fakeStorage(initial = {}) {
    const data = { ...initial };
    return {
        getItem: (key) => data[key] ?? null,
        setItem: (key, value) => {
            data[key] = String(value);
        },
        removeItem: (key) => {
            delete data[key];
        },
        get size() {
            return Object.keys(data).length;
        },
    };
}

function spyReload() {
    const calls = [];
    const reload = (path) => calls.push(path);
    reload.calls = calls;
    return reload;
}

/* ── 오류 판정 ────────────────────────────────────────── */

test('브라우저별 청크 오류 문구를 모두 알아본다', () => {
    const messages = [
        'Failed to fetch dynamically imported module: https://x/assets/GroupChatView-D8j1Y9OC.js',
        'Importing a module script failed.',
        'error loading dynamically imported module',
        'Unable to preload CSS for /assets/GroupChatView-Dzs74q_g.css',
    ];

    for (const message of messages) {
        assert.equal(isStaleChunkError(new Error(message)), true, message);
    }
});

test('청크와 무관한 오류는 새로고침 대상이 아니다', () => {
    assert.equal(isStaleChunkError(new Error('Network Error')), false);
    assert.equal(isStaleChunkError(new Error('참여자가 아닙니다.')), false);
    assert.equal(isStaleChunkError(null), false);
    assert.equal(isStaleChunkError(undefined), false);
    assert.equal(isStaleChunkError({}), false);
});

/* ── 복구 · 무한 루프 방지 ────────────────────────────── */

test('청크 오류면 목적지를 그대로 다시 연다', () => {
    const storage = fakeStorage();
    const reload = spyReload();

    const handled = recoverFromStaleChunk(
        new Error('Failed to fetch dynamically imported module'),
        '/group-challenges/1/chat',
        { storage, reload },
    );

    assert.equal(handled, true);
    assert.deepEqual(reload.calls, ['/group-challenges/1/chat']);
});

test('다른 오류는 새로고침하지 않는다 - 진짜 오류를 새로고침으로 덮으면 안 된다', () => {
    const storage = fakeStorage();
    const reload = spyReload();

    const handled = recoverFromStaleChunk(new Error('Network Error'), '/x', { storage, reload });

    assert.equal(handled, false);
    assert.equal(reload.calls.length, 0);
    assert.equal(storage.size, 0, '가드 플래그도 남기지 않는다');
});

/*
 * 가드가 없으면 진짜로 파일이 없을 때 새로고침이 무한히 돈다.
 * 새로고침해도 같은 오류가 나는 상황을 그대로 재현한다.
 */
test('청크가 정말 없으면 두 번째부터는 새로고침하지 않는다', () => {
    const storage = fakeStorage();
    const reload = spyReload();
    const error = new Error('Failed to fetch dynamically imported module');

    assert.equal(recoverFromStaleChunk(error, '/a', { storage, reload }), true);
    assert.equal(recoverFromStaleChunk(error, '/a', { storage, reload }), false);
    assert.equal(recoverFromStaleChunk(error, '/a', { storage, reload }), false);

    assert.deepEqual(reload.calls, ['/a'], '새로고침은 한 번뿐이다');
});

test('정상 이동에 한 번 성공하면 가드가 풀려 다음 배포에서도 복구된다', () => {
    const storage = fakeStorage();
    const reload = spyReload();
    const error = new Error('Failed to fetch dynamically imported module');

    recoverFromStaleChunk(error, '/a', { storage, reload });
    clearStaleChunkFlag(storage);
    recoverFromStaleChunk(error, '/b', { storage, reload });

    assert.deepEqual(reload.calls, ['/a', '/b']);
});

/* ── 설치 ─────────────────────────────────────────────── */

function fakeRouter() {
    const router = {
        errorHandlers: [],
        afterHooks: [],
        onError: (handler) => router.errorHandlers.push(handler),
        afterEach: (hook) => router.afterHooks.push(hook),
    };
    return router;
}

function fakeWindow(pathname = '/group-challenges/1') {
    const listeners = {};
    return {
        location: { pathname, search: '', hash: '' },
        addEventListener: (type, handler) => {
            (listeners[type] ??= []).push(handler);
        },
        emit: (type, event) => (listeners[type] ?? []).forEach((handler) => handler(event)),
        has: (type) => (listeners[type] ?? []).length > 0,
    };
}

test('Vite 의 vite:preloadError 를 잡아 현재 자리를 다시 연다', () => {
    const router = fakeRouter();
    const target = fakeWindow('/group-challenges/1');
    const storage = fakeStorage();
    const reload = spyReload();
    let prevented = false;

    installStaleChunkRecovery(router, { storage, reload, target });
    assert.equal(target.has('vite:preloadError'), true, 'Vite 공식 이벤트를 구독해야 한다');

    target.emit('vite:preloadError', {
        payload: new Error('Failed to fetch dynamically imported module'),
        preventDefault: () => {
            prevented = true;
        },
    });

    assert.deepEqual(reload.calls, ['/group-challenges/1']);
    assert.equal(prevented, true, '기본 throw 를 막아 콘솔을 어지럽히지 않는다');
});

/*
 * 실제로 난 증상이다. 상세 화면은 이미 로드돼 있어 보이는데,
 * 채팅으로 넘어가는 순간 그 청크를 처음 받으면서 실패했다.
 */
test('라우터 이동 중 청크 오류는 목적지로 다시 연다', () => {
    const router = fakeRouter();
    const target = fakeWindow('/group-challenges/1');
    const storage = fakeStorage();
    const reload = spyReload();

    installStaleChunkRecovery(router, { storage, reload, target });
    assert.equal(router.errorHandlers.length, 1, 'router.onError 훅이 없으면 조용히 죽는다');

    router.errorHandlers[0](new Error('Failed to fetch dynamically imported module'), {
        fullPath: '/group-challenges/1/chat',
    });

    assert.deepEqual(reload.calls, ['/group-challenges/1/chat']);
});

test('정상 이동이 끝나면 설치된 afterEach 가 가드를 푼다', () => {
    const router = fakeRouter();
    const target = fakeWindow();
    const storage = fakeStorage({ 'tt-stale-chunk-reloaded': '1' });
    const reload = spyReload();

    installStaleChunkRecovery(router, { storage, reload, target });
    assert.equal(router.afterHooks.length, 1);

    router.afterHooks[0]();

    assert.equal(storage.size, 0);
});
