import test from 'node:test';
import assert from 'node:assert/strict';

import { toChatMessage, toChatMessagePage, toChatRoom } from '../src/api/groupChatAdapter.js';

/*
 * 최종 리뷰 C2: 서버 DTO → 화면 모델 정규화. 이 어댑터가 REST·소켓 두 경로의 유일한 관문이다.
 * 서버 계약은 docs/API_SPEC.md 「그룹 채팅」 절이다.
 */

test('메시지: 서버 필드명을 화면 모델로 옮긴다', () => {
    const message = toChatMessage({
        messageId: 12,
        type: 'TEXT',
        senderId: 3,
        senderNickname: '절약왕',
        content: '안녕',
        sentAt: '2026-08-16T12:34:56',
    });

    assert.equal(message.messageId, 12);
    assert.equal(message.type, 'TEXT');
    assert.equal(message.isSystem, false);
    assert.equal(message.senderId, 3);
    assert.equal(message.senderName, '절약왕');
    assert.equal(message.content, '안녕');
    assert.ok(message.sentAt instanceof Date);
    assert.equal(message.sentAt.getHours(), 12);
});

test('메시지: SYSTEM 은 isSystem 이 참이고 발신자가 비어 있다', () => {
    const message = toChatMessage({
        messageId: 3,
        type: 'SYSTEM',
        senderId: null,
        senderNickname: null,
        content: '재판이 열렸어요',
        sentAt: '2026-08-16T09:00:00',
    });

    assert.equal(message.isSystem, true);
    assert.equal(message.senderId, null);
    assert.equal(message.senderName, '');
});

/* 서버가 ISO 문자열을 보내도록 고쳤지만(WebSocketConfig#jsonConverter), 옛 숫자 배열도 견딘다 */
test('메시지: sentAt 이 숫자 배열이어도 Date 로 읽는다', () => {
    const message = toChatMessage({ messageId: 1, type: 'TEXT', sentAt: [2026, 8, 16, 12, 34, 56] });

    assert.ok(message.sentAt instanceof Date);
    assert.equal(message.sentAt.getFullYear(), 2026);
    assert.equal(message.sentAt.getMonth(), 7);
    assert.equal(message.sentAt.getDate(), 16);
});

test('메시지: 해석할 수 없는 sentAt 은 null 이다 (NaN 이 화면으로 새지 않는다)', () => {
    assert.equal(toChatMessage({ messageId: 1, sentAt: undefined }).sentAt, null);
    assert.equal(toChatMessage({ messageId: 1, sentAt: '알 수 없음' }).sentAt, null);
});

/* catchUp 은 REST 응답을 appendMessage 로 흘려보낸다 — 두 번 지나도 같은 결과여야 한다 */
test('메시지: 어댑터를 두 번 통과해도 결과가 같다', () => {
    const once = toChatMessage({
        messageId: 5,
        type: 'TEXT',
        senderId: 3,
        senderNickname: '절약왕',
        content: '안녕',
        sentAt: '2026-08-16T12:34:56',
    });
    const twice = toChatMessage(once);

    assert.equal(twice.senderName, '절약왕');
    assert.equal(twice.content, '안녕');
    assert.deepEqual(twice.sentAt, once.sentAt);
});

test('페이지: hasMore 가 없으면 false 다', () => {
    const page = toChatMessagePage({ messages: [{ messageId: 1, type: 'TEXT' }] });

    assert.equal(page.messages.length, 1);
    assert.equal(page.hasMore, false);
});

test('방: groupName·status 를 그대로 쓰고 상태 파생값을 만든다', () => {
    const room = toChatRoom({
        groupId: 7,
        groupName: '절약단',
        status: 'JUDGING',
        memberCount: 4,
        unreadCount: 2,
    });

    assert.equal(room.groupName, '절약단');
    assert.equal(room.memberCount, 4);
    assert.equal(room.unreadCount, 2);
    assert.equal(room.isJudging, true);
    assert.equal(room.isEnded, false);
});

test('방: CLOSED 만 종료 상태다 (목업의 ENDED 는 서버에 없다)', () => {
    assert.equal(toChatRoom({ status: 'CLOSED' }).isEnded, true);
    assert.equal(toChatRoom({ status: 'ENDED' }).isEnded, false);
    assert.equal(toChatRoom({ status: 'ACTIVE' }).isEnded, false);
});

