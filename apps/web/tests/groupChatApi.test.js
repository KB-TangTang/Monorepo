import test from 'node:test';
import assert from 'node:assert/strict';
import { registerHooks } from 'node:module';

const STUB_URL = new URL('./stubs/httpStub.js', import.meta.url).href;

registerHooks({
    resolve(specifier, context, nextResolve) {
        if (specifier === '@/api/http') {
            return { url: STUB_URL, shortCircuit: true };
        }
        return nextResolve(specifier, context);
    },
});

const stub = await import('./stubs/httpStub.js');
const { fetchChatMessages, fetchChatRoomInfo, resetUnreadCount } = await import(
    '../src/api/groupChat.js'
);

test('방 정보를 조회한다', async () => {
    stub.reset();

    await fetchChatRoomInfo(7);

    assert.deepEqual(stub.calls, [{ method: 'get', args: ['/groups/7/chat/room'] }]);
});

test('커서가 없으면 limit 만 보낸다', async () => {
    stub.reset();

    await fetchChatMessages(7, {});

    assert.deepEqual(stub.calls, [
        { method: 'get', args: ['/groups/7/chat/messages', { params: { limit: 50 } }] },
    ]);
});

test('before 커서를 그대로 전달한다', async () => {
    stub.reset();

    await fetchChatMessages(7, { before: 21 });

    assert.deepEqual(stub.calls, [
        {
            method: 'get',
            args: ['/groups/7/chat/messages', { params: { limit: 50, before: 21 } }],
        },
    ]);
});

test('after 커서를 그대로 전달한다 (재연결 보충)', async () => {
    stub.reset();

    await fetchChatMessages(7, { after: 10 });

    assert.deepEqual(stub.calls, [
        {
            method: 'get',
            args: ['/groups/7/chat/messages', { params: { limit: 50, after: 10 } }],
        },
    ]);
});

test('before 와 after 를 함께 주면 요청 전에 막는다', async () => {
    stub.reset();

    await assert.rejects(() => fetchChatMessages(7, { before: 21, after: 10 }));
    assert.deepEqual(stub.calls, []);
});

test('limit 이 0 이하이면 최소값 1 로 보정한다', async () => {
    stub.reset();

    await fetchChatMessages(7, { limit: 0 });

    assert.deepEqual(stub.calls, [
        { method: 'get', args: ['/groups/7/chat/messages', { params: { limit: 1 } }] },
    ]);
});

test('limit 이 100 을 넘으면 100 으로 보정한다', async () => {
    stub.reset();

    await fetchChatMessages(7, { limit: 500 });

    assert.deepEqual(stub.calls, [
        { method: 'get', args: ['/groups/7/chat/messages', { params: { limit: 100 } }] },
    ]);
});

test('안 읽은 수를 초기화한다', async () => {
    stub.reset();

    await resetUnreadCount(7);

    assert.deepEqual(stub.calls, [{ method: 'post', args: ['/groups/7/chat/read'] }]);
});
