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
/* 채팅방 진입이 멤버 프로필 이미지를 받으러 그룹 상세도 부른다 — 그쪽도 http.js 로 내려간다 */
const GROUP_STUB_URL = new URL('./stubs/groupChallengeApiStub.js', import.meta.url).href;

registerHooks({
    resolve(specifier, context, nextResolve) {
        if (specifier === '@/api/groupChat') {
            return { url: STUB_URL, shortCircuit: true };
        }
        if (specifier === '@/api/groupChallenge') {
            return { url: GROUP_STUB_URL, shortCircuit: true };
        }
        return nextResolve(specifier, context);
    },
});

const stub = await import(STUB_URL);
const groupStub = await import(GROUP_STUB_URL);
const { useGroupChatStore } = await import('../src/stores/groupChat.js');

function newStore() {
    setActivePinia(createPinia());
    stub.reset();
    groupStub.reset();
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

    stub.setRoomInfoResponse({
        groupId: 7,
        groupName: '절약단',
        status: 'JUDGING',
        memberCount: 3,
    });
    await store.enterRoom(7);
    assert.equal(store.isEnded, false, 'JUDGING 은 대화가 가장 활발한 구간이라 입력을 막지 않는다');

    stub.setRoomInfoResponse({ groupId: 7, groupName: '절약단', status: 'CLOSED', memberCount: 3 });
    await store.enterRoom(7);
    assert.equal(store.isEnded, true);
});

/*
 * 채팅 아바타에 프로필 이미지가 붙은 적이 없었다(#407). 프로필을 바꿔도 채팅방만 이니셜 원으로
 * 남았다. 메시지에 이미지 URL 을 실으면 Redis 에 발송 시점 값이 굳어 같은 증상이 재현되므로,
 * 메시지는 senderId 만 싣고 이미지는 멤버 목록에서 찾는다. 그 연결을 여기서 고정한다.
 */
test('채팅방에 들어가면 멤버 프로필을 발신자 id 로 찾을 수 있다', async () => {
    const store = newStore();
    groupStub.setGroupDetailResponse({
        members: [
            { userId: 7, nickname: '탕이', profileImage: 'https://cdn/7.png' },
            { userId: 9, nickname: '재판장', profileImage: null },
        ],
    });

    await store.enterRoom(3);

    assert.deepEqual(groupStub.groupDetailCalls, [3]);
    assert.equal(store.imageOf(7), 'https://cdn/7.png');
    // 이미지가 없는 멤버·모르는 발신자·시스템 메시지(senderId 없음)는 전부 null 이어야 한다
    assert.equal(store.imageOf(9), null);
    assert.equal(store.imageOf(999), null);
    assert.equal(store.imageOf(null), null);
});

test('발신자 id 는 문자열로 와도 같은 이미지를 찾는다', async () => {
    /* 소켓 경로의 senderId 가 문자열로 오는 일이 있어 화면이 Number() 로 비교하고 있다. */
    const store = newStore();
    groupStub.setGroupDetailResponse({
        members: [{ userId: 7, nickname: '탕이', profileImage: 'https://cdn/7.png' }],
    });

    await store.enterRoom(3);

    assert.equal(store.imageOf('7'), 'https://cdn/7.png');
});

test('멤버 조회가 실패해도 채팅은 그대로 뜬다', async () => {
    /*
     * 이미지는 없으면 이니셜로 떨어지는 장식이다. 여기서 오류를 올리면 곁가지 때문에
     * 대화 전체가 안 보이게 된다 — 그 회귀를 막는다.
     */
    const store = newStore();
    stub.setMessagesResponse({ messages: [{ messageId: 1, content: '안녕' }], hasMore: false });
    groupStub.setGroupDetailError(Object.assign(new Error('boom'), { code: 'GROUP_NOT_FOUND' }));

    await store.enterRoom(3);

    assert.equal(store.error, null);
    assert.equal(store.closed, false);
    assert.equal(store.messages.length, 1);
    assert.equal(store.imageOf(7), null);
});

test('채팅방을 나가면 멤버 표를 비운다', async () => {
    /* 다음 방에 들어갔을 때 이전 방 멤버의 이미지가 남아 엉뚱한 얼굴이 뜨는 것을 막는다. */
    const store = newStore();
    groupStub.setGroupDetailResponse({
        members: [{ userId: 7, nickname: '탕이', profileImage: 'https://cdn/7.png' }],
    });
    await store.enterRoom(3);
    assert.equal(store.imageOf(7), 'https://cdn/7.png');

    store.leaveRoom();

    assert.equal(store.imageOf(7), null);
});

/*
 * 닉네임도 메시지에 실려 온 값이 아니라 멤버 목록에서 찾는다 (#414).
 * senderNickname 은 Redis 에 저장된 발송 시점 값이라, 닉네임을 바꿔도 과거 메시지가
 * 옛 이름으로 남았다. 이미지와 같은 뿌리의 문제다.
 */
test('닉네임도 발신자 id 로 지금 값을 찾는다', async () => {
    const store = newStore();
    groupStub.setGroupDetailResponse({
        members: [{ userId: 7, nickname: '바뀐닉', profileImage: null }],
    });

    await store.enterRoom(3);

    assert.equal(store.nicknameOf(7), '바뀐닉');
    assert.equal(store.nicknameOf('7'), '바뀐닉');
});

test('멤버 목록에 없는 발신자는 null 이다 — 화면이 메시지의 옛 이름으로 되돌아간다', async () => {
    /*
     * 나간 참여자·삭제된 계정의 과거 메시지가 「익명」이 되면 안 된다.
     * 못 찾을 때 null 을 주어야 버블이 message.senderName 으로 되돌아간다.
     */
    const store = newStore();
    groupStub.setGroupDetailResponse({ members: [{ userId: 7, nickname: '탕이' }] });

    await store.enterRoom(3);

    assert.equal(store.nicknameOf(999), null);
    assert.equal(store.nicknameOf(null), null);
});

