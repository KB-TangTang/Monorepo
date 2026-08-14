export function normalizeMonthlyScore(monthlyScore) {
    return {
        score: Number(monthlyScore?.totalScore) || 0,
        topPercent:
            monthlyScore?.topPercent === null || monthlyScore?.topPercent === undefined
                ? null
                : Number(monthlyScore.topPercent),
    };
}

export function calculateRankingProgress(topPercent) {
    if (topPercent === null || topPercent === undefined) return 0;

    const normalizedPercent = Math.min(Math.max(Number(topPercent), 1), 100);
    return Math.round(((100 - normalizedPercent) / 99) * 100);
}
