import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync, readdirSync } from 'node:fs';

/*
 * 목숨 0 표시 회귀 방지 (이슈 #353).
 *
 * 그룹 상세의 목숨 바가 `ch.livesCount || ch.maxLives` 였다. 0 은 falsy 라
 * **목숨을 전부 잃은 사람에게 「7 / 7개」와 살아있는 판사봉 7개**가 그려졌다.
 * 0 은 이 화면에서 가장 중요한 상태(탈락)인데 정확히 그 상태만 정반대로 표시된 것이다.
 *
 * 서버는 참여자면 항상 정수를 내린다(`ChallengeGroupService:547`
 * `me == null ? null : me.getLivesCount()`). 즉 null 은 **비참여자 응답 전용**이라
 * 폴백은 `?? ch.maxLives` 로 남겨 둔다 — 의도가 바뀌지 않는다.
 *
 * 렌더링 하네스가 없어(`node:test` + 순수 JS) 소스를 검사한다.
 */
function source(path) {
    return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

const DETAIL = 'src/views/challenge/group/GroupChallengeDetailView.vue';
const LIVES_BAR = 'src/components/challenge/group/GroupDetailLivesBar.vue';

test('상세 목숨 바가 목숨을 truthy 로 검사하지 않는다 — 0 이 만땅으로 뒤집히지 않는다', () => {
    const src = source(DETAIL);
    assert.ok(
        !/livesCount\s*\|\|/.test(src),
        'livesCount 를 || 로 폴백한다 — 목숨 0 이 maxLives 로 대체된다',
    );
    assert.match(
        src,
        /:lives-count="ch\.livesCount \?\? ch\.maxLives"/,
        '목숨 바에 ?? 폴백이 없다',
    );
});

/*
 * 폴백을 없애 버리면 비참여자 응답(livesCount === null)에서 목숨 바가 NaN 을 그린다.
 * `??` 로 바꾸는 것과 폴백 자체를 지우는 것은 다르다.
 */
test('null 폴백은 유지한다 — 모집 중 upcoming 모드는 livesCount 를 보지 않는다', () => {
    const src = source(LIVES_BAR);
    assert.match(
        src,
        /const aliveCount = computed\(\(\) => isUpcoming\.value \? props\.maxLives : props\.livesCount\)/,
        'upcoming 모드가 maxLives 로 그리지 않는다 — 폴백의 전제가 깨졌다',
    );
    assert.match(
        src,
        /const depletedCount = computed\(\(\) => isUpcoming\.value \? 0 : props\.maxLives - props\.livesCount\)/,
        'active 모드의 차감 목숨 계산이 바뀌었다',
    );
});

/* active 모드는 받은 값을 그대로 쓴다 — 여기서 0 을 걸러내면 화면에서 다시 뒤집힌다. */
test('목숨 바가 라벨에서 목숨을 다시 폴백하지 않는다', () => {
    const src = source(LIVES_BAR);
    assert.match(
        src,
        /return `\$\{props\.livesCount\} \/ \$\{props\.maxLives\}개`/,
        'active 라벨이 livesCount 를 그대로 쓰지 않는다',
    );
    assert.ok(
        !/props\.livesCount\s*\|\|/.test(src),
        '목숨 바 안에서 livesCount 를 || 로 폴백한다',
    );
});

/*
 * 같은 실수가 다른 화면으로 번지지 않게 규칙 자체를 고정한다.
 * 2026-08-20 실측 — 수정 전에도 `livesCount ||` 는 저장소에 위 한 줄뿐이었고
 * 나머지는 전부 `?? 0` 이거나 원본을 그대로 쓴다.
 */
test('저장소 전체에서 목숨을 || 로 폴백하는 곳이 없다', () => {
    const root = new URL('../src/', import.meta.url);
    const offenders = readdirSync(root, { recursive: true, withFileTypes: true })
        .filter((e) => e.isFile() && /\.(vue|js)$/.test(e.name))
        .map((e) => `${e.parentPath ?? e.path}/${e.name}`)
        .filter((p) => /livesCount\s*\|\|/.test(readFileSync(p, 'utf8')));

    assert.deepEqual(
        offenders,
        [],
        `목숨을 || 로 폴백하는 파일이 있다 — 0 이 falsy 라 만땅으로 뒤집힌다:\n${offenders.join('\n')}`,
    );
});
