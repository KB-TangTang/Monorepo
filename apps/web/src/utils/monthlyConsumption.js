const CATEGORY_TONES = ['primary', 'accent', 'success', 'muted', 'soft'];

export function composeMonthlyConsumptionReport(summary, trend, categoryReport) {
    return {
        period: summary.yearMonth,
        status: 'report',
        hasPreviousComparison: summary.hasPreviousComparison,
        totalSpent: summary.totalSpent,
        monthOverMonthRate: summary.monthOverMonthRate,
        fixedExpenseCandidateCount: summary.fixedExpenseCandidateCount,
        monthlyTrend: trend.items.map((item) => ({
            yearMonth: item.yearMonth,
            month: Number(item.yearMonth.slice(5)),
            amount: item.amount,
            hasData: item.hasData,
        })),
        categories: categoryReport.categories.map((category, index) => ({
            ...category,
            name: category.categoryName,
            code: category.categoryName.slice(0, 1),
            tone: CATEGORY_TONES[index % CATEGORY_TONES.length],
        })),
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
    if (month.status === 'onboarding') {
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
    if (report.status === 'onboarding') {
        return 'onboarding';
    }
    if (report.hasPreviousComparison === false) {
        return 'first-report';
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
