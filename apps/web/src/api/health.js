import http from './http'

/** 백엔드 연결 확인용. GET /api/health */
export function fetchHealth() {
  return http.get('/health')
}
