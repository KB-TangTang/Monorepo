import test from 'node:test';
import assert from 'node:assert/strict';
import { clampHomeProgress, formatHomeAmount, formatHomeRate } from '../src/utils/home.js';

test('홈 금액을 천 단위 구분자로 표시한다', () => {
    assert.equal(formatHomeAmount(12840000), '12,840,000');
    assert.equal(formatHomeAmount(null), '0');
});

test('홈 증감률에 방향 부호를 붙인다', () => {
    assert.equal(formatHomeRate(8.4), '+8.4%');
    assert.equal(formatHomeRate(-3.2), '-3.2%');
});

test('챌린지 진행률을 0에서 100 사이로 제한한다', () => {
    assert.equal(clampHomeProgress(-10), 0);
    assert.equal(clampHomeProgress(56), 56);
    assert.equal(clampHomeProgress(120), 100);
});
