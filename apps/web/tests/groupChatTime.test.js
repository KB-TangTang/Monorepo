import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

import { clockLabel, isGroupedMessage, shouldShowTime } from '@/utils/groupChat';

/*
 * 같은 사람이 연달아 보낸 메시지에 시간이 매 줄 찍히던 것을 고쳤다.
 * 시간은 "같은 사람 · 같은 분" 묶음의 마지막 줄에만 남는다(카카오톡 방식).
 *
 * 이름·아바타 숨김(5분 창)과 기준이 다르다는 점이 이 수정의 핵심이라 두 함수를 함께 검증한다.
 */

const at = (iso) => new Date(iso);

function msg(overrides = {}) {
    return {
        messageId: 1,
        isSystem: false,
        senderId: 19,
        senderName: '재한',
        content: '안녕',
        sentAt: at('2026-08-17T10:00:10'),
        ...overrides,
    };
}

test('같은 사람이 같은 분에 연달아 보내면 마지막 줄에만 시간이 남는다', () => {
    const first = msg({ messageId: 1, sentAt: at('2026-08-17T10:00:10') });
    const second = msg({ messageId: 2, sentAt: at('2026-08-17T10:00:47') });

    assert.equal(shouldShowTime(first, second), false, '앞줄의 시간은 숨겨야 한다');
    assert.equal(shouldShowTime(second, null), true, '묶음의 마지막 줄에는 시간을 남긴다');
});

test('분이 바뀌면 다시 시간을 보여준다', () => {
    const first = msg({ messageId: 1, sentAt: at('2026-08-17T10:00:59') });
    const second = msg({ messageId: 2, sentAt: at('2026-08-17T10:01:02') });

    assert.equal(shouldShowTime(first, second), true, '3초 차이라도 분이 바뀌면 보여준다');
});

test('5분 창으로 묶으면 안 된다 — 같은 묶음이어도 분이 다르면 시간은 남는다', () => {
    const first = msg({ messageId: 1, sentAt: at('2026-08-17T10:00:10') });
    const second = msg({ messageId: 2, sentAt: at('2026-08-17T10:02:10') });

    assert.equal(isGroupedMessage(first, second), true, '이름·아바타는 5분 창이라 숨는다');
    assert.equal(shouldShowTime(first, second), true, '그래도 시간은 남아야 정보가 줄지 않는다');
});

test('보낸 사람이 다르면 시간을 보여준다', () => {
    const mine = msg({ messageId: 1, senderId: 19 });
    const other = msg({ messageId: 2, senderId: 20, sentAt: at('2026-08-17T10:00:30') });

    assert.equal(shouldShowTime(mine, other), true);
});

test('senderId 타입이 달라도 같은 사람으로 본다 (문자열 · 숫자 혼용)', () => {
    const first = msg({ messageId: 1, senderId: '19', sentAt: at('2026-08-17T10:00:10') });
    const second = msg({ messageId: 2, senderId: 19, sentAt: at('2026-08-17T10:00:40') });

    assert.equal(shouldShowTime(first, second), false);
});

test('다음이 시스템 메시지면 시간을 남긴다 — 묶음이 거기서 끊긴다', () => {
    const mine = msg({ messageId: 1 });
    const system = msg({ messageId: 2, isSystem: true, sentAt: at('2026-08-17T10:00:30') });

    assert.equal(shouldShowTime(mine, system), true);
});

test('다음 메시지가 없으면(목록의 마지막) 시간을 남긴다', () => {
    assert.equal(shouldShowTime(msg(), null), true);
});

test('시각이 없는 메시지는 빈 시간 자리를 만들지 않는다', () => {
    assert.equal(shouldShowTime(msg({ sentAt: null }), null), false);
});

test('다음 메시지의 시각이 없으면 이 줄의 시간은 남긴다', () => {
    const first = msg({ messageId: 1 });
    const broken = msg({ messageId: 2, sentAt: null });

    assert.equal(shouldShowTime(first, broken), true);
});

test('날짜가 다르면 시·분이 같아도 다른 묶음이다', () => {
    const yesterday = msg({ messageId: 1, sentAt: at('2026-08-16T10:00:10') });
    const today = msg({ messageId: 2, sentAt: at('2026-08-17T10:00:10') });

    assert.equal(shouldShowTime(yesterday, today), true);
});

/* ── 시각 문자열 ───────────────────────────────────────────────────── */

/*
 * 말풍선과 재판 알림 필이 같은 함수를 쓴다. 사본을 두면 한쪽만 12시간제가 되거나
 * 한쪽만 0 을 안 채우는 식으로 조용히 갈라진다.
 */
test('시각은 0 을 채운 24시간제다', () => {
    assert.equal(clockLabel(at('2026-08-17T09:05:00')), '09:05');
    assert.equal(clockLabel(at('2026-08-17T23:59:00')), '23:59');
    assert.equal(clockLabel(at('2026-08-17T00:00:00')), '00:00', '자정을 12:00 으로 적지 않는다');
});

test('시각이 없으면 빈 문자열이다 — NaN:NaN 을 그리지 않는다', () => {
    assert.equal(clockLabel(null), '');
    assert.equal(clockLabel(undefined), '');
});

/* ── 화면 연결 계약 (렌더링 하네스가 없어 소스 텍스트로 확인한다) ───── */

function read(path) {
    return readFileSync(new URL(path, import.meta.url), 'utf8');
}

test('말풍선은 showTime 이 참일 때만 시간을 그린다', () => {
    const src = read('../src/components/challenge/group/chat/GroupChatBubble.vue');

    assert.match(
        src,
        /showTime:\s*\{\s*type:\s*Boolean,\s*default:\s*true\s*\}/,
        'showTime prop 이 있어야 한다 (단독으로 쓰면 기존처럼 시간이 보이도록 기본값 true)',
    );
    const timeSpans = src.match(/class="bubble-row__time"/g) ?? [];
    assert.equal(timeSpans.length, 2, '시간 span 은 내 것·상대 것 두 개다');
    assert.equal(
        (src.match(/v-if="isMine && showTime"|v-if="!isMine && showTime"/g) ?? []).length,
        2,
        '두 시간 span 모두 showTime 으로 막아야 한다',
    );
});

test('화면은 다음 메시지를 보고 showTime 을 넘긴다', () => {
    const src = read('../src/views/challenge/group/GroupChatView.vue');

    assert.match(src, /shouldShowTime/, 'GroupChatView 가 shouldShowTime 판정을 써야 한다');
    assert.match(src, /:show-time="item\.showTime"/, 'GroupChatBubble 에 show-time 을 넘겨야 한다');
    assert.ok(
        !/function isGrouped\(/.test(src),
        '묶기 규칙은 utils/groupChat.js 한 곳에 둔다 — 화면에 사본을 남기지 않는다',
    );
});
