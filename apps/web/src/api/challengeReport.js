import http from '@/api/http';
import { toChallengeReportModel } from '@/utils/challengeReport';

export async function fetchChallengeReport(period) {
    return toChallengeReportModel(
        await http.get('/reports/challenge', { params: { yearMonth: period } }),
    );
}

export async function fetchChallengeReportMonths() {
    return http.get('/reports/challenge/months');
}
