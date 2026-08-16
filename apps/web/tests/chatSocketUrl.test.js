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

test('chatSocket 은 REST 와 같은 값에서 URL 을 유도한다', () => {
    const source = readFileSync(new URL('../src/api/chatSocket.js', import.meta.url), 'utf8');

    assert.match(
        source,
        /buildChatSocketUrl\(\s*import\.meta\.env\?\.VITE_API_BASE_URL/,
        'window.location.host 고정으로 되돌아가면 프로덕션에서 소켓이 프론트 호스트를 가리킨다',
    );
});
