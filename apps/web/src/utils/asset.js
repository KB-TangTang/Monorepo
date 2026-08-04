const EOK = 100000000;
const MAN = 10000;

function formatWon(amount) {
    const sign = amount < 0 ? '-' : '';
    return `${sign}₩${Math.abs(Math.trunc(amount)).toLocaleString('ko-KR')}`;
}

function formatSignedWon(amount) {
    const arrow = amount < 0 ? '▼' : '▲';
    return `${arrow} ${formatWon(Math.abs(amount))}`;
}

function formatCompactWon(amount) {
    const sign = amount < 0 ? '-' : '';
    const abs = Math.abs(amount);
    if (abs >= EOK) {
        return `${sign}₩${(abs / EOK).toFixed(1)}억`;
    }
    if (abs >= MAN) {
        const man = Math.round(abs / MAN);
        if (man >= 10000) {
            return `${sign}₩${(abs / EOK).toFixed(1)}억`;
        }
        return `${sign}₩${man.toLocaleString('ko-KR')}만`;
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

function getSparklinePoints(trend, width, height) {
    if (trend.length === 0) {
        return { pointsAttr: '', lastPoint: null };
    }
    const min = Math.min(...trend);
    const max = Math.max(...trend);
    const range = max - min || 1;
    const stepX = trend.length > 1 ? width / (trend.length - 1) : 0;
    const coords = trend.map((value, index) => {
        const x = stepX * index;
        const y = height - ((value - min) / range) * height;
        return { x, y };
    });
    const pointsAttr = coords.map(({ x, y }) => `${x},${y}`).join(' ');
    return { pointsAttr, lastPoint: coords[coords.length - 1] };
}

export {
    formatWon,
    formatSignedWon,
    formatCompactWon,
    getCompositionTotal,
    getCompositionRatios,
    getSparklinePoints,
};
