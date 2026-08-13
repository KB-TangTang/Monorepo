import http from '@/api/http';

export function fetchTodayMission() {
    return http.get('/missions/today');
}
