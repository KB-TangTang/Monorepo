import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

/*
 * 목숨 0 표시 회귀 방지 (이슈 #353).
 *
 * 그룹 상세의 목숨 바가 `ch.livesCount || ch.maxLives` 로 값을 넘기고 있었다.
 * **0 은 falsy 라 maxLives 로 대체된다** — 목숨을 전부 잃은(ELIMINATED) 사람에게
 * 「7 / 7개」와 살아있는 판사봉 7개가 그려졌다. 0 은 이 화면에서 가장 중요한 상태인데
 * 정확히 그 상태만 정반대로 표시되던 셈이다.
 *
 * 같은 화면의 랭킹표(GroupDetailRankingTable)와 홈 카드는 처음부터 `?? 0` 을 쓰고 있어
 * 한 화면 안에서 말이 갈렸다.
 *
 * 렌더링 하네스가 없어(`node:test` + 순수 JS) 소스를 검사한다.
 */
function source(path) {
    return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

const DETAIL = 'src/views/challenge/group/GroupChallengeDetailView.vue';
const LIVES_BAR = 'src/components/challenge/group/GroupDetailLivesBar.vue';

test('상세 목숨 바가 livesCount 를 truthy 로 검사하지 않는다 — 0 이 만땅으로 뒤집히지 않는다', () => {
    const src = source(DETAIL);
    assert.ok(
        !/:lives-count="[^"]*\|\|/.test(src),
        'lives-count 를 `||` 로 넘긴다 — 목숨 0 이 maxLives 로 대체된다',
    );
    assert.match(
        src,
        /:lives-count="ch\.livesCount \?\? ch\.maxLives"/,
        'lives-count 가 `??` 로 넘어가지 않는다',
    );
});

/* fallback 을 통째로 지우면 안 된다. 비참여자 응답에서 livesCount 는 null 이다. */
test('null fallback 은 남아 있다', () => {
    const src = source(DETAIL);
    assert.ok(
        src.includes('ch.livesCount ?? ch.maxLives'),
        'null 일 때 쓸 값이 없다 — 목숨 바가 NaN 을 그린다',
    );
});

/* 0 을 넘겨도 컴포넌트가 그것을 그대로 쓰는지 (여기서도 truthy 검사가 있으면 소용없다). */
test('목숨 바 컴포넌트가 livesCount 를 그대로 쓴다', () => {
    const src = source(LIVES_BAR);
    assert.ok(
        !/props\.livesCount \|\|/.test(src),
        '컴포넌트 안에서 livesCount 를 truthy 로 검사한다',
    );
    assert.match(
        src,
        /aliveCount = computed\(\(\) => isUpcoming\.value \? props\.maxLives : props\.livesCount\)/,
        '남은 목숨 개수 계산이 바뀌었다 — 이 테스트를 함께 손봐야 한다',
    );
});
