export const MOCK_GROUPS = {
    'mock-group-1': {
        id: 'mock-group-1',
        groupName: '배달 소비 줄이기',
        evalType: 'DAILY',
        categoryName: '총 소비',
        limitAmount: 15000,
        startDate: '2026-08-06',
        endDate: '2026-08-12',
        inviteCode: '22222',
        displayCode: 'TANG-0806',
        status: 'RECRUITING',
        memberCount: 1,
        maxMembers: 6,
        rules: '하루 배달 1회 이하, 커피는 허용',
    },
    'mock-group-2': {
        id: 'mock-group-2',
        groupName: '카페비 방어단',
        evalType: 'PERIOD',
        categoryName: '카페',
        limitAmount: 70000,
        startDate: '2026-08-10',
        endDate: '2026-08-16',
        inviteCode: '11111',
        displayCode: 'TANG-0810',
        status: 'RECRUITING',
        memberCount: 4,
        maxMembers: 6,
        rules: '평일에는 테이크아웃 커피만 인정해요.',
    },
};

export const MOCK_INVITE_CODES = {
    '22222': { groupId: 'mock-group-1', expired: false },
    '11111': { groupId: 'mock-group-2', expired: true },
};

/* ── 그룹 챌린지 홈 · 재판 위젯 목데이터 ─────────────────── */

/**
 * 투표 대기 (판결 참여) 요약 — 카드 1장에 건수만 표시.
 * 투표하기를 누르면 개별 목록 화면으로 이동할 예정.
 */
export const MOCK_TRIAL_SUMMARY = {
    count: 2,
    /** 대표로 보여줄 최근 건 정보 */
    latestDefendantName: '지판',
    totalVoters: 5,
    votedCount: 3,
    votes: [
        { verdict: 'GUILTY' },
        { verdict: 'GUILTY' },
        { verdict: 'NOT_GUILTY' },
    ],
    deadlineLabel: '02:14:03',
};

/**
 * 기소 (변론 요청) 요약 — 카드 1장에 건수만 표시.
 */
export const MOCK_INDICTMENT_SUMMARY = {
    count: 1,
    challengeName: '배달 소비 줄이기',
    chargeAmount: 6800,
    daysLeft: 1,
};

/* ── 그룹 챌린지 전체보기 · 3탭 목데이터 ─────────────── */

/** 시작 전 챌린지 목록 */
export const MOCK_PRE_START_CHALLENGES = [
    {
        id: 'pre-1',
        name: '카페비 방어단',
        evalType: 'PERIOD',
        totalDays: 14,
        dailyLimit: 12000,
        daysUntilStart: 2,
        members: [
            { initial: '김', color: '--tt-gold' },
            { initial: '준', color: '--tt-blue' },
            { initial: '세', color: '--tt-green' },
        ],
        memberCount: 4,
        maxMembers: 5,
        inviteCode: '7K2',
        isOwner: false,
    },
    {
        id: 'pre-2',
        name: '무지출 주말 클럽',
        evalType: 'DAILY',
        totalDays: 7,
        dailyLimit: 0,
        daysUntilStart: 5,
        members: [],
        memberCount: 2,
        maxMembers: 6,
        inviteCode: null,
        isOwner: true,
    },
];

/** 전체보기용 진행 중 챌린지 목록 */
export const MOCK_ACTIVE_LIST_CHALLENGES = [
    {
        id: 'active-list-1',
        name: '배달 소비 줄이기',
        evalType: 'DAILY',
        currentDay: 6,
        totalDays: 14,
        dailyLimit: 15000,
        lives: 3,
        maxLives: 5,
        memberCount: 5,
        status: 'TRIAL_PENDING',
        trialCount: 1,
        deadlineLabel: '02:14:03',
    },
    {
        id: 'active-list-2',
        name: '택시 대신 지하철',
        evalType: 'DAILY',
        currentDay: 9,
        totalDays: 21,
        dailyLimit: 8000,
        lives: 4,
        maxLives: 5,
        memberCount: 4,
        status: 'OBJECTION',
        trialCount: 0,
        deadlineLabel: '08:40',
    },
    {
        id: 'active-list-3',
        name: '신상 지름신 봉인령',
        evalType: 'PERIOD',
        currentDay: 12,
        totalDays: 30,
        totalLimit: 200000,
        lives: 5,
        maxLives: 5,
        memberCount: 6,
        status: 'PEACEFUL',
        trialCount: 0,
        deadlineLabel: null,
    },
];

/** 종료됨 챌린지 목록 */
export const MOCK_ENDED_CHALLENGES = [
    {
        id: 'ended-1',
        name: '편의점 커피 끊기',
        endDate: '2026.07.28',
        totalDays: 14,
        memberCount: 6,
        result: 'WIN',
        rank: 2,
        totalMembers: 6,
        livesLeft: 4,
        maxLives: 5,
        savingsAmount: 128000,
        isFeatured: true,
    },
    {
        id: 'ended-2',
        name: '주말 배달 금지령',
        endDate: '2026.07.14',
        totalDays: 7,
        memberCount: 5,
        result: 'LOSE',
        livesLeft: 0,
        maxLives: 5,
        summaryText: '목숨 0 / 5 · 벌금 12,000원 정산 완료',
        isFeatured: false,
    },
    {
        id: 'ended-3',
        name: '택시 대신 지하철 · 6월',
        endDate: '2026.06.30',
        totalDays: 21,
        memberCount: 4,
        result: 'WIN',
        livesLeft: 5,
        maxLives: 5,
        summaryText: '목숨 5 / 5 · 1위 · 절약 96,000원',
        isFeatured: false,
    },
];

/** 진행 중인 챌린지 목데이터 (홈 위젯용) */
export const MOCK_ACTIVE_CHALLENGES = [
    {
        id: 'active-1',
        name: '배달 소비 줄이기',
        evalType: 'DAILY',
        currentDay: 6,
        totalDays: 14,
        dailyLimit: 15000,
        lives: 3,
        maxLives: 5,
        memberCount: 5,
        pendingTrials: 1,
    },
    {
        id: 'active-2',
        name: '택시 대신 지하철',
        evalType: 'DAILY',
        currentDay: 9,
        totalDays: 21,
        dailyLimit: 8000,
        lives: 4,
        maxLives: 5,
        memberCount: 4,
        pendingTrials: 0,
        hasObjection: true,
    },
];