/*
 * 일차·D-day 는 서버(Asia/Seoul)가 계산해 준다. 프론트에서 날짜를 세면 기기 시계·시간대에 따라
 * 값이 달라진다. 여기서 확인하는 것은 "서버 값을 그대로 옮기는가" 이지 계산 결과가 아니다.
 */
test('방: 서버가 준 일차·D-day 를 그대로 옮긴다', () => {
    const room = toChatRoom({
        groupId: 7, groupName: '절약단', status: 'ACTIVE', memberCount: 4,
        dayIndex: 3, daysLeft: 4,
    });

    assert.equal(room.dayIndex, 3);
    assert.equal(room.daysLeft, 4);
    /* 목업 시절 이름은 여전히 서버 계약에 없다 */
    assert.equal(room.currentDay, undefined);
    assert.equal(room.challengeName, undefined);
});

test('방: 일차·D-day 가 없는 응답도 0 으로 받아 화면이 표기를 생략한다', () => {
    const room = toChatRoom({ groupId: 7, groupName: '절약단', status: 'ACTIVE', memberCount: 4 });

    assert.equal(room.dayIndex, 0);
    assert.equal(room.daysLeft, 0);
});

test('메시지: 시스템 필드(종류·딥링크·사건번호)를 그대로 옮긴다', () => {
    const message = toChatMessage({
        messageId: 9, type: 'SYSTEM', content: '재판이 열렸어요',
        systemType: 'TRIAL_OPENED', deepLink: '/challenge/group/7/trial/55', caseNo: '2026-재판-0055',
    });

    assert.equal(message.systemType, 'TRIAL_OPENED');
    assert.equal(message.deepLink, '/challenge/group/7/trial/55');
    assert.equal(message.caseNo, '2026-재판-0055');
});

test('메시지: 시스템 필드가 없던 시절 메시지는 null 로 복원된다', () => {
    const message = toChatMessage({ messageId: 1, type: 'SYSTEM', content: '재판이 열렸어요' });

    assert.equal(message.systemType, null);
    assert.equal(message.deepLink, null);
    assert.equal(message.caseNo, null);
});

/*
 * 판결 결과 (이슈 #304). 화면은 이 값으로만 유죄·무죄를 가른다 — 문구를 파싱하면 문구
 * 한 글자에 색이 뒤집힌다. 표는 0 과 null 을 구분해야 한다.
 * (필 표시 규칙 자체는 tests/groupChatTrialPill.test.js 에 있다)
 */
test('메시지: 판결 결과를 그대로 옮긴다', () => {
    const message = toChatMessage({
        messageId: 9,
        type: 'SYSTEM',
        content: '절약왕님, 유죄예요. 목숨 1개가 차감됐어요.',
        sentAt: '2026-08-19T09:00:00',
        systemType: 'VERDICT_CONFIRMED',
        verdict: { outcome: 'GUILTY', guiltyVotes: 4, innocentVotes: 2, livesLost: 1 },
    });

    assert.deepEqual(message.verdict, {
        outcome: 'GUILTY',
        guiltyVotes: 4,
        innocentVotes: 2,
        livesLost: 1,
    });
});

test('메시지: 판결 표의 0 과 null 을 뭉개지 않는다', () => {
    const confession = toChatMessage({
        type: 'SYSTEM',
        messageId: 1,
        verdict: { outcome: 'GUILTY', guiltyVotes: null, innocentVotes: null, livesLost: 1 },
    });
    const noVote = toChatMessage({
        type: 'SYSTEM',
        messageId: 2,
        verdict: { outcome: 'INNOCENT', guiltyVotes: 0, innocentVotes: 0, livesLost: 0 },
    });

    assert.equal(confession.verdict.guiltyVotes, null, '없는 투표를 0 으로 만들면 표를 지어내게 된다');
    assert.equal(noVote.verdict.guiltyVotes, 0, '실제로 0표인 것은 0 으로 남아야 한다');
});

test('메시지: 결과가 없는 옛 판결 메시지는 verdict 가 null 이다', () => {
    const message = toChatMessage({ messageId: 3, type: 'SYSTEM', systemType: 'VERDICT_CONFIRMED' });

    assert.equal(message.verdict, null);
});
