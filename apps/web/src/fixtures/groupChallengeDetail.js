/*
 * 그룹 챌린지 상세화면 목데이터.
 *
 * 상세 API 가 반환할 형태를 그대로 목업한다.
 * tbl_challenge_group + tbl_group_member + tbl_group_challenge_daily_result +
 * tbl_indictment + tbl_defense + tbl_vote 를 조인해
 * 서버가 계산할 파생 필드를 포함한다.
 *
 * 키 = groupId (DB BIGINT).
 *
 * 주의: groupChallenge.js 의 MOCK_GROUPS / MOCK_*_CHALLENGES 와
 *       이름·evalType·limitAmount·멤버가 일치해야 한다.
 */

/* ── 멤버 아바타 색상 (팀 표준) ────────────────────────────── */
const AVATAR = {
    1: { nickname: '나',    initial: '나', avatarColor: '#232842', profileImage: new URL('@/assets/images/profile_ex/profile1.png', import.meta.url).href },
    2: { nickname: '유현', initial: '유', avatarColor: '#3E63D6', profileImage: new URL('@/assets/images/profile_ex/profile2.png', import.meta.url).href },
    3: { nickname: '준서',   initial: '준', avatarColor: '#2E9E6B', profileImage: new URL('@/assets/images/profile_ex/profile3.png', import.meta.url).href },
    4: { nickname: '세영',   initial: '세', avatarColor: '#E7A70C', profileImage: new URL('@/assets/images/profile_ex/profile4.png', import.meta.url).href },
    5: { nickname: '민지',   initial: '민', avatarColor: '#E0664B', profileImage: new URL('@/assets/images/profile_ex/profile5.png', import.meta.url).href },
    6: { nickname: '하은',   initial: '하', avatarColor: '#8B5CF6', profileImage: new URL('@/assets/images/profile_ex/profile6.png', import.meta.url).href },
};

function member(userId, isAdmin = false) {
    return { userId, ...AVATAR[userId], isAdmin };
}

/* ────────────────────────────────────────────────────────────
 * 1. MOCK_CHALLENGE_DETAILS — 상세화면 데이터
 *    시작 전(RECRUITING) / 진행 중(ACTIVE) / 종료(CLOSED)
 * ──────────────────────────────────────────────────────────── */

