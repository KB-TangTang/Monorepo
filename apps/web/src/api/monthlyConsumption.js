import http from '@/api/http';
import { composeMonthlyConsumptionReport } from '@/utils/monthlyConsumption';

const AI_ANALYSIS_TIMEOUT_MS = 30_000;

export async function fetchMonthlyConsumptionReport(period) {
    const params = { yearMonth: period };
    const [summary, trend, categories, aiAnalysis] = await Promise.all([
        http.get('/reports/monthly/summary', { params }),
        http.get('/reports/monthly/spending-trend', { params }),
        http.get('/reports/monthly/categories', { params }),
        http.get('/reports/monthly/ai-analysis', { params, timeout: AI_ANALYSIS_TIMEOUT_MS }).catch(() => null),
    ]);

    // 월초 배치가 누락된 경우에만 화면 진입을 보정한다.
    // FAILED 는 사용자가 명시적으로 다시 시도할 때까지 자동 호출하지 않는다.
    const resolvedAiAnalysis =
        aiAnalysis?.status === 'NOT_REQUESTED'
            ? await http
                  .post('/reports/monthly/ai-analysis', null, {
                      params,
                      timeout: AI_ANALYSIS_TIMEOUT_MS,
                  })
                  .catch(() => aiAnalysis)
            : aiAnalysis;

    return composeMonthlyConsumptionReport(summary, trend, categories, resolvedAiAnalysis);
}

export async function fetchMonthlyConsumptionMonths() {
    const result = await http.get('/reports/monthly/months');
    return result.months;
}
