import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

/*
 * 그룹챌린지 생성 진입로 회귀 방지 (이슈 #172).
 *
 * `groupChallengeCreate` 라우트(`/group-challenges/create`)는 있는데 홈·목록 어디에도
 * 버튼이 없어 **주소를 직접 쳐야만** 만들기 화면에 갈 수 있었다. 참여코드 진입로만 있어
 * 「이미 만들어진 그룹에 합류」는 되고 「새로 만들기」는 안 되는 상태였다.
 *
 * 렌더링 하네스가 없어(`node:test` + 순수 JS) 소스에 진입로가 들어 있는지를 검사한다.
 */
function source(path) {
    return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

const CREATE_ENTRIES = [
    ['그룹챌린지 홈', 'src/views/challenge/group/GroupChallengeHomeView.vue'],
    ['그룹챌린지 목록', 'src/views/challenge/group/GroupChallengeListView.vue'],
];

for (const [label, path] of CREATE_ENTRIES) {
    test(`${label} 에 그룹챌린지 생성 진입로가 있다`, () => {
        const src = source(path);

        assert.match(
            src,
            /function goToCreate\(\) \{\s*router\.push\(\{ name: 'groupChallengeCreate' \}\);\s*\}/,
            `${path}: groupChallengeCreate 로 가는 함수가 없다`,
        );
        assert.ok(
            src.includes('@click="goToCreate"'),
            `${path}: 생성 함수를 부르는 버튼이 템플릿에 없다`,
        );
    });
}

test('생성 진입로는 참여코드 진입로와 따로 있다 — 하나가 다른 하나를 대체하지 않는다', () => {
    /*
     * 「만들기」와 「참여코드로 합류」는 서로 다른 사용자다. 한쪽만 두면 나머지 절반이
     * 막힌다 — 실제로 이 이슈 전까지 만들기 쪽이 막혀 있었다.
     */
    for (const [label, path] of CREATE_ENTRIES) {
        const src = source(path);
        assert.ok(src.includes('showJoinSheet = true'), `${label}: 참여코드 진입로가 사라졌다`);
    }
});
