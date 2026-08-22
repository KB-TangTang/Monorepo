import test from 'node:test';
import assert from 'node:assert/strict';
import {
    clampHomeProgress,
    formatHomeAmount,
    getCurrentYearMonth,
    getHomeGroupTrialRow,
    getHomeMissionBubbleCopy,
    getHomePersonalTrialRow,
    getHomeReportEmptyCopy,
    getHomeSpendingChange,
    getHomeWeeklyVerdicts,
    pickUpcomingFixedExpenses,
    toHomeMission,
    toHomeReportSummary,
    toHomeSpending,
} from '../src/utils/home.js';

test('홈 금액을 천 단위 구분자로 표시한다', () => {
    assert.equal(formatHomeAmount(12840000), '12,840,000');
    assert.equal(formatHomeAmount(43487.7), '43,488');
    assert.equal(formatHomeAmount(43487.4), '43,487');
    assert.equal(formatHomeAmount(null), '0');
});

test('현재 연월을 만든다', () => {
    assert.equal(getCurrentYearMonth(new Date(2026, 7, 18, 12)), '2026-08');
});

test('챌린지 진행률을 0에서 100 사이로 제한한다', () => {
    assert.equal(clampHomeProgress(-10), 0);
    assert.equal(clampHomeProgress(56), 56);
    assert.equal(clampHomeProgress(120), 100);
});

test('오늘 미션을 홈 진행 카드 모델로 변환한다', () => {
    const mission = toHomeMission({
        missionTitle: '카페 방어전',
        targetValue: 8000,
        currentAmount: 4500,
    });

    assert.deepEqual(mission, {
        title: '카페 방어전',
        isAbsoluteMission: false,
        categoryName: '',
        limitAmount: 8000,
        spentAmount: 4500,
        remainingAmount: 3500,
        exceededAmount: 0,
        progressRate: 56,
    });
});

test('위반하지 않은 절대형 미션을 공통 진행 카드 모델로 변환한다', () => {
    const mission = toHomeMission({
        missionTitle: '무지출 명령 · 배달',
        missionType: 'ABSOLUTE',
        categoryName: '배달앱',
        targetValue: 0,
        currentAmount: 0,
    });

    assert.equal(mission.isAbsoluteMission, true);
    assert.equal(mission.spentAmount, 0);
    assert.equal(mission.exceededAmount, 0);
    assert.equal(mission.progressRate, 0);
});

test('위반한 절대형 미션은 공통 진행 카드에서 100%로 표시한다', () => {
    const mission = toHomeMission({
        missionTitle: '무지출 명령 · 배달',
        missionType: 'ABSOLUTE',
        categoryName: '배달앱',
        targetValue: 0,
        currentAmount: 3200,
    });

    assert.equal(mission.isAbsoluteMission, true);
    assert.equal(mission.categoryName, '배달앱');
    assert.equal(mission.spentAmount, 3200);
    assert.equal(mission.limitAmount, 0);
    assert.equal(mission.exceededAmount, 3200);
    assert.equal(mission.progressRate, 100);
});

test('가장 절감액이 큰 카테고리로 홈 리포트 요약을 만든다', () => {
    const summary = toHomeReportSummary({
        period: '2026-07',
        hasChallengeHistory: true,
        savedAmount: 128000,
        categoryEffects: [
            { categoryName: '배달', savedAmount: 20000 },
            { categoryName: '카페·간식', savedAmount: 72000 },
        ],
    });

    assert.deepEqual(summary, {
        period: '2026-07',
        month: 7,
        savedAmount: 128000,
        topCategoryName: '카페·간식',
    });
});

test('확정 리포트가 없어도 홈 리포트 카드의 상태 안내를 제공한다', () => {
    assert.match(getHomeReportEmptyCopy('preparing').title, /준비 중/);
    assert.match(getHomeReportEmptyCopy('not-agreed').title, /시작/);
    assert.match(getHomeReportEmptyCopy('error').title, /불러오지 못했어요/);
    assert.match(getHomeReportEmptyCopy('empty').title, /확정된 절감 결과가 없어요/);
});

test('오늘 쓴 돈의 어제 대비 증감을 방향에 맞는 문구로 만든다', () => {
    assert.deepEqual(getHomeSpendingChange(-8.4), {
        text: '어제보다 8% 적게 쓰는 중',
        tone: 'down',
    });
    assert.deepEqual(getHomeSpendingChange(12), {
        text: '어제보다 12% 많이 쓰는 중',
        tone: 'up',
    });
    assert.equal(getHomeSpendingChange(0).tone, 'same');
    /* 어제 지출이 0이면 비율을 낼 수 없어 백엔드가 null 을 준다 — 문구를 만들지 않는다. */
    assert.equal(getHomeSpendingChange(null), null);
});

test('오늘 쓴 돈 요약 응답을 홈 카드 모델로 옮긴다', () => {
    assert.deepEqual(
        toHomeSpending({
            date: '2026-08-23',
            todayAmount: 32000,
            monthAmount: 486300,
            changeRate: -20.0,
        }),
        { todayAmount: 32000, monthAmount: 486300, changeRate: -20 },
    );
});

