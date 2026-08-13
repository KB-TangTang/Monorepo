import http from '@/api/http';

export function fetchTodayMission() {
    return http.get('/missions/today');
}

export function fetchMissionStreak() {
    return http.get('/missions/streak');
}

export function fetchMissionCategoryAnalysis() {
    return http.get('/missions/categoryAnalysis');
}

export function reassignTodayMission() {
    return http.post('/dev/missions/today/reassign');
}
