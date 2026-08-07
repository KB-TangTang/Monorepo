/**
 * SSE 텍스트 스트림 파서.
 *
 * EventSource 를 쓰지 않는 이유는 헤더를 붙일 수 없기 때문이다.
 * 이 앱의 액세스 토큰은 메모리에만 있고 Authorization 헤더로 전달되며,
 * 리프레시 쿠키는 Path=/api/auth 로 묶여 있어 /api/notifications/stream 에 가지 않는다.
 * 그래서 fetch + ReadableStream 으로 직접 읽고, 그 텍스트를 여기서 해석한다.
 */

/**
 * 버퍼에 새 청크를 붙여 완성된 이벤트만 꺼낸다.
 *
 * @param {string} buffer 이전 호출에서 남은 조각
 * @param {string} chunk 새로 받은 텍스트
 * @returns {{events: Array<{event: string, data: string}>, rest: string}}
 */
export function parseSseChunk(buffer, chunk) {
    const merged = buffer + chunk;
    const blocks = merged.split('\n\n');
    // 마지막 조각은 아직 끝나지 않았을 수 있으므로 다음 호출로 넘긴다
    const rest = blocks.pop() ?? '';
    const events = [];

    for (const block of blocks) {
        let name = 'message';
        const dataLines = [];
        for (const line of block.split('\n')) {
            if (line.startsWith(':') || line.trim() === '') {
                continue; // 주석(하트비트)과 빈 줄은 버린다
            }
            if (line.startsWith('event:')) {
                name = line.slice('event:'.length).trim();
            } else if (line.startsWith('data:')) {
                dataLines.push(line.slice('data:'.length).trim());
            }
        }
        if (dataLines.length > 0) {
            events.push({ event: name, data: dataLines.join('\n') });
        }
    }

    return { events, rest };
}
