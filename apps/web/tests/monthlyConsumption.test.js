import test from 'node:test';
import assert from 'node:assert/strict';
import {
    buildMonthlyTrendSlots,
    composeMonthlyConsumptionReport,
    fetchMonthlyConsumptionState,
    formatChangeRate,
    getPreviousPeriod,
    isAvailableReportMonth,
    MONTHLY_REPORT_STATUS,
    resolveSelectedReportPeriod,
    resolveFixedExpenseStatus,
    resolveReportState,
    splitMonthlyCategories,
} from '../src/utils/monthlyConsumption.js';
import { AVAILABLE_MONTHS, REPORTS } from '../src/fixtures/monthlyConsumption.js';
import {
    fetchTempMonthlyConsumptionMonths,
    fetchTempMonthlyConsumptionReport,
} from '../src/api/tempMonthlyConsumptionMock.js';

test('기준일 이전의 리포트 보유 월만 조회 가능 월로 계산한다', () => {
    const referenceDate = new Date(2026, 7, 4);
    assert.equal(getPreviousPeriod(referenceDate), '2026-07');
    assert.equal(
        isAvailableReportMonth({ value: '2026-07', hasReport: true }, referenceDate),
        true,
    );
    assert.equal(
        isAvailableReportMonth({ value: '2026-08', hasReport: true }, referenceDate),
        false,
    );
    assert.equal(
        isAvailableReportMonth({ value: '2026-04', hasReport: false }, referenceDate),
        false,
    );
});

test('유효한 요청 월을 유지하고 데이터 없는 월은 최신 리포트 월로 대체한다', () => {
    const referenceDate = new Date(2026, 7, 4);
    const months = [
        { value: '2026-07', hasReport: true },
        { value: '2026-06', hasReport: true },
        { value: '2026-05', hasReport: true },
        { value: '2026-04', hasReport: false },
        { value: '2026-03', hasReport: false },
        { value: '2026-08', hasReport: false },
    ];

    assert.equal(resolveSelectedReportPeriod(months, '2026-06', referenceDate), '2026-06');
    assert.equal(resolveSelectedReportPeriod(months, '2026-05', referenceDate), '2026-05');
    assert.equal(resolveSelectedReportPeriod(months, '2026-03', referenceDate), '2026-07');
});

test('2월 온보딩과 3~4월 리포트는 월 선택에서 활성화한다', () => {
    const referenceDate = new Date(2026, 7, 4);

    ['2026-02', '2026-03', '2026-04'].forEach((period) => {
        const month = AVAILABLE_MONTHS.find((item) => item.value === period);
        assert.equal(isAvailableReportMonth(month, referenceDate), true);
    });
    assert.equal(
        isAvailableReportMonth(
            AVAILABLE_MONTHS.find((item) => item.value === '2026-01'),
            referenceDate,
        ),
        false,
    );
});

test('카테고리 증감과 화면 상태를 표시한다', () => {
    assert.equal(formatChangeRate(28), '▲28%');
    assert.equal(formatChangeRate(-12), '▼12%');
    assert.equal(formatChangeRate(0), '−0%');
    assert.equal(resolveReportState({ loading: true, error: '실패', report: {} }), 'loading');
    assert.equal(resolveReportState({ error: '실패', report: {} }), 'error');
    assert.equal(resolveReportState({ report: null }), 'empty');
    assert.equal(
        resolveReportState({ report: { status: MONTHLY_REPORT_STATUS.ONBOARDING } }),
        'onboarding',
    );
    assert.equal(
        resolveReportState({ report: { status: MONTHLY_REPORT_STATUS.FIRST_REPORT } }),
        'first-report',
    );
    assert.equal(
        resolveReportState({ report: { status: MONTHLY_REPORT_STATUS.READY } }),
        'ready',
    );
});

