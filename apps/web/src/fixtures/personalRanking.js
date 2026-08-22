import profile1 from '@/assets/images/profile_ex/profile1.png';
import profile2 from '@/assets/images/profile_ex/profile2.png';
import profile3 from '@/assets/images/profile_ex/profile3.png';
import profile4 from '@/assets/images/profile_ex/profile4.png';
import profile5 from '@/assets/images/profile_ex/profile5.png';
import profile6 from '@/assets/images/profile_ex/profile6.png';

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
        totalUsers: 7900,
        streakDays: 4,
        bestStreakDays: 7,
        topRankings: [
            {
                rank: 1,
                nickname: '민서',
                totalScore: 1280,
            },
            {
                rank: 2,
                nickname: '준호',
                totalScore: 1100,
            },
            {
                rank: 3,
                nickname: '서영',
                totalScore: 980,
            },
            { rank: 4, nickname: '지우', totalScore: 940, profileImageUrl: profile2 },
            { rank: 5, nickname: '하린', totalScore: 900, profileImageUrl: profile3 },
            { rank: 6, nickname: '도윤', totalScore: 860, profileImageUrl: profile4 },
            { rank: 7, nickname: '유나', totalScore: 820, profileImageUrl: profile5 },
            { rank: 8, nickname: '지민', totalScore: 780, profileImageUrl: profile6 },
            { rank: 9, nickname: '현우', totalScore: 740, profileImageUrl: profile2 },
            { rank: 10, nickname: '서아', totalScore: 700, profileImageUrl: profile3 },
        ],
        myRanking: {
            rank: 1420,
            nickname: '나',
            totalScore: 390,
            topPercent: 18,
            profileImageUrl: profile1,
        },
    },

    '2026-07': {
        period: '2026-07',
        totalUsers: 8214,
        streakDays: 5,
        bestStreakDays: 9,
        topRankings: [
            {
                rank: 1,
                nickname: '서영',
                totalScore: 1340,
            },
            {
                rank: 2,
                nickname: '준호',
                totalScore: 1120,
            },
            {
                rank: 3,
                nickname: '민서',
                totalScore: 1020,
            },
            { rank: 4, nickname: '지우', totalScore: 980, profileImageUrl: profile2 },
            { rank: 5, nickname: '하린', totalScore: 950, profileImageUrl: profile3 },
            { rank: 6, nickname: '도윤', totalScore: 920, profileImageUrl: profile4 },
            { rank: 7, nickname: '유나', totalScore: 890, profileImageUrl: profile5 },
            { rank: 8, nickname: '지민', totalScore: 860, profileImageUrl: profile6 },
            { rank: 9, nickname: '현우', totalScore: 830, profileImageUrl: profile2 },
            { rank: 10, nickname: '서아', totalScore: 800, profileImageUrl: profile3 },
        ],
        myRanking: {
            rank: 986,
            nickname: '나',
            totalScore: 480,
            topPercent: 12,
            profileImageUrl: profile1,
        },
    },
};

// 추후 AI 명예 타이틀 추천 API 응답으로 교체할 목데이터입니다.
export const MOCK_PERSONAL_CERTIFICATE_TITLES = [
    '상위 12%의 판결력',
    '이번 달 진짜 무죄',
    '절약 판결 갱신',
];
