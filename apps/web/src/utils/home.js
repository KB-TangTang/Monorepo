export function clampHomeProgress(value) {
    const progress = Number(value ?? 0);

    return Math.min(Math.max(progress, 0), 100);
}

export function formatHomeAmount(value) {
    return Number(value ?? 0).toLocaleString('ko-KR');
}

export function formatHomeRate(value) {
    const rate = Number(value ?? 0);

    return `${rate >= 0 ? '+' : ''}${rate}%`;
}
