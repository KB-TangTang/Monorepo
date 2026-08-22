export function formatInteger(value) {
    const number = Number(value);
    return Number.isFinite(number) ? Math.trunc(number).toLocaleString('ko-KR') : '0';
}

export function formatDecimal(value) {
    const number = Number(value);
    return Number.isFinite(number)
        ? number.toLocaleString('ko-KR', { maximumFractionDigits: 20 })
        : '0';
}

export function formatWon(value) {
    return `${formatInteger(value)}원`;
}

export function formatSignedWon(value) {
    const sign = Number(value) > 0 ? '+' : '';
    return `${sign}${formatWon(value)}`;
}

export function formatPercentage(value) {
    return `${formatDecimal(value)}%`;
}

export function formatPercentagePoint(value) {
    const sign = Number(value) > 0 ? '+' : '';
    return `${sign}${formatDecimal(value)}%`;
}

export function formatPeriod(period) {
    if (!/^\d{4}-\d{2}$/.test(period ?? '')) {
        return '';
    }
    const [year, month] = period.split('-').map(Number);
    return `${year}년 ${month}월`;
}

export function getPreviousPeriod(referenceDate = new Date()) {
    const previousMonth = new Date(referenceDate.getFullYear(), referenceDate.getMonth() - 1, 1);
    const year = previousMonth.getFullYear();
    const month = String(previousMonth.getMonth() + 1).padStart(2, '0');
    return `${year}-${month}`;
}

const DIFFICULTY_PRESENTATION = {
    EASY: { level: '하', tone: 'low' },
    NORMAL: { level: '중', tone: 'middle' },
    HARD: { level: '상', tone: 'high' },
};

const GROUP_RECORD_STATES = new Set(['JUDGING', 'READY', 'EMPTY']);

function toCategoryPresentation(categoryName) {
    return categoryName?.trim().slice(0, 1) || '카';
}

function toCategoryRows(categoryEffects) {
    return categoryEffects
        .flatMap((effect) => [
            {
                code: toCategoryPresentation(effect.categoryName),
                name: effect.categoryName,
                days: effect.successfulDays,
                amount: effect.savedAmount,
                status: 'success',
            },
            {
                code: toCategoryPresentation(effect.categoryName),
                name: effect.categoryName,
                days: effect.failedDays,
                amount: effect.overspentAmount,
                status: 'failure',
            },
        ])
        .filter((category) => category.days > 0);
}

function resolveCategoryRows(report) {
    const categories = Array.isArray(report.categories) ? report.categories : [];
    return categories.length > 0 || !Array.isArray(report.categoryEffects)
        ? categories
        : toCategoryRows(report.categoryEffects);
}

export function resolveGroupRecordState({ groupRecordState, groupRecord }) {
    if (groupRecordState === 'JUDGING') {
        return 'JUDGING';
    }
    if (groupRecordState === 'READY' && groupRecord) {
        return 'READY';
    }
    if (GROUP_RECORD_STATES.has(groupRecordState) && groupRecordState === 'EMPTY') {
        return 'EMPTY';
    }
    return groupRecord ? 'READY' : 'EMPTY';
}

export function toChallengeReportModel(report) {
    return {
        ...report,
        challengeName: '메인 챌린지',
        difficultySummary: '난이도별 성과를 확인해 보세요',
        categories: resolveCategoryRows(report),
        groupRecordState: resolveGroupRecordState(report),
        ranking: report.ranking ?? null,
        difficulties: (report.difficulties ?? []).map((difficulty) => ({
            ...difficulty,
            ...(DIFFICULTY_PRESENTATION[difficulty.difficultyName] ?? {
                level: difficulty.difficultyName,
                tone: 'middle',
            }),
        })),
    };
}

export function isPublishedPeriod(period, referenceDate = new Date()) {
    return period <= getPreviousPeriod(referenceDate);
}

export function resolveChallengeReportState({ loading, error, report, entryState }) {
    if (loading) {
        return 'loading';
    }
    if (error) {
        return 'error';
    }
    if (entryState === 'NOT_AGREED') {
        return 'not-agreed';
    }
    if (entryState === 'PREPARING_FIRST_REPORT') {
        return 'preparing';
    }
    if (!report?.hasChallengeHistory) {
        return 'empty';
    }
    return 'ready';
}

export function getEmptyReportCopy(report, entryState) {
    if (entryState === 'NOT_AGREED') {
        return {
            title: '챌린지 참여 동의가 필요해요',
            description: '챌린지를 시작하면 한 달의 성과를 재판 보고서로 정리해 드릴게요.',
        };
    }
    if (entryState === 'PREPARING_FIRST_REPORT') {
        return {
            title: '첫 달의 기록을 모으고 있어요',
            description: '한 달을 함께 보낸 뒤 첫 챌린지 리포트를 준비해 드릴게요.',
        };
    }
    return {
        title: '아직 판결할 기록이 없어요',
        description: '첫 챌린지를 마치면\n성적표가 만들어져요',
    };
}
