import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

/*
 * 이슈 #174 Task 12: 화면 연결과 목업 제거.
 *
 * 이 프로젝트에는 컴포넌트 렌더링 테스트 하네스가 없다(node:test + 순수 JS 단위 테스트뿐,
 * tests/backNavigation.test.js 와 같은 방식). 그래서 소스 텍스트로 계약을 확인한다.
 *
 * 특히 아래 두 가지는 컨트롤러 판정(Ruling F)에서 못 박은 요구사항이다:
 *   - 구독 해제만으로는 서버의 접속 세션 추적이 UNSUBSCRIBE 를 인식하지 못해 알림이 안 나간다.
 *     그래서 화면을 벗어날 때 반드시 STOMP 연결 자체(disconnect)를 끊어야 한다.
 *   - onMounted 에서 store.enterRoom 이 끝난 뒤에만 소켓을 연결해야, 종료된 챌린지·비참여자
 *     진입 시 불필요한 연결을 시도하지 않는다.
 */
function source() {
    return readFileSync(
        new URL('../src/views/challenge/group/GroupChatView.vue', import.meta.url),
        'utf8',
    );
}

test('onUnmounted 에서 socket.disconnect 를 부른다', () => {
    const src = source();
    const match = src.match(/onUnmounted\(\(\) => \{[\s\S]*?\}\);/);
    assert.ok(match, 'onUnmounted 블록을 찾지 못했다');
    assert.ok(
        /socket\??\.disconnect\(\)/.test(match[0]),
        'onUnmounted 안에서 socket.disconnect() 를 호출해야 한다 — 구독 해제만으로는 서버가 ' +
            '접속 중으로 계속 인식해 알림이 안 나간다',
    );
});

test('onMounted 는 store.enterRoom 이 끝난 뒤에만 소켓을 연결한다', () => {
    const src = source();
    const match = src.match(/onMounted\(async \(\) => \{[\s\S]*?\n\}\);/);
    assert.ok(match, 'onMounted 블록을 찾지 못했다');
    const body = match[0];

    const enterIndex = body.indexOf('store.enterRoom(');
    const connectIndex = body.indexOf('.connect()');
    assert.ok(enterIndex >= 0, 'onMounted 에서 store.enterRoom 을 불러야 한다');
    assert.ok(connectIndex >= 0, 'onMounted 에서 socket.connect() 를 불러야 한다');
    assert.ok(enterIndex < connectIndex, 'enterRoom 이 끝나기 전에 소켓을 연결하면 안 된다');

    assert.ok(
        /store\.closed|store\.error/.test(body),
        '진입 실패(종료된 챌린지·비참여자) 시에는 소켓을 연결하지 않아야 한다',
    );
});

test('createChatSocket 은 getToken 콜백으로 최신 토큰을 넘긴다', () => {
    const src = source();
    assert.ok(
        /getToken:\s*\(\)\s*=>\s*useAuthStore\(\)\.accessToken/.test(src),
        'getToken 콜백이 매 재연결마다 최신 accessToken 을 읽어야 한다(브리프의 token 문자열은 오래된 설명)',
    );
    assert.ok(/onMessage:\s*\(message\)\s*=>\s*store\.appendMessage\(message\)/.test(src));
    assert.ok(/onReconnect:\s*\(\)\s*=>\s*store\.catchUp\(\)/.test(src));
});

test('종료된 챌린지는 안내 문구를 보여준다', () => {
    const src = source();
    assert.ok(src.includes('store.closed'), 'store.closed 로 안내 화면을 분기해야 한다');
    assert.ok(src.includes('종료된 챌린지예요'));
});

test('스티커 피커·타이핑 표시는 이번 범위 밖이다 — 화면에서 쓰지 않는다', () => {
    const src = source();
    assert.ok(
        !src.includes('GroupChatStickerPicker'),
        'GroupChatStickerPicker 는 이번 기능 범위 밖이라 화면에서 제거해야 한다',
    );
    assert.ok(
        !src.includes('GroupChatTypingDots'),
        'GroupChatTypingDots 는 이번 기능 범위 밖이라 화면에서 쓰면 안 된다',
    );
});

