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
    return month.hasReport && month.value <= getPreviousPeriod(referenceDate);
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
    return 'ready';
}
