const PERSONAL_KEY = 'tangtang.personal-challenge.tutorial-seen';
const GROUP_KEY = 'tangtang.group-challenge.tutorial-seen';

function storage() {
    return typeof window === 'undefined' ? null : window.localStorage;
}

/* ── 개인(대법원) ───────────────────── */
export function hasSeenPersonalTutorial() {
    return storage()?.getItem(PERSONAL_KEY) === 'true';
}

export function markPersonalTutorialSeen() {
    storage()?.setItem(PERSONAL_KEY, 'true');
}

export function resetPersonalTutorial() {
    storage()?.removeItem(PERSONAL_KEY);
}

/* ── 그룹(지방법원) ──────────────────── */
export function hasSeenGroupTutorial() {
    return storage()?.getItem(GROUP_KEY) === 'true';
}

export function markGroupTutorialSeen() {
    storage()?.setItem(GROUP_KEY, 'true');
}

export function resetGroupTutorial() {
    storage()?.removeItem(GROUP_KEY);
}
