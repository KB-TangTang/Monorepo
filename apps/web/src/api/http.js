import axios from 'axios';

/**
 * 프로젝트 공용 axios 인스턴스.
 * 모든 API 호출은 이 인스턴스를 통해서만 한다. (컴포넌트에서 axios 직접 import 금지)
 *
 * 백엔드는 공통 래퍼로 응답한다:
 *   성공 { success: true,  data: ... }
 *   실패 { success: false, code: "...", message: "..." }
 * 인터셉터에서 data 만 꺼내주므로, 호출부는 실제 payload 만 받는다.
 */
const http = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
    timeout: 10000,
    headers: { 'Content-Type': 'application/json' },
});

// 요청: 토큰이 생기면 여기에 주입한다
http.interceptors.request.use((config) => {
    // const token = useAuthStore().token
    // if (token) config.headers.Authorization = `Bearer ${token}`
    return config;
});

// 응답: 공통 래퍼 언랩 + 에러 정규화
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
    (error) => {
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
