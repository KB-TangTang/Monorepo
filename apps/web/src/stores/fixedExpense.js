import { defineStore } from 'pinia';
import {
    confirmFixedExpenseCandidate,
    dismissFixedExpenseCandidate,
    fetchFixedExpenseCandidate,
    fetchFixedExpenseDetail,
    fetchFixedExpenseOverview,
    fetchFixedExpenseSavings,
} from '@/api/fixedExpense';
import {
    confirmTempFixedExpenseCandidate,
    dismissTempFixedExpenseCandidate,
    fetchTempFixedExpenseCandidate,
    fetchTempFixedExpenseDetail,
    fetchTempFixedExpenseOverview,
    fetchTempFixedExpenseSavings,
} from '@/api/tempFixedExpenseMock';

export const useFixedExpenseStore = defineStore('fixedExpense', {
    state: () => ({
        source: 'mock',
        savings: null,
        summary: null,
        confirmed: [],
        candidates: [],
        selectedExpense: null,
        selectedCandidate: null,
        loading: false,
        error: '',
        actionLoading: false,
    }),
    actions: {
        setSource(source) {
            if (!['api', 'mock'].includes(source) || source === this.source) {
                return;
            }
            this.source = source;
            this.resetLoadedData();
        },
        resetLoadedData() {
            this.savings = null;
            this.summary = null;
            this.confirmed = [];
            this.candidates = [];
            this.selectedExpense = null;
            this.selectedCandidate = null;
            this.error = '';
        },
        async runRequest(mockRequest, apiRequest, assign) {
            this.loading = true;
            this.error = '';
            try {
                const data = await (this.source === 'mock' ? mockRequest : apiRequest)();
                assign(data);
                return data;
            } catch (error) {
                this.error = error.message ?? '고정지출 정보를 불러오지 못했습니다.';
                return null;
            } finally {
                this.loading = false;
            }
        },
        async loadSavings() {
            return this.runRequest(
                fetchTempFixedExpenseSavings,
                fetchFixedExpenseSavings,
                (data) => {
                    this.savings = data;
                },
            );
        },
        async loadOverview() {
            return this.runRequest(
                fetchTempFixedExpenseOverview,
                fetchFixedExpenseOverview,
                (data) => {
                    this.summary = data.summary;
                    this.confirmed = data.confirmed;
                    this.candidates = data.candidates;
                },
            );
        },
        async loadExpense(expenseId) {
            this.selectedExpense = null;
            return this.runRequest(
                () => fetchTempFixedExpenseDetail(expenseId),
                () => fetchFixedExpenseDetail(expenseId),
                (data) => {
                    this.selectedExpense = data;
                },
            );
        },
        async loadCandidate(candidateId) {
            this.selectedCandidate = null;
            return this.runRequest(
                () => fetchTempFixedExpenseCandidate(candidateId),
                () => fetchFixedExpenseCandidate(candidateId),
                (data) => {
                    this.selectedCandidate = data;
                },
            );
        },
        async decideCandidate(candidateId, decision) {
            this.actionLoading = true;
            this.error = '';
            try {
                if (decision === 'confirm') {
                    await (this.source === 'mock'
                        ? confirmTempFixedExpenseCandidate(candidateId)
                        : confirmFixedExpenseCandidate(candidateId));
                } else {
                    await (this.source === 'mock'
                        ? dismissTempFixedExpenseCandidate(candidateId)
                        : dismissFixedExpenseCandidate(candidateId));
                }
                await this.loadOverview();
                return true;
            } catch (error) {
                this.error = error.message ?? '탐지 후보를 처리하지 못했습니다.';
                return false;
            } finally {
                this.actionLoading = false;
            }
        },
    },
});
