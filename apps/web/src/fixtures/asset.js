const NET_WORTH_TREND_VALUES = [12200000, 12350000, 12100000, 12500000, 12700000, 12846000];

const ASSET_SUMMARY = {
    netWorth: 12846000,
    monthOverMonthChange: 320000,
    trend: NET_WORTH_TREND_VALUES,
    composition: [
        { code: 'checking', label: '입출금', amount: 2066800, tone: 'navy' },
        { code: 'savings', label: '예적금', amount: 5420000, tone: 'blue' },
        { code: 'investment', label: '투자', amount: 3214200, tone: 'teal' },
        { code: 'paymoney', label: '페이머니', amount: 244500, tone: 'gray' },
        { code: 'loan', label: '대출', amount: -1500000, tone: 'danger' },
    ],
    accounts: [
        {
            code: 'checking',
            label: '입출금 계좌',
            badge: 'W',
            count: 2,
            amount: 2066800,
            tone: 'navy',
        },
        {
            code: 'savings',
            label: '예금·적금',
            badge: 'S',
            count: 2,
            amount: 5420000,
            tone: 'blue',
        },
        {
            code: 'investment',
            label: '투자·증권',
            badge: 'I',
            count: 1,
            amount: 3214200,
            tone: 'teal',
        },
        {
            code: 'paymoney',
            label: '페이머니',
            badge: 'P',
            count: 1,
            amount: 244500,
            tone: 'gray',
        },
        {
            code: 'loan',
            label: '대출',
            badge: 'L',
            count: 1,
            amount: -1500000,
            tone: 'danger',
        },
    ],
    netWorthTrend: {
        months: ['1월', '2월', '3월', '4월', '5월', '6월'],
        netWorth: NET_WORTH_TREND_VALUES,
        totalDebt: [1800000, 1750000, 1700000, 1650000, 1600000, 1500000],
    },
};

const CHECKING_DETAIL = {
    total: 2066800,
    syncedLabel: '방금 동기화',
    accounts: [
        {
            code: 'kakaobank-checking',
            label: '카카오뱅크',
            meta: '****1234 · 입출금통장',
            amount: 1200800,
            badge: 'K',
            tone: 'accent',
        },
        {
            code: 'kb-checking',
            label: '국민은행',
            meta: '****5678 · 입출금통장',
            amount: 866000,
            badge: '국',
            tone: 'navy',
        },
    ],
};

const SAVINGS_DETAIL = {
    total: 5420000,
    syncedLabel: '방금 동기화',
    accounts: [
        {
            code: 'shinhan-savings',
            label: '신한 정기적금',
            meta: '****9012 · 만기 2027.03',
            amount: 3000000,
            badge: '신',
            tone: 'blue',
        },
        {
            code: 'kakaobank-freesaving',
            label: '카카오뱅크 자유적금',
            meta: '****3344 · 만기 2026.11',
            amount: 1420000,
            badge: 'K',
            tone: 'accent',
        },
        {
            code: 'woori-deposit',
            label: '우리은행 정기예금',
            meta: '****7788 · 만기 2027.01',
            amount: 1000000,
            badge: '우',
            tone: 'teal',
        },
    ],
};

const INVESTMENT_DETAIL = {
    totalValuation: 3214200,
    totalCost: 2940000,
    asOfLabel: '09:41',
    holdings: [
        {
            code: 'samsung',
            name: '삼성전자',
            badge: '삼',
            tone: 'navy',
            quantity: 20,
            amount: 1600000,
            gainAmount: 100000,
            returnRate: 0.0667,
        },
        {
            code: 'kakao',
            name: '카카오',
            badge: '카',
            tone: 'accent',
            quantity: 10,
            amount: 500000,
            gainAmount: 20000,
            returnRate: 0.0417,
        },
        {
            code: 'tiger-sp500',
            name: 'TIGER 미국S&P500',
            badge: 'T',
            tone: 'teal',
            quantity: 30,
            amount: 1114200,
            gainAmount: 154200,
            returnRate: 0.1606,
        },
    ],
};

const LOAN_DETAIL = {
    total: 1500000,
    loans: [
        {
            code: 'hana-credit-loan',
            label: '하나은행 신용대출',
            meta: '금리 4.5% · 만기 2027.12',
            amount: 1500000,
            badge: '하',
            tone: 'danger',
        },
    ],
};

export { ASSET_SUMMARY, CHECKING_DETAIL, SAVINGS_DETAIL, INVESTMENT_DETAIL, LOAN_DETAIL };
