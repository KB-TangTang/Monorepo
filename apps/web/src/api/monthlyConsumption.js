import http from '@/api/http';
import { composeMonthlyConsumptionReport } from '@/utils/monthlyConsumption';

export async function fetchMonthlyConsumptionReport(period) {
    const params = { yearMonth: period };
    const [summary, trend, categories, aiAnalysis] = await Promise.all([
        http.get('/reports/monthly/summary', { params }),
        http.get('/reports/monthly/spending-trend', { params }),
        http.get('/reports/monthly/categories', { params }),
        http.get('/reports/monthly/ai-analysis', { params }).catch(() => null),
    ]);
    return composeMonthlyConsumptionReport(summary, trend, categories, aiAnalysis);
}

export async function fetchMonthlyConsumptionMonths() {
    const result = await http.get('/reports/monthly/months');
    return result.months;
}
