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

/** 진행 중인 챌린지 목데이터 */
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
