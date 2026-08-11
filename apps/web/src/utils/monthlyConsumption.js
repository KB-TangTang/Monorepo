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
    const hasMonthlyContent = month.hasReport || month.status === 'onboarding';
    return hasMonthlyContent && month.value <= getPreviousPeriod(referenceDate);
}

export function resolveSelectedReportPeriod(months, requestedPeriod, referenceDate = new Date()) {
    const availableMonths = months
        .filter((month) => isAvailableReportMonth(month, referenceDate))
        .sort((first, second) => second.value.localeCompare(first.value));
    const requestedMonth = availableMonths.find((month) => month.value === requestedPeriod);

    return requestedMonth?.value ?? availableMonths[0]?.value ?? '';
}

export function formatChangeRate(rate) {
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
    if (Array.isArray(candidates)) {
        return candidates.length > 0 ? 'detected' : 'clear';
    }
    if (candidates === false) {
        return 'clear';
    }
    return 'unknown';
}

export function buildMonthlyTrendSlots(period, monthlyTrend = []) {
    const currentMonth = Number(period?.slice(5));
    const trendByMonth = new Map(monthlyTrend.map((item) => [item.month, item.amount]));

    return Array.from({ length: 6 }, (_, index) => {
        const month = ((currentMonth - 6 + index + 12) % 12) + 1;
        const amount = trendByMonth.get(month) ?? null;
        return { month, amount, hasData: amount !== null, active: index === 5 };
    });
}
