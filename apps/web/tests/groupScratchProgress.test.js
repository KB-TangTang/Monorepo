import assert from 'node:assert/strict';
import test from 'node:test';
import {
    calculateScratchProgress,
    SCRATCH_COMPLETION_RATIO,
} from '../src/views/challenge/group/groupScratchProgress.js';

test('스크래치 표본이 없으면 진행률은 0이다', () => {
    assert.equal(calculateScratchProgress(0, 0), 0);
});

test('완료 기준까지의 투명 비율을 백분율로 변환한다', () => {
    const totalSamples = 1000;
    const halfwaySamples = totalSamples * SCRATCH_COMPLETION_RATIO * 0.5;

    assert.equal(calculateScratchProgress(halfwaySamples, totalSamples), 50);
});

test('완료 기준 이상 긁으면 진행률은 100을 넘지 않는다', () => {
    assert.equal(calculateScratchProgress(500, 1000), 100);
});
