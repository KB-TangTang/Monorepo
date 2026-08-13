export const CHALLENGE_CONSENT_STATE = Object.freeze({
    ACTIVE: 'ACTIVE',
    FIRST: 'FIRST',
    WITHDRAWN: 'WITHDRAWN',
});

export function resolveChallengeConsentState(consent) {
    if (consent?.agreed) {
        return CHALLENGE_CONSENT_STATE.ACTIVE;
    }

    return consent?.termsVersion
        ? CHALLENGE_CONSENT_STATE.WITHDRAWN
        : CHALLENGE_CONSENT_STATE.FIRST;
}
