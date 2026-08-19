import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

import { BACK_STATE_KEY, entryState, resolveBack } from '@/utils/groupChallengeNavigation';

/*
 * 그룹챌린지 상세·초대의 뒤로가기 (이슈 #303).
 *
 * 목록에서 상세로 들어가도 뒤로가기가 언제나 그룹챌린지 홈으로 갔다. 뒤로가기가 홈으로
 * 하드코딩돼 있어서다.
 *
 * **단순히 router.back() 으로 바꾸면 이슈 #172 를 되돌리는 것**이라 그게 이 테스트의 핵심이다.
 * 판결 플로우가 `replace` 로 상세에 돌아오므로, 그 경우 앞 항목은 방금 본 재판 화면이다.
 * 그래서 「push 로 들어왔다」는 표시가 있을 때만 back 을 쓴다.
 */

function source(path) {
    return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

const FALLBACK = 'groupChallenge';

/* ── 규칙 ────────────────────────────────────────────────────────── */

test('목록에서 push 로 들어왔으면 히스토리를 되돌린다', () => {
    const state = entryState('groupChallengeList');

    assert.deepEqual(resolveBack(state, FALLBACK), { type: 'back' });
});

test('홈에서 push 로 들어왔어도 되돌린다 — 스크롤 위치가 살아난다', () => {
    assert.deepEqual(resolveBack(entryState('groupChallenge'), FALLBACK), { type: 'back' });
});

/*
 * 이 케이스가 이 파일에서 가장 중요하다. 판결 플로우는 `replace` 로 상세에 돌아오므로
 * 진입 표시가 없다. 여기서 back 을 내주면 사용자가 방금 본 판결문·투표 화면으로 되돌아간다.
 */
test('판결 플로우가 replace 로 돌아온 경우에는 back 하지 않는다', () => {
    assert.deepEqual(resolveBack(null, FALLBACK), { type: 'route', name: FALLBACK });
    assert.deepEqual(resolveBack(undefined, FALLBACK), { type: 'route', name: FALLBACK });
});

test('vue-router 가 자기 값만 넣어 둔 state 도 진입 표시로 보지 않는다', () => {
    /* replace 로 들어온 항목에도 vue-router 는 position·current 같은 자기 키를 채워 둔다 */
    const routerOwnState = {
        back: '/group-challenges/1/vote',
        current: '/group-challenges/1',
        position: 3,
    };

    assert.deepEqual(resolveBack(routerOwnState, FALLBACK), { type: 'route', name: FALLBACK });
});

test('표시가 빈 문자열이면 진입로를 모르는 것으로 본다', () => {
    assert.deepEqual(resolveBack({ [BACK_STATE_KEY]: '' }, FALLBACK), {
        type: 'route',
        name: FALLBACK,
    });
});

test('폴백은 호출부가 정한다 — 화면마다 나갈 곳이 다르다', () => {
    assert.deepEqual(resolveBack(null, 'groupChallengeList'), {
        type: 'route',
        name: 'groupChallengeList',
    });
});

test('진입 표시는 다른 history.state 키와 섞이지 않게 접두가 붙어 있다', () => {
    /* 오버레이가 쓰는 ttOverlay 와 같은 규칙이다. 접두 없이 from 을 쓰면 충돌 위험이 있다 */
    assert.ok(BACK_STATE_KEY.startsWith('tt'), `접두가 없다: ${BACK_STATE_KEY}`);
    assert.deepEqual(entryState('groupChallengeList'), { [BACK_STATE_KEY]: 'groupChallengeList' });
});

/* ── 화면이 규칙을 실제로 쓰는지 ────────────────────────────────── */

const ENTERS_DETAIL = [
    ['그룹챌린지 홈', 'src/views/challenge/group/GroupChallengeHomeView.vue'],
    ['그룹챌린지 목록', 'src/views/challenge/group/GroupChallengeListView.vue'],
];

for (const [label, path] of ENTERS_DETAIL) {
    test(`${label} 은 상세로 push 할 때 진입 표시를 남긴다`, () => {
        const src = source(path);
        const call = src.match(/router\.push\(\{[^}]*groupChallengeDetail[\s\S]*?\}\);/);

        assert.ok(call, `${path}: 상세로 가는 push 를 찾지 못했다`);
        assert.match(
            call[0],
            /state:\s*entryState\(/,
            `${path}: 표시가 없으면 상세가 진입로를 알 수 없어 홈으로 나간다`,
        );
    });
}

const RESOLVES_BACK = [
    ['그룹챌린지 상세', 'src/views/challenge/group/GroupChallengeDetailView.vue'],
    ['그룹 초대', 'src/views/challenge/group/GroupInviteView.vue'],
];

for (const [label, path] of RESOLVES_BACK) {
    test(`${label} 의 뒤로가기는 목적지를 하드코딩하지 않는다`, () => {
        const src = source(path);
        const goBack = src.match(/function goBack\(\)\s*\{[\s\S]*?\n\}/);

        assert.ok(goBack, `${path}: goBack 을 찾지 못했다`);
        assert.match(goBack[0], /resolveBack\(/, `${path}: 진입 경로를 보지 않는다`);
        assert.match(goBack[0], /router\.back\(\)/, `${path}: 되돌아갈 수단이 없다`);
    });
}

/*
 * 이슈 #172 회귀 방지. 판결 플로우가 상세로 `push` 하기 시작하면 진입 표시가 없어도
 * 앞 항목이 재판 화면인 채로 스택이 쌓여, 규칙과 무관하게 증상이 되살아난다.
 * (플로우 화면들이 replace 를 쓰는지는 backNavigation.test.js 가 따로 지킨다)
 */
test('판결 플로우로 들어갈 때는 진입 표시를 남기지 않는다', () => {
    const FLOW_VIEWS = [
        'src/views/challenge/group/judgment/TrialProgressView.vue',
        'src/views/challenge/group/judgment/VerdictResultView.vue',
        'src/views/challenge/group/judgment/VerdictDetailView.vue',
        'src/views/challenge/group/judgment/GroupAiVerdictResultView.vue',
        'src/views/challenge/group/VoteDoneView.vue',
    ];

    for (const path of FLOW_VIEWS) {
        const src = source(path);
        assert.doesNotMatch(
            src,
            /state:\s*entryState\(/,
            `${path}: replace 로 돌아가면서 진입 표시를 남기면 back 이 재판 화면으로 되돌아간다`,
        );
    }
});
