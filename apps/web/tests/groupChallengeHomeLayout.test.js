import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

/*
 * 지방법원 홈 레이아웃 위계 회귀 방지 (이슈 #448).
 *
 * 이 화면은 성격이 다른 세 덩어리를 세로로 쌓는다 — 마감이 걸린 **할 일 큐**(재판 현황),
 * 내가 속한 그룹 **명부**(내 챌린지), 그리고 **진입로**(만들기·참여코드).
 * 셋이 같은 몸집의 흰 카드로 나오면 화면 전체가 평평해져 「카드가 계속 나온다」로만 읽힌다.
 *
 * 렌더링 하네스가 없어(`node:test` + 순수 JS) 소스에 규칙이 들어 있는지를 검사한다.
 */

function source(path) {
    return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

const HOME = 'src/views/challenge/group/GroupChallengeHomeView.vue';
const TRIAL_CARD = 'src/components/challenge/group/GroupTrialStatusCard.vue';

test('홈은 내 챌린지를 상위 몇 건까지만 그린다', () => {
    /*
     * 전부 그리면 참여 그룹이 늘수록 이 섹션이 세로로 그대로 자라, 위의 재판 현황을 화면
     * 밖으로 밀어낸다 — 지금 할 일이 지금 할 일 아닌 것에 밀리는 구조다.
     * 나머지는 이미 있는 목록 화면(`groupChallengeList`)이 맡는다.
     */
    const src = source(HOME);
    assert.match(src, /const HOME_CHALLENGE_LIMIT = \d+;/);
    assert.match(src, /homeChallenges = computed\([\s\S]*?\.slice\(0, HOME_CHALLENGE_LIMIT\)/);
    assert.match(src, /v-for="ch in homeChallenges"/);
    /* 여기가 sortedChallenges 로 되돌아가면 자르기가 통째로 무효가 된다 */
    assert.doesNotMatch(src, /v-for="ch in sortedChallenges"/);
});

test('잘려 나간 건수를 전체보기 자리가 밝힌다', () => {
    /* 안 밝히면 홈이 「내 챌린지는 3개뿐」이라고 잘못 말한다 */
    const src = source(HOME);
    assert.match(src, /hiddenChallengeCount = computed\(/);
    assert.match(src, /hiddenChallengeCount \?/);
});

test('빈 상태 판정은 자른 목록이 아니라 전체로 본다', () => {
    /*
     * `!homeChallenges.length` 로 바꿔도 지금은 결과가 같지만, LIMIT 이 0 이 되거나
     * 필터가 하나 끼는 순간 챌린지가 있는데 「아직 없어요」가 뜬다.
     */
    assert.match(source(HOME), /v-if="!sortedChallenges\.length"/);
});

test('할 일 큐만 바닥에서 떠 있다', () => {
    /*
     * 위계를 만드는 건 그림자 하나가 아니라 **차이**다. 재판 현황에 그림자를 주면서
     * 아래 명부 행에도 같이 주면 아무것도 안 준 것과 같다.
     */
    assert.match(source(TRIAL_CARD), /\.trial-status \{[^}]*box-shadow: var\(--tt-elevation-2\)/);
    assert.doesNotMatch(source(HOME), /\.gc-group-row \{[^}]*box-shadow/);
});

test('명부 행은 할 일 카드보다 한 급 작다', () => {
    /*
     * 재판 현황 카드는 `--tt-radius-xl`(22px)에 아이콘이 없는 한 줄, 명부 행은 그보다
     * 작은 라운드에 아이콘을 단다. 여기가 같아지면 두 블록이 다시 같은 무게로 읽힌다.
     */
    const src = source(HOME);
    const radius = src.match(/\.gc-group-row \{[^}]*border-radius: (\d+)px/);
    assert.ok(radius, '.gc-group-row 에 border-radius 가 있어야 한다');
    assert.ok(
        Number(radius[1]) < 22,
        `명부 행 라운드(${radius[1]}px)가 카드(22px)보다 작아야 한다`,
    );
});

test('두 섹션 사이는 토큰 간격으로 벌린다', () => {
    /*
     * 10px 로는 두 덩어리가 한 스크롤 흐름으로 이어져 섹션 제목이 앞 카드에 붙어 보였다.
     * 임의 px 로 되돌아가면 다음 사람이 다시 눈대중으로 만진다.
     */
    assert.match(source(HOME), /\.gc-section \{[^}]*margin-top: var\(--tt-space-\d\)/);
});
