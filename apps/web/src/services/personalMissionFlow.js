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

export function toTodayMissionBriefing(mission) {
    if (!mission) {
        return null;
    }

    const targetAmount = Number(mission.targetValue ?? 0);
    const currentAmount = Number(mission.currentAmount ?? 0);
    const isRelativeMission = mission.missionType === 'RELATIVE';

    return {
        missionTitle: mission.missionTitle ?? '',
        missionContent: mission.missionContent ?? '',
        categoryName: mission.categoryName ?? mission.parentCategoryName ?? '',
        alibiCondition: isRelativeMission
            ? `오늘 ${formatWon(targetAmount)} 이하`
            : (mission.guideMessage ?? mission.missionContent ?? mission.missionTitle ?? ''),
        currentAmount: Number.isFinite(currentAmount) ? currentAmount : 0,
        limitAmount: Number.isFinite(targetAmount) ? targetAmount : 0,
        streakDays: Number.isFinite(Number(mission.streakDays)) ? Number(mission.streakDays) : 0,
        difficultyName: mission.difficultyName ?? '',
        assignDate: mission.assignDate ?? '',
        assignmentReason: mission.assignmentReason ?? '',
    };
}

export function formatWon(amount) {
    const numericAmount = Number(amount);
    const roundedAmount = Number.isFinite(numericAmount) ? Math.round(numericAmount) : 0;
    return `${roundedAmount.toLocaleString('ko-KR')}원`;
}

export function toMissionVerdictModel(verdict, images = {}) {
    if (!verdict) {
        return null;
    }

    const isSuccess = verdict.result === 'SUCCESS';
    return {
        assignmentId: verdict.assignmentId,
        type: verdict.result,
        date: formatKoreanDate(verdict.assignDate),
        categoryName: verdict.categoryName ?? '',
        tangiQuote: isSuccess
            ? '"알리바이, 인정하겠습니다"'
            : '"이번 알리바이는 인정하기 어렵겠군요"',
        tangiImage: isSuccess ? images.success : images.fail,
        currentAmount: Number(verdict.currentAmount) || 0,
        limitAmount: Number(verdict.targetValue) || 0,
        remainAmount: Number(verdict.remainAmount) || 0,
        overAmount: Number(verdict.overAmount) || 0,
        points: Number(verdict.points) || 0,
        bonusPoints: Number(verdict.bonusPoints) || 0,
        streakDays: Number(verdict.streakDays) || 0,
        pendingCount: Number(verdict.pendingCount) || 0,
        transactions: (verdict.transactions ?? []).map((transaction) => ({
            id: transaction.transactionId,
            name: transaction.merchantName,
            amount: Number(transaction.amount) || 0,
        })),
    };
}

export function formatMissionAssignmentSummary(mission) {
    if (!mission) {
        return '배정 없음';
    }

    const difficultyName = mission.difficultyName ?? '난이도 없음';
    const targetRate = Number(mission.targetRate);
    const rateLabel = Number.isFinite(targetRate) ? `${targetRate}%` : '절감률 없음';

    return `${difficultyName} · ${rateLabel} · 목표 ${formatWon(mission.targetValue)}`;
}

export function toWatchCategoryModel(analysis) {
    const topCategories = analysis?.topCategories ?? [];

    return {
        period: formatAnalysisPeriod(analysis?.analysisStartDate, analysis?.analysisEndDate),
        items: topCategories.map((category) => ({
            categoryId: category.categoryId,
            name: category.categoryName ?? category.parentCategoryName ?? '',
            ratio: Math.round(Number(category.spendingRatio) || 0),
            missionRound: Number(category.missionRound) || 1,
            rotationAssignDate: formatShortDate(category.rotationAssignDate),
            rotationResult: category.rotationResult ?? 'WAITING',
        })),
    };
}

export function formatWatchlistRotationStatus(item) {
    if (item?.rotationResult === 'PENDING') {
        return '오늘 수사 중';
    }

    if (item?.rotationResult === 'SUCCESS' || item?.rotationResult === 'FAIL') {
        const resultLabel = item.rotationResult === 'SUCCESS' ? '인정' : '기각';
        return item.rotationAssignDate ? `${item.rotationAssignDate} ${resultLabel}` : resultLabel;
    }

    return '대기';
}

export function formatWatchlistMissionRound(item) {
    const missionRound = Math.max(Number(item?.missionRound) || 1, 1);
    if (missionRound === 1 && item?.rotationResult === 'PENDING') {
        return '첫 수사';
    }
    if (missionRound === 1 && item?.rotationResult === 'WAITING') {
        return '첫 수사 예정';
    }
    if (item?.rotationResult === 'WAITING') {
        return `수사 ${missionRound}회차 예정`;
    }
    return `수사 ${missionRound}회차`;
}

export function toWeeklyVerdictModel(streak, today = new Date()) {
    const dayNames = ['월', '화', '수', '목', '금', '토', '일'];
    const resultsByDate = new Map(
        (streak?.weeklyResults ?? []).map((item) => [item.date, item.result]),
    );
    const todayKey = formatLocalDateKey(today);
    const weekStart = streak?.weekStartDate
        ? new Date(`${streak.weekStartDate}T00:00:00`)
        : startOfCurrentWeek(today);

    return {
        streakDays: Number(streak?.streakCount) || 0,
        longestStreakDays: Number(streak?.longestStreakCount) || 0,
        days: dayNames.map((dow, index) => {
            const date = new Date(weekStart);
            date.setDate(weekStart.getDate() + index);
            const dateKey = formatLocalDateKey(date);
            const result = resultsByDate.get(dateKey);

            let status = 'pending';
            if (dateKey === todayKey && result === 'PENDING') status = 'today';
            else if (result === 'SUCCESS') status = 'success';
            else if (result === 'FAIL') status = 'failed';

            return { dow, date: dateKey, status };
        }),
    };
}

function startOfCurrentWeek(date) {
    const monday = new Date(date);
    const daysFromMonday = (monday.getDay() + 6) % 7;
    monday.setHours(0, 0, 0, 0);
    monday.setDate(monday.getDate() - daysFromMonday);
    return monday;
}

function formatLocalDateKey(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

function formatAnalysisPeriod(startDate, endDate) {
    if (!startDate || !endDate) return '';
    return `${formatShortDate(startDate)} – ${formatShortDate(endDate)}`;
}

function formatShortDate(dateValue) {
    if (!dateValue) return '';
    const [, month, day] = dateValue.split('-');
    return `${Number(month)}/${Number(day)}`;
}

function formatKoreanDate(dateValue) {
    if (!dateValue) return '';
    const [, month, day] = dateValue.split('-');
    return `${Number(month)}월 ${Number(day)}일`;
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
