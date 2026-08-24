import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { formatMonthDay, toRankingViewModel } from '../src/utils/groupRanking.js';

/*
 * 서버 계약(`challenge/dto/GroupRankingDto`) 모양 그대로 적는다 — `profileImageUrl` · `mine` ·
 * `totalConsumption`. 화면 이름(`profileImage` · `isMe`)으로 적으면 실제로는 지나지 않는
 * 경로를 테스트하게 된다 (`groupTrialVote.test.js` 머리말과 같은 이유).
 */
function member(overrides = {}) {
    return {
        rank: 1,
        userId: 7,
        nickname: '절약왕',
        profileImageUrl: 'https://cdn/u7.png',
        mine: false,
        livesCount: 5,
        totalConsumption: 12000,
        finalOutcome: null,
        finalChargeAmount: null,
        ...overrides,
    };
}

function dailyDto(members, overrides = {}) {
    return {
        evalType: 'DAILY',
        status: 'ACTIVE',
        limitAmount: 10000,
        maxLives: 7,
        memo: null,
        lastSettlementDate: '2026-08-06',
        members,
        ...overrides,
    };
}

function periodDto(members, overrides = {}) {
    return {
        evalType: 'PERIOD',
        status: 'ACTIVE',
        limitAmount: 30000,
        maxLives: 1,
        memo: null,
        lastSettlementDate: '2026-08-07',
        members,
        ...overrides,
    };
}

/* ══ 목숨 상태 배지 (일일평가) ═══════════════════════════════ */

test('목숨이 절반 이상이면 alive, 절반 미만이면 danger 다', () => {
    const vm = toRankingViewModel(
        dailyDto([
            member({ rank: 1, userId: 1, livesCount: 4 }),
            member({ rank: 2, userId: 2, livesCount: 3 }),
        ]),
    );

    assert.deepEqual(
        vm.rankings.map((r) => [r.statusType, r.statusLabel, r.isEliminated]),
        [
            ['alive', '4', false],
            ['danger', '3', false],
        ],
    );
});

test('진행 중의 목숨 0 은 탈락이 아니라 「탈락 위기」다', () => {
    const vm = toRankingViewModel(dailyDto([member({ livesCount: 0, finalOutcome: null })]));

    const row = vm.rankings[0];
    assert.equal(row.isEliminated, false);
    assert.equal(row.statusType, 'danger');
    assert.equal(row.statusLabel, '탈락 위기');
});

test('종료 후 ELIMINATED 확정값만 「탈락」으로 그린다', () => {
    const vm = toRankingViewModel(
        dailyDto(
            [
                member({ rank: 1, userId: 1, livesCount: 3, finalOutcome: 'SURVIVED' }),
                member({ rank: 2, userId: 2, livesCount: 0, finalOutcome: 'ELIMINATED' }),
            ],
            { status: 'CLOSED' },
        ),
    );

    /* 생존자는 목숨이 적어도 탈락이 아니다 — 배지는 남은 목숨 숫자 그대로. */
    assert.deepEqual(
        vm.rankings.map((r) => [r.isEliminated, r.statusType, r.statusLabel]),
        [
            [false, 'danger', '3'],
            [true, 'eliminated', '탈락'],
        ],
    );
});

test('일일평가 행에는 목숨 아이콘 분모(maxLives)가 실린다', () => {
    const vm = toRankingViewModel(dailyDto([member({ livesCount: 5 })]));

    assert.equal(vm.rankings[0].livesCount, 5);
    assert.equal(vm.rankings[0].maxLives, 7);
});

/* ══ 시상대 ═══════════════════════════════════════════════════ */

test('시상대는 3위 이내만 올린다', () => {
    const vm = toRankingViewModel(
        dailyDto([
            member({ rank: 1, userId: 1 }),
            member({ rank: 2, userId: 2 }),
            member({ rank: 3, userId: 3 }),
            member({ rank: 4, userId: 4 }),
        ]),
    );

    assert.deepEqual(
        vm.podium.map((p) => p.userId),
        [1, 2, 3],
    );
});

test('참여자가 둘이면 시상대도 둘이다', () => {
    const vm = toRankingViewModel(
        dailyDto([member({ rank: 1, userId: 1 }), member({ rank: 2, userId: 2 })]),
    );

    assert.equal(vm.podium.length, 2);
});

