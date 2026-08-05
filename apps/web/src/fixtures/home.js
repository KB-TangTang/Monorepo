/*
 * 홈 화면 UI 확인용 임시 데이터.
 * 개발 환경에서만 사용하고 실제 서비스에서는 GET /api/v1/home 응답을 사용한다.
 */

export const MOCK_HOME_EMPTY = {
    userName: '지윤',
    notificationCount: 0,
    challenge: null,
    pendingVote: {
        count: 0,
        closingSoonHours: null,
    },
    assetSummary: {
        netAssetAmount: 12840000,
        monthlyChangeRate: null,
    },
    honorCourt: null,
};

export const MOCK_HOME_ACTIVE = {
    userName: '지윤',
    notificationCount: 2,
    challenge: {
        title: '카페 8천원 방어전',
        limitAmount: 8000,
        spentAmount: 4500,
        remainingAmount: 3500,
        progressRate: 56,
    },
    pendingVote: {
        count: 1,
        closingSoonHours: 2,
    },
    assetSummary: {
        netAssetAmount: 12840000,
        monthlyChangeRate: 8.4,
    },
    honorCourt: {
        month: 6,
        rank: 128,
        topPercent: 12,
        reportOpenDays: 18,
    },
};
