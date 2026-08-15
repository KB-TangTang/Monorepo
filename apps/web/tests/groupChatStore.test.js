import test from 'node:test';
import assert from 'node:assert/strict';
import { registerHooks } from 'node:module';
import { createPinia, setActivePinia } from 'pinia';

/*
 * stores/groupChat.js 는 `@/api/groupChat` 을 통해 `@/api/http`(axios + import.meta.env)까지
 * 내려간다. Vite 밖(node --test)에서는 import.meta.env 가 undefined 라 그대로 두면 터진다.
 * `@/api/groupChat` 을 이 파일 안에서만 대역으로 갈아끼운다(tests/tutorialGuide.test.js 와 같은 방식).
 */
const STUB_URL = new URL('./stubs/groupChatApiStub.js', import.meta.url).href;

registerHooks({
    resolve(specifier, context, nextResolve) {
        if (specifier === '@/api/groupChat') {
            return { url: STUB_URL, shortCircuit: true };
        }
        return nextResolve(specifier, context);
    },
});

const stub = await import(STUB_URL);
const { useGroupChatStore } = await import('../src/stores/groupChat.js');

function newStore() {
    setActivePinia(createPinia());
    stub.reset();
    return useGroupChatStore();
}

/*
 * 회귀 테스트: fetchChatMessages 가 개수(offset)가 아니라 커서(before)를 받도록 바뀌었는데
 * (이슈 #174 Task 11), loadOlderMessages 가 옛 시그니처(messages.value.length)로 부르고
 * 있었다. 그러면 before/after 가 전부 undefined 가 돼 "최근 메시지"를 다시 받아 맨 위에
 * 붙이는 조용한 회귀가 난다 — 빌드도 테스트도 잡아주지 않는 종류라 이 호출부를 직접 본다.
 */
test('스크롤-업은 가장 오래된 메시지의 messageId 를 before 로 보낸다', async () => {
    const store = newStore();
    store.messages = [
        { messageId: 101, content: '오래된 메시지' },
        { messageId: 102, content: '그 다음' },
    ];
    store.hasMore = true;
    stub.setMessagesResponse({ messages: [{ messageId: 90, content: '더 오래된 것' }], hasMore: false });

    await store.loadOlderMessages(7);

    assert.deepEqual(stub.messagesCalls, [[7, { before: 101 }]]);
    assert.deepEqual(
        store.messages.map((m) => m.messageId),
        [90, 101, 102],
    );
    assert.equal(store.hasMore, false);
});

test('로드된 메시지가 없으면 커서 없이 요청한다', async () => {
    const store = newStore();
    store.messages = [];
    store.hasMore = true;
    stub.setMessagesResponse({ messages: [], hasMore: false });

    await store.loadOlderMessages(7);

    assert.deepEqual(stub.messagesCalls, [[7, {}]]);
});

test('hasMore 가 false 면 요청하지 않는다', async () => {
    const store = newStore();
    store.messages = [{ messageId: 101 }];
    store.hasMore = false;

    await store.loadOlderMessages(7);

    assert.deepEqual(stub.messagesCalls, []);
});
