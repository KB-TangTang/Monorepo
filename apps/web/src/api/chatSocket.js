/**
 * 채팅 STOMP 클라이언트 (이슈 #174).
 *
 * 브라우저 WebSocket 은 핸드셰이크에 헤더를 못 싣는다. 그래서 토큰을 CONNECT 프레임의
 * connectHeaders 로 보낸다 — 서버의 StompAuthChannelInterceptor 가 여기서 인증한다.
 *
 * 액세스 토큰은 15분이다. 만료되면 재연결이 실패하므로 connect 시점의 최신 토큰을
 * getToken() 으로 매번 새로 읽는다.
 *
 * 구독·발행 목적지는 서버 인터셉터가 `/sub/chat/{groupId}` · `/pub/chat/{groupId}` 만
 * 허용한다(deny-by-default). 다른 목적지를 쓰면 연결 자체가 끊긴다.
 */
import { Client, ReconnectionTimeMode } from '@stomp/stompjs';
import { buildChatSocketUrl } from '@/api/chatSocketUrl';

const INITIAL_RECONNECT_DELAY_MS = 2000;
const MAX_RECONNECT_DELAY_MS = 30000;

/*
 * 인증·인가 실패는 STOMP ERROR 프레임 + 서버의 세션 종료로 나타난다(WebSocket close 로 이어짐).
 * 이런 경우 재시도해도 계속 실패하므로, 연속 실패가 이 횟수를 넘으면 재연결을 멈춘다.
 * 지수 백오프(2s→4s→8s→16s→30s 상한)와 함께 걸어 서버를 무한정 두드리지 않게 한다.
 */
const MAX_RECONNECT_ATTEMPTS = 5;

/*
 * 소켓 주소는 **소켓 전용 값을 먼저** 본다 (이슈 #268).
 *
 * REST 와 소켓이 배포 환경에서 다른 길로 간다. REST 는 Vercel rewrite 를 거쳐 EC2:8080 으로,
 * 소켓은 rewrite 가 업그레이드를 프록시하지 못해 EC2:443(nginx) 으로 직행한다.
 * 그래서 하나의 값에서 둘 다 유도할 수 없다.
 *
 *   VITE_WS_BASE_URL  있으면 이 값 → wss://kb-tangtang.duckdns.org/ws/chat
 *   없으면 VITE_API_BASE_URL 로 폴백 → 로컬 개발이 이 경로다(vite 프록시가 :8080 으로 넘긴다)
 *
 * 폴백을 남겨 두는 이유는 롤백 때문이다. Vercel 에서 VITE_WS_BASE_URL 만 지우고 재배포하면
 * 코드 변경 없이 이전 동작으로 돌아간다.
 *
 * ⚠ VITE_WS_BASE_URL 에는 `https://<도메인>` 을 넣는다. `wss://` 를 넣으면 chatSocketUrl.js 의
 *   `/^https?:\/\//i` 에 걸리지 않아 **조용히 현재 호스트(Vercel)로 폴백**한다. 증상이
 *   「그냥 안 붙는다」로만 나와 원인을 찾기 어렵다.
 *
 * 예전에는 무조건 window.location.host 였다. 로컬은 :5173(프록시에 /ws 가 없어 실패),
 * 프로덕션은 Vercel 호스트를 가리켜 dev·prod 어디에도 도달하지 못했다.
 */
function socketUrl() {
    const env = import.meta.env ?? {};
    return buildChatSocketUrl(env.VITE_WS_BASE_URL || env.VITE_API_BASE_URL, window.location);
}

export function createChatSocket({ groupId, getToken, onMessage, onReconnect }) {
    let connectedBefore = false;
    let failedAttempts = 0;

    const client = new Client({
        brokerURL: socketUrl(),
        reconnectDelay: INITIAL_RECONNECT_DELAY_MS,
        maxReconnectDelay: MAX_RECONNECT_DELAY_MS,
        reconnectTimeMode: ReconnectionTimeMode.EXPONENTIAL,
        beforeConnect: () => {
            client.connectHeaders = { Authorization: `Bearer ${getToken()}` };
        },
        onConnect: () => {
            failedAttempts = 0;
            client.subscribe(`/sub/chat/${groupId}`, (frame) => {
                onMessage(JSON.parse(frame.body));
            });
            // 첫 연결이 아니면 끊긴 동안의 메시지를 호출자가 보충 조회해야 한다
            if (connectedBefore && onReconnect) onReconnect();
            connectedBefore = true;
        },
        onWebSocketClose: () => {
            // 연결이 끊길 때마다(최초 연결 실패든 재연결 실패든) 센다. 성공하면 onConnect 가 0으로 되돌린다.
            // 인증 실패처럼 재시도해도 회복되지 않는 상황에서 상한 없이 계속 두드리지 않도록
            // 여기서 클라이언트 자체를 비활성화해 stompjs 의 자동 재연결 루프를 끊는다.
            failedAttempts += 1;
            if (failedAttempts >= MAX_RECONNECT_ATTEMPTS) {
                client.deactivate();
            }
        },
    });

    return {
        connect: () => client.activate(),
        // 구독 해제만으로는 서버의 ChatSessionRegistry 가 "접속 중"으로 계속 인식해 알림이
        // 안 나간다(SUBSCRIBE/DISCONNECT 만 추적, UNSUBSCRIBE 는 추적하지 않음). 그래서 화면을
        // 벗어날 때는 반드시 연결 자체를 끊는다 — deactivate() 가 DISCONNECT 프레임을 보내고
        // 소켓을 닫으며, stompjs 의 자동 재연결도 함께 멈춘다.
        disconnect: () => client.deactivate(),
        send: (content) => {
            client.publish({
                destination: `/pub/chat/${groupId}`,
                body: JSON.stringify({ content }),
            });
        },
    };
}
