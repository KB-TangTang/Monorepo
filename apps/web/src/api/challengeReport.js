import http from '@/api/http';
import { AVAILABLE_MONTHS, REPORTS } from '@/fixtures/challengeReport';
import {
    getPreviousPeriod,
    isPublishedPeriod,
    toChallengeReportModel,
} from '@/utils/challengeReport';

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
            available:
                month.value <= latestPeriod && (month.hasReport || Boolean(month.entryState)),
            reason:
                month.value > latestPeriod
                    ? 'future'
                    : month.hasReport || month.entryState
                      ? undefined
                      : 'unavailable',
        })),
    );
}

/** TEMP(#241): Mock 경로를 유지한 채 상세 API 응답만 화면 모델로 바꾼다. */
export async function fetchChallengeReport(period) {
    return toChallengeReportModel(
        await http.get('/reports/challenge', { params: { yearMonth: period } }),
    );
}

export async function fetchChallengeReportMonths() {
    return http.get('/reports/challenge/months');
}
