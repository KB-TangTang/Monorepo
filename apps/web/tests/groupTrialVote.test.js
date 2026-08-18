import test from 'node:test';
import assert from 'node:assert/strict';
import { canVote, toVoteScreen } from '../src/utils/groupTrial.js';

/*
 * `toTrialDetailViewModel` 을 이미 지난 모양이다. `accused.isMine` · `defense.images` 처럼
 * 그 함수가 붙여 주는 이름을 쓴다 — 서버 원본 이름(`mine` · `imageUrls`)으로 적으면
 * 실제로는 통과하지 않는 경로를 테스트하게 된다.
 */
function detail(overrides = {}) {
    return {
        indictmentId: 42,
        groupId: 3,
        status: 'VOTING',
        accused: { userId: 9, nickname: '지판', profileImage: null, isMine: false },
        limitAmount: 25000,
        currentAmount: 37400,
        exceededAmount: 12400,
        voteDeadline: '2026-08-19T02:00:00',
        defense: null,
        myVerdict: null,
        voteCount: 2,
        totalVoters: 5,
        guiltyCount: null,
        innocentCount: null,
        comments: null,
        ...overrides,
    };
}

function withDefense(overrides = {}) {
    return detail({
        defense: {
            content: '팀 회식비를 제가 먼저 결제했어요',
            actualBurdenAmount: 8000,
            deductionAmount: 29400,
            images: ['https://cdn/receipt.jpg', 'https://cdn/transfer.png'],
            createdAt: '2026-08-18T20:32:00',
            ...overrides,
        },
    });
}

/* ══ 진입 조건 ══════════════════════════════════════════════ */

test('투표 중이고 아직 안 던진 배심원만 투표 화면에 들어간다', () => {
    assert.equal(canVote(detail()), true);
});

test('피고 본인은 자기 재판에 투표할 수 없다', () => {
    const mine = detail({ accused: { userId: 9, nickname: '나', isMine: true } });

    assert.equal(canVote(mine), false);
});

test('이미 던졌으면 다시 들어가지 않는다 — 투표 수정은 지원하지 않는다', () => {
    assert.equal(canVote(detail({ myVerdict: 'GUILTY' })), false);
    assert.equal(canVote(detail({ myVerdict: 'INNOCENT' })), false);
});

test('변론 대기·판결 확정 상태는 투표 대상이 아니다', () => {
    for (const status of ['DEFENSE_WAIT', 'GUILTY', 'INNOCENT']) {
        assert.equal(canVote(detail({ status })), false, status);
    }
    assert.equal(canVote(null), false);
});

/*
 * 개표 배치(#172)가 없어 마감이 지나도 상태는 `VOTING` 그대로다. 여기서 마감 시각까지 보면
 * 화면은 막는데 서버는 받는 구간이 생긴다 — 마감 판단은 서버 한 곳(`VoteService`)에만 둔다.
 */
test('마감 시각이 지나도 화면에서 미리 막지 않는다', () => {
    assert.equal(canVote(detail({ voteDeadline: '2020-01-01T00:00:00' })), true);
});

/* ══ 뷰모델 매핑 ════════════════════════════════════════════ */

test('변론 내용과 증거를 화면 어휘로 옮긴다', () => {
    const screen = toVoteScreen(withDefense());

    assert.equal(screen.hasDefense, true);
    assert.equal(screen.defendant.nickname, '지판');
    assert.equal(screen.defenseMessage, '팀 회식비를 제가 먼저 결제했어요');
    assert.equal(screen.actualCostAmount, 8000);
    assert.equal(screen.defenseDate, '2026-08-18T20:32:00');
    assert.deepEqual(screen.evidences, [
        { id: 1, label: '증 제1호', url: 'https://cdn/receipt.jpg' },
        { id: 2, label: '증 제2호', url: 'https://cdn/transfer.png' },
    ]);
});

/*
 * 변론 마감 배치가 변론 없이 상태만 `VOTING` 으로 넘긴 재판이다(#170 리뷰 6절).
 * 화면이 여기서 깨지면 그 재판은 아무도 투표하지 못해 영원히 끝나지 않는다.
 */
test('변론 없이 투표로 넘어간 재판도 매핑된다', () => {
    const screen = toVoteScreen(detail());

    assert.equal(screen.hasDefense, false);
    assert.equal(screen.defenseMessage, '');
    assert.deepEqual(screen.evidences, []);
    assert.equal(screen.defendant.nickname, '지판');
});

/*
 * **0 으로 채우지 않는다.** 변론이 없는데 실제 부담금을 0 으로 적으면 변론서에
 * 「실제 부담금 0원 인정 시 기준 내 · 25,000원 여유」라는 피고가 하지 않은 주장이 생긴다.
 */
test('변론이 없으면 실제 부담금은 0 이 아니라 null 이다', () => {
    assert.equal(toVoteScreen(detail()).actualCostAmount, null);
    assert.equal(toVoteScreen(detail()).defenseDate, null);
});

test('실제 부담금 0원은 「없음」과 구분된다', () => {
    const screen = toVoteScreen(withDefense({ actualBurdenAmount: 0 }));

    assert.equal(screen.actualCostAmount, 0);
    assert.equal(screen.hasDefense, true);
});

test('이미지 없이 글만 낸 변론은 증거 목록이 비어 있다', () => {
    const screen = toVoteScreen(withDefense({ images: [] }));

    assert.equal(screen.hasDefense, true);
    assert.deepEqual(screen.evidences, []);
});

/* ══ 개표 전 비공개 ═════════════════════════════════════════ */

/*
 * 투표 중에는 서버가 셋 다 `null` 로 내려준다. 매핑이 `?? 0` · `?? []` 로 채우면
 * 투표 화면이 「0 : 0」을 그려 만장일치 0표처럼 보이고, 개표 전 비율 비공개 정책이 무너진다.
 */
test('개표 전 표 분포와 코멘트는 null 그대로 통과한다', () => {
    const screen = toVoteScreen(detail());

    assert.equal(screen.guiltyCount, null);
    assert.equal(screen.innocentCount, null);
    assert.equal(screen.comments, null);
});

test('개표 후에는 숫자와 코멘트가 그대로 통과한다', () => {
    const screen = toVoteScreen(detail({
        status: 'GUILTY',
        guiltyCount: 4,
        innocentCount: 1,
        comments: [{ comment: '이번엔 좀 심했어', createdAt: '2026-08-19T10:00:00' }],
    }));

    assert.equal(screen.guiltyCount, 4);
    assert.equal(screen.innocentCount, 1);
    assert.equal(screen.comments.length, 1);
    /* 코멘트에 투표자를 특정할 값이 붙지 않는다. 붙으면 익명 코멘트가 곧 표 공개가 된다. */
    assert.deepEqual(Object.keys(screen.comments[0]).sort(), ['comment', 'createdAt']);
});

test('상세가 붙여 둔 표시용 값과 진행 상황은 그대로 남는다', () => {
    const screen = toVoteScreen(detail());

    assert.equal(screen.voteCount, 2);
    assert.equal(screen.totalVoters, 5);
    assert.equal(screen.indictmentId, 42);
    assert.equal(toVoteScreen(null), null);
});
