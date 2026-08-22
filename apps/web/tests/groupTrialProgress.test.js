import test from 'node:test';
import assert from 'node:assert/strict';
import { toTrialProgress, formatDeadlineLabel } from '../src/utils/groupTrial.js';

/*
 * 오늘 날짜 기준 `2026-08-05T22:00:00`.
 *
 * 픽스처에 절대시각을 박으면 하루 뒤 테스트가 깨진다. **`toISOString()` 을 쓰지 않는다.**
 * UTC 로 바뀌면서 오프셋이 사라지는데, 서버가 주는 `LocalDateTime` 은 오프셋 없는 현지 시각이고
 * `new Date('...T22:00:00')` 도 현지 시각으로 읽는다. UTC 로 만들면 KST 기준 9시간이 밀린다.
 */
function localAt(days, hour, minute) {
    const at = new Date();
    at.setDate(at.getDate() + days);
    at.setHours(hour, minute, 0, 0);

    const pad = (n) => String(n).padStart(2, '0');
    return `${at.getFullYear()}-${pad(at.getMonth() + 1)}-${pad(at.getDate())}`
        + `T${pad(at.getHours())}:${pad(at.getMinutes())}:00`;
}

function todayAt(hour, minute) {
    return localAt(0, hour, minute);
}

function afterDays(days, hour, minute) {
    return localAt(days, hour, minute);
}

/** `toTrialDetailViewModel` 을 지난 뒤의 모양. 서버 상태 어휘를 그대로 쓴다. */
function detail(overrides = {}) {
    return {
        indictmentId: 42,
        groupId: 3,
        status: 'DEFENSE_WAIT',
        createdAt: todayAt(20, 14),
        voteDeadline: afterDays(1, 2, 0),
        defense: null,
        voteCount: 0,
        totalVoters: 3,
        caseNumber: '2026-재판-0805',
        evalLabel: '일일결산',
        ...overrides,
    };
}

test('변론 대기(DEFENSE_WAIT)를 화면의 기소 접수 단계로 옮긴다', () => {
    const progress = toTrialProgress(detail());

    assert.equal(progress.status, 'INDICTED');
    assert.equal(progress.serverStatus, 'DEFENSE_WAIT');
    assert.equal(progress.steps.indictedAt, '20:14');
    assert.equal(progress.steps.defenseSubmittedAt, null);
});

test('투표 중이면 변론 제출 시각이 타임라인에 찍힌다', () => {
    const progress = toTrialProgress(detail({
        status: 'VOTING',
        defense: { content: '정산받았어요', createdAt: todayAt(20, 32) },
        voteCount: 2,
    }));

    assert.equal(progress.status, 'VOTING');
    assert.equal(progress.steps.defenseSubmittedAt, '20:32');
    assert.deepEqual(progress.vote, { votedCount: 2, totalVoters: 3, remainingVoters: 1 });
});

test('유죄·무죄 확정은 둘 다 판결 완료 단계이고 원래 상태는 남는다', () => {
    for (const [serverStatus, result] of [['GUILTY', true], ['INNOCENT', false]]) {
        const progress = toTrialProgress(detail({ status: serverStatus, result }));

        assert.equal(progress.status, 'VERDICT_DONE');
        assert.equal(progress.serverStatus, serverStatus);
    }
});

/*
 * 서버에 판결 확정 시각 컬럼이 없다. 없는 값을 지어내면 혐의 인정처럼 투표 없이 끝난 재판에
 * 있지도 않은 개표 시각을 보여주게 된다.
 */
test('판결 확정 시각은 서버에 없으므로 만들지 않는다', () => {
    const progress = toTrialProgress(detail({ status: 'GUILTY', verdictMethod: 'CONFESSION' }));

    assert.equal(progress.steps.verdictAt, null);
});

test('혐의 인정으로 끝난 재판은 변론이 없어 제출 시각이 비어 있다', () => {
    const progress = toTrialProgress(detail({
        status: 'GUILTY',
        verdictMethod: 'CONFESSION',
        defense: null,
    }));

    assert.equal(progress.steps.defenseSubmittedAt, null);
});

/*
 * `tbl_indictment.status` 의 CHECK 제약을 벗어난 값이다. 화면이 단계를 지어내면
 * 이미 끝난 재판에 변론을 쓰라고 안내하게 된다.
 */
test('계약에 없는 상태는 null 로 돌려보낸다', () => {
    assert.equal(toTrialProgress(detail({ status: 'CLOSED' })), null);
    assert.equal(toTrialProgress(null), null);
});

test('투표 수가 배심원 수를 넘어도 남은 인원이 음수가 되지 않는다', () => {
    const progress = toTrialProgress(detail({ status: 'VOTING', voteCount: 5, totalVoters: 3 }));

    assert.equal(progress.vote.remainingVoters, 0);
});

test('배심원 수가 없으면 0 으로 채운다', () => {
    const progress = toTrialProgress(detail({ status: 'VOTING', voteCount: undefined, totalVoters: undefined }));

    assert.deepEqual(progress.vote, { votedCount: 0, totalVoters: 0, remainingVoters: 0 });
});

/*
 * 투표 마감은 변론 마감 + 24시간이라 대개 다음 날이다. 화면이 「오늘」로 적으면
 * 밤에 기소된 사람에게 이미 지난 시각을 마감으로 보여준다.
 */
test('다음 날 투표 마감은 「내일」로 표시한다', () => {
    const progress = toTrialProgress(detail({ status: 'VOTING', voteDeadline: afterDays(1, 2, 0) }));

    assert.equal(progress.voteDeadlineLabel, '내일 02:00');
    assert.equal(progress.steps.voteDeadline, '02:00');
});

test('마감 라벨은 오늘·내일·그 이후를 구분한다', () => {
    assert.equal(formatDeadlineLabel(todayAt(22, 0)), '오늘 22:00');
    assert.equal(formatDeadlineLabel(afterDays(1, 2, 0)), '내일 02:00');
    assert.equal(formatDeadlineLabel(''), '');
    assert.equal(formatDeadlineLabel('말이 안 되는 값'), '');
});

test('상세가 붙여 둔 표시용 값은 그대로 통과한다', () => {
    const progress = toTrialProgress(detail());

    assert.equal(progress.caseNumber, '2026-재판-0805');
    assert.equal(progress.evalLabel, '일일결산');
    assert.equal(progress.indictmentId, 42);
});
