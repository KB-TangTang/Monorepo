const AVAILABLE_MONTHS = [
    { value: '2026-09', year: 2026, month: 9, hasReport: false },
    { value: '2026-08', year: 2026, month: 8, hasReport: false },
    { value: '2026-07', year: 2026, month: 7, hasReport: true },
    { value: '2026-06', year: 2026, month: 6, hasReport: true },
    { value: '2026-05', year: 2026, month: 5, hasReport: true },
    { value: '2026-04', year: 2026, month: 4, hasReport: false },
];

const REPORTS = {
    '2026-07': {
        period: '2026-07',
        hasChallengeHistory: true,
        isFirstServiceMonth: false,
        challengeName: '메인 챌린지',
        missionSuccessRate: 82,
        successfulDays: 24,
        monthOverMonthPercentagePoint: 9,
        weeklyResults: [
            { week: 1, successDays: 6, totalDays: 7, successRate: 86 },
            { week: 2, successDays: 5, totalDays: 7, successRate: 71 },
            { week: 3, successDays: 7, totalDays: 7, successRate: 100 },
            { week: 4, successDays: 6, totalDays: 7, successRate: 86 },
        ],
        bestStreakDays: 8,
        bestWeekday: '화요일',
        challengeDays: 30,
        earnedPoints: 245,
        difficulties: [
            { level: '하', attempts: 12, successRate: 92, tone: 'low' },
            { level: '중', attempts: 14, successRate: 79, tone: 'middle' },
            { level: '상', attempts: 4, successRate: 50, tone: 'high' },
        ],
        difficultySummary: '중 난이도가 안정적이에요',
        groupRecord: {
            participatingGroups: 3,
            wins: 2,
            losses: 1,
            acquittals: 1,
            convictions: 1,
            dismissals: 2,
        },
        savedAmount: 96000,
        overspentAmount: 22000,
        netSavings: 74000,
        annualizedNetSavings: 888000,
        categories: [
            { code: '카', name: '카페', days: 8, amount: 40000, status: 'success' },
            { code: '배', name: '배달', days: 7, amount: 28000, status: 'success' },
            { code: '쇼', name: '쇼핑', days: 5, amount: 18000, status: 'success' },
            { code: '편', name: '편의점', days: 4, amount: 10000, status: 'success' },
            { code: '야', name: '야식', days: 2, amount: 22000, status: 'failure' },
            { code: '택', name: '택시', days: 4, amount: 0, status: 'failure' },
        ],
    },
    '2026-06': {
        period: '2026-06',
        hasChallengeHistory: false,
        isFirstServiceMonth: false,
    },
    '2026-05': {
        period: '2026-05',
        hasChallengeHistory: true,
        isFirstServiceMonth: true,
    },
};

export { AVAILABLE_MONTHS, REPORTS };