test('온보딩 상태는 완료 월 집계 API를 호출하지 않고 로컬 화면 상태를 만든다', async () => {
    let reportRequestCount = 0;
    const report = await fetchMonthlyConsumptionState(
        { value: '2026-08', status: MONTHLY_REPORT_STATUS.ONBOARDING },
        async () => {
            reportRequestCount += 1;
            return {};
        },
    );

    assert.equal(reportRequestCount, 0);
    assert.deepEqual(report, {
        period: '2026-08',
        status: MONTHLY_REPORT_STATUS.ONBOARDING,
    });
});

test('첫 리포트와 일반 리포트는 완료 월 집계 API를 호출한다', async () => {
    const requestedPeriods = [];
    const reportFetcher = async (period) => {
        requestedPeriods.push(period);
        return { period };
    };

    await fetchMonthlyConsumptionState(
        { value: '2026-07', status: MONTHLY_REPORT_STATUS.FIRST_REPORT },
        reportFetcher,
    );
    await fetchMonthlyConsumptionState(
        { value: '2026-06', status: MONTHLY_REPORT_STATUS.READY },
        reportFetcher,
    );

    assert.deepEqual(requestedPeriods, ['2026-07', '2026-06']);
});

test('고정 지출 의심 건은 명시적인 배열 또는 false일 때만 확정한다', () => {
    assert.equal(resolveFixedExpenseStatus(2), 'detected');
    assert.equal(resolveFixedExpenseStatus(0), 'clear');
    assert.equal(resolveFixedExpenseStatus([{ id: 1 }]), 'detected');
    assert.equal(resolveFixedExpenseStatus([]), 'clear');
    assert.equal(resolveFixedExpenseStatus(false), 'clear');
    assert.equal(resolveFixedExpenseStatus(null), 'unknown');
    assert.equal(resolveFixedExpenseStatus(undefined), 'unknown');
});

test('월간 리포트 API 응답을 기존 화면 모델로 조합한다', () => {
    const report = composeMonthlyConsumptionReport(
        {
            yearMonth: '2026-07',
            totalSpent: 150000,
            hasPreviousComparison: true,
            monthOverMonthRate: -25,
            fixedExpenseCandidateCount: 2,
        },
        {
            items: [
                { yearMonth: '2026-06', amount: 200000, hasData: true },
                { yearMonth: '2026-07', amount: 150000, hasData: true },
            ],
        },
        {
            parentCategories: [
                {
                    categoryId: 1,
                    categoryName: '식비',
                    amount: 150000,
                    ratio: 100,
                },
            ],
            categories: [
                {
                    parentCategoryId: 1,
                    parentCategoryName: '식비',
                    categoryId: 18,
                    categoryName: '카페/간식',
                    amount: 150000,
                    ratio: 100,
                    previousMonthAmount: 200000,
                    changeRate: -25,
                },
            ],
        },
    );

    assert.equal(report.period, '2026-07');
    assert.equal(report.fixedExpenseCandidateCount, 2);
    assert.deepEqual(
        report.monthlyTrend.map((item) => item.yearMonth),
        ['2026-06', '2026-07'],
    );
    assert.equal(report.parentCategories[0].name, '식비');
    assert.equal(report.categories[0].name, '카페/간식');
    assert.equal(report.categories[0].tone, report.parentCategories[0].tone);
});

test('대분류 차트는 비중 상위 5개와 전용 색상의 그 외 항목으로 요약한다', () => {
    const parentCategories = [
        ['기타', 12],
        ['식비', 30],
        ['교통', 15],
        ['주거', 20],
        ['쇼핑', 10],
        ['여가', 8],
        ['의료', 5],
    ].map(([categoryName, ratio], index) => ({
        categoryId: index + 1,
        categoryName,
        amount: ratio * 1000,
        ratio,
    }));
    const report = composeMonthlyConsumptionReport(
        {
            yearMonth: '2026-07',
            totalSpent: 100000,
            hasPreviousComparison: true,
            monthOverMonthRate: 0,
            fixedExpenseCandidateCount: 0,
        },
        { items: [] },
        {
            parentCategories,
            categories: parentCategories.map((category) => ({
                ...category,
                parentCategoryId: category.categoryId,
                parentCategoryName: category.categoryName,
            })),
        },
    );

    assert.deepEqual(
        report.parentCategories.map((category) => category.name),
        ['식비', '주거', '교통', '기타', '쇼핑', '그 외'],
    );
    assert.equal(report.parentCategories.at(-1).ratio, 13);
    assert.equal(report.parentCategories.at(-1).tone, 'other');
    assert.equal(report.parentCategories[3].tone, 'danger');
    assert.equal(report.parentCategories[4].tone, 'info');
    assert.equal(report.categories.find((category) => category.name === '여가').tone, 'other');
    assert.notEqual(
        report.parentCategories.find((category) => category.name === '기타').tone,
        report.parentCategories.at(-1).tone,
    );
});

