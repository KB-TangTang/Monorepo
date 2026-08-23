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
});

/*
 * 개인 「명예의 전당」 배너와 형태로 갈라야 한다. 한 번 공용 배너로 합쳐 글만 갈아끼웠더니
 * 두 입구가 똑같이 생겨 개인 랭킹인지 그룹 순위인지 구별되지 않았다.
 * 구별의 근거는 문구가 아니라 탕이 수다 — 개인은 하나, 그룹은 시상대 셋.
 */
test('그룹 명예 법정 배너는 탕이 셋을 시상대 순서로 세운다', () => {
    const banner = source('src/components/challenge/group/GroupHonorCourtEntry.vue');

    for (const rank of ['first', 'second', 'third']) {
        assert.match(
            banner,
            new RegExp(`ranking/tang-ranking-${rank}\\.png`),
            `${rank} 탕이가 빠졌다. 셋이 모여야 「여럿이 겨룬 순위」로 읽힌다`,
        );
    }

    /* 2위–1위–3위 순서. 바꾸면 1위가 가장자리로 밀려 시상대로 안 읽힌다 */
    assert.ok(
        banner.indexOf('secondTangi"') < banner.indexOf('firstTangi"'),
        '1위가 2위보다 앞에 있다. 시상대 배치가 아니다',
    );
    assert.ok(
        banner.indexOf('firstTangi"') < banner.indexOf('thirdTangi"'),
        '3위가 1위보다 앞에 있다. 시상대 배치가 아니다',
    );

    /* 갈색은 판사봉·인장·종이 질감 전용이다. 순위는 문서가 아니다 */
    assert.doesNotMatch(banner, /--tt-(wood|kraft)\)/, '갈색 토큰이 다시 들어갔다');
});

test('지방법원 홈에 전체 기록 진입이 있다', () => {
    const view = source('src/views/challenge/group/GroupChallengeHomeView.vue');

    assert.match(view, /fetchAllMyTrialRecords/);
    assert.match(view, /name: 'groupTrialRecordsAll'/);
    /* 0건이면 줄을 통째로 숨긴다 — 「재판 기록 0건」은 아무 정보도 주지 않는다 */
    assert.match(view, /v-if="recordCount"/);
});

/*
 * 이 줄은 바로 위 「기다리는 재판 N건」과 모양이 같다(`.gc-watching` 공유).
 * 그래서 **라벨이 유일한 구별 수단**인데, 한때 「지난 재판 N건」이라 둘 다
 * 「…재판 N건」으로 끝났다. 기다리는 재판이 0건이면 위 줄이 사라지고 이 줄이
 * 그 자리로 올라오는 탓에 「이름만 바뀐 같은 줄」로 읽혔다.
 *
 * 목적지 화면(`GroupTrialRecordsView`)의 제목과 같은 낱말을 쓴다.
 */
test('홈 기록 진입 라벨은 목적지 화면 제목과 같다', () => {
    const view = source('src/views/challenge/group/GroupChallengeHomeView.vue');
    const recordsView = source('src/views/challenge/group/GroupTrialRecordsView.vue');

    assert.match(recordsView, /BaseBackHeader title="재판 기록"/);
    assert.match(view, /재판 기록 \{\{ recordCount \}\}건/);

    /* 위 줄과 끝이 겹치면 안 된다 */
    assert.doesNotMatch(view, /지난 재판 \{\{ recordCount \}\}건/);
});

/* ── 기록 목록 화면 ────────────────────── */

/*
 * 행에 담는 건 판결 · 챌린지 · 누구 · 개표 넷뿐이다. 결산일 · 초과액 · 판결 사유를 같이 늘어놓으면
 * 세 줄이 다 회색 잔글씨가 돼 판결 도장이 묻힌다. 나머지는 판결문에 전부 있다.
 */
test('기록 행은 결산일·금액·판결 사유를 늘어놓지 않는다', () => {
    const card = source('src/components/challenge/group/GroupTrialRecordCard.vue');

    assert.doesNotMatch(card, /settlementDate/, '결산일이 다시 들어갔다');
    assert.doesNotMatch(card, /exceededAmount/, '초과액이 다시 들어갔다');
    assert.doesNotMatch(card, /item\.reason/, '판결 사유가 다시 들어갔다');
});

/*
 * 왼쪽 강조선(`box-shadow: inset` + `padding-left`)은 그 행만 도장을 안쪽으로 밀었다.
 * 목록을 세로로 훑을 때 유무죄 도장이 한 줄로 서지 않으면 도장이 앵커 노릇을 못 한다.
 * 내 재판 구별은 배지가 맡는다 — 배지는 폭을 먹어도 도장 위치를 건드리지 않는다.
 */
test('내 재판을 왼쪽 선으로 표시하지 않는다 — 도장이 행마다 밀린다', () => {
    const card = source('src/components/challenge/group/GroupTrialRecordCard.vue');

    assert.doesNotMatch(card, /trial-record__button--mine/, '왼쪽 강조선이 다시 들어갔다');
    assert.match(card, /class="trial-record__mine">내 재판</, '내 재판 배지가 사라졌다');

    /* 연한 배지는 옆 닉네임과 명도가 비슷해 그냥 글자로 읽혔다. 면을 채워야 눈에 걸린다 */
    const badge = card.match(/\.trial-record__mine \{[^}]*\}/)[0];
    assert.match(badge, /background: var\(--tt-primary\)/, '배지가 다시 연해졌다');
});

