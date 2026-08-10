/*
 * 개인 미션 챌린지의 판단 규칙을 모은 파일
 *
 * 화면 컴포넌트가 직접 계산하지 않도록 분리
 * 나중에 배정 규칙이 변경돼도 이 파일 위주로 수정 가능
 */

/*
 * 연동 계좌에서 최근 28일 소비 데이터와 전체 소비 50건이 확보됐는지 확인
 *
 * 요구사항:
 * 두 조건을 모두 충족해야 개인 맞춤 미션을 제공
 */
export function hasEnoughPersonalMissionData(profile) {
    return (
        profile.availableTransactionDays >= profile.requiredTransactionDays &&
        profile.transactionCount >= profile.requiredTransactionCount
    );
}

export function calculatePersonalMissionProgress(currentAmount, targetAmount) {
    if (targetAmount <= 0) {
        return currentAmount === 0 ? 100 : 0;
    }

    return Math.min(Math.round((currentAmount / targetAmount) * 100), 100);
}

export function formatWon(amount) {
    return `${Number(amount).toLocaleString('ko-KR')}원`;
}

/* ── v4 추가 함수 ──────────────────────────────────── */

/**
 * 오늘 날짜를 "2026. 07. 30 (목)" 형식으로 포맷
 */
export function formatCourtDate(date = new Date()) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const dayNames = ['일', '월', '화', '수', '목', '금', '토'];
    const dayName = dayNames[date.getDay()];

    return `${year}. ${month}. ${day} (${dayName})`;
}

/**
 * 데이터 수집 진행률 계산 (증거 부족 화면)
 */
export function calculateDataProgress(requirements) {
    const dayProgress = Math.min(requirements.availableDays / requirements.requiredDays, 1);
    const txProgress = Math.min(
        requirements.transactionCount / requirements.requiredTransactionCount,
        1,
    );

    return Math.round(((dayProgress + txProgress) / 2) * 100);
}

/**
 * 요주의 대상 상태 라벨 반환
 */
export function getWatchlistStatusLabel(status, clearedDate) {
    switch (status) {
        case 'CLEARED':
            return `${clearedDate} 인정`;
        case 'TODAY':
            return '오늘 수사 중';
        case 'PENDING':
            return '대기';
        default:
            return '';
    }
}