/* ── 방 밖에서 보는 채팅 요약 (이슈 #423) ────────────── */

/*
 * 채팅은 접근성이 나빴다 — 목록 화면까지 세 번 들어가야 대화가 보였다.
 * 그래서 홈 행과 지방법원 토글이 「안 읽은 개수 · 마지막 한 줄」을 직접 그리게 했고,
 * 그 값은 이 스토어가 앱 단위로 들고 있다. 아래는 그 값이 살아남는 조건들이다.
 */

test('요약은 목록 응답으로 통째로 교체된다 — 나간 그룹이 남지 않는다', () => {
    const store = newStore();
    store.syncChatSummary([{ id: 1, unreadChatCount: 3, lastChatMessage: '준서: 참았다' }]);
    /* 2번 그룹만 남은 응답. 1번이 병합으로 살아남으면 이미 끝난 방의 배지가 계속 뜬다 */
    store.syncChatSummary([{ id: 2, unreadChatCount: 1, lastChatMessage: '민지: 편의점' }]);

    assert.equal(store.unreadByGroup[1], undefined);
    assert.equal(store.unreadByGroup[2], 1);
    assert.equal(store.lastMessageByGroup[1], undefined);
});

test('실시간 채팅 알림은 개수를 올리고 마지막 줄을 갈아끼운다', () => {
    const store = newStore();
    store.syncChatSummary([{ id: 5, unreadChatCount: 2, lastChatMessage: '유현: 예전 말' }]);

    store.receiveChatAlert({ groupId: 5, senderNickname: '민지', content: '지금 말' });

    assert.equal(store.unreadByGroup[5], 3);
    assert.equal(store.lastMessageByGroup[5], '민지: 지금 말');
});

test('알림은 문자열 groupId 로 와도 같은 방에 쌓인다', () => {
    /* SSE 본문은 JSON 이라 서버가 Long 을 문자열로 흘려도 이상하지 않다 */
    const store = newStore();
    store.receiveChatAlert({ groupId: '9', senderNickname: '탕이', content: '안녕' });

    assert.equal(store.unreadByGroup[9], 1);
});

test('지금 열어 둔 방의 알림은 무시한다 — 보고 있는 대화에 배지가 붙으면 안 된다', async () => {
    const store = newStore();
    await store.enterRoom(4);

    store.receiveChatAlert({ groupId: 4, senderNickname: '민지', content: '보고 있다' });

    assert.equal(store.unreadByGroup[4] ?? 0, 0);
    assert.equal(store.hasUnreadChat, false);
});

test('채팅방에 들어가면 그 방 배지가 내려간다', async () => {
    /* 서버 카운터는 resetUnreadCount 가 0 으로 만든다. 화면 사본만 남으면 배지가 안 꺼진다 */
    const store = newStore();
    store.syncChatSummary([{ id: 6, unreadChatCount: 7, lastChatMessage: '하은: 왔다' }]);

    await store.enterRoom(6);

    assert.equal(store.unreadByGroup[6], 0);
    assert.equal(store.hasUnreadChat, false);
});

test('leaveRoom 은 요약을 지우지 않는다 — 방 밖에서 읽는 값이다', async () => {
    /*
     * 여기가 이 기능의 급소다. 요약을 방 상태와 같이 초기화하면 채팅방을 한 번 열었다 닫는
     * 것만으로 다른 방들의 배지가 통째로 사라진다 — 화면에서는 「그냥 안 뜨네」로 보인다.
     */
    const store = newStore();
    store.syncChatSummary([{ id: 8, unreadChatCount: 4, lastChatMessage: '준서: 남아야 한다' }]);
    await store.enterRoom(3);

    store.leaveRoom();

    assert.equal(store.unreadByGroup[8], 4);
    assert.equal(store.lastMessageByGroup[8], '준서: 남아야 한다');
});

test('로그아웃은 요약을 비운다 — 다음 사람에게 앞사람 대화가 보이면 안 된다', () => {
    const store = newStore();
    store.syncChatSummary([{ id: 8, unreadChatCount: 4, lastChatMessage: '준서: 비밀' }]);

    store.clearChatSummary();

    assert.deepEqual(store.unreadByGroup, {});
    assert.deepEqual(store.lastMessageByGroup, {});
    assert.equal(store.hasUnreadChat, false);
});

test('씨 뿌리기는 시작 전 그룹까지 물어본다', async () => {
    /*
     * `['ACTIVE']` 만 물으면 정작 대화가 가장 활발한 방이 빠진다. 채팅에는 상태 제한이 없고
     * (ChatMessageService 에 status 검사가 없다) 서버가 시작일을 반드시 내일 이후로 막아서
     * **만들고 나서 처음 떠드는 구간이 통째로 RECRUITING** 이기 때문이다.
     * 실제로 이 한 줄 때문에 배지도 점도 안 뜨는 상태였다.
     */
    const store = newStore();
    groupStub.setMyChallengesResponse([{ id: 2, unreadChatCount: 5, lastChatMessage: '민지: 야' }]);

    await store.refreshChatSummary();

    assert.deepEqual(groupStub.myChallengeCalls, [['ACTIVE', 'RECRUITING']]);
    assert.equal(store.unreadByGroup[2], 5);
});

test('씨 뿌리기가 실패해도 조용히 넘어간다', async () => {
    /* 점 하나 때문에 로그인 직후 첫 화면이 막히면 안 된다 */
    const store = newStore();
    groupStub.setMyChallengesError(new Error('네트워크'));

    await store.refreshChatSummary();

    assert.deepEqual(store.unreadByGroup, {});
});