/*
 * 그룹 기록에서는 모든 행의 챌린지 제목이 같아 중복이다. 그렇다고 빼면 그 화면만 두 줄이 돼
 * 도장 세로 위치가 전체 기록과 달라진다. 행 구조를 두 벌로 만들지 않는다.
 */
test('행 구조는 한 벌이다 — 챌린지 제목을 감추는 분기가 없다', () => {
    const card = source('src/components/challenge/group/GroupTrialRecordCard.vue');
    const view = source('src/views/challenge/group/GroupTrialRecordsView.vue');

    assert.doesNotMatch(card, /showGroup/, '그룹명 표시 분기가 다시 생겼다');
    assert.doesNotMatch(view, /show-group/);

    /* 자백·무투표 건은 표가 없다. 줄을 비우면 그 행만 두 줄이 된다 */
    assert.match(card, /trial-record__novote/, '개표 없는 행의 셋째 줄이 비었다');
});

test('세그먼트는 「친구」가 아니라 「그룹원」이라고 쓴다', () => {
    const segment = source('src/components/challenge/group/GroupRecordScopeSegment.vue');
    const tabs = segment.slice(segment.indexOf('const TABS'), segment.indexOf('</script>'));

    assert.match(tabs, /label: '전체'/);
    assert.match(tabs, /label: '내 재판'/);
    assert.match(tabs, /label: '그룹원 재판'/);
    assert.doesNotMatch(tabs, /친구/, '이 앱의 사용자 문구는 「그룹원」으로 통일돼 있다');
});

/*
 * 세그먼트로 목록을 좁혔는데 요약이 전체를 세면 「총 8건」 아래에 1건만 놓인다.
 * 요약은 목록을 설명하는 줄이라 목록과 다른 말을 하면 안 된다.
 */
test('요약 스트립은 세그먼트를 따라 같이 좁혀진다', () => {
    const view = source('src/views/challenge/group/GroupTrialRecordsView.vue');

    assert.match(view, /총 \{\{ visibleRecords\.length \}\}건/, '요약이 전체 건수를 세고 있다');
    assert.doesNotMatch(view, /총 \{\{ records\.length \}\}건/);
    assert.match(view, /visibleRecords\.value\.filter\(\(item\) => item\.verdict === 'GUILTY'\)/);
    assert.match(view, /:items="visibleRecords"/);
});

/*
 * 정렬은 SQL 이 끝낸다 — `IndictmentMapper.xml` 의 `ORDER BY i.updated_at DESC, i.id DESC`
 * (백엔드 쪽은 `ChallengeMapperXmlTest:360` · `GroupTrialServiceTest:468` 이 못박아 뒀다).
 * 화면이 한 번 더 정렬하면 두 곳이 축을 따로 갖게 되고, 목데이터와 실제 응답이 다른 순서로 뜬다.
 * 「최근 확정순」 표시는 그 보증에 기대는 문구라 재정렬이 들어오면 같이 거짓말이 된다.
 */
test('기록 목록을 화면에서 다시 정렬하지 않는다', () => {
    const view = source('src/views/challenge/group/GroupTrialRecordsView.vue');

    assert.match(
        view,
        /최근 확정순/,
        '정렬 축 표시가 없다. 행에 시각이 없어 순서를 확인할 수 없다',
    );
    assert.doesNotMatch(view, /\.sort\(/, '화면이 정렬을 다시 하고 있다');
    assert.match(view, /records\.value = list\.map\(toTrialRecordCard\)/, '서버 순서를 흘렸다');
});

/* 목데이터도 최근 확정순이어야 한다 — /dev 에서만 순서가 뒤집히면 표시가 거짓말이 된다 */
test('목데이터가 최근 확정순으로 놓여 있다', () => {
    const fixtures = source('src/fixtures/groupChallenge.js');
    const block = fixtures.slice(fixtures.indexOf('MOCK_TRIAL_RECORDS'));
    const stamps = [...block.matchAll(/confirmedAt: '([^']+)'/g)].map((m) => m[1]);

    assert.ok(stamps.length >= 2, '목 기록이 없다');
    assert.deepEqual(stamps, [...stamps].sort().reverse(), '목데이터 순서가 최근순이 아니다');
});

test('건수가 적어도 세그먼트를 접지 않는다 — 들어올 때마다 화면 구조가 달라지면 안 된다', () => {
    const view = source('src/views/challenge/group/GroupTrialRecordsView.vue');
    const segment = view.match(/<GroupRecordScopeSegment[\s\S]*?\/>/)[0];

    assert.doesNotMatch(segment, /v-if/, '세그먼트에 조건이 붙었다');
    assert.match(segment, /v-model="scope"/);
    assert.match(segment, /:counts="scopeCounts"/);
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
