import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

import { buildChatSocketUrl } from '../src/api/chatSocketUrl.js';

/*
 * 최종 리뷰 C3: 소켓 URL 이 dev·prod 어디에도 도달하지 못했다.
 * 무조건 window.location.host 를 썼는데, 로컬은 :5173(프록시에 /ws 가 없었다)이고
 * 프로덕션은 Vercel 호스트라 REST 만 EC2 로 가고 WS 는 갈 곳이 없었다.
 */

const LOCAL = { protocol: 'http:', host: 'localhost:5173' };
const VERCEL = { protocol: 'https:', host: 'monorepo-three-ruby-81.vercel.app' };

test('VITE_API_BASE_URL 이 없으면 현재 호스트로 폴백한다 (로컬: Vite 프록시)', () => {
    assert.equal(buildChatSocketUrl(undefined, LOCAL), 'ws://localhost:5173/ws/chat');
});

test('상대 경로(/api)도 현재 호스트로 폴백한다', () => {
    assert.equal(buildChatSocketUrl('/api', LOCAL), 'ws://localhost:5173/ws/chat');
});

test('절대 URL 이면 그 호스트로 붙는다 — 프론트 호스트가 아니라 API 서버다', () => {
    assert.equal(
        buildChatSocketUrl('http://3.35.24.153:8080/api', VERCEL),
        'ws://3.35.24.153:8080/ws/chat',
    );
});

test('https 기반이면 wss 로 올린다', () => {
    assert.equal(buildChatSocketUrl('https://api.example.com/api', VERCEL), 'wss://api.example.com/ws/chat');
});

test('컨텍스트 경로 아래 배포돼도 /api 만 떼고 그 경로를 유지한다', () => {
    assert.equal(
        buildChatSocketUrl('https://api.example.com/tangtang/api', VERCEL),
        'wss://api.example.com/tangtang/ws/chat',
    );
});

test('끝의 슬래시가 있어도 경로가 중복되지 않는다', () => {
    assert.equal(buildChatSocketUrl('https://api.example.com/api/', VERCEL), 'wss://api.example.com/ws/chat');
});

/*
 * 로컬은 프록시가 없으면 소켓이 SPA fallback(index.html)으로 떨어져 붙지 않는다.
 * ws: true 가 빠지면 업그레이드가 일어나지 않으므로 둘 다 확인한다.
 */
test('vite 개발 서버가 /ws 를 백엔드로 넘긴다 (ws 업그레이드 포함)', () => {
    const config = readFileSync(new URL('../vite.config.js', import.meta.url), 'utf8');
    const wsBlock = config.match(/'\/ws':\s*\{[\s\S]*?\}/);

    assert.ok(wsBlock, "vite.config.js 에 '/ws' 프록시가 있어야 한다");
    assert.match(wsBlock[0], /target:\s*'http:\/\/localhost:8080'/);
    assert.match(wsBlock[0], /ws:\s*true/);
});

/*
 * ── 이슈 #268: 소켓만 EC2 직행 ────────────────────────────────
 * 배포에서 REST 와 소켓의 길이 갈린다. REST 는 Vercel rewrite 를 거쳐 EC2:8080 으로,
 * 소켓은 rewrite 가 업그레이드를 프록시하지 못해 EC2:443(nginx) 으로 직행한다.
 * 그래서 소켓 전용 환경변수(VITE_WS_BASE_URL)를 따로 둔다.
 */

test('소켓 전용 도메인은 /api 없이 넣어도 그대로 wss 주소가 된다 (프로덕션 실제 값)', () => {
    assert.equal(
        buildChatSocketUrl('https://kb-tangtang.duckdns.org', VERCEL),
        'wss://kb-tangtang.duckdns.org/ws/chat',
    );
});

/*
 * 이 케이스가 이 파일에서 가장 중요하다. wss:// 를 넣으면 절대 URL 판정(/^https?:\/\//i)에
 * 걸리지 않아 조용히 Vercel 호스트로 폴백한다 — 소켓이 프론트 호스트를 가리켜 절대 안 붙는데
 * 증상은 "그냥 안 붙는다" 뿐이라 원인을 찾기 어렵다. 환경변수에는 https:// 를 넣어야 한다.
 */
test('wss:// 를 넣으면 절대 URL 로 인식되지 않고 현재 호스트로 폴백한다 — 환경변수 함정', () => {
    assert.equal(
        buildChatSocketUrl('wss://kb-tangtang.duckdns.org', VERCEL),
        'wss://monorepo-three-ruby-81.vercel.app/ws/chat',
    );
});

test('chatSocket 은 소켓 전용 값을 먼저 보고 없으면 REST 값으로 폴백한다', () => {
    const source = readFileSync(new URL('../src/api/chatSocket.js', import.meta.url), 'utf8');

    /*
     * 폴백을 지우면 로컬 개발이 깨진다(로컬에는 VITE_WS_BASE_URL 이 없다).
     * 우선순위를 뒤집으면 배포에서 소켓이 다시 Vercel rewrite 로 가 붙지 않는다.
     * window.location.host 고정으로 되돌아가면 dev·prod 어디에도 도달하지 못한다.
     */
    assert.match(
        source,
        /buildChatSocketUrl\(\s*env\.VITE_WS_BASE_URL\s*\|\|\s*env\.VITE_API_BASE_URL/,
        '소켓 전용 값(VITE_WS_BASE_URL) → REST 값 순서로 폴백해야 한다',
    );
});
