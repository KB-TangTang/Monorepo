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
    stub.setMessagesResponse({
        messages: [{ messageId: 90, content: '더 오래된 것' }],
        hasMore: false,
    });

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

/*
 * 이슈 #174 Task 12: 화면 연결과 목업 제거.
 * Task 11 이 빌드를 살리려고 넣어 둔 로컬 발송 스텁(local-<timestamp> messageId)을 걷어내고
 * 실 STOMP 송수신에 맞춰 store 를 다시 짠다. 아래부터는 그 계약을 확인한다.
 */

test('입장하면 방 정보와 최근 메시지를 불러오고 안 읽은 수를 지운다', async () => {
    const store = newStore();
    stub.setRoomInfoResponse({
        groupId: 7,
        groupName: '절약단',
        status: 'ACTIVE',
        memberCount: 4,
        unreadCount: 2,
    });
    stub.setMessagesResponse({ messages: [{ messageId: 1 }], hasMore: false });

    await store.enterRoom(7);

    assert.equal(store.messages.length, 1);
    assert.deepEqual(stub.resetUnreadCalls, [7]);
    assert.equal(store.roomInfo.groupName, '절약단');
    assert.equal(store.closed, false);
    assert.equal(store.error, null);
});

test('메시지가 없는 새 방도 정상 진입한다', async () => {
    const store = newStore();
    stub.setMessagesResponse({ messages: [], hasMore: false });

    await store.enterRoom(7);

    assert.deepEqual(store.messages, []);
    assert.equal(store.closed, false);
    assert.equal(store.error, null);
});

test('수신한 메시지를 뒤에 붙인다', () => {
    const store = newStore();
    store.messages = [{ messageId: 1 }];

    store.appendMessage({ messageId: 2 });

    assert.deepEqual(
        store.messages.map((m) => m.messageId),
        [1, 2],
    );
});

test('이미 있는 messageId 는 중복으로 넣지 않는다', () => {
    const store = newStore();
    store.messages = [{ messageId: 1 }];

    store.appendMessage({ messageId: 1 });

    assert.equal(store.messages.length, 1);
});

test('재연결하면 마지막 messageId 이후를 보충한다', async () => {
    const store = newStore();
    store.groupId = 7;
    store.messages = [{ messageId: 4 }];
    stub.setMessagesResponse({ messages: [{ messageId: 5 }], hasMore: false });

    await store.catchUp();

    assert.deepEqual(stub.messagesCalls, [[7, { after: 4 }]]);
    assert.deepEqual(
        store.messages.map((m) => m.messageId),
        [4, 5],
    );
});

test('재연결 보충으로 받은 메시지도 중복이면 무시한다', async () => {
    const store = newStore();
    store.groupId = 7;
    store.messages = [{ messageId: 4 }];
    stub.setMessagesResponse({ messages: [{ messageId: 4 }, { messageId: 5 }], hasMore: false });

    await store.catchUp();

    assert.deepEqual(
        store.messages.map((m) => m.messageId),
        [4, 5],
    );
});

/*
 * http.js 응답 인터셉터는 실패를 `{ response: { data: { code } } }` 가 아니라
 * `ApiError(code, message, status)` (code 를 직접 들고 있는 Error) 로 정규화해 던진다
 * (src/api/http.js 참고, src/stores/personalMission.js 의 error.code 사용도 같은 패턴).
 * 그래서 store 는 e.code 를 본다.
 */
test('종료된 챌린지는 closed 플래그를 세운다', async () => {
    const store = newStore();
    const err = new Error('종료된 챌린지의 대화는 볼 수 없어요.');
    err.code = 'CHAT_ROOM_CLOSED';
    stub.setRoomInfoError(err);

    await store.enterRoom(7);

    assert.equal(store.closed, true);
    assert.equal(store.error, null);
    assert.equal(store.loading, false);
});

test('참여자가 아니면 error 상태를 세우고 closed 는 세우지 않는다', async () => {
    const store = newStore();
    const err = new Error('이 챌린지의 참여자가 아니에요.');
    err.code = 'CHAT_NOT_MEMBER';
    stub.setRoomInfoError(err);

    await store.enterRoom(7);

    assert.equal(store.closed, false);
    assert.equal(store.error.code, 'CHAT_NOT_MEMBER');
});

test('leaveRoom 은 groupId 를 포함한 모든 상태를 초기화한다', async () => {
    const store = newStore();
    stub.setMessagesResponse({ messages: [{ messageId: 1 }], hasMore: true });
    await store.enterRoom(7);

    store.leaveRoom();

    assert.equal(store.groupId, null);
    assert.deepEqual(store.messages, []);
    assert.equal(store.roomInfo, null);
    assert.equal(store.hasMore, false);
    assert.equal(store.closed, false);
    assert.equal(store.error, null);
});

/*
 * 최종 리뷰 C2: 화면이 삭제된 목업 스키마(messageType · createdAt · senderName · challengeName)를
 * 보는 동안 서버는 {type, sentAt, senderNickname, groupName} 을 보내고 있었다. 스토어가 매핑 없이
 * 그대로 넣어 텍스트 메시지가 한 건도 렌더링되지 않았다.
 * 아래 두 테스트는 REST 경로와 소켓 경로가 **같은 어댑터**를 지나는지를 확인한다.
 */
test('REST 로 받은 메시지는 화면 모델로 정규화된다', async () => {
    const store = newStore();
    stub.setMessagesResponse({
        messages: [
            {
                messageId: 1,
                type: 'TEXT',
                senderId: 3,
                senderNickname: '절약왕',
                content: '안녕',
                sentAt: '2026-08-16T12:34:56',
            },
        ],
        hasMore: false,
    });

    await store.enterRoom(7);

    const message = store.messages[0];
    assert.equal(message.senderName, '절약왕');
    assert.equal(message.isSystem, false);
    assert.ok(message.sentAt instanceof Date);
    assert.equal(message.sentAt.getMonth(), 7); // 8월
    assert.equal(message.sentAt.getDate(), 16);
});

test('소켓으로 받은 메시지도 같은 어댑터를 지난다', () => {
    const store = newStore();

    store.appendMessage({
        messageId: 9,
        type: 'SYSTEM',
        senderId: null,
        senderNickname: null,
        content: '재판이 열렸어요',
        sentAt: '2026-08-16T09:00:00',
    });

    const message = store.messages[0];
    assert.equal(message.isSystem, true);
    assert.equal(message.senderName, '');
    assert.ok(message.sentAt instanceof Date);
});

/* 서버 상태값은 RECRUITING · ACTIVE · JUDGING · CLOSED 다. 목업의 'ENDED' 는 존재하지 않는다 */
test('isEnded 는 서버 상태 CLOSED 에서만 참이다', async () => {
    const store = newStore();
    stub.setRoomInfoResponse({ groupId: 7, groupName: '절약단', status: 'ACTIVE', memberCount: 3 });
    await store.enterRoom(7);
    assert.equal(store.isEnded, false);

    stub.setRoomInfoResponse({ groupId: 7, groupName: '절약단', status: 'JUDGING', memberCount: 3 });
    await store.enterRoom(7);
    assert.equal(store.isEnded, false, 'JUDGING 은 대화가 가장 활발한 구간이라 입력을 막지 않는다');

    stub.setRoomInfoResponse({ groupId: 7, groupName: '절약단', status: 'CLOSED', memberCount: 3 });
    await store.enterRoom(7);
    assert.equal(store.isEnded, true);
});
