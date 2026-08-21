import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

import {
    calculateRankingFill,
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

/* 게이지는 「내가 앞선 비율」을 채운다 — 상위 12% 면 88% 를 앞선 것이다 */
test('상위 백분율을 게이지 채움 비율로 뒤집는다', () => {
    assert.equal(calculateRankingFill(1), 99);
    assert.equal(calculateRankingFill(12), 88);
    assert.equal(calculateRankingFill(50), 50);
    assert.equal(calculateRankingFill(100), 0);
});

/* null 이라야 게이지를 아예 안 그린다. 0 을 돌려주면 순위 없는 사람이 꼴찌로 보인다. */
test('순위가 없거나 숫자가 아니면 null 을 돌려준다', () => {
    for (const value of [null, undefined, '', 'abc', NaN]) {
        assert.equal(calculateRankingFill(value), null, `${String(value)} 는 null 이어야 한다`);
    }
});

test('범위를 벗어난 값은 게이지 밖으로 나가지 않게 자른다', () => {
    assert.equal(calculateRankingFill(-5), 100);
    assert.equal(calculateRankingFill(140), 0);
});

function readComponent(name) {
    return readFile(
        new URL(`../src/components/challenge/personal/${name}.vue`, import.meta.url),
        'utf8',
    );
}

test('월간 점수 카드는 상위 백분율과 순위 게이지를 표시한다', async () => {
    const componentSource = await readComponent('PersonalScoreCard');

    assert.match(componentSource, /상위 \$\{topPercent\}%/);
    assert.match(componentSource, /순위 집계 전/);
    assert.doesNotMatch(componentSource, /상위 10%까지/);
    assert.match(componentSource, /score-card__gauge-fill/);
    /* 방향 라벨이 없으면 게이지를 거꾸로 읽는다 */
    assert.match(componentSource, /score-card__gauge-cap">상위</);
    assert.match(componentSource, /score-card__gauge-cap">하위</);
    /* 순위가 없을 때 점선을 그으면 없는 위치를 지어낸다 */
    assert.match(componentSource, /v-if="rankingFill !== null"[\s\S]*?score-card__gauge-mark/);
    /* 가로 프로그래스바로 되돌아가면 백분위를 「쌓이는 값」으로 다시 오해하게 된다 */
    assert.doesNotMatch(componentSource, /score-card__ranking-(track|fill|marker)/);
    assert.match(componentSource, /requestAnimationFrame/);
    assert.match(componentSource, /prefers-reduced-motion: reduce/);
});

/*
 * 주간(이번 주 판정)과 월간(이번 달 누적)은 보는 주기가 달라 카드를 나눴다.
 * 한 카드로 되돌아가면 명예의 전당과 나란히 세우는 반쪽 폭 배치가 깨진다.
 */
test('주간 판정과 월간 누적은 서로 다른 카드가 담는다', async () => {
    const weeklySource = await readComponent('PersonalWeeklyVerdictCard');
    const scoreSource = await readComponent('PersonalScoreCard');

    assert.match(weeklySource, /연속 성공 \{\{ streakDays \}\}일째/);
    assert.doesNotMatch(weeklySource, /topPercent/);
    assert.match(scoreSource, /이번 달 누적/);
    assert.doesNotMatch(scoreSource, /streakDays/);
});

/* 인정은 앱 공통 성공색, 기각은 연한 빨강. 파랑 그라데이션 시안으로 되돌아가지 않게 못박는다. */
test('주간 판정 인정 원은 성공 토큰으로 채운다', async () => {
    const componentSource = await readComponent('PersonalWeeklyVerdictCard');

    assert.match(componentSource, /--success\s*\{[^}]*background:\s*var\(--tt-success\)/);
    assert.match(componentSource, /--failed\s*\{[^}]*background:\s*var\(--tt-danger-subtle\)/);
    assert.doesNotMatch(componentSource, /gradient/);
});

/*
 * 반쪽 폭 자리에서는 탕이를 글 위로 올려 세로로 쌓는다.
 * 가로 배너를 그대로 좁히면 탕이가 글을 밀어내 제목이 접힌다.
 */
test('명예의 전당 배너는 반쪽 폭 변형을 갖는다', async () => {
    const componentSource = await readComponent('PersonalMissionHonorBanner');

    assert.match(componentSource, /compact: \{ type: Boolean/);
    assert.match(componentSource, /honor-banner--compact['"]?: compact/);
    assert.match(componentSource, /\.honor-banner--compact\s*\{[^}]*flex-direction:\s*column/);
    /* 절대배치를 풀지 않으면 탕이가 카드 오른쪽 아래에 그대로 얹혀 있다 */
    assert.match(
        componentSource,
        /\.honor-banner--compact \.honor-banner__image\s*\{[^}]*position:\s*static/,
    );
});
