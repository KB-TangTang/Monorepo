export function clampHomeProgress(value) {
    const progress = Number(value ?? 0);

    return Math.min(Math.max(progress, 0), 100);
}

export function formatHomeAmount(value) {
    return Number(value ?? 0).toLocaleString('ko-KR', {
        maximumFractionDigits: 0,
    });
}

export function formatHomeRate(value) {
    const rate = Number(value ?? 0);

    return `${rate >= 0 ? '+' : ''}${rate}%`;
}

export function getHomeAssetChange(rate) {
    if (rate === null || rate === undefined || !Number.isFinite(Number(rate))) {
        return null;
    }

    const numericRate = Number(rate);
    const rateLabel = `${Math.abs(numericRate).toLocaleString('ko-KR', {
        maximumFractionDigits: 2,
    })}%`;
    if (numericRate > 0) {
        return { text: `전월 대비 ${rateLabel} 증가`, tone: 'success' };
    }
    if (numericRate < 0) {
        return { text: `전월 대비 ${rateLabel} 감소`, tone: 'danger' };
    }
    return { text: '전월과 동일', tone: 'muted' };
}

export function getCurrentYearMonth(referenceDate = new Date()) {
    const year = referenceDate.getFullYear();
    const month = String(referenceDate.getMonth() + 1).padStart(2, '0');
    return `${year}-${month}`;
}

export function getDaysUntilNextMonth(referenceDate = new Date()) {
    const nextMonth = new Date(referenceDate.getFullYear(), referenceDate.getMonth() + 1, 1);
    const today = new Date(
        referenceDate.getFullYear(),
        referenceDate.getMonth(),
        referenceDate.getDate(),
    );
    return Math.ceil((nextMonth.getTime() - today.getTime()) / 86400000);
}

export function toHomeMission(mission) {
    if (!mission) return null;

    const limitAmount = Math.max(Number(mission.targetValue) || 0, 0);
    const spentAmount = Math.max(Number(mission.currentAmount) || 0, 0);
    const progressRate = limitAmount > 0 ? Math.round((spentAmount / limitAmount) * 100) : 0;
    const isAbsoluteMission = mission.missionType === 'ABSOLUTE';

    return {
        title: mission.missionTitle || mission.missionContent || '오늘의 메인 챌린지',
        isAbsoluteMission,
        categoryName: mission.categoryName ?? mission.parentCategoryName ?? '',
        limitAmount,
        spentAmount,
        remainingAmount: Math.max(limitAmount - spentAmount, 0),
        progressRate: clampHomeProgress(progressRate),
    };
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

/**
 * 홈 그룹 재판 카드의 상태를 한 곳에서 결정한다.
 * 조회 실패를 빈 목록처럼 취급하면 실제 할 일을 두고도 "순항 중"이라고 안내하므로 분리한다.
 */
export function getHomeGroupStatus({ trials = [], activeGroupCount = 0, failed = false } = {}) {
    if (failed) {
        return {
            kind: 'error',
            title: '그룹 재판을 확인하지 못했어요',
            caption: '눌러서 다시 확인해 주세요',
            item: null,
        };
    }

    if (trials.length > 0) {
        const [item] = trials;
        const remainingCount = trials.length - 1;
        return {
            kind: item.type === 'accuse' ? 'accuse' : 'vote',
            title: item.title,
            caption: remainingCount > 0 ? `그 외 할 일 ${remainingCount}건` : item.challengeName,
            item,
        };
    }

    if (activeGroupCount > 0) {
        return {
            kind: 'cruising',
            title: '그룹 챌린지가 순항 중이에요',
            caption: '지금 처리할 재판이 없어요',
            item: null,
        };
    }

    return {
        kind: 'empty',
        title: '진행 중인 그룹 챌린지가 없어요',
        caption: '새로운 챌린지에 참여해 보세요',
        item: null,
    };
}
