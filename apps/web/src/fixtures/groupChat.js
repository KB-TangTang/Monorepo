/**
 * 그룹 채팅 목업 데이터
 *
 * - MOCK_CHAT_ROOMS   : 채팅방 메타 (groupId → room info)
 * - MOCK_CHAT_MESSAGES: 방별 메시지 배열 (최신이 뒤)
 * - MOCK_OLDER_MESSAGES: 스크롤-업 페이징 테스트용
 *
 * Redis 키 패턴 대응:
 *   chat:messages:{groupId}   → MOCK_CHAT_MESSAGES[groupId]
 *   chat:unread:{groupId}:{userId} → resetUnreadCount 에서 0으로
 */

/* ── 헬퍼 ───────────────────────────────────────────────── */
let _seq = 1000;

function msg(groupId, senderId, senderName, senderColor, content, minutesAgo) {
    const createdAt = new Date(Date.now() - minutesAgo * 60_000).toISOString();
    return {
        messageId: `m${_seq++}`,
        groupId,
        senderId,
        senderName,
        senderColor,
        messageType: 'USER',
        contentType: 'TEXT',
        content,
        systemSubType: null,
        metadata: null,
        createdAt,
    };
}

function sticker(groupId, senderId, senderName, senderColor, stickerId, minutesAgo) {
    const createdAt = new Date(Date.now() - minutesAgo * 60_000).toISOString();
    return {
        messageId: `m${_seq++}`,
        groupId,
        senderId,
        senderName,
        senderColor,
        messageType: 'USER',
        contentType: 'STICKER',
        content: stickerId,
        systemSubType: null,
        metadata: null,
        createdAt,
    };
}

function sys(groupId, subType, metadata, minutesAgo) {
    const createdAt = new Date(Date.now() - minutesAgo * 60_000).toISOString();
    return {
        messageId: `m${_seq++}`,
        groupId,
        senderId: null,
        senderName: null,
        senderColor: null,
        messageType: 'SYSTEM',
        contentType: null,
        content: null,
        systemSubType: subType,
        metadata,
        createdAt,
    };
}

/* ── 멤버 색상 (groupChallengeDetail.js AVATAR 대응) ──── */
const C = {
    1: '#232842',   // 나
    2: '#3E63D6',   // 유현
    3: '#2E9E6B',   // 준서
    4: '#E7A70C',   // 세영
    5: '#E0664B',   // 민지
    6: '#8B5CF6',   // 하은
};

/* ══════════════════════════════════════════════════════════
   Room 1 — 배달 소비 줄이기 (멤버 5명, 완전 재판 흐름)
   ══════════════════════════════════════════════════════════ */
const room1Messages = [
    msg(1, 2, '유현', C[2], '오늘부터 배달 줄이기 시작이다!', 1440),
    msg(1, 3, '준서', C[3], '화이팅 💪', 1435),
    msg(1, 1, '나', C[1], '배달비가 진짜 아깝긴 하지', 1430),
    sticker(1, 5, '민지', C[5], 'emotion_excited', 1425),
    msg(1, 6, '하은', C[6], '저도 오늘부터 도시락 싸갈게요', 1420),

    // 재판 게시
    sys(1, 'TRIAL_STARTED', {
        indictmentId: 'ind-101',
        defendantName: '민지',
        category: '배달',
        overAmount: 12500,
    }, 720),
    msg(1, 2, '유현', C[2], '민지 기소됐네 ㅋㅋ', 715),
    msg(1, 5, '민지', C[5], '어제 야근해서 어쩔 수 없었어 😭', 710),
    sticker(1, 3, '준서', C[3], 'prosecutor_magnifying', 705),

    // 투표 개시
    sys(1, 'VOTE_OPENED', {
        indictmentId: 'ind-101',
        defendantName: '민지',
        deadline: new Date(Date.now() + 12 * 3600_000).toISOString(),
    }, 360),
    msg(1, 6, '하은', C[6], '투표 완료~', 350),
    msg(1, 1, '나', C[1], '나도 투표했어', 340),

    // 판결 확정
    sys(1, 'VERDICT_CONFIRMED', {
        indictmentId: 'ind-101',
        defendantName: '민지',
        verdict: 'GUILTY',
        voteResult: { guilty: 4, innocent: 1 },
        penalty: '목숨 1 차감',
    }, 120),
    msg(1, 5, '민지', C[5], '유죄라니... 다음엔 꼭 참을게', 115),
    sticker(1, 2, '유현', C[2], 'judge_gavel_strike', 110),
    msg(1, 1, '나', C[1], '다들 배달 참자 ㅋㅋ', 60),
    sticker(1, 6, '하은', C[6], 'emotion_thumbs_up', 55),
];

