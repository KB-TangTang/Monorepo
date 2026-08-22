import test from 'node:test';
import assert from 'node:assert/strict';
import {
    formatDecimal,
    formatInteger,
    formatPercentagePoint,
    formatPeriod,
    formatSignedWon,
    formatWon,
    getPreviousPeriod,
    isPublishedPeriod,
    resolveGroupRecordState,
    resolveChallengeReportState,
} from '../src/utils/challengeReport.js';

test('리포트 표시 값을 포맷한다', () => {
    assert.equal(formatWon(74000), '74,000원');
    assert.equal(formatWon(74000.98), '74,000원');
    assert.equal(formatSignedWon(40000), '+40,000원');
    assert.equal(formatSignedWon(-22000), '-22,000원');
    assert.equal(formatInteger(12.75), '12');
    assert.equal(formatInteger(-12.75), '-12');
    assert.equal(formatDecimal(12.75), '12.75');
    assert.equal(formatPercentagePoint(9), '+9%');
    assert.equal(formatPercentagePoint(-1.25), '-1.25%');
    assert.equal(formatPeriod('2026-06'), '2026년 6월');
    assert.equal(formatPeriod(''), '');
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
    assert.equal(
        resolveChallengeReportState({
            report: { hasChallengeHistory: true, isFirstServiceMonth: true },
        }),
        'ready',
    );
});

test('API 진입 상태를 리포트 빈 상태와 구분한다', () => {
    assert.equal(resolveChallengeReportState({ entryState: 'NOT_AGREED' }), 'not-agreed');
    assert.equal(
        resolveChallengeReportState({ entryState: 'PREPARING_FIRST_REPORT' }),
        'preparing',
    );
});

test('그룹 전적 카드는 재판 진행 상태를 확정 전적보다 우선한다', () => {
    const groupRecord = { participatingGroups: 1 };

    assert.equal(resolveGroupRecordState({ groupRecordState: 'JUDGING', groupRecord }), 'JUDGING');
    assert.equal(resolveGroupRecordState({ groupRecordState: 'READY', groupRecord }), 'READY');
    assert.equal(
        resolveGroupRecordState({ groupRecordState: 'EMPTY', groupRecord: null }),
        'EMPTY',
    );
    assert.equal(resolveGroupRecordState({ groupRecord: null }), 'EMPTY');
});
