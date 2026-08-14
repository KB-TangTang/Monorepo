// TEMP(#154): 월간 리포트 백엔드 전체 개발 완료 후 이 파일과 화면의 TEMP 연결 코드를 함께 삭제한다.
import { AVAILABLE_MONTHS, REPORTS } from '@/fixtures/monthlyConsumption';
import { MONTHLY_REPORT_STATUS } from '@/utils/monthlyConsumption';

export async function fetchTempMonthlyConsumptionMonths() {
    return AVAILABLE_MONTHS.map((month) => ({
        ...month,
        available: month.status === MONTHLY_REPORT_STATUS.ONBOARDING || month.hasReport,
        status:
            month.status ??
            (month.hasReport ? MONTHLY_REPORT_STATUS.READY : MONTHLY_REPORT_STATUS.CURRENT),
    }));
}

export async function fetchTempMonthlyConsumptionReport(period) {
    const source = REPORTS[period];
    if (!source) {
        return null;
    }

    const [currentYear, currentMonth] = period.split('-').map(Number);
    return {
        ...source,
        feedbacks: source.comment ? [source.comment] : [],
        savingsAnalogy: source.savingsStatement
            ? `이번달 아낀 ${source.savingsStatement.amount.toLocaleString('ko-KR')}원은 ${source.savingsStatement.category} ${source.savingsStatement.count}잔`
            : null,
        fixedExpenseCandidateCount: source.fixedExpenseCandidates?.length ?? 0,
        confirmedFixedExpenseCount: source.confirmedFixedExpenseCount ?? 0,
        parentCategories: source.categories?.map((category) => ({ ...category })),
        categories: source.categories?.map((category) => ({
            ...category,
            parentCategoryId: category.categoryId ?? null,
            parentCategoryName: category.name,
        })),
        monthlyTrend: source.monthlyTrend?.map((item) => {
            const year = item.month <= currentMonth ? currentYear : currentYear - 1;
            return {
                ...item,
                yearMonth: `${year}-${String(item.month).padStart(2, '0')}`,
                hasData: true,
            };
        }),
    };
}
