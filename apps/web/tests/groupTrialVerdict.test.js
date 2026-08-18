import test from 'node:test';
import assert from 'node:assert/strict';
import {
    isAiVerdict,
    isVerdictConfirmed,
    toTrialProgress,
    toVerdictScreen,
    verdictRouteName,
} from '../src/utils/groupTrial.js';

/**
 * `toTrialDetailViewModel` 을 지난 뒤의 모양. 확정된 재판이라 개표 값이 채워져 있다.
 * 서버는 `guiltyCount`·`innocentCount`·`aiVerdictReason` 을 개표 뒤에만 내려보낸다(이슈 #171·#172).
 */
function confirmedDetail(overrides = {}) {
    return {
        indictmentId: 11,
        status: 'GUILTY',
        result: true,
        verdictMethod: 'VOTE',
        aiVerdictReason: null,
        evalLabel: '일일결산',
        settlementLabel: '8월 5일',
        caseNumber: '2026-재판-0805',
        challengeName: '배달 소비 줄이기',
        currentAmount: 37400,
        limitAmount: 25000,
        guiltyCount: 4,
        innocentCount: 2,
        defense: { actualBurdenAmount: 8000 },
        createdAt: '2026-08-05T20:14:00',
        ...overrides,
    };
}

test('개표 전 재판은 판결 화면 모양을 만들지 않는다', () => {
    const voting = confirmedDetail({
        status: 'VOTING',
        verdictMethod: null,
        guiltyCount: null,
        innocentCount: null,
    });

    assert.equal(isVerdictConfirmed(voting), false);
    assert.equal(toVerdictScreen(voting), null);
    /* 갈 곳이 없다 → 호출부가 진행 현황으로 되돌린다. */
    assert.equal(verdictRouteName(voting), null);
});

test('다수결로 끝난 재판은 판결문으로, 동률 재판은 동점 안내로 간다', () => {
    assert.equal(verdictRouteName(confirmedDetail()), 'verdictResult');
    assert.equal(
        verdictRouteName(confirmedDetail({ verdictMethod: 'NO_VOTE', status: 'INNOCENT' })),
        'verdictResult',
    );
    assert.equal(
        verdictRouteName(confirmedDetail({ verdictMethod: 'AI_JUDGMENT' })),
        'groupVoteTie',
    );
});

test('유죄와 AI 유죄는 status 로 구분되지 않는다 — verdictMethod 를 봐야 한다', () => {
    const byVote = confirmedDetail({ verdictMethod: 'VOTE' });
    const byAi = confirmedDetail({ verdictMethod: 'AI_JUDGMENT' });

    assert.equal(byVote.status, byAi.status);
    assert.equal(isAiVerdict(byVote), false);
    assert.equal(isAiVerdict(byAi), true);
});

test('판결 화면 모양은 표수·금액·유무죄 어휘를 함께 담는다', () => {
    const screen = toVerdictScreen(confirmedDetail());

    assert.equal(screen.outcome, 'GUILTY');
    assert.equal(screen.label, '유죄');
    assert.equal(screen.guiltyVotes, 4);
    assert.equal(screen.innocentVotes, 2);
    assert.equal(screen.evaluationLabel, '일일결산');
    assert.equal(screen.detail.judgmentDate, '8월 5일');
    assert.equal(screen.detail.currentAmount, 37400);
    assert.equal(screen.detail.limitAmount, 25000);
    assert.equal(screen.detail.actualCostAmount, 8000);
});

test('AI 판결은 서버 사유 원문을 그대로 쓴다', () => {
    const screen = toVerdictScreen(
        confirmedDetail({
            verdictMethod: 'AI_JUDGMENT',
            aiVerdictReason: '변론의 실제 부담금 주장이 거래내역과 맞지 않아 유죄로 판결합니다.',
        }),
    );

    assert.equal(
        screen.detail.reason,
        '변론의 실제 부담금 주장이 거래내역과 맞지 않아 유죄로 판결합니다.',
    );
    /* 타이핑 애니메이션도 같은 문장을 읽는다 — 고정 문구를 넣으면 매번 같은 말을 한다. */
    assert.equal(screen.reviewMessage, screen.detail.reason);
});

test('AI 가 아닌 판결은 방식이 곧 사유다 — 서버에 문장이 없다', () => {
    const noVote = toVerdictScreen(
        confirmedDetail({
            status: 'INNOCENT',
            verdictMethod: 'NO_VOTE',
            innocentCount: 0,
            guiltyCount: 0,
        }),
    );

    assert.match(noVote.detail.reason, /무죄 추정/);
    assert.equal(noVote.guiltyVotes, 0);
    assert.equal(noVote.innocentVotes, 0);
});

test('변론이 없는 재판의 실제 부담금은 0원이 아니라 「없음」이다', () => {
    const screen = toVerdictScreen(confirmedDetail({ defense: null }));

    assert.equal(screen.detail.actualCostAmount, null);
});

test('진행 현황이 「판결 보기」가 열 화면을 미리 정해 둔다', () => {
    /* `status` 가 화면 어휘(VERDICT_DONE)로 덮이기 때문에 여기서 정해 두지 않으면 다시 못 판단한다. */
    const byVote = toTrialProgress(confirmedDetail());
    const byAi = toTrialProgress(confirmedDetail({ verdictMethod: 'AI_JUDGMENT' }));
    const voting = toTrialProgress(
        confirmedDetail({
            status: 'VOTING',
            verdictMethod: null,
            guiltyCount: null,
            innocentCount: null,
        }),
    );

    assert.equal(byVote.status, 'VERDICT_DONE');
    assert.equal(byVote.verdictRoute, 'verdictResult');
    assert.equal(byAi.verdictRoute, 'groupVoteTie');
    assert.equal(voting.verdictRoute, null);
});
