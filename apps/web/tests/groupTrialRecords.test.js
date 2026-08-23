import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { toTrialRecordCard, recordReason } from '../src/utils/groupTrial.js';

/*
 * 확정된 재판 기록 목록 (「재판 기록」 화면).
 *
 * 두 가지를 지킨다.
 * 1. `toTrialRecordCard` 의 변환 — 유무죄 어휘 · 톤 · 사유 · 목적지
 * 2. 진입 재배선 — 「재판 기록」이 명예 법정으로 새지 않는지, 라우트가 그림자에 안 먹히는지
 *
 * 입력은 `api/groupChallenge.js` 의 `toTrialRecordViewModel` 을 지난 모양이다.
 * 서버 원본 이름(`mine` · `profileImageUrl`)으로 적으면 실제로 안 도는 경로를 테스트하게 된다.
 */
function record(overrides = {}) {
    return {
        id: 201,
        groupId: 3,
        groupName: '배달 소비 줄이기',
        nickname: '지판',
        profileImage: null,
        isMine: false,
        status: 'GUILTY',
        verdictMethod: 'VOTE',
        aiVerdictReason: null,
        settlementDate: '8월 12일',
        exceededAmount: 12400,
        myVote: 'GUILTY',
        guiltyCount: 3,
        innocentCount: 1,
        totalVoters: 4,
        confirmedAt: '2026-08-13T09:12:00',
        ...overrides,
    };
}

