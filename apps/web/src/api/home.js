import http from './http';

//API : /v1/home
export function fetchHome() {
    return http.get('/home');
}
