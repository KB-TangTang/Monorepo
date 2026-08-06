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