test('전송은 REST 스텁이 아니라 소켓으로 나간다', () => {
    const src = source();
    assert.ok(
        !src.includes('store.sendText') && !src.includes('store.sendSticker'),
        'sendText/sendSticker 로컬 스텁 호출이 남아 있으면 안 된다',
    );
    assert.ok(/socket\??\.send\(/.test(src), 'socket.send() 로 실전송해야 한다');
});

/*
 * 리뷰 Fix round 1 (Important): GroupChatInput 의 스티커 버튼이 toggle-sticker 를 emit 하지만
 * 이 화면에는 더 이상 그 리스너가 없어 눌러도 반응이 없는 죽은 버튼으로 남아 있었다.
 * 스티커는 설계상 범위 밖이므로 버튼 자체를 감춘다(마크업은 남겨 나중에 되살리기 쉽게 유지).
 */
test('입력바에서 스티커 버튼을 강제로 켜지 않는다', () => {
    const src = source();
    assert.ok(
        !/show-sticker-button\s*=\s*"true"/.test(src),
        'GroupChatView 가 스티커 버튼을 다시 켜면 안 된다 — 이번 범위 밖이다',
    );
});

test('GroupChatInput 의 스티커 버튼은 기본적으로 숨겨져 있다', () => {
    const inputSrc = readFileSync(
        new URL('../src/components/challenge/group/chat/GroupChatInput.vue', import.meta.url),
        'utf8',
    );

    assert.match(
        inputSrc,
        /showStickerButton:\s*\{\s*type:\s*Boolean,\s*default:\s*false\s*\}/,
        'showStickerButton prop 기본값이 false 여야 눌러도 반응 없는 버튼이 화면에 안 보인다',
    );
    assert.match(
        inputSrc,
        /<button\s+v-if="showStickerButton"[\s\S]*?chat-input__sticker-btn/,
        '스티커 버튼은 v-if="showStickerButton" 으로 감싸야 한다 — 마크업은 지우지 않고 감춘다',
    );
});

/*
 * 최종 리뷰 C2: 화면이 삭제된 목업 스키마를 보고 있어 메시지가 한 건도 그려지지 않았다.
 *   - msg.messageType === 'USER'  → 서버는 type: 'TEXT'  (모든 텍스트 메시지가 v-else-if 를 빠져나감)
 *   - msg.createdAt               → 서버는 sentAt        (날짜 구분선이 "NaN월 NaN일")
 *   - roomInfo.challengeName      → 서버는 groupName     (제목 빈칸)
 *   - currentDay · daysLeft       → 서버가 주지 않음      ("undefined일차 · D-undefined")
 * 렌더링 하네스가 없으므로 소스 텍스트로 계약을 확인한다.
 */
const MOCKUP_FIELDS = [
    'messageType',
    'systemSubType',
    'createdAt',
    'challengeName',
    'senderColor',
    'currentDay',
    'daysLeft',
    'contentType',
];

const LIVE_CHAT_FILES = [
    '../src/views/challenge/group/GroupChatView.vue',
    '../src/components/challenge/group/chat/GroupChatHeader.vue',
    '../src/components/challenge/group/chat/GroupChatBubble.vue',
    '../src/components/challenge/group/chat/GroupChatSystemPill.vue',
];

test('화면에 목업 전용 필드가 남아 있지 않다', () => {
    for (const path of LIVE_CHAT_FILES) {
        const src = readFileSync(new URL(path, import.meta.url), 'utf8');
        for (const field of MOCKUP_FIELDS) {
            assert.ok(
                !src.includes(field),
                `${path} 에 목업 필드 '${field}' 가 남아 있다 — 서버 DTO 에 없는 이름이다`,
            );
        }
    }
});

test('메시지는 type 이 무엇이든 화면에서 사라지지 않는다', () => {
    const src = source();
    assert.ok(
        /<GroupChatBubble\s+v-else\b/.test(src),
        '참여자 메시지는 v-else 로 받아야 한다 — v-else-if 로 좁히면 모르는 type 이 통째로 사라진다',
    );
    assert.ok(
        /v-else-if="item\.data\.isSystem"/.test(src),
        '시스템 메시지는 어댑터가 만든 isSystem 으로 분기해야 한다',
    );
});

test('종료 판정은 스토어의 isEnded 를 쓴다 (목업 상태값 ENDED 를 직접 비교하지 않는다)', () => {
    for (const path of LIVE_CHAT_FILES) {
        const src = readFileSync(new URL(path, import.meta.url), 'utf8');
        assert.ok(!src.includes("'ENDED'"), `${path} 에 서버에 없는 상태값 'ENDED' 비교가 남아 있다`);
    }
});
