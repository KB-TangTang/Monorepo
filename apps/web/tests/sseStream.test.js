import test from 'node:test';
import assert from 'node:assert/strict';
import { parseSseChunk } from '../src/utils/sseStream.js';

test('이벤트 하나를 읽는다', () => {
    const result = parseSseChunk('', 'event: connected\ndata: ok\n\n');
    assert.deepEqual(result.events, [{ event: 'connected', data: 'ok' }]);
    assert.equal(result.rest, '');
});

test('event 줄이 없으면 message 로 본다', () => {
    const result = parseSseChunk('', 'data: hello\n\n');
    assert.deepEqual(result.events, [{ event: 'message', data: 'hello' }]);
});

test('data 가 여러 줄이면 줄바꿈으로 잇는다', () => {
    const result = parseSseChunk('', 'data: a\ndata: b\n\n');
    assert.deepEqual(result.events, [{ event: 'message', data: 'a\nb' }]);
});

test('주석(: ping)은 무시한다 — 프록시 유지용 하트비트다', () => {
    const result = parseSseChunk('', ': ping\n\ndata: x\n\n');
    assert.deepEqual(result.events, [{ event: 'message', data: 'x' }]);
});

test('잘린 청크는 rest 에 남겨 다음 청크와 잇는다', () => {
    const first = parseSseChunk('', 'event: notifi');
    assert.deepEqual(first.events, []);
    assert.equal(first.rest, 'event: notifi');

    const second = parseSseChunk(first.rest, 'cation\ndata: {"id":1}\n\n');
    assert.deepEqual(second.events, [{ event: 'notification', data: '{"id":1}' }]);
});

test('한 청크에 이벤트가 여러 개여도 모두 읽는다', () => {
    const result = parseSseChunk('', 'data: 1\n\ndata: 2\n\n');
    assert.equal(result.events.length, 2);
});
