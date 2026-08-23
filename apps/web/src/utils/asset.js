const EOK = 100000000;
const MAN = 10000;

/*
 * v1 디자인 토큰(--tt-gray-*, --tt-accent-700 등)은 tokens.css v2(Ink/Gold 체계)로 교체되며
 * 사라졌다 — 그 값을 그대로 쓰면 정의되지 않은 var() 라 무효 선언이 되어 색이 아예 안 먹거나
 * 서로 다른 tone 이 같은 값으로 보인다(예: navy·blue 가 둘 다 흐려 보이던 문제).
 * v2 의미 토큰 5종(Ink/Blue/Green/Gold/Red)으로 다시 매핑한다.
 */
const TONE_COLORS = {
    navy: 'var(--tt-primary)',
    blue: 'var(--tt-info)',
    teal: 'var(--tt-success)',
    gray: 'var(--tt-text-hint)',
    danger: 'var(--tt-danger)',
    accent: 'var(--tt-accent)',
};

function toneColor(tone) {
    return TONE_COLORS[tone] ?? 'var(--tt-text-hint)';
}

function formatWon(amount) {
    const sign = amount < 0 ? '-' : '';
    return `${sign}${Math.abs(Math.trunc(amount)).toLocaleString('ko-KR')}원`;
}

function formatSignedWon(amount) {
    const arrow = amount < 0 ? '▼' : '▲';
    return `${arrow} ${formatWon(Math.abs(amount))}`;
}

function formatSignedPercent(rate) {
    const sign = rate < 0 ? '-' : '+';
    return `${sign}${Math.abs(rate * 100).toFixed(1)}%`;
}

function getHoldingCost(amount, gainAmount) {
    return amount - gainAmount;
}

function getHoldingAveragePrice(cost, quantity) {
    return quantity > 0 ? cost / quantity : 0;
}

/*
 * 도넛 중앙처럼 폭이 좁은 자리에 쓴다(이슈 #326). 만 단위에 콤마를 넣지 않는다.
 *
 * 자산 화면의 금액은 전부 formatWon 으로 전체 자릿수를 적는다 - 「1585만원」처럼 줄이면
 * 끝자리가 사라져 다른 화면의 같은 값과 어긋나 보인다. 좁아서 넘칠 때는 단위를 줄이는
 * 대신 글자 크기를 줄인다(AssetNetWorthCard).
 */
function formatCompactWon(amount) {
    const sign = amount < 0 ? '-' : '';
    const abs = Math.abs(amount);
    if (abs >= EOK) {
        return `${sign}${(abs / EOK).toFixed(1)}억원`;
    }
    if (abs >= MAN) {
        const man = Math.round(abs / MAN);
        if (man >= 10000) {
            return `${sign}${(abs / EOK).toFixed(1)}억원`;
        }
        return `${sign}${man}만원`;
    }
    return formatWon(amount);
}

function getCompositionTotal(composition) {
    return composition.reduce((sum, item) => sum + item.amount, 0);
}

function getCompositionRatios(composition) {
    const total = getCompositionTotal(composition);
    let offset = 0;
    return composition.map((item) => {
        const ratio = total === 0 ? 0 : item.amount / total;
        const start = offset;
        offset += ratio;
        return { ...item, ratio, offset: start };
    });
}

/*
 * padding 은 선 끝의 마커(원)나 stroke-width 가 viewBox 가장자리에서 잘리지 않게
 * 그려지는 영역을 안쪽으로 줄인다(이슈 #444). 값이 최고/최저치인 달은 y 가 정확히
 * 0 또는 height 에 닿고, 마지막 달은 x 가 항상 정확히 width 에 닿는다 — SVG 는
 * 기본적으로 viewBox 밖을 잘라내므로(overflow: hidden) 그 지점의 마커가 매번 잘려 보였다.
 */
function getSparklinePoints(trend, width, height, padding = 0) {
    if (trend.length === 0) {
        return { pointsAttr: '', lastPoint: null };
    }
    const min = Math.min(...trend);
    const max = Math.max(...trend);
    const range = max - min || 1;
    const drawWidth = width - padding * 2;
    const drawHeight = height - padding * 2;
    const stepX = trend.length > 1 ? drawWidth / (trend.length - 1) : 0;
    const coords = trend.map((value, index) => {
        const x = padding + stepX * index;
        const y = padding + drawHeight - ((value - min) / range) * drawHeight;
        return { x, y };
    });
    const pointsAttr = coords.map(({ x, y }) => `${x},${y}`).join(' ');
    return { pointsAttr, lastPoint: coords[coords.length - 1] };
}

function getBarHeights(netWorthArr, debtArr, maxHeightPx) {
    // 스냅샷이 없는 달은 null 로 들어온다 — 막대 높이는 0 으로 그리고,
    // "데이터 없음" 표시는 호출부가 별도로 판단한다(hasData 는 이 함수의 관심사가 아니다).
    const safeNetWorth = netWorthArr.map((v) => v ?? 0);
    const safeDebt = debtArr.map((v) => v ?? 0);
    const totals = safeNetWorth.map((netWorth, i) => netWorth + safeDebt[i]);
    const maxTotal = Math.max(...totals);
    const scale = maxTotal === 0 ? 0 : maxHeightPx / maxTotal;
    return safeNetWorth.map((netWorth, i) => ({
        netWorthHeight: netWorth * scale,
        debtHeight: safeDebt[i] * scale,
    }));
}

function getSignedPercent(from, to) {
    if (from === 0) {
        return '+0.0%';
    }
    const pct = ((to - from) / from) * 100;
    const sign = pct < 0 ? '-' : '+';
    return `${sign}${Math.abs(pct).toFixed(1)}%`;
}

export {
    toneColor,
    formatWon,
    formatSignedWon,
    formatSignedPercent,
    formatCompactWon,
    getCompositionTotal,
    getCompositionRatios,
    getSparklinePoints,
    getBarHeights,
    getSignedPercent,
    getHoldingCost,
    getHoldingAveragePrice,
};
