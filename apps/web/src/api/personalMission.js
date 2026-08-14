import http from '@/api/http';

export function fetchTodayMission() {
    return http.get('/missions/today');
}

export function fetchMissionStreak() {
    return http.get('/missions/streak');
}

export function fetchMissionMonthlyScore() {
    return http.get('/missions/monthly-score');
}

export function fetchMissionRankings(yearMonth) {
    return http.get('/missions/rankings', {
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
