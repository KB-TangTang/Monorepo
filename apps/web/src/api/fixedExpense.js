import { FIXED_EXPENSE_FIXTURE } from '@/fixtures/fixedExpense';
import { applyCandidateDecision } from '@/utils/fixedExpense';

function clone(value) {
    return JSON.parse(JSON.stringify(value));
}

function createMockState() {
    return clone(FIXED_EXPENSE_FIXTURE);
}

let mockState = createMockState();

function findById(items, id, message) {
    const item = items.find((entry) => entry.id === id);
    if (!item) {
        const error = new Error(message);
        error.code = 'NOT_FOUND';
        throw error;
    }
    return item;
}

export async function fetchFixedExpenseSavings() {
    return clone(mockState.savings);
}

export async function fetchFixedExpenseOverview() {
    return clone({
        summary: mockState.overview,
        confirmed: mockState.confirmed,
        candidates: mockState.candidates,
    });
}

export async function fetchFixedExpenseDetail(expenseId) {
    return clone(findById(mockState.confirmed, expenseId, '고정지출 정보를 찾을 수 없습니다.'));
}

export async function fetchFixedExpenseCandidate(candidateId) {
    return clone(findById(mockState.candidates, candidateId, '탐지 후보 정보를 찾을 수 없습니다.'));
}

export async function confirmFixedExpenseCandidate(candidateId) {
    const { state, result } = applyCandidateDecision(mockState, candidateId, 'confirm');
    mockState = state;
    return clone(result);
}

export async function dismissFixedExpenseCandidate(candidateId) {
    const { state, result } = applyCandidateDecision(mockState, candidateId, 'dismiss');
    mockState = state;
    return clone(result);
}

export function resetFixedExpenseMock() {
    mockState = createMockState();
}