/* ══════════════════════════════════════════════════════════
   Room 4 — 택시 대신 지하철 (멤버 4명, 투표 진행 중)
   ══════════════════════════════════════════════════════════ */
const room4Messages = [
    msg(4, 3, '준서', C[3], '택시비 아끼기 챌린지 시작!', 2880),
    msg(4, 1, '나', C[1], '지하철이 훨씬 싸긴 하지', 2870),
    msg(4, 4, '세영', C[4], '저 오늘 버스 탔어요 ㅎㅎ', 2860),
    sticker(4, 6, '하은', C[6], 'emotion_cheering', 2855),

    msg(4, 3, '준서', C[3], '세영이 어제 택시 탄 거 다 봤어', 1440),
    msg(4, 4, '세영', C[4], '비가 너무 많이 와서...', 1435),

    // 재판 게시
    sys(4, 'TRIAL_STARTED', {
        indictmentId: 'ind-401',
        defendantName: '세영',
        category: '교통',
        overAmount: 18000,
    }, 480),
    msg(4, 1, '나', C[1], '비 오면 택시 타고 싶긴 하지', 475),
    sticker(4, 3, '준서', C[3], 'prosecutor_direct_stare', 470),
    msg(4, 6, '하은', C[6], '변론 기대된다', 465),

    // 투표 개시 (진행 중)
    sys(4, 'VOTE_OPENED', {
        indictmentId: 'ind-401',
        defendantName: '세영',
        deadline: new Date(Date.now() + 6 * 3600_000).toISOString(),
    }, 180),
    msg(4, 1, '나', C[1], '투표 했어요~', 170),
    msg(4, 4, '세영', C[4], '제발 무죄로...', 160),
    sticker(4, 4, '세영', C[4], 'emotion_please', 155),
];

/* ══════════════════════════════════════════════════════════
   Room 5 — 편의점 간식 줄이기 (멤버 6명, 평화 — 재판 없음)
   ══════════════════════════════════════════════════════════ */
const room5Messages = [
    msg(5, 4, '세영', C[4], '편의점 간식 절약 화이팅!', 4320),
    msg(5, 2, '유현', C[2], '삼각김밥 참기 힘든데...', 4310),
    sticker(5, 1, '나', C[1], 'emotion_hungry', 4305),
    msg(5, 3, '준서', C[3], '집에서 간식 만들어오면 되지', 4300),
    msg(5, 5, '민지', C[5], '맞아 저는 과일 가져와요', 4295),
    msg(5, 6, '하은', C[6], '오이 깎아왔어 🥒', 4290),

    msg(5, 4, '세영', C[4], '오늘도 편의점 안 갔어요!', 1440),
    msg(5, 2, '유현', C[2], '나도! 이틀째 성공', 1430),
    sticker(5, 5, '민지', C[5], 'emotion_proud', 1425),
    msg(5, 1, '나', C[1], '아 근데 야근하면 편의점 가고 싶어지더라', 720),
    msg(5, 3, '준서', C[3], '참아!! 우리 다 같이 성공하자', 715),
    sticker(5, 6, '하은', C[6], 'emotion_cheering', 710),
    msg(5, 4, '세영', C[4], '내일이면 4일차! 반이상 왔다', 360),
    sticker(5, 2, '유현', C[2], 'emotion_celebration', 355),
    msg(5, 1, '나', C[1], '다들 대단하다 진짜', 120),
    sticker(5, 5, '민지', C[5], 'emotion_thumbs_up', 115),
];

/* ══════════════════════════════════════════════════════════
   Room 11 — 배달비 절약 도전 (멤버 4명, 혐의 인정 케이스)
   ══════════════════════════════════════════════════════════ */
