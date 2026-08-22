export const EXPENSE_CATEGORIES = [
    {
        id: 'food',
        name: '식비',
        icon: 'Cake',
        children: [
            { id: 'food-dining', name: '음식점/외식' },
            { id: 'food-delivery', name: '배달앱' },
            { id: 'food-cafe', name: '카페/간식' },
            { id: 'food-convenience', name: '편의점' },
        ],
    },
    {
        id: 'shopping',
        name: '쇼핑',
        icon: 'ShoppingBag',
        children: [
            { id: 'shopping-online', name: '온라인쇼핑' },
            { id: 'shopping-fashion', name: '패션' },
            { id: 'shopping-beauty', name: '뷰티' },
        ],
    },
    {
        id: 'transport',
        name: '교통',
        icon: 'Truck',
        children: [
            { id: 'transport-public', name: '대중교통' },
            { id: 'transport-taxi', name: '택시/모빌리티' },
            { id: 'transport-fuel', name: '주유/충전' },
            { id: 'transport-parking', name: '주차/통행료' },
        ],
    },
    {
        id: 'housing',
        name: '주거/공과금',
        icon: 'Home',
        children: [
            { id: 'housing-rent', name: '월세' },
            { id: 'housing-fee', name: '관리비' },
            { id: 'housing-utility', name: '전기·가스·수도' },
        ],
    },
    {
        id: 'living',
        name: '생활',
        icon: 'ShoppingCart',
        children: [
            { id: 'living-grocery', name: '장보기/마트' },
            { id: 'living-supplies', name: '생활용품' },
            { id: 'living-laundry', name: '세탁비' },
            { id: 'living-gift', name: '경조사/선물' },
            { id: 'living-child', name: '자녀' },
            { id: 'living-pet', name: '반려동물' },
        ],
    },
    {
        id: 'subscription',
        name: '구독/디지털',
        icon: 'DevicePhoneMobile',
        children: [
            { id: 'subscription-telecom', name: '통신비' },
            { id: 'subscription-ott', name: 'OTT' },
            { id: 'subscription-music', name: '음원' },
            { id: 'subscription-app', name: '앱·게임' },
            { id: 'subscription-software', name: '소프트웨어·클라우드' },
        ],
    },
    {
        id: 'culture',
        name: '문화/여가',
        icon: 'Film',
        children: [
            { id: 'culture-show', name: '영화·공연·전시' },
            { id: 'culture-book', name: '도서' },
            { id: 'culture-hobby', name: '취미' },
            { id: 'culture-leisure', name: '레저' },
        ],
    },
    {
        id: 'travel',
        name: '여행',
        icon: 'PaperAirplane',
        children: [
            { id: 'travel-ticket', name: '교통권' },
            { id: 'travel-lodging', name: '숙박' },
            { id: 'travel-package', name: '관광·여행상품' },
        ],
    },
    {
        id: 'health',
        name: '건강',
        icon: 'Heart',
        children: [
            { id: 'health-hospital', name: '병원' },
            { id: 'health-pharmacy', name: '약국' },
            { id: 'health-fitness', name: '운동시설' },
            { id: 'health-care', name: '건강관리' },
        ],
    },
    {
        id: 'education',
        name: '교육/자기계발',
        icon: 'AcademicCap',
        children: [
            { id: 'education-academy', name: '학원' },
            { id: 'education-online', name: '온라인 강의' },
            { id: 'education-exam', name: '시험·자격증' },
            { id: 'education-material', name: '학습 교재' },
        ],
    },
    {
        id: 'finance',
        name: '금융/보험',
        icon: 'ShieldCheck',
        children: [
            { id: 'finance-insurance', name: '보험료' },
            { id: 'finance-interest', name: '이자' },
            { id: 'finance-fee', name: '수수료' },
            { id: 'finance-product', name: '금융상품' },
        ],
    },
    {
        id: 'etc',
        name: '기타',
        icon: 'EllipsisHorizontalCircle',
        children: [],
    },
];

export const INCOME_CATEGORIES = [
    { id: 'income-salary', name: '급여', icon: 'Banknotes' },
    { id: 'income-bonus', name: '상여금', icon: 'Gift' },
    { id: 'income-allowance', name: '용돈', icon: 'Wallet' },
    { id: 'income-side', name: '부수입', icon: 'Sparkles' },
    { id: 'income-interest', name: '이자/배당', icon: 'BuildingLibrary' },
    { id: 'income-refund', name: '환급/캐시백', icon: 'ReceiptRefund' },
    { id: 'income-etc', name: '수입 기타', icon: 'EllipsisHorizontalCircle' },
];
