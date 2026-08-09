import {
    ASSET_SUMMARY,
    CHECKING_DETAIL,
    SAVINGS_DETAIL,
    INVESTMENT_DETAIL,
    LOAN_DETAIL,
    PAYMONEY_DETAIL,
} from '@/fixtures/asset';

function clone(value) {
    return JSON.parse(JSON.stringify(value));
}

export async function fetchAssetSummary() {
    return clone(ASSET_SUMMARY);
}

export async function fetchCheckingAccountDetail() {
    return clone(CHECKING_DETAIL);
}

export async function fetchSavingsAccountDetail() {
    return clone(SAVINGS_DETAIL);
}

export async function fetchInvestmentAccountDetail() {
    return clone(INVESTMENT_DETAIL);
}

export async function fetchLoanAccountDetail() {
    return clone(LOAN_DETAIL);
}

export async function fetchPaymoneyDetail() {
    return clone(PAYMONEY_DETAIL);
}

export async function fetchNetWorthTrend() {
    return clone(ASSET_SUMMARY.netWorthTrend);
}
