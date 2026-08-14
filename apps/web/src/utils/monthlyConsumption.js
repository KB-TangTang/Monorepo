const CATEGORY_TONES = ['primary', 'accent', 'success', 'danger', 'info'];
const TOP_PARENT_CATEGORY_COUNT = 5;
const OTHER_PARENT_CATEGORY_NAME = '그 외';
const OTHER_PARENT_CATEGORY_TONE = 'other';

export const MONTHLY_REPORT_STATUS = Object.freeze({
    ONBOARDING: 'ONBOARDING',
    FIRST_REPORT: 'FIRST_REPORT',
    READY: 'READY',
    CURRENT: 'CURRENT',
});

export function splitMonthlyCategories(categories, previewCount = 5) {
    return {
        primaryCategories: categories.slice(0, previewCount),
        additionalCategories: categories.slice(previewCount),
    };
}

function summarizeParentCategories(categories) {
    const sortedCategories = [...categories].sort(
        (first, second) =>
            second.ratio - first.ratio ||
            second.amount - first.amount ||
            first.categoryName.localeCompare(second.categoryName, 'ko'),
    );
    const topCategories = sortedCategories
        .slice(0, TOP_PARENT_CATEGORY_COUNT)
        .map((category, index) => ({
            ...category,
            name: category.categoryName,
            code: category.categoryName.slice(0, 1),
            tone: CATEGORY_TONES[index],
        }));
    const remainingCategories = sortedCategories.slice(TOP_PARENT_CATEGORY_COUNT);

    if (!remainingCategories.length) {
        return { chartCategories: topCategories, remainingCategories };
    }

    const otherCategory = {
        categoryId: 'other-parent-categories',
        categoryName: OTHER_PARENT_CATEGORY_NAME,
        name: OTHER_PARENT_CATEGORY_NAME,
        code: OTHER_PARENT_CATEGORY_NAME.slice(0, 1),
        amount: remainingCategories.reduce((sum, category) => sum + category.amount, 0),
        ratio: Number(
            remainingCategories.reduce((sum, category) => sum + category.ratio, 0).toFixed(2),
        ),
        tone: OTHER_PARENT_CATEGORY_TONE,
        summarized: true,
    };

    return { chartCategories: [...topCategories, otherCategory], remainingCategories };
}

export function composeMonthlyConsumptionReport(summary, trend, categoryReport, aiAnalysis = null) {
    const { chartCategories: parentCategories, remainingCategories } = summarizeParentCategories(
        categoryReport.parentCategories,
    );
    const parentToneByKey = new Map(
        parentCategories
            .filter((category) => !category.summarized)
            .map((category) => [
                `${category.categoryId ?? 'unclassified'}:${category.categoryName}`,
                category.tone,
            ]),
    );
    remainingCategories.forEach((category) => {
        parentToneByKey.set(
            `${category.categoryId ?? 'unclassified'}:${category.categoryName}`,
            OTHER_PARENT_CATEGORY_TONE,
        );
    });

    return {
        period: summary.yearMonth,
        status: summary.hasPreviousComparison
            ? MONTHLY_REPORT_STATUS.READY
            : MONTHLY_REPORT_STATUS.FIRST_REPORT,
        hasPreviousComparison: summary.hasPreviousComparison,
        totalSpent: summary.totalSpent,
        monthOverMonthRate: summary.monthOverMonthRate,
        fixedExpenseCandidateCount: summary.fixedExpenseCandidateCount,
        confirmedFixedExpenseCount: summary.confirmedFixedExpenseCount,
        aiAnalysisStatus: aiAnalysis?.status ?? 'NOT_REQUESTED',
        feedbacks: Array.isArray(aiAnalysis?.feedbacks) ? aiAnalysis.feedbacks.slice(0, 3) : [],
        savingsAnalogy: aiAnalysis?.savingsAnalogy ?? null,
        monthlyTrend: trend.items.map((item) => ({
            yearMonth: item.yearMonth,
            month: Number(item.yearMonth.slice(5)),
            amount: item.amount,
            hasData: item.hasData,
        })),
        parentCategories,
        categories: categoryReport.categories.map((category) => ({
            ...category,
            name: category.categoryName,
            code: category.categoryName.slice(0, 1),
            tone:
                parentToneByKey.get(
                    `${category.parentCategoryId ?? 'unclassified'}:${category.parentCategoryName}`,
                ) ?? 'muted',
        })),
    };
}

