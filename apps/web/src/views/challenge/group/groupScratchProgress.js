export const SCRATCH_COMPLETION_RATIO = 0.28;

export function calculateScratchProgress(transparentSamples, totalSamples) {
    if (totalSamples <= 0) {
        return 0;
    }

    const transparentRatio = transparentSamples / totalSamples;
    return Math.min(100, Math.round((transparentRatio / SCRATCH_COMPLETION_RATIO) * 100));
}
