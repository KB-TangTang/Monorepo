/*
 * 그룹 챌린지 목데이터.
 *
 * 필드명은 DB 스키마(tbl_challenge_group, tbl_group_member)의 camelCase 변환을 따른다.
 * 서버에서 계산해 내려줄 파생 필드는 "파생" 주석으로 표시한다.
 * DB CHECK 제약: 기간 최대 7일, 정원 2~6명, evalType DAILY|PERIOD,
 *               status RECRUITING|ACTIVE|JUDGING|CLOSED.
 */

/* ────────────────────────────────────────────────────────────
 * 1. MOCK_GROUPS — 기준 원본. 다른 목데이터는 여기서 파생한다.
 *    키 = 숫자 id (DB BIGINT). 생성/참여/초대 API 가 참조한다.
 * ──────────────────────────────────────────────────────────── */

const CURRENT_USER_ID = 1; // 로그인한 사용자 (목데이터 기준)

export const MOCK_GROUPS = {
    1: {
        id: 1,
        adminId: 2,
        groupName: '배달 소비 줄이기',
        categoryId: null,            // null = 총 소비
        categoryName: '총 소비',     /* 파생: tbl_category JOIN */
        evalType: 'DAILY',
        limitAmount: 15000,
        maxMembers: 6,
        startDate: '2026-08-01',
        endDate: '2026-08-07',
        inviteCode: 'DL7K2',
        status: 'ACTIVE',
        memo: '하루 배달 1회 이하, 커피는 허용',
    },
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
        memo: '평일에는 테이크아웃 커피만 인정해요.',
    },
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
        memo: null,
    },
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
        memo: null,
    },
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
        memo: null,
    },
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
        memo: null,
    },
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
        memo: null,
    },
    8: {
        id: 8,
        adminId: 3,
        groupName: '택시 대신 지하철 · 7월',
        categoryId: 5,
        categoryName: '교통',
        evalType: 'DAILY',
        limitAmount: 8000,
        maxMembers: 4,
        startDate: '2026-06-24',
        endDate: '2026-06-30',
        inviteCode: 'JL5N3',
        status: 'CLOSED',
        memo: null,
    },
};

/* ────────────────────────────────────────────────────────────
 * 2. MOCK_INVITE_CODES — 초대코드 → 그룹 매핑 (5자리)
 * ──────────────────────────────────────────────────────────── */

export const MOCK_INVITE_CODES = {
    'DL7K2': { groupId: 1, expired: false },
    'CF9X1': { groupId: 2, expired: false },
    'NZ3P5': { groupId: 3, expired: false },
    'TX8M4': { groupId: 4, expired: true },
};

/* ────────────────────────────────────────────────────────────
 * 3. 그룹 챌린지 홈 · 재판 위젯 목데이터
 * ──────────────────────────────────────────────────────────── */

/**
 * 투표 대기 (판결 참여) 요약.
 * API: GET /api/v1/groupChallenges/court/summary
 */
export const MOCK_TRIAL_SUMMARY = {
    count: 2,
    latestDefendantName: '지판',
    totalVoters: 5,
    votedCount: 3,
    votes: [
        { verdict: 'GUILTY' },
        { verdict: 'GUILTY' },
        { verdict: 'INNOCENT' },  // DB: 'INNOCENT' (NOT_GUILTY 아님)
    ],
    deadlineLabel: '02:14:03',
};

/** 기소 (변론 요청) 요약. */
export const MOCK_INDICTMENT_SUMMARY = {
    count: 1,
    groupName: '배달 소비 줄이기',  // DB: group_name
    chargeAmount: 6800,
    daysLeft: 1,
};

/* ────────────────────────────────────────────────────────────
 * 4. 전체보기 3탭 목데이터
 *    실제 API 는 GET /api/v1/groupChallenges?status=RECRUITING 등으로
 *    조회하고, 서버가 파생 필드를 계산해 내려준다.
 *    여기서는 "API 응답 형태"를 그대로 목업한다.
 * ──────────────────────────────────────────────────────────── */