export function resolveMonthlySavingsAnalogyCard(report) {
    if (report.savingsAnalogy) {
        return {
            variant: 'saving',
            eyebrow: '이번 달의 절약 한 장면',
            title: report.savingsAnalogy,
            description: '이 흐름, 다음 달에도 이어가봐요.',
        };
    }

    if (report.hasPreviousComparison && Number(report.monthOverMonthRate) > 0) {
        return {
            variant: 'increase',
            eyebrow: '소비 흐름 점검',
            title: '지난달보다 소비가 늘어났어요',
            description: '가장 자주 쓴 항목부터 가볍게 점검해봐요.',
        };
    }

    return {
        variant: 'start',
        eyebrow: '다음 달을 위한 한 걸음',
        title: '탕이와 함께 절약해봐요',
        description: report.hasPreviousComparison
            ? '이번 달 소비 흐름을 살피고, 다음 목표를 정해봐요.'
            : '이번 달 소비를 기준으로 다음 달 목표를 세워봐요.',
    };
}

export function formatWon(value) {
    return `${new Intl.NumberFormat('ko-KR').format(value)}원`;
}

export function formatPeriod(period) {
    const [year, month] = period.split('-').map(Number);
    return `${year}년 ${month}월`;
}

export function getPreviousPeriod(referenceDate = new Date()) {
    const previousMonth = new Date(referenceDate.getFullYear(), referenceDate.getMonth() - 1, 1);
    return `${previousMonth.getFullYear()}-${String(previousMonth.getMonth() + 1).padStart(2, '0')}`;
}

export function isAvailableReportMonth(month, referenceDate = new Date()) {
    if (month.status === MONTHLY_REPORT_STATUS.ONBOARDING) {
        return true;
    }
    return month.hasReport && month.value <= getPreviousPeriod(referenceDate);
}

export function resolveSelectedReportPeriod(months, requestedPeriod, referenceDate = new Date()) {
    const availableMonths = months
        .filter((month) => isAvailableReportMonth(month, referenceDate))
        .sort((first, second) => second.value.localeCompare(first.value));
    const requestedMonth = availableMonths.find((month) => month.value === requestedPeriod);

    return requestedMonth?.value ?? availableMonths[0]?.value ?? '';
}

export function isLatestAvailableCompletedReport(
    months,
    selectedPeriod,
    referenceDate = new Date(),
) {
    const latestCompletedMonth = months
        .filter(
            (month) =>
                month.hasReport &&
                month.status !== MONTHLY_REPORT_STATUS.ONBOARDING &&
                isAvailableReportMonth(month, referenceDate),
        )
        .sort((first, second) => second.value.localeCompare(first.value))[0];

    return latestCompletedMonth?.value === selectedPeriod;
}

export async function fetchMonthlyConsumptionState(month, reportFetcher) {
    if (month.status === MONTHLY_REPORT_STATUS.ONBOARDING) {
        return { period: month.value, status: MONTHLY_REPORT_STATUS.ONBOARDING };
    }
    return reportFetcher(month.value);
}

export function formatChangeRate(rate) {
    if (rate === null || rate === undefined) {
        return '비교 없음';
    }
    if (rate === 0) {
        return '−0%';
    }
    return `${rate > 0 ? '▲' : '▼'}${Math.abs(rate)}%`;
}

export function resolveReportState({ loading, error, report }) {
    if (loading) {
        return 'loading';
    }
    if (error) {
        return 'error';
    }
    if (!report) {
        return 'empty';
    }
    if (report.status === MONTHLY_REPORT_STATUS.ONBOARDING) {
        return 'onboarding';
    }
    if (
        report.status === MONTHLY_REPORT_STATUS.FIRST_REPORT ||
        report.hasPreviousComparison === false
    ) {
        return 'first-report';
    }
    if (report.status === MONTHLY_REPORT_STATUS.READY) {
        return 'ready';
    }
    return 'ready';
}

export function resolveFixedExpenseStatus(candidates) {
    if (typeof candidates === 'number') {
        return candidates > 0 ? 'detected' : 'clear';
    }
    if (Array.isArray(candidates)) {
        return candidates.length > 0 ? 'detected' : 'clear';
    }
    if (candidates === false) {
        return 'clear';
    }
    return 'unknown';
}

export function canOpenFixedExpenseSavings(candidateCount, confirmedCount) {
    return Number(candidateCount) > 0 || Number(confirmedCount) > 0;
}

export function buildMonthlyTrendSlots(period, monthlyTrend = []) {
    if (!period) {
        return [];
    }
    const [currentYear, currentMonth] = period.split('-').map(Number);
    const trendByPeriod = new Map(
        monthlyTrend.map((item) => {
            const inferredYear = item.month <= currentMonth ? currentYear : currentYear - 1;
            const yearMonth =
                item.yearMonth ?? `${inferredYear}-${String(item.month).padStart(2, '0')}`;
            return [yearMonth, item];
        }),
    );

    return Array.from({ length: 6 }, (_, index) => {
        const date = new Date(currentYear, currentMonth - 6 + index, 1);
        const yearMonth = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
        const source = trendByPeriod.get(yearMonth);
        const amount = source?.amount ?? null;
        return {
            yearMonth,
            month: date.getMonth() + 1,
            amount,
            hasData: source?.hasData ?? amount !== null,
            active: index === 5,
        };
    });
}
