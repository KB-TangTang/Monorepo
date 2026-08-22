export function formatWon(value) {
    return `${new Intl.NumberFormat('ko-KR').format(value)}원`;
}

export function formatSavingsWon(value) {
    return `-${formatWon(Math.abs(value))}`;
}

export function formatPaymentDate(value) {
    const [year, month, day] = value.split('-').map(Number);
    return `${year}.${String(month).padStart(2, '0')}.${String(day).padStart(2, '0')}`;
}

export function formatShortDate(value) {
    const [, month, day] = value.split('-').map(Number);
    return `${month}월 ${day}일`;
}

export function formatBillingCycle(cycle) {
    if (cycle?.type === 'monthly') {
        return `매월 ${cycle.day}일`;
    }
    return '결제 주기 미정';
}

export function calculateDday(value, referenceDate = new Date()) {
    const [year, month, day] = value.split('-').map(Number);
    const target = Date.UTC(year, month - 1, day);
    const reference = Date.UTC(
        referenceDate.getFullYear(),
        referenceDate.getMonth(),
        referenceDate.getDate(),
    );
    return Math.ceil((target - reference) / 86400000);
}

export function formatDday(value, referenceDate = new Date()) {
    const day = calculateDday(value, referenceDate);
    if (day === 0) {
        return 'D-day';
    }
    return day > 0 ? `D-${day}` : `D+${Math.abs(day)}`;
}

export function resolveFixedExpenseState({ loading, error, data }) {
    if (loading) {
        return 'loading';
    }
    if (error) {
        return 'error';
    }
    if (!data) {
        return 'empty';
    }
    return 'ready';
}
