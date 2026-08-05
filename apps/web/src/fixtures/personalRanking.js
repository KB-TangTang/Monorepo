/*
 * 개인 미션 월간 랭킹 화면용 임시 데이터
 *
 * 실제 API 연동 후에는 이 파일 대신
 * 개인 미션 랭킹 조회 API의 응답을 사용합니다.
 */

export const MOCK_PERSONAL_RANKING_MONTHS = [
    {
        value: '2026-06',
        year: 2026,
        month: 6,
        available: true,
    },
    {
        value: '2026-07',
        year: 2026,
        month: 7,
        available: true,
    },
    {
        value: '2026-08',
        year: 2026,
        month: 8,
        available: false,
    },
];

export const MOCK_PERSONAL_RANKINGS = {
    '2026-06': {
        period: '2026-06',
        percentile: 18,
        myRank: 1420,
        totalUsers: 7900,
        score: 390,
        streakDays: 4,
        bestStreakDays: 7,
        podium: [
            {
                rank: 1,
                name: '민서',
                score: 1280,
            },
            {
                rank: 2,
                name: '준호',
                score: 1100,
            },
            {
                rank: 3,
                name: '서영',
                score: 980,
            },
        ],
    },

    '2026-07': {
        period: '2026-07',
        percentile: 12,
        myRank: 986,
        totalUsers: 8214,
        score: 480,
        streakDays: 5,
        bestStreakDays: 9,
        podium: [
            {
                rank: 1,
                name: '서영',
                score: 1340,
            },
            {
                rank: 2,
                name: '준호',
                score: 1120,
            },
            {
                rank: 3,
                name: '민서',
                score: 1020,
            },
        ],
    },
};

// 추후 AI 명예 타이틀 추천 API 응답으로 교체할 목데이터입니다.
export const MOCK_PERSONAL_CERTIFICATE_TITLES = [
    '상위 12%의 판결력',
    '이번 달 진짜 무죄',
    '절약 판결 갱신',
];