export const MOCK_CHALLENGE_DETAILS = {
    /* ── id=1 · 배달 소비 줄이기 · DAILY · ACTIVE ────────────── */
    1: {
        id: 1,
        adminId: 2,
        groupName: '배달 소비 줄이기',
        categoryId: null,
        categoryName: '총 소비',
        evalType: 'DAILY',
        limitAmount: 15000,
        maxMembers: 6,
        startDate: '2026-08-01',
        endDate: '2026-08-07',
        inviteCode: 'DL7K2',
        status: 'ACTIVE',

        /* 파생 필드 */
        totalDays: 7,
        currentDay: 6,
        settleTime: '매일 23:30 결산',

        /* 내 소비 상태 (tbl_group_challenge_daily_result) */
        myDailyAmount: 12750,
        myUsagePercent: 85,
        myRemainingAmount: 2250,

        /* 내 목숨 (tbl_group_member) */
        livesCount: 5,
        maxLives: 7,

        /* 약속 메모 */
        memo: '하루 배달 1회 이하, 커피는 허용',
        memoAuthor: '유현',
        memoDate: '7월 30일',

        /* 멤버 (tbl_group_member + tbl_user) — groupChallenge.js 와 동일 */
        memberCount: 5,
        members: [
            member(1),
            member(2, true),
            member(3),
            member(5),
            member(6),
        ],

        /* 채팅 (그룹 채팅방 요약) */
        chat: {
            unreadCount: 3,
            lastMessage: { sender: '유현', text: '나 오늘 진짜 참았다' },
        },

        /*
         * 재판 캐러셀 (tbl_indictment + tbl_defense + tbl_vote)
         * 일일결산 ACTIVE 에서만 노출.
         * 카드 4종:
         *   DEFENSE_WAIT + 내가 피고 → "내 변론이 필요해요"
         *   VOTING + 내가 피고     → "내 재판" (변론 제출됨)
         *   VOTING + 내가 투표자   → "재판이 열렸어요" (투표 필요/투표 완료)
         */
        indictments: [
            {
                id: 101,
                groupId: 1,
                userId: 1, /* 피기소자 = 나 */
                nickname: '나',
                initial: '나',
                avatarColor: '#232842',
                status: 'DEFENSE_WAIT',
                settlementDate: '8월 5일',
                exceededAmount: 3600,
                defenseDeadline: '02:14:03',
                isMine: true,
                hasDefended: false,
                voteCount: 0,
                totalVoters: 3,
                myVote: null,
            },
            {
                id: 102,
                groupId: 1,
                userId: 5, /* 민지 */
                ...AVATAR[5],
                status: 'VOTING',
                settlementDate: '8월 4일',
                exceededAmount: 2400,
                defenseDeadline: null,
                voteDeadline: '21:40:12',
                isMine: false,
                hasDefended: true,
                voteCount: 1,
                totalVoters: 3,
                myVote: null, /* 아직 내가 투표 안 함 */
            },
            {
                id: 103,
                groupId: 1,
                userId: 3, /* 준서 */
                ...AVATAR[3],
                status: 'VOTING',
                settlementDate: '8월 3일',
                exceededAmount: 9120,
                defenseDeadline: null,
                voteDeadline: '04:18:33',
                isMine: false,
                hasDefended: true,
                voteCount: 2,
                totalVoters: 3,
                myVote: null,
            },
            {
                id: 104,
                groupId: 1,
                userId: 2, /* 유현 */
                ...AVATAR[2],
                status: 'VOTING',
                settlementDate: '8월 3일',
                exceededAmount: 1800,
                defenseDeadline: null,
                voteDeadline: '03:52:06',
                isMine: false,
                hasDefended: true,
                voteCount: 0,
                totalVoters: 3,
                myVote: null,
            },
        ],

        /* 다른 멤버 오늘 소비 상태 (나 제외) */
        dailyMembers: [
            {
                userId: 2, ...AVATAR[2],
                dailyAmount: 5900,
                usagePercent: 39,
                isExceeded: false,
                trialStatus: null,
            },
            {
                userId: 3, ...AVATAR[3],
                dailyAmount: 8550,
                usagePercent: 57,
                isExceeded: false,
                trialStatus: null,
            },
            {
                userId: 5, ...AVATAR[5],
                dailyAmount: 16500,
                usagePercent: 110,
                isExceeded: true,
                trialStatus: 'VOTING', /* 재판중 */
            },
            {
                userId: 6, ...AVATAR[6],
                dailyAmount: 11200,
                usagePercent: 75,
                isExceeded: false,
                trialStatus: null,
            },
        ],
    },

    /* ── id=2 · 카페비 방어단 · PERIOD · RECRUITING ──────────── */
    2: {
        id: 2,
        adminId: 1,
        groupName: '카페비 방어단',
        categoryId: 3,
        categoryName: '카페',
        evalType: 'PERIOD',
        limitAmount: 70000,
        maxMembers: 5,
        startDate: '2026-08-08',
        endDate: '2026-08-14',
        inviteCode: 'CF9X1',
        status: 'RECRUITING',

        totalDays: 7,
        daysUntilStart: 2,
        maxLives: 1, /* PERIOD 결산은 목숨 1개 */

        memo: '평일에는 테이크아웃 커피만 인정해요.',
        memoAuthor: '나',
        memoDate: '8월 5일',

        memberCount: 4,
        members: [
            member(1, true),
            member(2),
            member(3),
            member(4),
        ],
    },

    /* ── id=3 · 무지출 주말 클럽 · DAILY · RECRUITING ────────── */
    3: {
        id: 3,
        adminId: 1,
        groupName: '무지출 주말 클럽',
        categoryId: null,
        categoryName: '총 소비',
        evalType: 'DAILY',
        limitAmount: 0,
        maxMembers: 6,
        startDate: '2026-08-11',
        endDate: '2026-08-13',
        inviteCode: 'NZ3P5',
        status: 'RECRUITING',

        totalDays: 3,
        daysUntilStart: 5,
        maxLives: 3, /* DAILY: 목숨 = totalDays */

        memo: null,
        memoAuthor: null,
        memoDate: null,

        memberCount: 2,
        members: [
            member(1, true),
            member(5),
        ],
    },

    /* ── id=4 · 택시 대신 지하철 · DAILY · ACTIVE ────────────── */
    4: {
        id: 4,
        adminId: 3,
        groupName: '택시 대신 지하철',
        categoryId: 5,
        categoryName: '교통',
        evalType: 'DAILY',
        limitAmount: 8000,
        maxMembers: 5,
        startDate: '2026-07-30',
        endDate: '2026-08-05',
        inviteCode: 'TX8M4',
        status: 'ACTIVE',

        totalDays: 7,
        currentDay: 7,
        settleTime: '매일 23:30 결산',

        myDailyAmount: 3200,
        myUsagePercent: 40,
        myRemainingAmount: 4800,

        livesCount: 5,
        maxLives: 7,

        memo: null,
        memoAuthor: null,
        memoDate: null,

        memberCount: 4,
        members: [
            member(1),
            member(3, true),
            member(4),
            member(6),
        ],

        chat: {
            unreadCount: 1,
            lastMessage: { sender: '세영', text: '오늘 택시 안 탔다!' },
        },

        indictments: [
            {
                id: 201,
                groupId: 4,
                userId: 6, /* 하은 */
                ...AVATAR[6],
                status: 'VOTING',
                settlementDate: '8월 4일',
                exceededAmount: 1200,
                defenseDeadline: null,
                voteDeadline: '18:30:00',
                isMine: false,
                hasDefended: true,
                voteCount: 1,
                totalVoters: 2,
                myVote: null,
            },
        ],

        dailyMembers: [
            {
                userId: 3, ...AVATAR[3],
                dailyAmount: 2100,
                usagePercent: 26,
                isExceeded: false,
                trialStatus: null,
            },
            {
                userId: 4, ...AVATAR[4],
                dailyAmount: 6400,
                usagePercent: 80,
                isExceeded: false,
                trialStatus: null,
            },
            {
                userId: 6, ...AVATAR[6],
                dailyAmount: 9200,
                usagePercent: 115,
                isExceeded: true,
                trialStatus: 'VOTING',
            },
        ],
    },

    /* ── id=5 · 편의점 간식 줄이기 · PERIOD · ACTIVE ─────────── */
    5: {
        id: 5,
        adminId: 4,
        groupName: '편의점 간식 줄이기',
        categoryId: 7,
        categoryName: '편의점',
        evalType: 'PERIOD',
        limitAmount: 30000,
        maxMembers: 6,
        startDate: '2026-08-02',
        endDate: '2026-08-07',
        inviteCode: 'SN2Q7',
        status: 'ACTIVE',

        totalDays: 6,
        currentDay: 5,
        settleTime: '종료 다음날 판정',

        myDailyAmount: 13500,
        myUsagePercent: 45,
        myRemainingAmount: 16500,

        livesCount: 1,
        maxLives: 1, /* PERIOD 결산은 목숨 1개 */

        memo: null,
        memoAuthor: null,
        memoDate: null,

        memberCount: 6,
        members: [
            member(1),
            member(2),
            member(3),
            member(4, true),
            member(5),
            member(6),
        ],

        chat: {
            unreadCount: 5,
            lastMessage: { sender: '세영', text: '편의점 들렸는데 참았다 ㅋㅋ' },
        },

        dailyMembers: [
            {
                userId: 2, ...AVATAR[2],
                dailyAmount: 10500,
                usagePercent: 35,
                isExceeded: false,
                trialStatus: null,
            },
            {
                userId: 3, ...AVATAR[3],
                dailyAmount: 12900,
                usagePercent: 43,
                isExceeded: false,
                trialStatus: null,
            },
            {
                userId: 4, ...AVATAR[4],
                dailyAmount: 29100,
                usagePercent: 97,
                isExceeded: false,
                trialStatus: null, /* 97%지만 기간결산이라 아직 초과 아님 */
            },
            {
                userId: 5, ...AVATAR[5],
                dailyAmount: 18600,
                usagePercent: 62,
                isExceeded: false,
                trialStatus: null,
            },
            {
                userId: 6, ...AVATAR[6],
                dailyAmount: 31200,
                usagePercent: 104,
                isExceeded: true,
                trialStatus: null, /* 기간결산은 종료 후 판정이라 진행 중 재판 없음 */
            },
        ],
    },

    /* ── id=6 · 편의점 커피 끊기 · DAILY · CLOSED ────────────── */
    6: {
        id: 6,
        adminId: 2,
        groupName: '편의점 커피 끊기',
        categoryId: 7,
        categoryName: '편의점',
        evalType: 'DAILY',
        limitAmount: 3000,
        maxMembers: 6,
        startDate: '2026-07-22',
        endDate: '2026-07-28',
        inviteCode: 'CV4R8',
        status: 'CLOSED',

        totalDays: 7,
        maxLives: 7,

        /* 종료 결과 (tbl_group_member) */
        finalOutcome: 'SURVIVED',
        finalRank: 2,
        finalChargeAmount: 0,
        savingsAmount: 128000,

        /* 재판 통계 (tbl_indictment 집계) */
        trialStats: { totalTrials: 2, guiltyCount: 1, innocentCount: 1 },

        memo: null,
        memoAuthor: null,
        memoDate: null,

        memberCount: 6,
        members: [
            member(1),
            member(2, true),
            member(3),
            member(4),
            member(5),
            member(6),
        ],

        /* 최종 멤버 결과 */
        finalMembers: [
            { userId: 3, ...AVATAR[3], finalRank: 1, finalOutcome: 'SURVIVED', livesCount: 7 },
            { userId: 1, ...AVATAR[1], finalRank: 2, finalOutcome: 'SURVIVED', livesCount: 6 },
            { userId: 2, ...AVATAR[2], finalRank: 3, finalOutcome: 'SURVIVED', livesCount: 5 },
            { userId: 5, ...AVATAR[5], finalRank: 4, finalOutcome: 'SURVIVED', livesCount: 3 },
            { userId: 6, ...AVATAR[6], finalRank: 5, finalOutcome: 'ELIMINATED', livesCount: 0 },
            { userId: 4, ...AVATAR[4], finalRank: 6, finalOutcome: 'ELIMINATED', livesCount: 0 },
        ],
    },

    /* ── id=7 · 주말 배달 금지령 · DAILY · CLOSED ────────────── */
    7: {
        id: 7,
        adminId: 1,
        groupName: '주말 배달 금지령',
        categoryId: null,
        categoryName: '총 소비',
        evalType: 'DAILY',
        limitAmount: 5000,
        maxMembers: 5,
        startDate: '2026-07-08',
        endDate: '2026-07-14',
        inviteCode: 'WK1J6',
        status: 'CLOSED',

        totalDays: 7,
        maxLives: 7,

        finalOutcome: 'ELIMINATED',
        finalRank: 5,
        finalChargeAmount: 12000,
        savingsAmount: null,

        trialStats: { totalTrials: 3, guiltyCount: 2, innocentCount: 1 },

        memo: null,
        memoAuthor: null,
        memoDate: null,

        memberCount: 5,
        members: [
            member(1, true),
            member(2),
            member(3),
            member(5),
            member(6),
        ],

        finalMembers: [
            { userId: 2, ...AVATAR[2], finalRank: 1, finalOutcome: 'SURVIVED', livesCount: 7 },
            { userId: 3, ...AVATAR[3], finalRank: 2, finalOutcome: 'SURVIVED', livesCount: 5 },
            { userId: 6, ...AVATAR[6], finalRank: 3, finalOutcome: 'SURVIVED', livesCount: 4 },
            { userId: 5, ...AVATAR[5], finalRank: 4, finalOutcome: 'ELIMINATED', livesCount: 0 },
            { userId: 1, ...AVATAR[1], finalRank: 5, finalOutcome: 'ELIMINATED', livesCount: 0 },
        ],
    },

    /* ── id=8 · 카페비 방어단 · 7월 · PERIOD · CLOSED ──────── */
    8: {
        id: 8,
        adminId: 3,
        groupName: '카페비 방어단 · 7월',
        categoryId: 3,
        categoryName: '카페',
        evalType: 'PERIOD',
        limitAmount: 50000,
        maxMembers: 4,
        startDate: '2026-06-24',
        endDate: '2026-06-30',
        inviteCode: 'JL5N3',
        status: 'CLOSED',

        totalDays: 7,
        maxLives: 1,

        finalOutcome: 'SURVIVED',
        finalRank: 1,
        finalChargeAmount: 0,
        savingsAmount: 86000,

        trialStats: { totalTrials: 1, guiltyCount: 1, innocentCount: 0 },

        memo: '카페는 주 3회까지만!',
        memoAuthor: '준서',
        memoDate: '6월 22일',

        memberCount: 4,
        members: [
            member(1),
            member(3, true),
            member(4),
            member(6),
        ],

        finalMembers: [
            { userId: 1, ...AVATAR[1], finalRank: 1, finalOutcome: 'SURVIVED', livesCount: 1 },
            { userId: 3, ...AVATAR[3], finalRank: 2, finalOutcome: 'SURVIVED', livesCount: 1 },
            { userId: 6, ...AVATAR[6], finalRank: 3, finalOutcome: 'SURVIVED', livesCount: 1 },
            { userId: 4, ...AVATAR[4], finalRank: 4, finalOutcome: 'ELIMINATED', livesCount: 0 },
        ],
    },

    /* ── id=9 · 야식 참기 챌린지 · DAILY · RECRUITING ──────── */
    9: {
        id: 9,
        adminId: 2,
        groupName: '야식 참기 챌린지',
        categoryId: null,
        categoryName: '총 소비',
        evalType: 'DAILY',
        limitAmount: 10000,
        maxMembers: 4,
        startDate: '2026-08-10',
        endDate: '2026-08-14',
        inviteCode: 'YS7K9',
        status: 'RECRUITING',

        totalDays: 5,
        daysUntilStart: 4,
        maxLives: 5,

        memo: '밤 9시 이후 지출 금지!',
        memoAuthor: '유현',
        memoDate: '8월 5일',

        memberCount: 3,
        members: [
            member(1),
            member(2, true),
            member(3),
        ],
    },

    /* ── id=10 · 쇼핑 절약 챌린지 · PERIOD · RECRUITING ────── */
    10: {
        id: 10,
        adminId: 3,
        groupName: '쇼핑 절약 챌린지',
        categoryId: 9,
        categoryName: '쇼핑',
        evalType: 'PERIOD',
        limitAmount: 100000,
        maxMembers: 6,
        startDate: '2026-08-12',
        endDate: '2026-08-18',
        inviteCode: 'SP4M2',
        status: 'RECRUITING',

        totalDays: 7,
        daysUntilStart: 6,
        maxLives: 1,

        memo: null,
        memoAuthor: null,
        memoDate: null,

        memberCount: 3,
        members: [
            member(1),
            member(3, true),
            member(5),
        ],
    },

    /* ── id=11 · 배달비 절약 도전 · PERIOD · ACTIVE ─────────── */
    11: {
        id: 11,
        adminId: 1,
        groupName: '배달비 절약 도전',
        categoryId: 2,
        categoryName: '배달',
        evalType: 'PERIOD',
        limitAmount: 50000,
        maxMembers: 5,
        startDate: '2026-08-01',
        endDate: '2026-08-07',
        inviteCode: 'DV3R8',
        status: 'ACTIVE',

        totalDays: 7,
        currentDay: 6,
        settleTime: '종료 다음날 판정',

        myDailyAmount: 28500,
        myUsagePercent: 57,
        myRemainingAmount: 21500,

        livesCount: 1,
        maxLives: 1,

        memo: '배달은 주 2회까지!',
        memoAuthor: '나',
        memoDate: '7월 30일',

        memberCount: 4,
        members: [
            member(1, true),
            member(2),
            member(4),
            member(5),
        ],

        chat: {
            unreadCount: 0,
            lastMessage: { sender: '민지', text: '배달 시켜먹고 싶다...' },
        },

        dailyMembers: [
            {
                userId: 2, ...AVATAR[2],
                dailyAmount: 22100,
                usagePercent: 44,
                isExceeded: false,
                trialStatus: null,
            },
            {
                userId: 4, ...AVATAR[4],
                dailyAmount: 47800,
                usagePercent: 96,
                isExceeded: false,
                trialStatus: null,
            },
            {
                userId: 5, ...AVATAR[5],
                dailyAmount: 51200,
                usagePercent: 102,
                isExceeded: true,
                trialStatus: null, /* 기간결산이라 종료 후 재판 */
            },
        ],
    },

    /* ── id=12 · 간식비 절약 · 7월 · PERIOD · CLOSED ────────── */
    12: {
        id: 12,
        adminId: 4,
        groupName: '간식비 절약 · 7월',
        categoryId: 7,
        categoryName: '편의점',
        evalType: 'PERIOD',
        limitAmount: 40000,
        maxMembers: 5,
        startDate: '2026-07-01',
        endDate: '2026-07-07',
        inviteCode: 'GS6T1',
        status: 'CLOSED',

        totalDays: 7,
        maxLives: 1,

        finalOutcome: 'ELIMINATED',
        finalRank: 4,
        finalChargeAmount: 8500,
        savingsAmount: null,

        trialStats: { totalTrials: 3, guiltyCount: 2, innocentCount: 1 },

        memo: null,
        memoAuthor: null,
        memoDate: null,

        memberCount: 5,
        members: [
            member(1),
            member(3),
            member(4, true),
            member(5),
            member(6),
        ],

        finalMembers: [
            { userId: 3, ...AVATAR[3], finalRank: 1, finalOutcome: 'SURVIVED', livesCount: 1 },
            { userId: 5, ...AVATAR[5], finalRank: 2, finalOutcome: 'SURVIVED', livesCount: 1 },
            { userId: 6, ...AVATAR[6], finalRank: 3, finalOutcome: 'SURVIVED', livesCount: 1 },
            { userId: 1, ...AVATAR[1], finalRank: 4, finalOutcome: 'ELIMINATED', livesCount: 0 },
            { userId: 4, ...AVATAR[4], finalRank: 5, finalOutcome: 'ELIMINATED', livesCount: 0 },
        ],
    },
};

