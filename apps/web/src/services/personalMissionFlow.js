/*
 * 개인 미션 챌린지의 판단 규칙을 모은 파일
 *
 * 화면 컴포넌트가 직접 계산하지 않도록 분리
 * 나중에 배정 규칙이 변경돼도 이 파일 위주로 수정 가능
 */

export const PERSONAL_MISSION_TYPE = {
    ABSOLUTE: 'ABSOLUTE',
    RELATIVE: 'RELATIVE',
};

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

export function shouldShowPersonalMissionUnlock({
    hasAgreed,
    hasEnoughData,
    wasDataInsufficient,
    hasSeenDataUnlock,
}) {
    return hasAgreed && hasEnoughData && wasDataInsufficient && !hasSeenDataUnlock;
}

/*
 * 데이터가 부족하면 무조건 절대형 미션을 반환
 * 데이터가 충분하면 절대형과 상대형 중 하나를 임시로 선택
 *
 * 실제 서비스에서는 백엔드가 하루의 미션을 확정한 뒤 내려줘야 함
 */
export function selectPersonalMissionType(profile, randomValue = Math.random()) {
    if (!hasEnoughPersonalMissionData(profile)) {
        return PERSONAL_MISSION_TYPE.ABSOLUTE;
    }

    return randomValue < 0.5 ? PERSONAL_MISSION_TYPE.ABSOLUTE : PERSONAL_MISSION_TYPE.RELATIVE;
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
