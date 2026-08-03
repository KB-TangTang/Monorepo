import axios from 'axios';
import { useAuthStore } from '@/stores/auth';

/**
 * 프로젝트 공용 axios 인스턴스.
 * 모든 API 호출은 이 인스턴스를 통해서만 한다. (컴포넌트에서 axios 직접 import 금지)
 *
 * 백엔드는 공통 래퍼로 응답한다:
 *   성공 { success: true,  data: ... }
 *   실패 { success: false, code: "...", message: "..." }
 * 인터셉터에서 data 만 꺼내주므로, 호출부는 실제 payload 만 받는다.
 *
 * withCredentials: 리프레시 토큰이 httpOnly 쿠키라 자격증명을 같이 보내야 한다.
 */
const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

const http = axios.create({
    baseURL: BASE_URL,
    timeout: 10000,
    withCredentials: true,
    headers: { 'Content-Type': 'application/json' },
});

/*
 * 재발급 전용 인스턴스.
 * 아래 응답 인터셉터가 달려 있지 않아야 401 → refresh → 401 무한 재귀가 생기지 않는다.
 * api/auth.js 를 import 하면 http.js ↔ auth.js 순환이 생기므로 여기서 직접 만든다.
 */
const refreshClient = axios.create({
    baseURL: BASE_URL,
    timeout: 10000,
    withCredentials: true,
    headers: { 'Content-Type': 'application/json' },
});

/*
 * 동시에 여러 요청이 401 을 받으면 재발급이 N 번 날아간다.
 * 리프레시 토큰은 회전 방식이라 두 번째 호출부터는 "폐기된 토큰 재사용"으로 감지돼
 * 사용자의 전체 토큰이 폐기된다. 그래서 진행 중인 재발급을 하나로 묶는다.
 */
let refreshPromise = null;

function requestRefresh() {
    if (!refreshPromise) {
        refreshPromise = refreshClient
            .post('/auth/refresh')
            .then((response) => response.data.data)
            .finally(() => {
                refreshPromise = null;
            });
    }
    return refreshPromise;
}

// 요청: 액세스 토큰 주입
http.interceptors.request.use((config) => {
    // 스토어를 구조분해(const { accessToken } = ...)하지 않는다.
    // 여기서는 요청마다 호출돼 결과가 같지만, 그 패턴이 컴포넌트로 복사되면
    // 반응성이 끊겨 토큰이 갱신돼도 화면이 옛 값을 계속 들고 있게 된다.
    const auth = useAuthStore();
    if (auth.accessToken) {
        config.headers.Authorization = `Bearer ${auth.accessToken}`;
    }
    return config;
});

// 응답: 공통 래퍼 언랩 + 401 재발급 재시도 + 에러 정규화
http.interceptors.response.use(
    (response) => {
        const body = response.data;
        if (body && typeof body.success === 'boolean') {
            if (!body.success) {
                return Promise.reject(new ApiError(body.code, body.message, response.status));
            }
            return body.data;
        }
        return body;
    },
    async (error) => {
        const config = error.config;
        const status = error.response?.status;
        const isAuthCall = config?.url?.startsWith('/auth/');

        // 재시도는 요청당 1회. 인증 경로 자체는 재시도하지 않는다.
        if (status === 401 && config && !config.isRetry && !isAuthCall) {
            config.isRetry = true;
            try {
                const session = await requestRefresh();
                useAuthStore().setSession(session);
                config.headers.Authorization = `Bearer ${session.accessToken}`;
                return http(config);
            } catch {
                // 재발급도 실패 = 세션이 끝났다. 로그인 화면으로 되돌린다.
                useAuthStore().clear();
                window.location.assign('/login?error=expired');
            }
        }

        const body = error.response?.data;
        return Promise.reject(
            new ApiError(
                body?.code ?? 'NETWORK_ERROR',
                body?.message ?? '서버와 통신할 수 없습니다.',
                error.response?.status ?? 0,
            ),
        );
    },
);

export class ApiError extends Error {
    constructor(code, message, status) {
        super(message);
        this.name = 'ApiError';
        this.code = code;
        this.status = status;
    }
}

export default http;
