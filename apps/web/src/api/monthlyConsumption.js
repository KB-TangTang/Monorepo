import http from '@/api/http';
import { composeMonthlyConsumptionReport } from '@/utils/monthlyConsumption';

export async function fetchMonthlyConsumptionReport(period) {
    const params = { yearMonth: period };
    const [summary, trend, categories] = await Promise.all([
        http.get('/reports/monthly/summary', { params }),
        http.get('/reports/monthly/spending-trend', { params }),
        http.get('/reports/monthly/categories', { params }),
    ]);
    return composeMonthlyConsumptionReport(summary, trend, categories);
}

export async function fetchMonthlyConsumptionMonths() {
    const result = await http.get('/reports/monthly/months');
    return result.months;
}
