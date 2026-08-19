import http from '@/api/http';

export function fetchTodayMission() {
    return http.get('/missions/today');
}

export function syncPersonalMissionUnlock(enoughData) {
    return http.post('/main-challenge/mission-unlock/status', { enoughData });
}

export function acknowledgePersonalMissionUnlock() {
    return http.patch('/main-challenge/mission-unlock/acknowledge');
}

export function fetchMissionStreak() {
    return http.get('/missions/streak');
}

export function fetchPendingMissionVerdict() {
    return http.get('/missions/verdicts/pending');
}

export function acknowledgeMissionVerdict(assignmentId) {
    return http.post(`/missions/verdicts/${assignmentId}/acknowledge`);
}

export function fetchMissionMonthlyScore() {
    return http.get('/missions/monthly-score');
}

export function fetchMissionRankings(yearMonth) {
    return http.get('/missions/rankings', {
        params: { yearMonth },
    });
}

export function fetchMissionCertificate(yearMonth) {
    return http.get('/missions/rankings/certificate', {
        params: { yearMonth },
    });
}

export function fetchMissionCertificateTitles(yearMonth) {
    return http.get('/missions/rankings/certificate/titles', {
        params: { yearMonth },
    });
}

export function fetchMissionRankingMonths() {
    return http.get('/missions/rankings/months');
}

export function fetchMissionCategoryAnalysis() {
    return http.get('/missions/categoryAnalysis');
}

export function reassignTodayMission() {
    return http.post('/dev/missions/today/reassign');
}