/** 시작 전 (status: RECRUITING) — 멤버 목록 + 파생 필드 포함 */
export const MOCK_PRE_START_CHALLENGES = [
    {
        /* --- tbl_challenge_group --- */
        id: 2,
        adminId: 1,
        groupName: '카페비 방어단',
        evalType: 'PERIOD',
        limitAmount: 70000,
        maxMembers: 5,
        startDate: '2026-08-08',
        endDate: '2026-08-14',
        inviteCode: 'CF9X1',
        status: 'RECRUITING',
        /* --- 파생 필드 (서버 계산) --- */
        totalDays: 7,                 // DATEDIFF(end, start) + 1
        daysUntilStart: 2,            // DATEDIFF(start, CURDATE())
        memberCount: 4,               // COUNT(tbl_group_member)
        isOwner: true,                // adminId === CURRENT_USER_ID
        members: [                    // tbl_group_member JOIN tbl_user
            { userId: 1, nickname: '나', initial: '나' },
            { userId: 2, nickname: '김수현', initial: '김' },
            { userId: 3, nickname: '준서', initial: '준' },
            { userId: 4, nickname: '세영', initial: '세' },
        ],
    },
    {
        id: 3,
        adminId: 1,
        groupName: '무지출 주말 클럽',
        evalType: 'DAILY',
        limitAmount: 0,
        maxMembers: 6,
        startDate: '2026-08-11',
        endDate: '2026-08-13',
        inviteCode: 'NZ3P5',
        status: 'RECRUITING',
        totalDays: 3,
        daysUntilStart: 5,
        memberCount: 2,
        isOwner: true,
        members: [
            { userId: 1, nickname: '나', initial: '나' },
            { userId: 5, nickname: '민지', initial: '민' },
        ],
    },
];

/**
 * 진행 중 (status: ACTIVE).
 *
 * 재판 표시 상태(pendingTrialCount / pendingDefenseCount)는
 * tbl_indictment 에서 파생한다. 카드 컴포넌트가 이 값으로
 * 판결 대기 / 이의 심리 / 평온 뱃지를 결정한다.
 */
export const MOCK_ACTIVE_LIST_CHALLENGES = [
    {
        /* --- tbl_challenge_group --- */
        id: 1,
        groupName: '배달 소비 줄이기',
        evalType: 'DAILY',
        limitAmount: 15000,
        startDate: '2026-08-01',
        endDate: '2026-08-07',
        /* --- tbl_group_member (본인) --- */
        livesCount: 5,
        /* --- 파생 필드 --- */
        totalDays: 7,
        currentDay: 6,
        maxLives: 7,
        memberCount: 5,
        /* --- 내 재판 상태 (tbl_indictment + tbl_vote 집계) --- */
        myVoteStatus: 'PENDING',      // 'PENDING' = 내가 아직 투표 안 함, 'DONE' = 투표 완료, null = 투표 대상 아님
        isDefendant: false,           // 내가 피고인인 기소건이 있는지
        /* --- 멤버 목록 --- */
        members: [
            { userId: 1, nickname: '나', initial: '나' },
            { userId: 2, nickname: '김수현', initial: '김' },
            { userId: 3, nickname: '준서', initial: '준' },
            { userId: 5, nickname: '민지', initial: '민' },
            { userId: 6, nickname: '하은', initial: '하' },
        ],
        /* --- 채팅 (추후 tbl_chat_message 연동) --- */
        unreadChatCount: 3,
        lastChatMessage: '준서: 오늘 배달 참았다 ㅋㅋ',
        lastChatTime: '오후 2:03',
    },
    {
        id: 4,
        groupName: '택시 대신 지하철',
        evalType: 'DAILY',
        limitAmount: 8000,
        startDate: '2026-07-30',
        endDate: '2026-08-05',
        livesCount: 5,
        totalDays: 7,
        currentDay: 7,
        maxLives: 7,
        memberCount: 4,
        myVoteStatus: null,
        isDefendant: true,            // 내가 기소당함 → 변론필요
        members: [
            { userId: 1, nickname: '나', initial: '나' },
            { userId: 3, nickname: '준서', initial: '준' },
            { userId: 4, nickname: '세영', initial: '세' },
            { userId: 6, nickname: '하은', initial: '하' },
        ],
        unreadChatCount: 0,
        lastChatMessage: '세영: 택시비 영수증 올려요',
        lastChatTime: '어제',
    },
    {
        id: 5,
        groupName: '편의점 간식 줄이기',
        evalType: 'PERIOD',
        limitAmount: 30000,
        startDate: '2026-08-02',
        endDate: '2026-08-07',
        livesCount: 1,
        totalDays: 6,
        currentDay: 5,
        maxLives: 1,
        memberCount: 6,
        myVoteStatus: 'DONE',         // 투표는 했지만 아직 마감 안 됨
        isDefendant: false,
        members: [
            { userId: 1, nickname: '나', initial: '나' },
            { userId: 2, nickname: '김수현', initial: '김' },
            { userId: 3, nickname: '준서', initial: '준' },
            { userId: 4, nickname: '세영', initial: '세' },
            { userId: 5, nickname: '민지', initial: '민' },
            { userId: 6, nickname: '하은', initial: '하' },
        ],
        unreadChatCount: 12,
        lastChatMessage: '민지: 편의점 앞에서 참는 중...',
        lastChatTime: '오전 11:42',
    },
];

