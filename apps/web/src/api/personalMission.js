import http from '@/api/http';

export function fetchTodayMission() {
    return http.get('/missions/today');
}

export function reassignTodayMission() {
    return http.post('/dev/missions/today/reassign');
}
