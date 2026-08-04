import test from 'node:test';
import assert from 'node:assert/strict';
import {
    formatChangeRate,
    getPreviousPeriod,
    isAvailableReportMonth,
    resolveReportState,
} from '../src/utils/monthlyConsumption.js';

test('기준일 이전의 리포트 보유 월만 조회 가능 월로 계산한다', () => {
    const referenceDate = new Date(2026, 7, 4);
    assert.equal(getPreviousPeriod(referenceDate), '2026-07');
    assert.equal(
        isAvailableReportMonth({ value: '2026-07', hasReport: true }, referenceDate),
        true,
    );
    assert.equal(
        isAvailableReportMonth({ value: '2026-08', hasReport: true }, referenceDate),
        false,
    );
    assert.equal(
        isAvailableReportMonth({ value: '2026-04', hasReport: false }, referenceDate),
        false,
    );
});

test('카테고리 증감과 화면 상태를 표시한다', () => {
    assert.equal(formatChangeRate(28), '▲28%');
    assert.equal(formatChangeRate(-12), '▼12%');
    assert.equal(formatChangeRate(0), '−0%');
    assert.equal(resolveReportState({ loading: true, error: '실패', report: {} }), 'loading');
    assert.equal(resolveReportState({ error: '실패', report: {} }), 'error');
    assert.equal(resolveReportState({ report: null }), 'empty');
    assert.equal(resolveReportState({ report: {} }), 'ready');
});