test('공동 순위로 3위가 넷이어도 시상대는 세 칸이다', () => {
    const vm = toRankingViewModel(
        dailyDto([
            member({ rank: 1, userId: 1 }),
            member({ rank: 1, userId: 2 }),
            member({ rank: 3, userId: 3 }),
            member({ rank: 3, userId: 4 }),
        ]),
    );

    /* 공동 순위(1, 1, 3)는 그대로 통과한다 — 순위를 다시 매기지 않는다. */
    assert.deepEqual(
        vm.rankings.map((r) => r.rank),
        [1, 1, 3, 3],
    );
    assert.deepEqual(
        vm.podium.map((p) => p.userId),
        [1, 2, 3],
    );
});

/* ══ 사용률 (기간평가) ════════════════════════════════════════ */

test('사용률은 반올림 정수 퍼센트고 100 을 넘을 수 있다', () => {
    const vm = toRankingViewModel(
        periodDto([
            member({ rank: 1, userId: 1, totalConsumption: 10500 }),
            member({ rank: 2, userId: 2, totalConsumption: 31200 }),
        ]),
    );

    assert.deepEqual(
        vm.rankings.map((r) => [r.currentAmount, r.usagePercent]),
        [
            [10500, 35],
            [31200, 104],
        ],
    );
});

test('한도 0원(무지출)은 한 푼이라도 쓰면 100, 안 쓰면 0 이다', () => {
    const vm = toRankingViewModel(
        periodDto(
            [
                member({ rank: 1, userId: 1, totalConsumption: 0 }),
                member({ rank: 2, userId: 2, totalConsumption: 1200 }),
            ],
            { limitAmount: 0 },
        ),
    );

    assert.deepEqual(
        vm.rankings.map((r) => r.usagePercent),
        [0, 100],
    );
});

/* ══ 이름 변환 · 헤더 ═════════════════════════════════════════ */

test('서버 이름을 화면 이름으로 옮긴다 — 이미지 없으면 null 유지', () => {
    const vm = toRankingViewModel(dailyDto([member({ mine: true, profileImageUrl: null })]));

    const row = vm.rankings[0];
    assert.equal(row.isMe, true);
    assert.equal(row.profileImage, null);
});

test('결산일은 「M월 D일」로, 아직 없으면 빈 문자열로 내린다', () => {
    assert.equal(formatMonthDay('2026-08-06'), '8월 6일');

    const vm = toRankingViewModel(dailyDto([member()], { lastSettlementDate: null }));
    assert.equal(vm.lastSettlementDate, '');
});

/* ══ 순위 렌더링은 명예 법정 한 곳이 전담한다 ═══════════════════ */

/*
 * 종료 상세(`/group-challenges/:id`)와 명예 법정(`/group-challenges/:id/ranking`)이
 * 시상대와 「전체 피고인 현황」을 각자 그리고 있었다. 같은 `tbl_group_member` 를 같은
 * 순서로 읽고 제목 문자열까지 같아, 값이 어긋나도 어느 쪽이 원본인지 알 수 없었다.
 * 종료 상세를 「내 결과」로 좁히고 순위는 명예 법정만 그린다 — 아래가 그 경계다.
 */
function source(path) {
    return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

test('종료 상세는 시상대·전체 피고인 현황을 그리지 않는다', () => {
    const view = source('src/views/challenge/group/GroupChallengeDetailView.vue');

    assert.ok(!/<GroupDetailPodium/.test(view), '시상대는 명예 법정에만 있어야 한다');
    assert.ok(!/<GroupDetailRankingTable/.test(view), '순위표는 명예 법정에만 있어야 한다');
    assert.ok(!/GroupDetail(Podium|RankingTable)\.vue/.test(view), '죽은 import 가 남아 있다');
});

test('종료 상세에는 내 결과와 명예 법정 진입이 남는다', () => {
    const view = source('src/views/challenge/group/GroupChallengeDetailView.vue');

    assert.match(view, /최종 결과/);
    assert.match(view, /<GroupHonorCourtEntry/);
});

test('명예 법정은 여전히 순위를 그린다', () => {
    const view = source('src/views/challenge/group/GroupHonorCourtView.vue');

    assert.match(view, /<GroupHonorPodium/);
    assert.match(view, /<GroupHonorRankList/);
    assert.match(view, /<GroupHonorPeriodRankList/);
});
