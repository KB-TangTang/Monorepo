import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

import { toChatMessage } from '@/api/groupChatAdapter';
import { verdictMetaParts, verdictStampKind } from '@/utils/groupChat';

/*
 * 판결 확정 채팅 카드 (이슈 #304).
 *
 * 오래 중립 「판결」 도장만 찍혔다. 서버가 요약 문구 한 줄만 줘서 승패를 알 수 없었기 때문이다.
 * 이제 VerdictConfirmed 이벤트가 결과를 싣는다. 여기서 지키는 것은 셋이다.
 *
 *   1. 도장은 outcome 으로 고른다 — 문구를 파싱해 고르면 문구 한 글자에 도장이 뒤집힌다
 *   2. 없는 표를 지어내지 않는다 — 혐의 인정(null)과 무투표(0:0)는 표 줄을 그리지 않는다
 *   3. 옛 메시지(verdict 없음)도 그려진다 — Redis 에 TTL 이 남은 동안 계속 조회된다
 */

function source(path) {
    return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

/* ── 도장 ────────────────────────────────────────────────────────── */

test('도장은 서버가 준 outcome 으로 고른다', () => {
    assert.equal(verdictStampKind({ outcome: 'GUILTY' }), 'GUILTY');
    assert.equal(verdictStampKind({ outcome: 'INNOCENT' }), 'INNOCENT');
});

test('결과가 없는 옛 판결 메시지는 중립 도장으로 남는다', () => {
    assert.equal(verdictStampKind(null), null, 'verdict 자체가 없는 메시지');
    assert.equal(verdictStampKind(undefined), null);
    assert.equal(verdictStampKind({}), null, 'outcome 만 빠진 경우도 중립이다');
    assert.equal(verdictStampKind({ outcome: 'PENDING' }), null, '모르는 값을 유죄로 넘기지 않는다');
});

/* ── 「투표 4:2 · 목숨 1 차감」 ───────────────────────────────────── */

test('투표로 갈린 유죄는 표와 차감된 목숨을 함께 적는다', () => {
    assert.deepEqual(verdictMetaParts({ outcome: 'GUILTY', guiltyVotes: 4, innocentVotes: 2, livesLost: 1 }), [
        '투표 4:2',
        '목숨 1 차감',
    ]);
});

test('무죄는 표만 적는다 — 깎인 목숨이 없다', () => {
    assert.deepEqual(
        verdictMetaParts({ outcome: 'INNOCENT', guiltyVotes: 2, innocentVotes: 4, livesLost: 0 }),
        ['투표 2:4'],
    );
});

test('혐의 인정은 표 줄을 그리지 않는다 — 투표라는 절차가 없었다', () => {
    assert.deepEqual(
        verdictMetaParts({ outcome: 'GUILTY', guiltyVotes: null, innocentVotes: null, livesLost: 1 }),
        ['목숨 1 차감'],
    );
});

test('아무도 투표하지 않은 무죄 추정은 「투표 0:0」 을 적지 않는다', () => {
    assert.deepEqual(
        verdictMetaParts({ outcome: 'INNOCENT', guiltyVotes: 0, innocentVotes: 0, livesLost: 0 }),
        [],
        '그릴 조각이 없으면 줄 자체가 사라져야 한다',
    );
});

/*
 * 남은 목숨이 0이던 피고가 유죄를 받으면 서버가 livesLost 0 을 준다
 * (GroupVerdictTransitionService 가 문구도 「남은 목숨이 없어요」로 가른다).
 * 여기서 「목숨 1 차감」을 적으면 같은 카드 안에서 문구와 수치가 정반대가 된다.
 */
test('남은 목숨이 없어 못 깎은 유죄는 목숨 줄을 적지 않는다', () => {
    assert.deepEqual(
        verdictMetaParts({ outcome: 'GUILTY', guiltyVotes: 3, innocentVotes: 1, livesLost: 0 }),
        ['투표 3:1'],
    );
});

test('결과가 없는 옛 메시지에는 수치 줄이 없다', () => {
    assert.deepEqual(verdictMetaParts(null), []);
});

/* ── 어댑터가 넘겨주는 모양 ──────────────────────────────────────── */

test('어댑터가 verdict 를 그대로 옮긴다', () => {
    const message = toChatMessage({
        messageId: 9,
        type: 'SYSTEM',
        content: '절약왕님 재판 - 유죄. 목숨 1개가 차감됐어요.',
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

test('어댑터는 표의 0 과 null 을 뭉개지 않는다', () => {
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

    assert.equal(confession.verdict.guiltyVotes, null, '없는 투표를 0 으로 만들면 카드가 표를 지어낸다');
    assert.equal(noVote.verdict.guiltyVotes, 0, '실제로 0표인 것은 0 으로 남아야 한다');
});

test('verdict 가 없는 메시지는 null 로 정규화된다', () => {
    const message = toChatMessage({ messageId: 3, type: 'SYSTEM', systemType: 'VERDICT_CONFIRMED' });

    assert.equal(message.verdict, null);
});

/* ── 카드가 실제로 그 값을 쓰는지 ────────────────────────────────── */

test('카드는 문구가 아니라 outcome 으로 도장을 고른다', () => {
    const src = source('src/components/challenge/group/chat/GroupChatVerdictCard.vue');

    assert.match(src, /guilty_stamp\.png/, '유죄 인장 자산을 써야 한다');
    assert.match(src, /innocent_stamp\.png/, '무죄 인장 자산을 써야 한다');
    assert.match(src, /verdictStampKind|verdictMetaParts/, '표시 규칙은 utils 에서 가져다 쓴다');
    assert.doesNotMatch(
        src,
        /content\s*\.\s*(includes|indexOf|match)/,
        '문구를 파싱해 승패를 정하면 문구 한 글자에 도장이 뒤집힌다',
    );
});

test('결과가 없으면 중립 도장이 그대로 남는다', () => {
    const src = source('src/components/challenge/group/chat/GroupChatVerdictCard.vue');

    assert.match(src, /v-else[\s\S]{0,200}판결/, 'stampImage 가 없을 때 그릴 중립 도장이 남아 있어야 한다');
});