test('선고 명세는 상위 5개와 더보기 목록으로 나눈다', () => {
    const categories = Array.from({ length: 8 }, (_, index) => ({ categoryId: index + 1 }));
    const { primaryCategories, additionalCategories } = splitMonthlyCategories(categories);

    assert.deepEqual(
        primaryCategories.map((category) => category.categoryId),
        [1, 2, 3, 4, 5],
    );
    assert.deepEqual(
        additionalCategories.map((category) => category.categoryId),
        [6, 7, 8],
    );
});

test('임시 목업 소스도 API 화면 모델과 같은 필드를 제공한다', async () => {
    const months = await fetchTempMonthlyConsumptionMonths();
    const report = await fetchTempMonthlyConsumptionReport('2026-07');

    assert.equal(months.find((month) => month.value === '2026-07').available, true);
    assert.equal(report.fixedExpenseCandidateCount, 1);
    assert.equal(report.parentCategories.length, report.categories.length);
    assert.equal(report.monthlyTrend.at(-1).yearMonth, '2026-07');
    assert.equal(report.monthlyTrend.at(-1).hasData, true);
});

test('3월 첫 리포트와 4월 두 번째 리포트 fixture 계약을 유지한다', () => {
    assert.equal(REPORTS['2026-02'].status, MONTHLY_REPORT_STATUS.ONBOARDING);
    assert.equal(REPORTS['2026-03'].status, MONTHLY_REPORT_STATUS.FIRST_REPORT);
    assert.equal(REPORTS['2026-04'].status, MONTHLY_REPORT_STATUS.READY);
    assert.equal(REPORTS['2026-03'].hasPreviousComparison, false);
    assert.equal(REPORTS['2026-03'].monthlyTrend.length, 1);
    assert.equal(
        REPORTS['2026-03'].categories.every((category) => category.changeRate === undefined),
        true,
    );
    assert.equal(REPORTS['2026-04'].hasPreviousComparison, true);
    assert.deepEqual(
        REPORTS['2026-04'].monthlyTrend.map((item) => item.month),
        [3, 4],
    );
    assert.deepEqual(REPORTS['2026-04'].fixedExpenseCandidates, []);
});

test('모든 리포트에 최근 6개월 슬롯을 만들고 내역 없는 달은 비워 둔다', () => {
    const aprilSlots = buildMonthlyTrendSlots(
        REPORTS['2026-04'].period,
        REPORTS['2026-04'].monthlyTrend,
    );
    assert.deepEqual(
        aprilSlots.map((item) => item.month),
        [11, 12, 1, 2, 3, 4],
    );
    assert.deepEqual(
        aprilSlots.map((item) => item.hasData),
        [false, false, false, false, true, true],
    );
});

test('5~7월 소비 흐름은 3월 데이터부터 일관되게 누적된다', () => {
    const expectedMonths = {
        '2026-05': [3, 4, 5],
        '2026-06': [3, 4, 5, 6],
        '2026-07': [3, 4, 5, 6, 7],
    };

    Object.entries(expectedMonths).forEach(([period, months]) => {
        const report = REPORTS[period];
        assert.deepEqual(
            report.monthlyTrend.map((item) => item.month),
            months,
        );
        assert.equal(
            report.categories.reduce((sum, category) => sum + category.amount, 0),
            report.totalSpent,
        );
    });
});
