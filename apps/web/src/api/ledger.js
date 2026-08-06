import { getLedgerMonths, getLedgerTransactions } from '@/fixtures/ledger';
import { shiftPeriod } from '@/utils/ledger';

export function sumByDirection(transactions) {
    let totalSpent = 0;
    let totalDeposit = 0;
    for (const tx of transactions) {
        if (tx.classification === 'CONSUMPTION') {
            totalSpent += Math.abs(tx.amount);
        } else if (tx.classification === 'INCOME') {
            totalDeposit += tx.amount;
        }
    }
    return { totalSpent, totalDeposit };
}

export async function fetchLedgerMonths() {
    return getLedgerMonths();
}

export async function fetchLedgerSummary(period) {
    const transactions = getLedgerTransactions(period);
    const { totalSpent, totalDeposit } = sumByDirection(transactions);

    const previousSpent = sumByDirection(getLedgerTransactions(shiftPeriod(period, -1))).totalSpent;
    const monthOverMonthRate =
        previousSpent > 0 ? Math.round(((totalSpent - previousSpent) / previousSpent) * 100) : 0;

    const paymentMethods = [...new Set(transactions.map((tx) => tx.paymentMethod))];

    return {
        period,
        totalSpent,
        totalDeposit,
        monthOverMonthRate,
        paymentMethods,
    };
}

export async function fetchLedgerTransactions(period) {
    return getLedgerTransactions(period);
}

/* 검색 화면용. 데이터가 있는 모든 월의 거래를 합쳐서 반환한다 — 검색은 특정 월에 갇히지 않는다. */
export async function fetchLedgerSearchTransactions() {
    const months = getLedgerMonths().filter((month) => month.hasData);
    return months.flatMap((month) => getLedgerTransactions(month.value));
}
