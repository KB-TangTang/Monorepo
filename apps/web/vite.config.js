import { fileURLToPath, URL } from 'node:url';

import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import vueDevTools from 'vite-plugin-vue-devtools';

// https://vite.dev/config/
export default defineConfig({
    plugins: [vue(), vueDevTools()],
    resolve: {
        alias: {
            '@': fileURLToPath(new URL('./src', import.meta.url)),
        },
    },
    server: {
        port: 5173,
        // 개발 중 /api 요청을 톰캣(백엔드)으로 넘겨 CORS 를 피한다.
        // 백엔드 포트가 다르면 이 값만 바꾼다.
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
            },
            // 프로필 이미지 등 정적 업로드 파일. 없으면 /uploads/... 를 SPA fallback 이 삼켜
            // index.html(200)을 돌려주고, <img> 는 HTML 디코딩에 실패해 이니셜로 조용히 폴백한다.
            '/uploads': {
                target: 'http://localhost:8080',
                changeOrigin: true,
            },
            // 그룹 채팅 STOMP(이슈 #174). ws: true 가 없으면 업그레이드 요청이 그대로 SPA
            // fallback 으로 떨어져 소켓이 붙지 않는다.
            '/ws': {
                target: 'http://localhost:8080',
                changeOrigin: true,
                ws: true,
            },
        },
    },
});
