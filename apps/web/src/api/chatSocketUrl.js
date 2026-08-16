/**
 * STOMP 접속 URL 유도 (이슈 #174).
 *
 * REST 는 http.js 가 `VITE_API_BASE_URL`(없으면 `/api`)로 붙는다. 소켓이 그와 다른 호스트를
 * 가리키면 프로덕션에서 REST 는 API 서버로, WS 는 프론트 호스트로 나가 절대 연결되지 않는다.
 * 그래서 **REST 와 같은 값에서 유도**한다.
 *
 * - `VITE_API_BASE_URL` 이 절대 URL 이면 그 호스트를 쓴다. 경로의 끝 `/api` 는 떼고 `/ws/chat`
 *   을 붙인다(컨텍스트 경로 아래에 배포된 경우까지 따라간다).
 * - 값이 없거나 상대 경로(`/api`)면 현재 호스트로 폴백한다. 로컬 개발이 이 경로이며,
 *   vite.config.js 의 `'/ws'` 프록시(ws: true)가 :8080 으로 넘겨 준다.
 */
const WS_PATH = '/ws/chat';

export function buildChatSocketUrl(apiBaseUrl, location) {
    if (typeof apiBaseUrl === 'string' && /^https?:\/\//i.test(apiBaseUrl)) {
        const base = new URL(apiBaseUrl);
        const prefix = base.pathname.replace(/\/+$/, '').replace(/\/api$/, '');
        return `${wsProtocol(base.protocol)}://${base.host}${prefix}${WS_PATH}`;
    }
    return `${wsProtocol(location.protocol)}://${location.host}${WS_PATH}`;
}

function wsProtocol(httpProtocol) {
    return httpProtocol === 'https:' ? 'wss' : 'ws';
}
