export function formatWon(value) {
    return `${new Intl.NumberFormat('ko-KR').format(value)}원`;
}

export function formatSignedWon(value) {
    const sign = value > 0 ? '+' : '';
    return `${sign}${formatWon(value)}`;
}

export function formatPercentage(value) {
    return `${value}%`;
}

export function formatPercentagePoint(value) {
    const sign = value > 0 ? '+' : '';
    return `${sign}${value}%`;
}

export function formatPeriod(period) {
    const [year, month] = period.split('-').map(Number);
    return `${year}년 ${month}월`;
}

export function getPreviousPeriod(referenceDate = new Date()) {
    const previousMonth = new Date(referenceDate.getFullYear(), referenceDate.getMonth() - 1, 1);
    const year = previousMonth.getFullYear();
    const month = String(previousMonth.getMonth() + 1).padStart(2, '0');
    return `${year}-${month}`;
}

export function isPublishedPeriod(period, referenceDate = new Date()) {
    return period <= getPreviousPeriod(referenceDate);
}

export function resolveChallengeReportState({ loading, error, report }) {
    if (loading) {
        return 'loading';
    }
    if (error) {
        return 'error';
    }
    if (!report?.hasChallengeHistory || report?.isFirstServiceMonth) {
        return 'empty';
    }
    return 'ready';
}

export function getEmptyReportCopy(report) {
    if (report?.isFirstServiceMonth) {
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
