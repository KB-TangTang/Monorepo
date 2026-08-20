import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { GROUP_CATEGORY_NAMES, toGroupCategorySections } from '../src/utils/groupCategory.js';
import { EXPENSE_CATEGORIES } from '../src/fixtures/category.js';

/*
 * 그룹 챌린지 방장 삭제 · 대상 카테고리 개편 (이슈 #352).
 *
 * 렌더링 하네스가 없어(`node:test` + 순수 JS) 화면 쪽은 소스 검사로 회귀만 막는다.
 * 순수 함수(`utils/groupCategory`)는 그대로 돌려 본다.
 */
function source(path) {
    return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

/** `GET /api/categories` 응답 흉내. seed_category.sql 의 이름·구조를 그대로 쓴다. */
function serverCategories() {
    const rows = [];
    let id = 1;
    for (const parent of EXPENSE_CATEGORIES) {
        const parentId = id++;
        rows.push({ id: parentId, name: parent.name, parentId: null });
        for (const child of parent.children) {
            rows.push({ id: id++, name: child.name, parentId });
        }
    }
    return rows;
}

/* ── 카테고리 화이트리스트 ───────────────────────────── */

test('고를 수 있는 카테고리는 15개다', () => {
    assert.equal(GROUP_CATEGORY_NAMES.length, 15);
});

/*
 * 이 테스트가 이 이슈의 핵심이다. 대분류 id 를 저장하면 거래(소분류 id)와 한 건도 매칭되지
 * 않아 집계가 늘 0원이 된다 — 화면이 대분류 id 1~5 를 박아 두고 있어서 실제로 그랬다.
 */
test('화이트리스트는 전부 소분류다 — 대분류가 섞이면 집계가 0원이 된다', () => {
    const parentNames = new Set(EXPENSE_CATEGORIES.map((parent) => parent.name));
    for (const name of GROUP_CATEGORY_NAMES) {
        assert.ok(!parentNames.has(name), `${name} 은 대분류다`);
    }
});

test('화이트리스트의 이름은 전부 실제 소분류에 있다', () => {
    const childNames = new Set(EXPENSE_CATEGORIES.flatMap((p) => p.children.map((c) => c.name)));
    for (const name of GROUP_CATEGORY_NAMES) {
        assert.ok(childNames.has(name), `${name} 은 소분류 목록에 없다`);
    }
});

test('화이트리스트에 중복이 없다', () => {
    assert.equal(new Set(GROUP_CATEGORY_NAMES).size, GROUP_CATEGORY_NAMES.length);
});

/* ── 대분류 섹션 묶기 ────────────────────────────────── */

test('toGroupCategorySections 는 대분류 5개 섹션으로 묶는다', () => {
    const sections = toGroupCategorySections(serverCategories());
    assert.deepEqual(
        sections.map((s) => s.parentName),
        ['식비', '쇼핑', '교통', '생활', '문화/여가'],
    );
});

test('섹션에 담긴 소분류 총합은 화이트리스트 개수와 같다', () => {
    const sections = toGroupCategorySections(serverCategories());
    const total = sections.reduce((sum, s) => sum + s.items.length, 0);
    assert.equal(total, GROUP_CATEGORY_NAMES.length);
});

/* id 를 박지 않는 이유. AUTO_INCREMENT 라 환경마다 값이 다를 수 있다. */
test('id 는 서버 응답에서 가져온다 — 화면에 박지 않는다', () => {
    const rows = serverCategories();
    const sections = toGroupCategorySections(rows);
    const picked = sections[0].items[0];
    const expected = rows.find((r) => r.name === picked.name && r.parentId != null);
    assert.equal(picked.id, expected.id);

    assert.ok(
        !/\bid:\s*[1-9]/.test(source('src/utils/groupCategory.js')),
        'groupCategory.js 에 숫자 id 가 박혀 있다',
    );
});

test('서버에 없는 이름은 조용히 건너뛴다 — 시드가 없어도 화면이 죽지 않는다', () => {
    const sections = toGroupCategorySections([
        { id: 7, name: '식비', parentId: null },
        { id: 8, name: '카페/간식', parentId: 7 },
    ]);
    assert.deepEqual(sections, [
        { parentId: 7, parentName: '식비', items: [{ id: 8, name: '카페/간식' }] },
    ]);
});

test('빈 응답이어도 터지지 않는다', () => {
    assert.deepEqual(toGroupCategorySections([]), []);
    assert.deepEqual(toGroupCategorySections(), []);
});

/* ── 만들기 화면 회귀 ────────────────────────────────── */

test('만들기 대상 단계에 하드코딩된 카테고리 목록이 남아 있지 않다', () => {
    const src = source('src/components/challenge/group/GroupCreateStepScope.vue');
    assert.ok(src.includes('fetchCategories'), '서버 목록을 부르지 않는다');
    assert.ok(src.includes('toGroupCategorySections'), '대분류로 묶지 않는다');
    assert.ok(!/\{ id: [0-9]+, label:/.test(src), '하드코딩된 카테고리 목록이 남아 있다');
});

test('최종 확인 화면은 id→이름 표를 갖지 않는다 — 대상 선택 단계가 넘긴 이름을 쓴다', () => {
    const src = source('src/components/challenge/group/GroupCreateStepConfirm.vue');
    assert.ok(!src.includes('CATEGORY_MAP'), 'id→이름 표가 남아 있다');
    assert.ok(src.includes('props.form.categoryName'), 'form 의 카테고리 이름을 쓰지 않는다');
});

test('선택한 이름이 form 까지 이어진다 — 끊기면 확인 화면이 늘 「총 소비」가 된다', () => {
    const scope = source('src/components/challenge/group/GroupCreateStepScope.vue');
    const view = source('src/views/challenge/group/GroupCreateView.vue');
    assert.ok(scope.includes("'update:category-name'"), '대상 단계가 이름을 올려보내지 않는다');
    assert.ok(
        view.includes('@update:category-name="form.categoryName = $event"'),
        '만들기 화면이 이름을 받지 않는다',
    );
});

/* ── 방장 삭제 ───────────────────────────────────────── */

test('삭제 API 는 목데이터 분기를 갖는다 — 목 모드에서 실서버로 새지 않는다', () => {
    const src = source('src/api/groupChallenge.js');
    assert.match(
        src,
        /export async function deleteGroupChallenge\(groupId\) \{\s*if \(isMockMode\.value\)/,
        'deleteGroupChallenge 에 목데이터 분기가 없다',
    );
    assert.ok(
        src.includes('http.delete(`/group-challenges/${groupId}`)'),
        'DELETE /group-challenges/{id} 를 부르지 않는다',
    );
});

/*
 * 서버도 같은 조건을 다시 본다. 여기 조건은 방어선이 아니라 편의지만, 빠지면 참여자에게
 * 눌러도 실패하는 버튼이 보인다.
 */
test('삭제 버튼은 방장이면서 모집 중일 때만 보인다', () => {
    const src = source('src/views/challenge/group/GroupChallengeDetailView.vue');
    assert.match(
        src,
        /const canDelete = computed\(\(\) => Boolean\(ch\.value\?\.isOwner\) && isRecruiting\.value\)/,
        'canDelete 가 방장·모집중 두 조건을 함께 보지 않는다',
    );
    assert.ok(src.includes('v-if="canDelete"'), '삭제 버튼에 노출 조건이 없다');
});

test('삭제는 확인 모달을 거친다 — 오버레이를 직접 만들지 않고 BaseModal 을 쓴다', () => {
    const src = source('src/views/challenge/group/GroupChallengeDetailView.vue');
    assert.ok(src.includes('BaseModal'), 'BaseModal 을 쓰지 않는다');
    assert.ok(src.includes('@click="openDeleteModal"'), '버튼이 모달을 열지 않는다');
    assert.ok(src.includes('되돌릴 수 없어요'), '되돌릴 수 없다는 안내가 없다');
    assert.ok(src.includes('채팅도 함께 사라져요'), '채팅이 사라진다는 안내가 없다');
});

test('삭제 실패 사유를 코드별 문구로 바꾼다', () => {
    const src = source('src/views/challenge/group/GroupChallengeDetailView.vue');
    for (const code of ['GROUP_NOT_OWNER', 'GROUP_NOT_DELETABLE', 'GROUP_NOT_FOUND']) {
        assert.ok(src.includes(code), `${code} 문구가 없다`);
    }
});

/* 삭제한 그룹의 상세가 히스토리에 남으면 뒤로가기가 없는 방으로 되돌아간다. */
test('삭제 성공 후 목록으로 replace 한다 — push 하지 않는다', () => {
    const src = source('src/views/challenge/group/GroupChallengeDetailView.vue');
    assert.match(
        src,
        /await deleteGroupChallenge\(ch\.value\.id\);[\s\S]{0,400}?router\.replace\(\{ name: 'groupChallengeList' \}\)/,
        '삭제 후 목록으로 replace 하지 않는다',
    );
});
