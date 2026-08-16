/**
 * STOMP 접속 URL 유도 (이슈 #174).
 *
 * 이 파일은 **주어진 베이스 URL 에서 소켓 주소를 만드는 규칙**만 갖는다.
 * 어떤 환경변수를 넘길지는 호출자(`chatSocket.js`)가 정한다 — 이슈 #268 이후로 소켓은
 * `VITE_WS_BASE_URL` 을 먼저 보고 없으면 `VITE_API_BASE_URL` 로 폴백한다.
 *
 * - 인자가 절대 URL 이면 그 호스트를 쓴다. 경로의 끝 `/api` 는 떼고 `/ws/chat` 을 붙인다
 *   (컨텍스트 경로 아래에 배포된 경우까지 따라간다).
 * - 값이 없거나 상대 경로(`/api`)면 현재 호스트로 폴백한다. 로컬 개발이 이 경로이며,
 *   vite.config.js 의 `'/ws'` 프록시(ws: true)가 :8080 으로 넘겨 준다.
 *
 * ⚠ 판정이 `http(s)://` 접두사다. `wss://...` 를 넘기면 절대 URL 로 인식되지 않아
 *   **조용히 현재 호스트로 폴백**한다. 환경변수에는 `https://<도메인>` 을 넣어야 한다.
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
