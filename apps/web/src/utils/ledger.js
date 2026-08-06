export function formatWon(value) {
    return `${new Intl.NumberFormat('ko-KR').format(value)}원`;
}

export function formatMonthLabel(period) {
    const [year, month] = period.split('-').map(Number);
    return `${year}년 ${month}월`;
}

const WEEKDAY_LABELS = ['일', '월', '화', '수', '목', '금', '토'];

/* new Date('YYYY-MM-DD') 는 UTC 자정으로 해석돼 타임존에 따라 하루가 밀릴 수 있어
 * 직접 파싱한다(로컬 Date 생성자는 연·월·일을 그대로 로컬 날짜로 취급한다). */
export function formatDayLabel(dateStr) {
    const [year, month, day] = dateStr.split('-').map(Number);
    const date = new Date(year, month - 1, day);
    return `${month}월 ${day}일 · ${WEEKDAY_LABELS[date.getDay()]}요일`;
}

export function shiftPeriod(period, delta) {
    const [year, month] = period.split('-').map(Number);
    const shifted = new Date(year, month - 1 + delta, 1);
    return `${shifted.getFullYear()}-${String(shifted.getMonth() + 1).padStart(2, '0')}`;
}

export function resolveDefaultLedgerPeriod(months) {
    const withData = months.filter((month) => month.hasData);
    if (withData.length === 0) {
        return months[0]?.value ?? '';
    }
    return withData.reduce((latest, month) => (month.value > latest.value ? month : latest)).value;
}

export function resolveLedgerState({ loading, error, data }) {
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

/* 달력 6주 그리드. 이전/다음 달 날짜는 inCurrentMonth: false 로 채워 비활성 표시한다. */
export function buildCalendarWeeks(period, transactionDates) {
    const [year, month] = period.split('-').map(Number);
    const firstWeekday = new Date(year, month - 1, 1).getDay();
    const daysInMonth = new Date(year, month, 0).getDate();
    const daysInPrevMonth = new Date(year, month - 1, 0).getDate();

    const cells = [];
    for (let i = firstWeekday - 1; i >= 0; i -= 1) {
        cells.push({ day: daysInPrevMonth - i, inCurrentMonth: false, dateStr: null });
    }
    for (let day = 1; day <= daysInMonth; day += 1) {
        const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
        cells.push({
            day,
            inCurrentMonth: true,
            dateStr,
            hasTransactions: transactionDates.has(dateStr),
        });
    }
    let nextDay = 1;
    while (cells.length % 7 !== 0) {
        cells.push({ day: nextDay, inCurrentMonth: false, dateStr: null });
        nextDay += 1;
    }

    const weeks = [];
    for (let i = 0; i < cells.length; i += 7) {
        weeks.push(cells.slice(i, i + 7));
    }
    return weeks;
}

/* 날짜 내림차순 그룹. 각 그룹에 그날 순합계(지출 음수 + 입금 양수)를 함께 담는다. */
export function groupTransactionsByDate(transactions) {
    const byDate = new Map();
    for (const tx of transactions) {
        if (!byDate.has(tx.date)) {
            byDate.set(tx.date, []);
        }
        byDate.get(tx.date).push(tx);
    }

    return [...byDate.entries()]
        .sort(([a], [b]) => b.localeCompare(a))
        .map(([date, items]) => ({
            date,
            items,
            netAmount: items.reduce((sum, item) => sum + item.amount, 0),
        }));
}

export function filterTransactionsByTab(transactions, tab) {
    if (tab === 'ALL') {
        return transactions;
    }
    return transactions.filter((tx) => tx.classification === tab);
}

export function resolveAnchorDate(groupedTransactions, requestedDate) {
    if (!requestedDate) {
        return null;
    }
    const exists = groupedTransactions.some((group) => group.date === requestedDate);
    return exists ? requestedDate : null;
}