const room11Messages = [
    msg(11, 1, '나', C[1], '배달비 절약 도전 시작합시다!', 2880),
    msg(11, 2, '유현', C[2], '네 화이팅!', 2875),
    msg(11, 4, '세영', C[4], '배달비 아까운 건 맞아요', 2870),
    sticker(11, 5, '민지', C[5], 'emotion_happy', 2865),

    msg(11, 2, '유현', C[2], '저 어제 치킨 시켜먹었어요 ㅠㅠ', 1440),
    msg(11, 1, '나', C[1], '배달비 얼마 나왔어?', 1435),
    msg(11, 2, '유현', C[2], '3천원이요...', 1430),

    // 재판 게시
    sys(11, 'TRIAL_STARTED', {
        indictmentId: 'ind-1101',
        defendantName: '유현',
        category: '배달',
        overAmount: 8500,
    }, 720),

    // 혐의 인정
    sys(11, 'CONFESSION', {
        indictmentId: 'ind-1101',
        defendantName: '유현',
    }, 600),
    msg(11, 2, '유현', C[2], '인정합니다... 치킨의 유혹을 못 참았어요', 595),
    sticker(11, 4, '세영', C[4], 'emotion_gentle_smile', 590),
    msg(11, 5, '민지', C[5], '솔직해서 좋다 ㅋㅋ', 585),
    msg(11, 1, '나', C[1], '다음엔 직접 가서 포장하자', 580),
    sticker(11, 2, '유현', C[2], 'emotion_apology', 575),
    msg(11, 4, '세영', C[4], '포장하면 배달비 0원이니까!', 300),
    msg(11, 5, '민지', C[5], '오늘 다들 점심 어떻게 해결했어요?', 180),
    msg(11, 1, '나', C[1], '도시락 싸왔지~', 175),
    sticker(11, 4, '세영', C[4], 'emotion_thumbs_up', 170),
];

/* ── 내보내기 ───────────────────────────────────────────── */
export const MOCK_CHAT_ROOMS = {
    1: {
        groupId: 1,
        challengeName: '배달 소비 줄이기',
        status: 'ACTIVE',
        memberCount: 5,
        currentDay: 5,
        daysLeft: 2,
        hasActiveTrial: false,
    },
    4: {
        groupId: 4,
        challengeName: '택시 대신 지하철',
        status: 'ACTIVE',
        memberCount: 4,
        currentDay: 6,
        daysLeft: 1,
        hasActiveTrial: true,
    },
    5: {
        groupId: 5,
        challengeName: '편의점 간식 줄이기',
        status: 'ACTIVE',
        memberCount: 6,
        currentDay: 4,
        daysLeft: 2,
        hasActiveTrial: false,
    },
    11: {
        groupId: 11,
        challengeName: '배달비 절약 도전',
        status: 'ACTIVE',
        memberCount: 4,
        currentDay: 5,
        daysLeft: 2,
        hasActiveTrial: false,
    },
};

export const MOCK_CHAT_MESSAGES = {
    1: room1Messages,
    4: room4Messages,
    5: room5Messages,
    11: room11Messages,
};

/* ── 스크롤-업 페이징 테스트용 (더 오래된 메시지) ──────── */
const olderBase = Date.now() - 7 * 24 * 3600_000; // 7일 전

function oldMsg(groupId, senderId, senderName, senderColor, content, idx) {
    return {
        messageId: `old-${groupId}-${idx}`,
        groupId,
        senderId,
        senderName,
        senderColor,
        messageType: 'USER',
        contentType: 'TEXT',
        content,
        systemSubType: null,
        metadata: null,
        createdAt: new Date(olderBase + idx * 60_000).toISOString(),
    };
}

export const MOCK_OLDER_MESSAGES = {
    1: [
        oldMsg(1, 2, '유현', C[2], '챌린지 참여할 사람 모집합니다~', 0),
        oldMsg(1, 3, '준서', C[3], '저요!', 1),
        oldMsg(1, 5, '민지', C[5], '나도 참여!', 2),
        oldMsg(1, 1, '나', C[1], '좋아 같이 하자', 3),
        oldMsg(1, 6, '하은', C[6], '저도 끼워주세요', 4),
    ],
    4: [
        oldMsg(4, 3, '준서', C[3], '택시비 줄이기 도전할 사람?', 0),
        oldMsg(4, 1, '나', C[1], '좋아요 참여합니다', 1),
        oldMsg(4, 4, '세영', C[4], '저도요', 2),
        oldMsg(4, 6, '하은', C[6], '지하철 환승 앱 추천해요', 3),
    ],
    5: [
        oldMsg(5, 4, '세영', C[4], '편의점 간식 줄이기 같이 해요', 0),
        oldMsg(5, 2, '유현', C[2], '어렵겠지만 해볼게', 1),
        oldMsg(5, 1, '나', C[1], '건강에도 좋겠다', 2),
        oldMsg(5, 3, '준서', C[3], '참여!', 3),
        oldMsg(5, 5, '민지', C[5], '저도 넣어주세요~', 4),
        oldMsg(5, 6, '하은', C[6], '풀방이네 ㅎㅎ', 5),
    ],
    11: [
        oldMsg(11, 1, '나', C[1], '배달비 줄여볼 사람!', 0),
        oldMsg(11, 2, '유현', C[2], '저요~ 배달비 진짜 아깝더라고요', 1),
        oldMsg(11, 4, '세영', C[4], '참여합니다', 2),
        oldMsg(11, 5, '민지', C[5], '같이 해요!', 3),
    ],
};