function source(path) {
    return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

/* ── 변환 ─────────────────────────────── */

test('유죄 기록은 「유죄」 어휘와 guilty 톤을 갖는다', () => {
    const card = toTrialRecordCard(record());

    assert.equal(card.verdict, 'GUILTY');
    assert.equal(card.verdictLabel, '유죄');
    assert.equal(card.tone, 'guilty');
});

test('무죄 기록은 「무죄」 어휘와 innocent 톤을 갖는다', () => {
    const card = toTrialRecordCard(record({ status: 'INNOCENT', verdictMethod: 'NO_VOTE' }));

    assert.equal(card.verdictLabel, '무죄');
    assert.equal(card.tone, 'innocent');
});

test('개표를 가리지 않는다 — 확정된 뒤에는 숨길 이유가 없다', () => {
    const card = toTrialRecordCard(record());

    assert.equal(card.guiltyCount, 3);
    assert.equal(card.innocentCount, 1);
    assert.equal(card.totalVoters, 4);
    assert.equal(card.myVote, 'GUILTY');
});

test('AI 판결은 서버가 만든 사유를 그대로 쓴다', () => {
    const reason = '한도를 넘겼지만 실제 부담금이 작아 무죄로 판단했어요.';
    const card = toTrialRecordCard(
        record({ status: 'INNOCENT', verdictMethod: 'AI_JUDGMENT', aiVerdictReason: reason }),
    );

    assert.equal(card.reason, reason);
});

test('AI 판결이 아니면 판결 방식별 고정 문구를 쓴다', () => {
    assert.match(recordReason(record({ verdictMethod: 'VOTE' })), /배심원 투표/);
    assert.match(recordReason(record({ verdictMethod: 'CONFESSION' })), /혐의를 인정/);
    assert.match(recordReason(record({ verdictMethod: 'NO_VOTE' })), /무죄 추정/);
});

test('사유를 모르는 판결 방식이라도 빈 문자열로 끝난다 — 화면이 undefined 를 그리면 안 된다', () => {
    assert.equal(recordReason(record({ verdictMethod: 'SCRATCH_LOTTERY' })), '');
    assert.equal(recordReason(record({ verdictMethod: 'AI_JUDGMENT', aiVerdictReason: null })), '');
});

test('AI 판결만 동점 안내 화면으로 간다', () => {
    assert.equal(
        toTrialRecordCard(record({ verdictMethod: 'AI_JUDGMENT' })).routeName,
        'groupVoteTie',
    );
    assert.equal(toTrialRecordCard(record({ verdictMethod: 'VOTE' })).routeName, 'verdictResult');
    assert.equal(
        toTrialRecordCard(record({ verdictMethod: 'CONFESSION' })).routeName,
        'verdictResult',
    );
    assert.equal(
        toTrialRecordCard(record({ verdictMethod: 'NO_VOTE' })).routeName,
        'verdictResult',
    );
});

test('확정되지 않은 행은 목적지가 없다 — 진행 중 재판이 섞여 들어와도 눌리지 않는다', () => {
    assert.equal(toTrialRecordCard(record({ status: 'VOTING' })).routeName, null);
});

test('확정 시각은 날짜까지만 읽는다', () => {
    assert.equal(toTrialRecordCard(record()).confirmedLabel, '8월 13일');
    assert.equal(toTrialRecordCard(record({ confirmedAt: null })).confirmedLabel, '');
});

test('자백·무투표 건의 표 수는 0 이다 — null 이 그대로 나가면 「null : null」이 그려진다', () => {
    const card = toTrialRecordCard(
        record({
            verdictMethod: 'CONFESSION',
            guiltyCount: null,
            innocentCount: null,
            totalVoters: null,
            myVote: undefined,
        }),
    );

    assert.equal(card.guiltyCount, 0);
    assert.equal(card.innocentCount, 0);
    assert.equal(card.totalVoters, 0);
    assert.equal(card.myVote, null);
});

/* ── 진입 재배선 ──────────────────────── */

test('그룹 상세의 「재판 기록」 진입은 기록 화면으로 간다', () => {
    const view = source('src/views/challenge/group/GroupChallengeDetailView.vue');

    assert.match(
        view,
        /function goToTrialRecords\(\)/,
        '기록 진입 함수가 없다. 「재판 기록」이 다시 명예 법정으로 새고 있다',
    );
    assert.match(view, /name: 'groupTrialRecords'/);

    /*
     * `goToRanking` 은 nav 의 「최종 순위」 한 곳에만 남아야 한다.
     * 하단 명예 법정 배너는 `@open` 이라 `@click` 으로 세면 걸리지 않는다.
     */
    const rankingClicks = view.match(/@click="goToRanking"/g) ?? [];
    assert.equal(rankingClicks.length, 1, '「재판 기록」이라고 적힌 버튼이 아직 순위로 가고 있다');
});

test('그룹 상세는 진행 중·종료 두 분기 모두 명예 법정 배너를 단다', () => {
    const view = source('src/views/challenge/group/GroupChallengeDetailView.vue');
    const banners = view.match(/<GroupHonorCourtEntry/g) ?? [];

    assert.equal(banners.length, 2, '한쪽 분기에서 순위로 나갈 길이 사라진다');
    assert.match(view, /variant="closed"/);
    assert.match(view, /variant="active"/);
});

test('지방법원 홈에 전체 기록 진입이 있다', () => {
    const view = source('src/views/challenge/group/GroupChallengeHomeView.vue');

    assert.match(view, /fetchAllMyTrialRecords/);
    assert.match(view, /name: 'groupTrialRecordsAll'/);
    /* 0건이면 줄을 통째로 숨긴다 — 「지난 재판 0건」은 아무 정보도 주지 않는다 */
    assert.match(view, /v-if="recordCount"/);
});

test('리터럴 라우트가 :id 보다 먼저 선언된다 — 뒤에 있으면 상세 화면이 열린다', () => {
    const router = source('src/router/index.js');

    const literal = router.indexOf("'/group-challenges/trial-records'");
    const template = router.indexOf("'/group-challenges/:id'");

    assert.ok(literal > -1, '전체 기록 라우트가 없다');
    assert.ok(template > -1, '그룹 상세 라우트가 없다');
    assert.ok(
        literal < template,
        "'/group-challenges/trial-records' 가 '/group-challenges/:id' 아래에 있다. " +
            'trial-records 가 그룹 id 로 먹혀 상세가 열린다',
    );
});

test('두 라우트가 같은 뷰를 공유한다', () => {
    const router = source('src/router/index.js');
    const views = router.match(/GroupTrialRecordsView\.vue/g) ?? [];

    assert.equal(views.length, 2, '전체 기록과 그룹 기록이 서로 다른 뷰로 갈라졌다');
});

test('두 api 함수 모두 목데이터 분기를 갖는다 — /dev 모드에서 화면이 죽으면 안 된다', () => {
    const api = source('src/api/groupChallenge.js');

    const body = api.slice(api.indexOf('export async function fetchAllMyTrialRecords'));
    const records = body.slice(0, body.indexOf('function toTrialRecordViewModel'));

    const mockBranches = records.match(/isMockMode\.value/g) ?? [];
    assert.equal(mockBranches.length, 2, '기록 조회 두 함수 중 하나에 목 분기가 없다');
});
