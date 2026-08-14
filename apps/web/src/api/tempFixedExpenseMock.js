// TEMP(#202): #204·#205 고정지출 API 연동이 완료되면 이 파일과 데이터 소스 전환 코드를 함께 삭제한다.
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

export async function fetchTempFixedExpenseSavings() {
    return clone(mockState.savings);
}

export async function fetchTempFixedExpenseOverview() {
    return clone({
        summary: mockState.overview,
        confirmed: mockState.confirmed,
        candidates: mockState.candidates,
    });
}

export async function fetchTempFixedExpenseDetail(expenseId) {
    return clone(findById(mockState.confirmed, expenseId, '고정지출 정보를 찾을 수 없습니다.'));
}

export async function fetchTempFixedExpenseCandidate(candidateId) {
    return clone(findById(mockState.candidates, candidateId, '탐지 후보 정보를 찾을 수 없습니다.'));
}

export async function confirmTempFixedExpenseCandidate(candidateId) {
    const { state, result } = applyCandidateDecision(mockState, candidateId, 'confirm');
    mockState = state;
    return clone(result);
}

export async function dismissTempFixedExpenseCandidate(candidateId) {
    const { state, result } = applyCandidateDecision(mockState, candidateId, 'dismiss');
    mockState = state;
    return clone(result);
}

export function resetTempFixedExpenseMock() {
    mockState = createMockState();
}
