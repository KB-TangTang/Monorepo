import test from 'node:test';
import assert from 'node:assert/strict';
import {
    formatChangeRate,
    getPreviousPeriod,
    isAvailableReportMonth,
    resolveSelectedReportPeriod,
    resolveReportState,
} from '../src/utils/monthlyConsumption.js';
import { AVAILABLE_MONTHS } from '../src/fixtures/monthlyConsumption.js';

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

test('유효한 요청 월을 유지하고 데이터 없는 월은 최신 리포트 월로 대체한다', () => {
    const referenceDate = new Date(2026, 7, 4);
    const months = [
        { value: '2026-07', hasReport: true },
        { value: '2026-06', hasReport: true },
        { value: '2026-05', hasReport: true },
        { value: '2026-04', hasReport: false },
        { value: '2026-03', hasReport: false },
        { value: '2026-08', hasReport: false },
    ];

    assert.equal(resolveSelectedReportPeriod(months, '2026-06', referenceDate), '2026-06');
    assert.equal(resolveSelectedReportPeriod(months, '2026-05', referenceDate), '2026-05');
    assert.equal(resolveSelectedReportPeriod(months, '2026-03', referenceDate), '2026-07');
});

test('데이터가 없는 1~4월은 월 선택에서 비활성화한다', () => {
    const referenceDate = new Date(2026, 7, 4);

    ['2026-01', '2026-02', '2026-03', '2026-04'].forEach((period) => {
        const month = AVAILABLE_MONTHS.find((item) => item.value === period);
        assert.equal(isAvailableReportMonth(month, referenceDate), false);
    });
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
