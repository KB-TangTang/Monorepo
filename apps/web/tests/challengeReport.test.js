import test from 'node:test';
import assert from 'node:assert/strict';
import {
    formatPercentagePoint,
    formatSignedWon,
    formatWon,
    getPreviousPeriod,
    isPublishedPeriod,
    resolveChallengeReportState,
} from '../src/utils/challengeReport.js';

test('리포트 표시 값을 포맷한다', () => {
    assert.equal(formatWon(74000), '74,000원');
    assert.equal(formatSignedWon(40000), '+40,000원');
    assert.equal(formatSignedWon(-22000), '-22,000원');
    assert.equal(formatPercentagePoint(9), '+9%');
});

test('현재 월이 아닌 이전 달까지만 공개한다', () => {
    const august = new Date(2026, 7, 3);
    assert.equal(getPreviousPeriod(august), '2026-07');
    assert.equal(isPublishedPeriod('2026-07', august), true);
    assert.equal(isPublishedPeriod('2026-08', august), false);
});

test('리포트 상태 우선순위를 판정한다', () => {
    const ready = { hasChallengeHistory: true, isFirstServiceMonth: false };
    assert.equal(
        resolveChallengeReportState({ loading: true, error: '오류', report: ready }),
        'loading',
    );
    assert.equal(resolveChallengeReportState({ error: '오류', report: ready }), 'error');
    assert.equal(resolveChallengeReportState({ report: { hasChallengeHistory: false } }), 'empty');
    assert.equal(resolveChallengeReportState({ report: ready }), 'ready');
});
