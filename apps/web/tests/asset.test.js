import test from 'node:test';
import assert from 'node:assert/strict';
import {
    formatWon,
    formatSignedWon,
    formatCompactWon,
    formatAssetHomeWon,
    getCompositionTotal,
    getCompositionRatios,
    getSparklinePoints,
    getBarHeights,
    getSignedPercent,
} from '../src/utils/asset.js';

test('formatWon 은 금액 뒤에 "원"을 붙인다', () => {
    assert.equal(formatWon(12846000), '12,846,000원');
    assert.equal(formatWon(-1500000), '-1,500,000원');
    assert.equal(formatWon(0), '0원');
});

test('formatSignedWon 은 증감 방향에 따라 화살표를 붙인다', () => {
    assert.equal(formatSignedWon(320000), '▲ 320,000원');
    assert.equal(formatSignedWon(-50000), '▼ 50,000원');
});

test('formatCompactWon 은 1만/1억 경계에서 단위를 바꾼다', () => {
    assert.equal(formatCompactWon(9999), '9,999원');
    assert.equal(formatCompactWon(13146000), '1,315만원');
    assert.equal(formatCompactWon(150000000), '1.5억원');
    assert.equal(formatCompactWon(-20000), '-2만원');
});

test('formatCompactWon 은 반올림으로 1억을 넘는 값도 억 단위로 표기한다', () => {
    assert.equal(formatCompactWon(99995000), '1.0억원');
    assert.equal(formatCompactWon(10000), '1만원');
    assert.equal(formatCompactWon(100000000), '1.0억원');
});

test('formatAssetHomeWon 은 천만원 이상 금액을 만원/억 단위로 줄인다', () => {
    assert.equal(formatAssetHomeWon(9999999), '9,999,999원');
    assert.equal(formatAssetHomeWon(10000000), '1000만원');
    assert.equal(formatAssetHomeWon(21320000000), '213억2000만원');
    assert.equal(formatAssetHomeWon(-150000000), '-1억5000만원');
});

test('getCompositionTotal 은 amount 합계를 반환한다', () => {
    const composition = [
        { code: 'a', amount: 100 },
        { code: 'b', amount: 200 },
    ];
    assert.equal(getCompositionTotal(composition), 300);
});

test('getCompositionRatios 는 비율 합이 1이고 누적 오프셋이 순서대로 쌓인다', () => {
    const composition = [
        { code: 'a', label: 'A', amount: 100, tone: 'navy' },
        { code: 'b', label: 'B', amount: 300, tone: 'blue' },
    ];
    const result = getCompositionRatios(composition);
    assert.equal(result[0].ratio, 0.25);
    assert.equal(result[0].offset, 0);
    assert.equal(result[1].ratio, 0.75);
    assert.equal(result[1].offset, 0.25);
    assert.equal(
        result.reduce((sum, item) => sum + item.ratio, 0),
        1,
    );
});

test('getCompositionRatios 는 합계가 0이면 비율도 0이다 (0으로 나누지 않음)', () => {
    const composition = [{ code: 'a', label: 'A', amount: 0, tone: 'navy' }];
    const result = getCompositionRatios(composition);
    assert.equal(result[0].ratio, 0);
});

test('getSparklinePoints 는 min/max 로 정규화한 좌표 문자열을 만든다', () => {
    const { pointsAttr, lastPoint } = getSparklinePoints([0, 5, 10], 20, 10);
    assert.equal(pointsAttr, '0,10 10,5 20,0');
    assert.deepEqual(lastPoint, { x: 20, y: 0 });
});

test('getSparklinePoints 는 모든 값이 같아도 0으로 나누지 않는다', () => {
    const { pointsAttr, lastPoint } = getSparklinePoints([5, 5, 5], 20, 10);
    assert.equal(pointsAttr, '0,10 10,10 20,10');
    assert.deepEqual(lastPoint, { x: 20, y: 10 });
});

test('getSparklinePoints 는 빈 배열에 안전하다', () => {
    const { pointsAttr, lastPoint } = getSparklinePoints([], 20, 10);
    assert.equal(pointsAttr, '');
    assert.equal(lastPoint, null);
});

test('getBarHeights 는 총자산이 가장 큰 달을 기준으로 높이를 정규화한다', () => {
    const result = getBarHeights([100, 200], [50, 0], 160);
    assert.deepEqual(result, [
        { netWorthHeight: 80, debtHeight: 40 },
        { netWorthHeight: 160, debtHeight: 0 },
    ]);
});

test('getBarHeights 는 총자산이 모두 0이어도 0으로 나누지 않는다', () => {
    const result = getBarHeights([0, 0], [0, 0], 160);
    assert.deepEqual(result, [
        { netWorthHeight: 0, debtHeight: 0 },
        { netWorthHeight: 0, debtHeight: 0 },
    ]);
});

test('getSignedPercent 는 증감률을 부호와 함께 포맷한다', () => {
    assert.equal(getSignedPercent(12200000, 12846000), '+5.3%');
    assert.equal(getSignedPercent(100, 50), '-50.0%');
});

test('getSignedPercent 는 from 이 0이면 안전한 기본값을 반환한다', () => {
    assert.equal(getSignedPercent(0, 500), '+0.0%');
});
