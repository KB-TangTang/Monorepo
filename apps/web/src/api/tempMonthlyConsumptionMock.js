// TEMP(#154): 백엔드 안정화 후 이 파일과 화면의 TEMP 연결 코드를 함께 삭제한다.
import { AVAILABLE_MONTHS, REPORTS } from '@/fixtures/monthlyConsumption';

export async function fetchTempMonthlyConsumptionMonths() {
    return AVAILABLE_MONTHS.map((month) => ({
        ...month,
        available: month.status === 'onboarding' || month.hasReport,
        status: month.status ?? (month.hasReport ? 'report' : 'unavailable'),
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
        fixedExpenseCandidateCount: source.fixedExpenseCandidates?.length ?? 0,
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
