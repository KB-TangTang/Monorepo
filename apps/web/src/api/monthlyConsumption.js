import { AVAILABLE_MONTHS, REPORTS } from '@/fixtures/monthlyConsumption';
import { getPreviousPeriod, isAvailableReportMonth } from '@/utils/monthlyConsumption';

function clone(value) {
    return JSON.parse(JSON.stringify(value));
}

export async function fetchMonthlyConsumptionReport(period, referenceDate = new Date()) {
    const report = REPORTS[period];
    if (!report || period > getPreviousPeriod(referenceDate)) {
        throw new Error('해당 월의 소비 리포트를 불러올 수 없습니다.');
    }
    return clone(report);
}

export async function fetchMonthlyConsumptionMonths(referenceDate = new Date()) {
    const latestPeriod = getPreviousPeriod(referenceDate);
    return clone(
        AVAILABLE_MONTHS.map((month) => ({
            ...month,
            available: isAvailableReportMonth(month, referenceDate),
            reason:
                month.value > latestPeriod ? 'future' : month.hasReport ? undefined : 'unavailable',
        })),
    );
}
