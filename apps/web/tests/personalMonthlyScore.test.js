import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

import {
    calculateRankingProgress,
    normalizeMonthlyScore,
} from '../src/services/missionMonthlyScore.js';

test('월간 점수 API 응답을 점수와 상위 백분율로 변환한다', () => {
    assert.deepEqual(normalizeMonthlyScore({ totalScore: 200, topPercent: 19 }), {
        score: 200,
        topPercent: 19,
    });
});

test('월간 랭킹 데이터가 없으면 상위 백분율을 null로 유지한다', () => {
    assert.deepEqual(normalizeMonthlyScore({ totalScore: 0, topPercent: null }), {
        score: 0,
        topPercent: null,
    });
});

test('상위 백분율을 전체 순위 프로그래스 값으로 변환한다', () => {
    assert.equal(calculateRankingProgress(100), 0);
    assert.equal(calculateRankingProgress(19), 82);
    assert.equal(calculateRankingProgress(10), 91);
    assert.equal(calculateRankingProgress(1), 100);
    assert.equal(calculateRankingProgress(null), 0);
});

test('월간 점수 카드는 상위 백분율과 기준선 없는 순위 프로그래스바를 표시한다', async () => {
    const componentSource = await readFile(
        new URL('../src/components/challenge/personal/PersonalScoreCard.vue', import.meta.url),
        'utf8',
    );

    assert.match(componentSource, /월간 상위 \$\{topPercent\}%/);
    assert.match(componentSource, /순위 집계 전/);
    assert.doesNotMatch(componentSource, /상위 10%까지/);
    assert.match(componentSource, /score-card__ranking-track/);
    assert.doesNotMatch(componentSource, /score-card__ranking-marker/);
    assert.match(componentSource, /requestAnimationFrame/);
    assert.match(componentSource, /prefers-reduced-motion: reduce/);
});