test('어제 지출이 0이라 증감률이 null 이면 0 으로 바꾸지 않는다', () => {
    /*
     * 0 으로 바꾸면 카드가 「어제와 비슷하게 쓰는 중」이라고 말한다 — 어제 아무것도 안 쓴 사람에게
     * 거짓말이 된다. 백엔드가 일부러 null 을 주는 이유(TransactionQueryService.calculateDailyChangeRate).
     */
    const summary = toHomeSpending({ todayAmount: 50000, monthAmount: 50000, changeRate: null });

    assert.equal(summary.changeRate, null);
    assert.equal(getHomeSpendingChange(summary.changeRate), null);
});

test('오늘 0원을 썼으면 「집계 중」이 아니라 0 이다', () => {
    /* 카드의 「—」·「집계 중」은 호출 실패 전용 자리다. 0 을 null 로 바꾸면 정상이 장애처럼 보인다. */
    const summary = toHomeSpending({ todayAmount: 0, monthAmount: 0, changeRate: 0 });

    assert.equal(summary.todayAmount, 0);
    assert.equal(summary.monthAmount, 0);
    assert.equal(summary.changeRate, 0);
});

test('환불이 소비보다 큰 날의 음수 금액은 그대로 통과시킨다', () => {
    /* 장부의 totalSpent 도 상계 결과를 그대로 보여준다 — 여기서만 0 으로 자르면 두 화면이 갈린다. */
    assert.equal(toHomeSpending({ todayAmount: -12000, monthAmount: 300000 }).todayAmount, -12000);
});

test('응답이 없거나 키가 비면 카드가 버틸 수 있게 null 을 준다', () => {
    assert.deepEqual(toHomeSpending(null), {
        todayAmount: null,
        monthAmount: null,
        changeRate: null,
    });
    assert.deepEqual(toHomeSpending({}), {
        todayAmount: null,
        monthAmount: null,
        changeRate: null,
    });
});

test('탕이 말풍선은 미션 종류에 따라 다른 문구를 쓴다', () => {
    const relative = getHomeMissionBubbleCopy(
        toHomeMission({
            missionTitle: '카페',
            categoryName: '카페/간식',
            targetValue: 8000,
            currentAmount: 4800,
        }),
    );
    assert.equal(relative.lead, '오늘의 카페/간식 미션,');
    assert.equal(relative.highlight, '3,200원 남았어요!');

    const absolute = getHomeMissionBubbleCopy(
        toHomeMission({
            missionTitle: '무지출',
            missionType: 'ABSOLUTE',
            categoryName: '배달앱',
            targetValue: 0,
            currentAmount: 0,
        }),
    );
    assert.equal(absolute.highlight, '아직 위반이 없어요!');

    assert.match(getHomeMissionBubbleCopy(null).highlight, /챌린지에 참여해 보세요/);
});

test('이번 주 판결은 확정된 것만 무죄·유죄로 센다', () => {
    const verdicts = getHomeWeeklyVerdicts([
        { date: '2026-08-17', result: 'SUCCESS' },
        { date: '2026-08-18', result: 'FAIL' },
        { date: '2026-08-19', result: 'SUCCESS' },
        { date: '2026-08-20', result: 'PENDING' },
    ]);

    assert.deepEqual(verdicts, { innocent: 2, guilty: 1 });
    assert.deepEqual(getHomeWeeklyVerdicts(null), { innocent: 0, guilty: 0 });
});

test('개인 재판 줄에 남은 한도와 연속 무죄 일수를 함께 보여준다', () => {
    const mission = toHomeMission({
        missionTitle: '카페 방어전',
        categoryName: '카페/간식',
        targetValue: 8000,
        currentAmount: 4800,
    });

    assert.deepEqual(getHomePersonalTrialRow(mission, 8), {
        text: '카페/간식 3,200원 남음',
        badge: '연속 8일',
    });
    /* 연속 기록이 끊겼으면 0일이라고 자랑하지 않는다. */
    assert.equal(getHomePersonalTrialRow(mission, 0).badge, null);
    assert.match(getHomePersonalTrialRow(null, 3).text, /배정된 미션이 없어요/);
});

test('그룹 재판 줄은 급한 할 일 하나만 뱃지로 띄운다', () => {
    const trials = [{ type: 'vote' }, { type: 'accuse' }, { type: 'vote' }];

    assert.deepEqual(getHomeGroupTrialRow({ trials, activeGroupCount: 2 }), {
        text: '2건 진행중',
        badge: '변론 1건',
    });
    assert.equal(
        getHomeGroupTrialRow({ trials: [{ type: 'vote' }], activeGroupCount: 1 }).badge,
        '투표 1건',
    );
    assert.equal(getHomeGroupTrialRow({ trials: [], activeGroupCount: 1 }).badge, null);
    assert.match(getHomeGroupTrialRow({ failed: true }).text, /확인하지 못했어요/);
});

test('다가오는 고정지출은 결제일이 지나지 않은 것만 가까운 순으로 3건 고른다', () => {
    const referenceDate = new Date(2026, 7, 23);
    const items = [
        { id: 1, nextPaymentDate: '2026-08-20' },
        { id: 2, nextPaymentDate: '2026-09-05' },
        { id: 3, nextPaymentDate: '2026-08-25' },
        { id: 4, nextPaymentDate: '2026-08-23' },
        { id: 5, nextPaymentDate: '2026-08-28' },
        { id: 6, nextPaymentDate: null },
    ];

    const picked = pickUpcomingFixedExpenses(items, { referenceDate });

    assert.deepEqual(
        picked.map((item) => item.id),
        [4, 3, 5],
    );
    assert.deepEqual(pickUpcomingFixedExpenses(null), []);
});
