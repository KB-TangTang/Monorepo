export function normalizeMonthlyScore(monthlyScore) {
    return {
        score: Number(monthlyScore?.totalScore) || 0,
        topPercent:
            monthlyScore?.topPercent === null || monthlyScore?.topPercent === undefined
                ? null
                : Number(monthlyScore.topPercent),
    };
}

/*
 * 월간 순위 게이지의 채움 비율.
 *
 * 백분위는 쌓이는 값이 아니라 무리 안에서의 「위치」라서 가로 프로그래스바로 채우면
 * 은유가 어긋난다. 세로 막대에 내가 앞선 만큼을 아래에서 채우고 그 경계에 점선을 그어
 * 「나는 여기 서 있다」로 읽히게 한다.
 *
 * 돌려주는 값은 바닥에서 채울 비율(%)이다. 상위 12% 는 88% 를 앞섰다는 뜻이라 88 이 된다.
 * 순위가 아직 없으면 null — 0 을 돌려주면 게이지가 꼴찌를 가리켜 없는 순위를 지어낸다.
 */
export function calculateRankingFill(topPercent) {
    /* 빈 문자열을 그냥 두면 Number('') === 0 이라 순위 없는 사람이 1등으로 꽉 찬다 */
    if (topPercent === null || topPercent === undefined || topPercent === '') return null;

    const percent = Number(topPercent);
    if (!Number.isFinite(percent)) return null;

    return Math.round(100 - Math.min(Math.max(percent, 0), 100));
}
