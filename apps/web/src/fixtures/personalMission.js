/*
 * 개인 미션 챌린지 화면에서 사용하는 임시 데이터
 * 실제 API가 완성되면 이 파일 대신 API 응답을 사용하면 됨
 */

export const MOCK_PERSONAL_MISSION_PROFILE = {
    userId: 1,

    // 연동 계좌에서 확보한 최근 소비 데이터의 일수
    availableTransactionDays: 28,

    // 지금까지 확보한 소비 거래 건수
    transactionCount: 64,

    // 개인 맞춤 미션을 열기 위해 필요한 소비 데이터 기간
    requiredTransactionDays: 28,

    // 개인 맞춤 미션을 열기 위해 필요한 최소 거래 건수
    requiredTransactionCount: 50,
};

export const MOCK_ABSOLUTE_MISSION = {
    id: 1,
    missionType: 'ABSOLUTE',
    missionTypeLabel: '절대형',
    category: '배달',
    title: '오늘은 배달앱 0원 도전',
    description: '누구에게나 동일하게 주어지는 기본 미션이에요.',
    currentAmount: 0,
    targetAmount: 0,
    rewardPoints: 10,
};

export const MOCK_RELATIVE_MISSION = {
    id: 2,
    missionType: 'RELATIVE',
    missionTypeLabel: '상대형',
    category: '카페',
    title: '오늘 카페 5,500원 안에서',
    description: '최근 28일 카페 지출 분포로 만든 맞춤 목표예요.',
    currentAmount: 3200,
    targetAmount: 5500,
    rewardPoints: 20,
    streakBonusPoints: 5,
};

export const PERSONAL_MISSION_DIFFICULTIES = [
    {
        id: 'EASY',
        label: '살짝 아끼기',
        shortLabel: '하',
        description: '평소보다 살짝만 줄여도 성공 · 절감률 10~20%',
        targetAmount: 5500,
        rewardPoints: 10,
    },
    {
        id: 'NORMAL',
        label: '제대로 아끼기',
        shortLabel: '중',
        description: '체감되는 절약 · 절감률 20~40%',
        targetAmount: 4400,
        rewardPoints: 20,
    },
    {
        id: 'HARD',
        label: '독하게 아끼기',
        shortLabel: '상',
        description: '고수용 도전 · 절감률 40~50%',
        targetAmount: 3400,
        rewardPoints: 35,
    },
];

export const MOCK_PERSONAL_MISSION_STREAK = [
    { date: 16, status: 'success' },
    { date: 17, status: 'success' },
    { date: 18, status: 'failed' },
    { date: 19, status: 'success' },
    { date: 20, status: 'success' },
    { date: 21, status: 'success' },
    { date: 22, status: 'success' },
    { date: 23, status: 'today' },
    { date: 24, status: 'pending' },
    { date: 25, status: 'pending' },
    { date: 26, status: 'pending' },
    { date: 27, status: 'pending' },
    { date: 28, status: 'pending' },
    { date: 29, status: 'pending' },
];
