export function clampHomeProgress(value) {
    const progress = Number(value ?? 0);

    return Math.min(Math.max(progress, 0), 100);
}

export function formatHomeAmount(value) {
    return Number(value ?? 0).toLocaleString('ko-KR', {
        maximumFractionDigits: 0,
    });
}

/**
 * 「오늘 쓴 돈」의 어제 대비 증감 문구.
 * rate 는 어제 대비 증감률(%)이고 음수면 덜 쓴 것이다.
 * 어제 지출이 0이면 비율을 낼 수 없어 백엔드가 null 을 주므로 그대로 문구를 생략한다.
 */
export function getHomeSpendingChange(rate) {
    if (rate === null || rate === undefined || !Number.isFinite(Number(rate))) {
        return null;
    }

    const numericRate = Number(rate);
    const rateLabel = `${Math.abs(Math.round(numericRate))}%`;
    if (numericRate < 0) {
        return { text: `어제보다 ${rateLabel} 적게 쓰는 중`, tone: 'down' };
    }
    if (numericRate > 0) {
        return { text: `어제보다 ${rateLabel} 많이 쓰는 중`, tone: 'up' };
    }
    return { text: '어제와 비슷하게 쓰는 중', tone: 'same' };
}

function toHomeSpendingValue(value) {
    const numeric = Number(value);
    return value === null || value === undefined || !Number.isFinite(numeric) ? null : numeric;
}

/**
 * 홈 「오늘 쓴 돈」 카드 모델. GET /api/transactions/summary/daily 응답을 받는다.
 *
 * 「호출 실패」와 「0원 쓴 날」을 가르는 자리다 — 카드는 null 을 받으면 「—」·「집계 중」으로 버티는데
 * 그건 실패 표시지 0원 표시가 아니다. 서버는 금액에 null 을 주지 않으므로 0 을 null 로 바꾸지 않는다.
 * changeRate 만 null 을 보존한다 — 어제 지출이 0이면 서버가 null 을 주고, 카드는 증감 문구를 생략한다.
 */
export function toHomeSpending(summary) {
    if (!summary) {
        return { todayAmount: null, monthAmount: null, changeRate: null };
    }

    return {
        todayAmount: toHomeSpendingValue(summary.todayAmount),
        monthAmount: toHomeSpendingValue(summary.monthAmount),
        changeRate: toHomeSpendingValue(summary.changeRate),
    };
}

export function getCurrentYearMonth(referenceDate = new Date()) {
    const year = referenceDate.getFullYear();
    const month = String(referenceDate.getMonth() + 1).padStart(2, '0');
    return `${year}-${month}`;
}

export function toHomeMission(mission) {
    if (!mission) return null;

    const limitAmount = Math.max(Number(mission.targetValue) || 0, 0);
    const spentAmount = Math.max(Number(mission.currentAmount) || 0, 0);
    const isAbsoluteMission = mission.missionType === 'ABSOLUTE';
    const progressRate = isAbsoluteMission
        ? spentAmount > 0
            ? 100
            : 0
        : limitAmount > 0
          ? Math.round((spentAmount / limitAmount) * 100)
          : 0;

    return {
        title: mission.missionTitle || mission.missionContent || '오늘의 메인 챌린지',
        isAbsoluteMission,
        categoryName: mission.categoryName ?? mission.parentCategoryName ?? '',
        limitAmount,
        spentAmount,
        remainingAmount: Math.max(limitAmount - spentAmount, 0),
        exceededAmount: Math.max(spentAmount - limitAmount, 0),
        progressRate: clampHomeProgress(progressRate),
    };
}

/**
 * 탕이 말풍선 문구. toHomeMission() 의 결과를 받는다.
 * ABSOLUTE(금지형)는 남은 금액이라는 개념이 없어 위반 여부로 말한다.
 * 이슈 #450 결정 ① — /api/missions/today 가 하루 1건만 주므로 여러 건을 넘기는 페이저는 없다.
 */
export function getHomeMissionBubbleCopy(mission) {
    if (!mission) {
        return { lead: '오늘의 미션이 아직 없어요,', highlight: '챌린지에 참여해 보세요!' };
    }

    const lead = mission.categoryName ? `오늘의 ${mission.categoryName} 미션,` : '오늘의 미션,';

    if (mission.isAbsoluteMission) {
        return {
            lead,
            highlight: mission.spentAmount > 0 ? '오늘은 위반이 있었어요' : '아직 위반이 없어요!',
        };
    }

    if (mission.remainingAmount <= 0) {
        return { lead, highlight: '한도를 모두 썼어요' };
    }

    return { lead, highlight: `${formatHomeAmount(mission.remainingAmount)}원 남았어요!` };
}

/**
 * 재판 카드 대법원(개인) 줄. 연속 무죄 일수(GET /api/missions/streak 의 streakCount)만 받는다.
 *
 * #450 수정 — 원래는 오늘 미션의 남은 한도를 띄웠는데 바로 위 초록 말풍선이 같은 값을 이미 말한다.
 * 한 화면에 같은 문장이 두 번 뜨는 게 이상해 연속 무죄로 바꿨다. 미션은 말풍선, 누적은 이 줄.
 */
export function getHomePersonalTrialRow(streakDays) {
    const streak = Number(streakDays);

    if (!Number.isFinite(streak) || streak <= 0) {
        return { text: '아직 연속 무죄 기록이 없어요', badge: null };
    }

    return { text: `연속 무죄 ${streak}일째`, badge: null };
}

/**
 * 「진행 중인 재판」 지방법원(그룹) 줄.
 * 뱃지는 급한 것 하나만 띄운다 — 변론은 시한이 짧아 투표보다 먼저다.
 */
export function getHomeGroupTrialRow({ trials = [], activeGroupCount = 0, failed = false } = {}) {
    if (failed) {
        return { text: '그룹 재판을 확인하지 못했어요', badge: null };
    }

    const text = activeGroupCount > 0 ? `${activeGroupCount}건 진행중` : '진행 중인 그룹이 없어요';
    const accuseCount = trials.filter((trial) => trial?.type === 'accuse').length;
    const voteCount = trials.filter((trial) => trial?.type === 'vote').length;

    if (accuseCount > 0) {
        return { text, badge: `변론 ${accuseCount}건` };
    }
    if (voteCount > 0) {
        return { text, badge: `투표 ${voteCount}건` };
    }
    return { text, badge: null };
}

export function toHomeReportSummary(report) {
    if (!report?.period || !report.hasChallengeHistory) return null;

    const topCategory = [...(report.categoryEffects ?? [])]
        .filter((category) => Number(category.savedAmount) > 0)
        .sort((a, b) => Number(b.savedAmount) - Number(a.savedAmount))[0];

    return {
        period: report.period,
        month: Number(report.period.split('-')[1]),
        savedAmount: Number(report.savedAmount) || 0,
        topCategoryName: topCategory?.categoryName ?? null,
    };
}

export function getHomeReportEmptyCopy(status) {
    if (status === 'not-agreed') {
        return {
            title: '챌린지를 시작해 보세요',
            description: '참여 후 소비습관 변화를 기록해 드릴게요',
        };
    }
    if (status === 'preparing') {
        return {
            title: '첫 리포트를 준비 중이에요',
            description: '한 달의 기록이 모이면 결과를 알려드릴게요',
        };
    }
    if (status === 'error') {
        return {
            title: '리포트를 불러오지 못했어요',
            description: '자세히 보기에서 다시 확인해 주세요',
        };
    }
    return {
        title: '아직 확정된 절감 결과가 없어요',
        description: '챌린지를 완료하면 변화가 기록돼요',
    };
}
