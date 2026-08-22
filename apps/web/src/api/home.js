import http from './http';

// API: /api/v1/home
export function fetchHome() {
    return http.get('/v1/home');
}
