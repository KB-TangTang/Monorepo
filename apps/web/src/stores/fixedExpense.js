import { defineStore } from 'pinia';
import {
    confirmFixedExpenseCandidate,
    dismissFixedExpenseCandidate,
    fetchFixedExpenseCandidate,
    fetchFixedExpenseDetail,
    fetchFixedExpenseOverview,
    fetchFixedExpenseSavings,
} from '@/api/fixedExpense';

export const useFixedExpenseStore = defineStore('fixedExpense', {
    state: () => ({
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
        async runRequest(request, assign) {
            this.loading = true;
            this.error = '';
            try {
                const data = await request();
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
            return this.runRequest(fetchFixedExpenseSavings, (data) => {
                this.savings = data;
            });
        },
        async loadOverview() {
            return this.runRequest(fetchFixedExpenseOverview, (data) => {
                this.summary = data.summary;
                this.confirmed = data.confirmed;
                this.candidates = data.candidates;
            });
        },
        async loadExpense(expenseId) {
            this.selectedExpense = null;
            return this.runRequest(
                () => fetchFixedExpenseDetail(expenseId),
                (data) => {
                    this.selectedExpense = data;
                },
            );
        },
        async loadCandidate(candidateId) {
            this.selectedCandidate = null;
            return this.runRequest(
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
                    await confirmFixedExpenseCandidate(candidateId);
                } else {
                    await dismissFixedExpenseCandidate(candidateId);
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
