import test from 'node:test';
import assert from 'node:assert/strict';
import {
    CHALLENGE_CONSENT_STATE,
    resolveChallengeConsentState,
} from '../src/services/challengeConsent.js';

test('동의 이력이 없으면 최초 참여 상태다', () => {
    assert.equal(
        resolveChallengeConsentState({ agreed: false, termsVersion: null }),
        CHALLENGE_CONSENT_STATE.FIRST,
    );
});

test('약관 버전이 남아 있는 미동의는 철회 상태다', () => {
    assert.equal(
        resolveChallengeConsentState({ agreed: false, termsVersion: '1.0' }),
        CHALLENGE_CONSENT_STATE.WITHDRAWN,
    );
});

test('동의 사용자는 참여 중 상태다', () => {
    assert.equal(
        resolveChallengeConsentState({ agreed: true, termsVersion: '1.0' }),
        CHALLENGE_CONSENT_STATE.ACTIVE,
    );
});
