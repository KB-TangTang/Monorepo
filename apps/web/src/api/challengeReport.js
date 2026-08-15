import http from '@/api/http';
import { AVAILABLE_MONTHS, REPORTS } from '@/fixtures/challengeReport';
import { getPreviousPeriod, isPublishedPeriod } from '@/utils/challengeReport';

function clone(value) {
    return JSON.parse(JSON.stringify(value));
}

export async function fetchMockChallengeReport(period, referenceDate = new Date()) {
    const report = REPORTS[period];
    if (!report || !isPublishedPeriod(period, referenceDate)) {
        throw new Error('해당 월의 챌린지 리포트를 불러올 수 없습니다.');
    }
    return clone(report);
}

export async function fetchMockChallengeReportMonths(referenceDate = new Date()) {
    const latestPeriod = getPreviousPeriod(referenceDate);
    return clone(
        AVAILABLE_MONTHS.map((month) => ({
            ...month,
            available: month.hasReport && month.value <= latestPeriod,
            reason:
                month.value > latestPeriod ? 'future' : month.hasReport ? undefined : 'unavailable',
        })),
    );
}

/** TEMP(#241): 챌린지 리포트 상세 API 계약이 확정돼 Mock 소스를 제거할 때까지 유지한다. */
export async function fetchChallengeReport(period) {
    return http.get('/reports/challenge', { params: { yearMonth: period } });
}

export async function fetchChallengeReportMonths() {
    return http.get('/reports/challenge/months');
}