/**
 * 종료됨 (status: CLOSED).
 *
 * finalOutcome: DB ENUM — 'SURVIVED' (승소) | 'ELIMINATED' (패소).
 */
export const MOCK_ENDED_CHALLENGES = [
    {
        /* --- tbl_challenge_group --- */
        id: 6,
        groupName: '편의점 커피 끊기',
        evalType: 'DAILY',
        limitAmount: 3000,
        startDate: '2026-07-22',
        endDate: '2026-07-28',
        /* --- tbl_group_member (본인) --- */
        finalOutcome: 'SURVIVED',
        finalRank: 2,
        finalChargeAmount: 0,
        /* --- 파생 필드 --- */
        totalDays: 7,
        memberCount: 6,
        totalMembers: 6,
        savingsAmount: 128000,
        members: [
            { userId: 1, nickname: '나', initial: '나' },
            { userId: 2, nickname: '김수현', initial: '김' },
            { userId: 3, nickname: '준서', initial: '준' },
            { userId: 4, nickname: '세영', initial: '세' },
            { userId: 5, nickname: '민지', initial: '민' },
            { userId: 6, nickname: '하은', initial: '하' },
        ],
    },
    {
        id: 7,
        groupName: '주말 배달 금지령',
        evalType: 'DAILY',
        limitAmount: 5000,
        startDate: '2026-07-08',
        endDate: '2026-07-14',
        finalOutcome: 'ELIMINATED',
        finalRank: 5,
        finalChargeAmount: 12000,
        totalDays: 7,
        memberCount: 5,
        totalMembers: 5,
        savingsAmount: null,
        members: [
            { userId: 1, nickname: '나', initial: '나' },
            { userId: 2, nickname: '김수현', initial: '김' },
            { userId: 3, nickname: '준서', initial: '준' },
            { userId: 5, nickname: '민지', initial: '민' },
            { userId: 6, nickname: '하은', initial: '하' },
        ],
    },
    {
        id: 8,
        groupName: '택시 대신 지하철 · 7월',
        evalType: 'DAILY',
        limitAmount: 8000,
        startDate: '2026-06-24',
        endDate: '2026-06-30',
        finalOutcome: 'SURVIVED',
        finalRank: 1,
        finalChargeAmount: 0,
        totalDays: 7,
        memberCount: 4,
        totalMembers: 4,
        savingsAmount: 96000,
        members: [
            { userId: 1, nickname: '나', initial: '나' },
            { userId: 3, nickname: '준서', initial: '준' },
            { userId: 4, nickname: '세영', initial: '세' },
            { userId: 6, nickname: '하은', initial: '하' },
        ],
    },
];

/* ────────────────────────────────────────────────────────────
 * 5. 홈 위젯용 진행 중 챌린지 (간소화 버전)
 * ──────────────────────────────────────────────────────────── */

export const MOCK_ACTIVE_CHALLENGES = [
    {
        id: 1,
        groupName: '배달 소비 줄이기',
        evalType: 'DAILY',
        limitAmount: 15000,
        currentDay: 6,
        totalDays: 7,
        livesCount: 5,
        maxLives: 7,
        memberCount: 5,
        pendingTrialCount: 1,
    },
    {
        id: 4,
        groupName: '택시 대신 지하철',
        evalType: 'DAILY',
        limitAmount: 8000,
        currentDay: 7,
        totalDays: 7,
        livesCount: 5,
        maxLives: 7,
        memberCount: 4,
        pendingTrialCount: 0,
        pendingDefenseCount: 1,
    },
];
