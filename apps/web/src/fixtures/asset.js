const ASSET_SUMMARY = {
    netWorth: 12846000,
    monthOverMonthChange: 320000,
    trend: [12200000, 12350000, 12100000, 12500000, 12700000, 12846000],
    composition: [
        { code: 'checking', label: '입출금', amount: 2066800, tone: 'navy' },
        { code: 'savings', label: '예적금', amount: 5420000, tone: 'blue' },
        { code: 'investment', label: '투자', amount: 3214200, tone: 'teal' },
        { code: 'etc', label: '포인트·기타', amount: 2445000, tone: 'gray' },
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
            code: 'loan',
            label: '대출',
            badge: 'L',
            count: 1,
            amount: -1500000,
            tone: 'danger',
        },
    ],
};

export { ASSET_SUMMARY };
