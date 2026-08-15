import { test } from 'node:test';
import assert from 'node:assert/strict';
import { EXPENSE_CATEGORIES, INCOME_CATEGORIES } from '../src/fixtures/category.js';
import {
    TONES,
    chunkCategories,
    findExpenseParentByChildName,
    findExpenseParentByName,
    resolveCategoryDirection,
    resolveCategoryIcon,
    resolveCategoryId,
    resolveCategoryTone,
} from '../src/utils/category.js';

test('지출 카테고리는 대분류 12개를 갖는다', () => {
    assert.equal(EXPENSE_CATEGORIES.length, 12);
});

test('지출 카테고리는 소분류 46개를 갖는다', () => {
    const total = EXPENSE_CATEGORIES.reduce((sum, parent) => sum + parent.children.length, 0);
    assert.equal(total, 46);
});

test('지출 카테고리 id 는 대분류·소분류 통틀어 중복이 없다', () => {
    const ids = EXPENSE_CATEGORIES.flatMap((parent) => [
        parent.id,
        ...parent.children.map((child) => child.id),
    ]);
    assert.equal(new Set(ids).size, ids.length);
});

test('수입 카테고리는 7개를 갖는다', () => {
    assert.equal(INCOME_CATEGORIES.length, 7);
});

test('수입 카테고리 id 는 중복이 없다', () => {
    const ids = INCOME_CATEGORIES.map((item) => item.id);
    assert.equal(new Set(ids).size, ids.length);
});

test('chunkCategories 는 지정한 크기로 배열을 나눈다', () => {
    const result = chunkCategories([1, 2, 3, 4, 5], 4);
    assert.deepEqual(result, [[1, 2, 3, 4], [5]]);
});

test('chunkCategories 는 12개를 4개씩 3행으로 나눈다', () => {
    assert.equal(chunkCategories(EXPENSE_CATEGORIES, 4).length, 3);
});

test('findExpenseParentByChildName 은 소분류 이름으로 대분류를 찾는다', () => {
    const parent = findExpenseParentByChildName('카페/간식');
    assert.equal(parent.id, 'food');
});

test('findExpenseParentByChildName 은 없는 이름이면 undefined 를 반환한다', () => {
    assert.equal(findExpenseParentByChildName('존재하지않음'), undefined);
});

test('resolveCategoryDirection 은 금액 부호로 방향을 판단한다', () => {
    assert.equal(resolveCategoryDirection(3350000), 'income');
    assert.equal(resolveCategoryDirection(-6800), 'expense');
});

test('resolveCategoryDirection 은 0원을 지출로 취급한다', () => {
    assert.equal(resolveCategoryDirection(0), 'expense');
});

test('findExpenseParentByName 은 대분류 이름 자체로도 대분류를 찾는다', () => {
    const parent = findExpenseParentByName('쇼핑');
    assert.equal(parent.id, 'shopping');
});

test('findExpenseParentByName 은 소분류 이름으로도 대분류를 찾는다', () => {
    const parent = findExpenseParentByName('카페/간식');
    assert.equal(parent.id, 'food');
});

test('resolveCategoryTone 은 지출 소분류의 대분류 인덱스로 톤을 정한다', () => {
    assert.equal(resolveCategoryTone('음식점/외식'), TONES[0]);
    assert.equal(resolveCategoryTone('온라인쇼핑'), TONES[1]);
});

test('resolveCategoryTone 은 대분류 이름 자체도 해당 대분류 톤으로 처리한다', () => {
    assert.equal(resolveCategoryTone('쇼핑'), TONES[1]);
});

test('resolveCategoryTone 은 수입 카테고리 인덱스로 톤을 정한다', () => {
    assert.equal(resolveCategoryTone('급여'), TONES[0]);
    assert.equal(resolveCategoryTone('상여금'), TONES[1]);
});

test('resolveCategoryTone 은 못 찾으면 muted 를 반환한다', () => {
    assert.equal(resolveCategoryTone('존재하지않음'), 'muted');
});

test('resolveCategoryIcon 은 지출 소분류의 대분류 아이콘을 반환한다', () => {
    assert.equal(resolveCategoryIcon('택시/모빌리티'), 'Truck');
});

test('resolveCategoryIcon 은 대분류 이름 자체도 해당 대분류 아이콘으로 처리한다', () => {
    assert.equal(resolveCategoryIcon('쇼핑'), 'ShoppingBag');
});

test('resolveCategoryIcon 은 수입 카테고리 아이콘을 반환한다', () => {
    assert.equal(resolveCategoryIcon('환급/캐시백'), 'ReceiptRefund');
});

test('resolveCategoryIcon 은 못 찾으면 기타 아이콘으로 폴백한다', () => {
    assert.equal(resolveCategoryIcon('존재하지않음'), 'EllipsisHorizontalCircle');
});

test('resolveCategoryId: 이름으로 id를 찾는다', () => {
    const categories = [
        { id: 1, name: '식비', parentId: null },
        { id: 2, name: '음식점/외식', parentId: 1 },
    ];
    assert.equal(resolveCategoryId(categories, '음식점/외식'), 2);
});

test('resolveCategoryId: 없는 이름이면 null이다', () => {
    assert.equal(resolveCategoryId([], '없는카테고리'), null);
});
