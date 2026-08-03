const GUIDE_PREFERENCE_KEY = 'tangtang.challenge-report.net-savings-guide-seen';

function resolveStorage(storage) {
    if (storage) {
        return storage;
    }
    return typeof window === 'undefined' ? null : window.localStorage;
}

export async function hasSeenNetSavingsGuide(storage) {
    return resolveStorage(storage)?.getItem(GUIDE_PREFERENCE_KEY) === 'true';
}

export async function markNetSavingsGuideSeen(storage) {
    resolveStorage(storage)?.setItem(GUIDE_PREFERENCE_KEY, 'true');
}

export { GUIDE_PREFERENCE_KEY };
