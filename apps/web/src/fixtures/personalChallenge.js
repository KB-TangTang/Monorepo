/*
 * 개인 챌린지 v4 — 주간 로테이션 화면 임시 데이터
 * 실제 API가 완성되면 이 파일 대신 API 응답을 사용하면 됨
 */

/* ── 검사 탕이 전용 이미지 (prosecutor_tangtang/) ──────── */
import tangiMagnify from '@/assets/images/prosecutor_tangtang/magnifying_stare.png';
import tangiDirect from '@/assets/images/prosecutor_tangtang/direct_stare_close_uncomfortable.png';
import tangiProsecutor from '@/assets/images/prosecutor_tangtang/prosecutor_tangtang_A.png';
import tangiFieldCheck from '@/assets/images/prosecutor_tangtang/field_verification_v1_one_leg_no_emphasis.png';
import tangiFieldVerify from '@/assets/images/prosecutor_tangtang/field_verification_v2.png';
import tangiRaidBust from '@/assets/images/prosecutor_tangtang/consumer_moment_raid_v1_2d_no_emphasis.png';
import tangiRaidWatch from '@/assets/images/prosecutor_tangtang/consumer_moment_raid_v2_2d_no_emphasis.png';
import tangiSceneRaid from '@/assets/images/prosecutor_tangtang/scene_raid_v1_one_leg_no_emphasis.png';
import tangiSceneWatch from '@/assets/images/prosecutor_tangtang/scene_raid_v2_one_leg_no_emphasis.png';

/* ── 검사 성향 3종 ──────────────────────────────────────
 * id 는 tbl_mission_difficulty.difficulty_name 과 같은 값을 쓴다(EASY/NORMAL/HARD).
 * API 요청·응답도 이 값이라 화면에서 따로 매핑하지 않는다.
 * (2026-08-11 팀 결정 — 구 ID TOUGH/STRICT/LENIENT 는 STRICT 가 '중' 에 붙어 있어
 *  영어 뜻과 난이도가 어긋나 있었다. DECISIONS.md 2026-08-11 참고)
 */
export const MOCK_PROSECUTORS = [
    {
        id: 'HARD',
        name: '깐깐한 탕이',
        bonusPoints: 35,
        targetReductionRange: '40-50%',
        quote: '"1원도 넘기면 안 됩니다."',
        description: '조건이 가장 빡세요',
        image: tangiMagnify,
        badgeBg: 'var(--tt-danger-subtle)',
        badgeColor: 'var(--tt-danger-deep)',
        recommended: false,
    },
    {
        id: 'NORMAL',
        name: '냉정한 탕이',
        bonusPoints: 20,
        targetReductionRange: '20-40%',
        quote: '"넘지 않으면 문제 삼지 않겠습니다."',
        description: '',
        image: tangiDirect,
        badgeBg: 'var(--tt-info-subtle)',
        badgeColor: 'var(--tt-info)',
        recommended: true,
    },
    {
        id: 'EASY',
        name: '너그러운 탕이',
        bonusPoints: 10,
        targetReductionRange: '10-20%',
        quote: '"이 정도는 눈감아 드리죠."',
        description: '가볍게 시작',
        image: tangiProsecutor,
        badgeBg: 'var(--tt-accent-subtle)',
        badgeColor: 'var(--tt-accent-deep)',
        recommended: false,
    },
];

/* ── 오늘의 수사 브리핑 ──────────────────────────────── */
export const MOCK_TODAY_BRIEFING = {
    categoryName: '카페/간식',
    alibiCondition: '오늘 6,000원 이하',
    currentAmount: 3200,
    limitAmount: 6000,
    streakDays: 5,
    prosecutorId: 'NORMAL',
    date: '2026-07-30',
};

/* ── 이번 주 요주의 대상 3곳 ─────────────────────────── */
export const MOCK_WEEKLY_WATCHLIST = [
    { name: '배달앱', ratio: 24, status: 'CLEARED', clearedDate: '7/29', progress: 48 },
    { name: '카페/간식', ratio: 18, status: 'TODAY', progress: 36 },
    { name: '택시/모빌리티', ratio: 11, status: 'PENDING', progress: 22 },
];

export const MOCK_WATCHLIST_META = {
    weekRange: '7/29 – 8/4',
    currentIndex: 2,
    totalCount: 3,
    comment: '"이번 주는 배달앱·카페/간식·택시를 지켜보겠습니다."',
};

/* ── 이번 주 판정 (스트릭) ────────────────────────────── */
export const MOCK_WEEKLY_VERDICT = {
    days: [
        { dow: '금', status: 'failed' },
        { dow: '토', status: 'success' },
        { dow: '일', status: 'success' },
        { dow: '월', status: 'success' },
        { dow: '화', status: 'success' },
        { dow: '수', status: 'success' },
        { dow: '목', status: 'today' },
    ],
    streakDays: 5,
    streakText: '토요일부터 5일 연속 인정 · 오늘 인정 시 6일',
};

/* ── 이번 달 누적 점수 ───────────────────────────────── */
export const MOCK_MONTHLY_SCORE = {
    score: 520,
    percentile: 15,
    nextTierGap: 80,
    tierProgress: 78,
};

/* ── 판정 결과 (어제 것) ─────────────────────────────── */
export const MOCK_VERDICT_SUCCESS = {
    type: 'SUCCESS',
    date: '7월 29일',
    categoryName: '배달앱',
    tangiQuote: '"알리바이, 인정하겠습니다"',
    tangiImage: tangiFieldCheck,
    currentAmount: 9800,
    limitAmount: 12000,
    remainAmount: 2200,
    points: 35,
    bonusPoints: 5,
    streakDays: 6,
    pendingCount: 0,
    transactions: [
        { name: '돈까스집 배달주문', amount: 6300 },
        { name: '한강 분식', amount: 3500, manuallyEdited: true },
    ],
};

export const MOCK_VERDICT_FAIL = {
    type: 'FAIL',
    date: '7월 29일',
    categoryName: '카페/간식',
    tangiQuote: '"이번 알리바이는 인정하기 어렵겠군요"',
    tangiImage: tangiSceneRaid,
    currentAmount: 15200,
    limitAmount: 12000,
    overAmount: 3200,
    points: 0,
    bonusPoints: 0,
    streakDays: 0,
    pendingCount: 3,
    transactions: [
        { name: '스타벅스', amount: 5800 },
        { name: '투썸플레이스', amount: 4200 },
        { name: '메가커피', amount: 3200 },
        { name: '편의점 음료', amount: 2000 },
    ],
};

/* ── 맞춤 사건 개시 조건 (증거 부족용) ───────────────── */
export const MOCK_DATA_REQUIREMENTS = {
    accountLinked: true,
    availableDays: 19,
    requiredDays: 28,
    transactionCount: 12,
    requiredTransactionCount: 50,
};

/* ── 공통 사건 (데이터 부족 시) ───────────────────────── */
export const MOCK_COMMON_MISSION = {
    title: '오늘은 배달앱 0원 도전',
    category: '배달앱',
    currentAmount: 0,
    rewardPoints: 10,
};

/* ── 이미지 내보내기 ─────────────────────────────────── */
export {
    tangiMagnify,
    tangiDirect,
    tangiProsecutor,
    tangiFieldCheck,
    tangiFieldVerify,
    tangiRaidBust,
    tangiRaidWatch,
    tangiSceneRaid,
    tangiSceneWatch,
};
