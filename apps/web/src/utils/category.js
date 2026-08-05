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

export function resolveCategoryDirection(amount) {
    return amount > 0 ? 'income' : 'expense';
}

export function resolveCategoryTone(categoryName) {
    const expenseParent = findExpenseParentByChildName(categoryName);
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
    const expenseParent = findExpenseParentByChildName(categoryName);
    if (expenseParent) {
        return expenseParent.icon;
    }
    const incomeMatch = INCOME_CATEGORIES.find((item) => item.name === categoryName);
    return incomeMatch?.icon ?? 'EllipsisHorizontalCircle';
}
