export const AVAILABLE_MONTHS = [
    { value: '2026-05', hasData: false },
    { value: '2026-06', hasData: true },
    { value: '2026-07', hasData: true },
    { value: '2026-08', hasData: false },
];

const TRANSACTIONS = {
    '2026-06': [
        {
            id: 'tx-2026-06-01',
            date: '2026-06-30',
            merchant: '월급',
            category: '급여',
            paymentMethod: '입금',
            amount: 3350000,
        },
        {
            id: 'tx-2026-06-02',
            date: '2026-06-24',
            merchant: '스타벅스 성수역점',
            category: '카페/간식',
            paymentMethod: '신한카드',
            amount: -6800,
        },
        {
            id: 'tx-2026-06-03',
            date: '2026-06-20',
            merchant: '배민 · 연희동찌개',
            category: '배달앱',
            paymentMethod: 'KB국민카드',
            amount: -23400,
        },
        {
            id: 'tx-2026-06-04',
            date: '2026-06-12',
            merchant: '이마트 성수점',
            category: '장보기/마트',
            paymentMethod: '신한카드',
            amount: -168200,
        },
        {
            id: 'tx-2026-06-05',
            date: '2026-06-05',
            merchant: '지하철',
            category: '대중교통',
            paymentMethod: '체크카드',
            amount: -12300,
        },
    ],
    '2026-07': [
        {
            id: 'tx-2026-07-01',
            date: '2026-07-29',
            merchant: '오늘의집',
            category: '온라인쇼핑',
            paymentMethod: '신한카드',
            amount: -48900,
        },
        {
            id: 'tx-2026-07-02',
            date: '2026-07-29',
            merchant: 'CU 성수점',
            category: '편의점',
            paymentMethod: 'KB국민 302',
            amount: -5400,
        },
        {
            id: 'tx-2026-07-03',
            date: '2026-07-25',
            merchant: '월급',
            category: '급여',
            paymentMethod: '입금',
            amount: 3420000,
        },
        {
            id: 'tx-2026-07-04',
            date: '2026-07-22',
            merchant: '배민 · 연희동찌개',
            category: '배달앱',
            paymentMethod: 'KB국민카드',
            amount: -19800,
        },
        {
            id: 'tx-2026-07-05',
            date: '2026-07-18',
            merchant: '스타벅스 성수역점',
            category: '카페/간식',
            paymentMethod: '신한카드',
            amount: -7200,
        },
        {
            id: 'tx-2026-07-06',
            date: '2026-07-14',
            merchant: '지하철',
            category: '대중교통',
            paymentMethod: '체크카드',
            amount: -14700,
        },
        {
            id: 'tx-2026-07-07',
            date: '2026-07-14',
            merchant: 'CGV 용산아이파크몰',
            category: '영화·공연·전시',
            paymentMethod: '신한카드',
            amount: -32000,
        },
        {
            id: 'tx-2026-07-08',
            date: '2026-07-09',
            merchant: '이마트 성수점',
            category: '장보기/마트',
            paymentMethod: 'KB국민카드',
            amount: -71600,
        },
        {
            id: 'tx-2026-07-09',
            date: '2026-07-03',
            merchant: '올리브영 홍대점',
            category: '뷰티',
            paymentMethod: '체크카드',
            amount: -29700,
        },
    ],
};

function clone(value) {
    return JSON.parse(JSON.stringify(value));
}

export function getLedgerMonths() {
    return clone(AVAILABLE_MONTHS);
}

export function getLedgerTransactions(period) {
    return clone(TRANSACTIONS[period] ?? []);
}
