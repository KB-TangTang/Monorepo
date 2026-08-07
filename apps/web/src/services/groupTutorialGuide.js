const GROUP_TUTORIAL_KEY = 'tangtang.group-challenge.tutorial-seen';

function resolveStorage(storage) {
    if (storage) {
        return storage;
    }
    return typeof window === 'undefined' ? null : window.localStorage;
}

export function hasSeenGroupTutorial(storage) {
    return resolveStorage(storage)?.getItem(GROUP_TUTORIAL_KEY) === 'true';
}

export function markGroupTutorialSeen(storage) {
    resolveStorage(storage)?.setItem(GROUP_TUTORIAL_KEY, 'true');
}

export function resetGroupTutorial(storage) {
    resolveStorage(storage)?.removeItem(GROUP_TUTORIAL_KEY);
}

export { GROUP_TUTORIAL_KEY };
