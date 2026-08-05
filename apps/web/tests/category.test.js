import { test } from 'node:test';
import assert from 'node:assert/strict';
import { EXPENSE_CATEGORIES, INCOME_CATEGORIES } from '../src/fixtures/category.js';

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
