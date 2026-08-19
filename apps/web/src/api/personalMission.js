import http from '@/api/http';

export function fetchTodayMission() {
    return http.get('/missions/today');
}

/* 자격 판정은 서버가 한다 - 보낼 본문이 없다(이슈 #315 (1)). */
export function syncPersonalMissionUnlock() {
    return http.post('/main-challenge/mission-unlock/status');
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
