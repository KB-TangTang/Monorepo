import { EXPENSE_CATEGORIES, INCOME_CATEGORIES } from '../fixtures/category.js';

export const TONES = ['primary', 'accent', 'success', 'muted'];

export function chunkCategories(categories, size) {
    const chunks = [];
    for (let i = 0; i < categories.length; i += size) {
        chunks.push(categories.slice(i, i + size));
    }
    return chunks;
}

export function findExpenseParentByChildName(childName) {
    return EXPENSE_CATEGORIES.find((parent) =>
        parent.children.some((child) => child.name === childName),
    );
}

/* 대분류 자체를 저장값으로 쓸 수도 있어서(소분류를 안 고르고 대분류만 골라도 유효한 카테고리다),
 * 이름이 대분류 자신과 일치하는 경우까지 함께 찾는다. */
export function findExpenseParentByName(categoryName) {
    return (
        EXPENSE_CATEGORIES.find((parent) => parent.name === categoryName) ??
        findExpenseParentByChildName(categoryName)
    );
}

export function resolveCategoryDirection(amount) {
    return amount > 0 ? 'income' : 'expense';
}

export function resolveCategoryTone(categoryName) {
    const expenseParent = findExpenseParentByName(categoryName);
    if (expenseParent) {
        return TONES[EXPENSE_CATEGORIES.indexOf(expenseParent) % TONES.length];
    }
    const incomeIndex = INCOME_CATEGORIES.findIndex((item) => item.name === categoryName);
    if (incomeIndex !== -1) {
        return TONES[incomeIndex % TONES.length];
    }
    return 'muted';
}

export function resolveCategoryIcon(categoryName) {
    const expenseParent = findExpenseParentByName(categoryName);
    if (expenseParent) {
        return expenseParent.icon;
    }
    const incomeMatch = INCOME_CATEGORIES.find((item) => item.name === categoryName);
    return incomeMatch?.icon ?? 'EllipsisHorizontalCircle';
}

/** 카테고리 이름으로 숫자 id를 찾는다. 없으면 null. */
export function resolveCategoryId(categories, categoryName) {
    return categories.find((c) => c.name === categoryName)?.id ?? null;
}