/* ────────────────────────────────────────────────────────────
 * 2. MOCK_CHALLENGE_RANKINGS — 명예 법정 (생존 순위 / 누적 순위)
 *    ACTIVE · CLOSED 챌린지에서 접근 가능
 * ──────────────────────────────────────────────────────────── */

export const MOCK_CHALLENGE_RANKINGS = {
    /* ── id=1 · 배달 소비 줄이기 · 일일결산 생존 순위 ──────────── */
    1: {
        groupId: 1,
        groupName: '배달 소비 줄이기',
        evalType: 'DAILY',
        lastSettlementDate: '8월 6일',

        trialStats: { totalTrials: 2, guiltyCount: 1, innocentCount: 1 },

        memo: '하루 배달 1회 이하, 커피는 허용',
        memoAuthor: '유현',
        memoDate: '7월 30일',

        podium: [
            { rank: 1, userId: 1, ...AVATAR[1], isMe: true },
            { rank: 2, userId: 3, ...AVATAR[3], isMe: false },
            { rank: 3, userId: 2, ...AVATAR[2], isMe: false },
        ],

        rankings: [
            {
                rank: 1, userId: 1, ...AVATAR[1],
                isMe: true, livesCount: 7, maxLives: 7,
                isEliminated: false, statusLabel: '7', statusType: 'alive',
            },
            {
                rank: 2, userId: 3, ...AVATAR[3],
                isMe: false, livesCount: 5, maxLives: 7,
                isEliminated: false, statusLabel: '5', statusType: 'alive',
            },
            {
                rank: 3, userId: 2, ...AVATAR[2],
                isMe: false, livesCount: 3, maxLives: 7,
                isEliminated: false, statusLabel: '3', statusType: 'danger',
            },
            {
                rank: 4, userId: 5, ...AVATAR[5],
                isMe: false, livesCount: 1, maxLives: 7,
                isEliminated: false, statusLabel: '1', statusType: 'danger',
            },
            {
                rank: 5, userId: 6, ...AVATAR[6],
                isMe: false, livesCount: 0, maxLives: 7,
                isEliminated: true, statusLabel: '탈락', statusType: 'eliminated',
            },
        ],
    },

    /* ── id=4 · 택시 대신 지하철 · 일일결산 생존 순위 ─────────── */
    4: {
        groupId: 4,
        groupName: '택시 대신 지하철',
        evalType: 'DAILY',
        lastSettlementDate: '8월 5일',

        trialStats: { totalTrials: 1, guiltyCount: 0, innocentCount: 1 },

        memo: null,
        memoAuthor: null,
        memoDate: null,

        podium: [
            { rank: 1, userId: 3, ...AVATAR[3], isMe: false },
            { rank: 2, userId: 1, ...AVATAR[1], isMe: true },
            { rank: 3, userId: 4, ...AVATAR[4], isMe: false },
        ],

        rankings: [
            {
                rank: 1, userId: 3, ...AVATAR[3],
                isMe: false, livesCount: 7, maxLives: 7,
                isEliminated: false, statusLabel: '7', statusType: 'alive',
            },
            {
                rank: 2, userId: 1, ...AVATAR[1],
                isMe: true, livesCount: 5, maxLives: 7,
                isEliminated: false, statusLabel: '5', statusType: 'alive',
            },
            {
                rank: 3, userId: 4, ...AVATAR[4],
                isMe: false, livesCount: 4, maxLives: 7,
                isEliminated: false, statusLabel: '4', statusType: 'alive',
            },
            {
                rank: 4, userId: 6, ...AVATAR[6],
                isMe: false, livesCount: 2, maxLives: 7,
                isEliminated: false, statusLabel: '2', statusType: 'danger',
            },
        ],
    },

    /* ── id=5 · 편의점 간식 줄이기 · 기간결산 누적 순위 ─────── */
    5: {
        groupId: 5,
        groupName: '편의점 간식 줄이기',
        evalType: 'PERIOD',
        limitAmount: 30000,
        lastSettlementDate: '8월 7일',

        trialStats: { totalTrials: 0, guiltyCount: 0, innocentCount: 0 },

        memo: null,
        memoAuthor: null,
        memoDate: null,

        podium: [
            { rank: 1, userId: 2, ...AVATAR[2], isMe: false },
            { rank: 2, userId: 3, ...AVATAR[3], isMe: false },
            { rank: 3, userId: 1, ...AVATAR[1], isMe: true },
        ],

        rankings: [
            {
                rank: 1, userId: 2, ...AVATAR[2],
                isMe: false, currentAmount: 10500, usagePercent: 35,
            },
            {
                rank: 2, userId: 3, ...AVATAR[3],
                isMe: false, currentAmount: 12900, usagePercent: 43,
            },
            {
                rank: 3, userId: 1, ...AVATAR[1],
                isMe: true, currentAmount: 13500, usagePercent: 45,
            },
            {
                rank: 4, userId: 5, ...AVATAR[5],
                isMe: false, currentAmount: 18600, usagePercent: 62,
            },
            {
                rank: 5, userId: 4, ...AVATAR[4],
                isMe: false, currentAmount: 29100, usagePercent: 97,
            },
            {
                rank: 6, userId: 6, ...AVATAR[6],
                isMe: false, currentAmount: 31200, usagePercent: 104,
            },
        ],
    },

    /* ── id=6 · 편의점 커피 끊기 · 최종 순위 ─────────────────── */
    6: {
        groupId: 6,
        groupName: '편의점 커피 끊기',
        evalType: 'DAILY',
        lastSettlementDate: '7월 28일',

        trialStats: { totalTrials: 2, guiltyCount: 1, innocentCount: 1 },

        memo: null,
        memoAuthor: null,
        memoDate: null,

        podium: [
            { rank: 1, userId: 3, ...AVATAR[3], isMe: false },
            { rank: 2, userId: 1, ...AVATAR[1], isMe: true },
            { rank: 3, userId: 2, ...AVATAR[2], isMe: false },
        ],

        rankings: [
            {
                rank: 1, userId: 3, ...AVATAR[3],
                isMe: false, livesCount: 7, maxLives: 7,
                isEliminated: false, statusLabel: '7', statusType: 'alive',
            },
            {
                rank: 2, userId: 1, ...AVATAR[1],
                isMe: true, livesCount: 6, maxLives: 7,
                isEliminated: false, statusLabel: '6', statusType: 'alive',
            },
            {
                rank: 3, userId: 2, ...AVATAR[2],
                isMe: false, livesCount: 5, maxLives: 7,
                isEliminated: false, statusLabel: '5', statusType: 'alive',
            },
            {
                rank: 4, userId: 5, ...AVATAR[5],
                isMe: false, livesCount: 3, maxLives: 7,
                isEliminated: false, statusLabel: '3', statusType: 'danger',
            },
            {
                rank: 5, userId: 6, ...AVATAR[6],
                isMe: false, livesCount: 0, maxLives: 7,
                isEliminated: true, statusLabel: '탈락', statusType: 'eliminated',
            },
            {
                rank: 6, userId: 4, ...AVATAR[4],
                isMe: false, livesCount: 0, maxLives: 7,
                isEliminated: true, statusLabel: '탈락', statusType: 'eliminated',
            },
        ],
    },

    /* ── id=7 · 주말 배달 금지령 · 최종 순위 ─────────────────── */
    7: {
        groupId: 7,
        groupName: '주말 배달 금지령',
        evalType: 'DAILY',
        lastSettlementDate: '7월 14일',

        trialStats: { totalTrials: 3, guiltyCount: 2, innocentCount: 1 },

        memo: null,
        memoAuthor: null,
        memoDate: null,

        podium: [
            { rank: 1, userId: 2, ...AVATAR[2], isMe: false },
            { rank: 2, userId: 3, ...AVATAR[3], isMe: false },
            { rank: 3, userId: 6, ...AVATAR[6], isMe: false },
        ],

        rankings: [
            {
                rank: 1, userId: 2, ...AVATAR[2],
                isMe: false, livesCount: 7, maxLives: 7,
                isEliminated: false, statusLabel: '7', statusType: 'alive',
            },
            {
                rank: 2, userId: 3, ...AVATAR[3],
                isMe: false, livesCount: 5, maxLives: 7,
                isEliminated: false, statusLabel: '5', statusType: 'alive',
            },
            {
                rank: 3, userId: 6, ...AVATAR[6],
                isMe: false, livesCount: 4, maxLives: 7,
                isEliminated: false, statusLabel: '4', statusType: 'alive',
            },
            {
                rank: 4, userId: 5, ...AVATAR[5],
                isMe: false, livesCount: 0, maxLives: 7,
                isEliminated: true, statusLabel: '탈락', statusType: 'eliminated',
            },
            {
                rank: 5, userId: 1, ...AVATAR[1],
                isMe: true, livesCount: 0, maxLives: 7,
                isEliminated: true, statusLabel: '탈락', statusType: 'eliminated',
            },
        ],
    },

    /* ── id=8 · 카페비 방어단 · 7월 · 기간결산 최종 순위 ──────── */
    8: {
        groupId: 8,
        groupName: '카페비 방어단 · 7월',
        evalType: 'PERIOD',
        limitAmount: 50000,
        lastSettlementDate: '7월 1일',

        trialStats: { totalTrials: 1, guiltyCount: 1, innocentCount: 0 },

        memo: '카페는 주 3회까지만!',
        memoAuthor: '준서',
        memoDate: '6월 22일',

        podium: [
            { rank: 1, userId: 1, ...AVATAR[1], isMe: true },
            { rank: 2, userId: 3, ...AVATAR[3], isMe: false },
            { rank: 3, userId: 6, ...AVATAR[6], isMe: false },
        ],

        rankings: [
            {
                rank: 1, userId: 1, ...AVATAR[1],
                isMe: true, currentAmount: 18200, usagePercent: 36,
            },
            {
                rank: 2, userId: 3, ...AVATAR[3],
                isMe: false, currentAmount: 24500, usagePercent: 49,
            },
            {
                rank: 3, userId: 6, ...AVATAR[6],
                isMe: false, currentAmount: 38900, usagePercent: 78,
            },
            {
                rank: 4, userId: 4, ...AVATAR[4],
                isMe: false, currentAmount: 52100, usagePercent: 104,
            },
        ],
    },

    /* ── id=11 · 배달비 절약 도전 · 기간결산 누적 순위 ─────── */
    11: {
        groupId: 11,
        groupName: '배달비 절약 도전',
        evalType: 'PERIOD',
        limitAmount: 50000,
        lastSettlementDate: '8월 7일',

        trialStats: { totalTrials: 0, guiltyCount: 0, innocentCount: 0 },

        memo: '배달은 주 2회까지!',
        memoAuthor: '나',
        memoDate: '7월 30일',

        podium: [
            { rank: 1, userId: 2, ...AVATAR[2], isMe: false },
            { rank: 2, userId: 1, ...AVATAR[1], isMe: true },
            { rank: 3, userId: 4, ...AVATAR[4], isMe: false },
        ],

        rankings: [
            {
                rank: 1, userId: 2, ...AVATAR[2],
                isMe: false, currentAmount: 22100, usagePercent: 44,
            },
            {
                rank: 2, userId: 1, ...AVATAR[1],
                isMe: true, currentAmount: 28500, usagePercent: 57,
            },
            {
                rank: 3, userId: 4, ...AVATAR[4],
                isMe: false, currentAmount: 47800, usagePercent: 96,
            },
            {
                rank: 4, userId: 5, ...AVATAR[5],
                isMe: false, currentAmount: 51200, usagePercent: 102,
            },
        ],
    },

    /* ── id=12 · 간식비 절약 · 7월 · 기간결산 최종 순위 ──────── */
    12: {
        groupId: 12,
        groupName: '간식비 절약 · 7월',
        evalType: 'PERIOD',
        limitAmount: 40000,
        lastSettlementDate: '7월 8일',

        trialStats: { totalTrials: 3, guiltyCount: 2, innocentCount: 1 },

        memo: null,
        memoAuthor: null,
        memoDate: null,

        podium: [
            { rank: 1, userId: 3, ...AVATAR[3], isMe: false },
            { rank: 2, userId: 5, ...AVATAR[5], isMe: false },
            { rank: 3, userId: 6, ...AVATAR[6], isMe: false },
        ],

        rankings: [
            {
                rank: 1, userId: 3, ...AVATAR[3],
                isMe: false, currentAmount: 15200, usagePercent: 38,
            },
            {
                rank: 2, userId: 5, ...AVATAR[5],
                isMe: false, currentAmount: 22800, usagePercent: 57,
            },
            {
                rank: 3, userId: 6, ...AVATAR[6],
                isMe: false, currentAmount: 31400, usagePercent: 79,
            },
            {
                rank: 4, userId: 1, ...AVATAR[1],
                isMe: true, currentAmount: 42300, usagePercent: 106,
            },
            {
                rank: 5, userId: 4, ...AVATAR[4],
                isMe: false, currentAmount: 48600, usagePercent: 122,
            },
        ],
    },
};
