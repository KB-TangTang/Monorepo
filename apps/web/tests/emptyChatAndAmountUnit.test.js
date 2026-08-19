import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

/*
 * 이슈 #323 - 2026-08-19 배포본 점검에서 나온 화면 결함 2건.
 *
 * 렌더링 하네스가 없어 소스를 검사한다. 둘 다 「코드가 그 자리에 있는가」로 잡히는 종류다
 * (tests/personalMissionUnlock.test.js 와 같은 방식).
 */

function source(path) {
    return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

/* ── 빈 채팅방 ────────────────────────────────────────── */

/*
 * 로드에 성공했는데 메시지가 0건인 방이 **아무 문구 없는 흰 화면**이었다.
 * loading · closed · error 분기는 있는데 「성공 + 0건」 분기만 없었다.
 * 다른 화면은 전부 빈 상태 문구가 있다(「확정된 고정지출이 없어요」 등).
 */
test('메시지가 0건인 채팅방은 빈 화면이 아니라 안내를 보여준다', () => {
    const chatView = source('src/views/challenge/group/GroupChatView.vue');

    assert.match(
        chatView,
        /v-else-if="store\.messages\.length === 0"/,
        '「로드 성공 + 0건」 분기가 있어야 한다',
    );
    assert.match(chatView, /chat-view__empty/, '빈 상태 블록이 있어야 한다');
    assert.match(chatView, /아직 오간 말이 없어요/, '안내 문구가 있어야 한다');
});

test('빈 상태 안내는 로딩 분기 뒤에 온다 - 불러오는 중에 「없어요」가 뜨면 안 된다', () => {
    const chatView = source('src/views/challenge/group/GroupChatView.vue');

    const loadingIndex = chatView.indexOf('chat-view__loading');
    const emptyIndex = chatView.indexOf('chat-view__empty');

    assert.ok(loadingIndex > -1 && emptyIndex > -1);
    assert.ok(loadingIndex < emptyIndex, 'v-if(로딩) → v-else-if(빈 상태) 순서여야 한다');
});

/* 챌린지가 끝난 방에 「첫 마디를 남겨보세요」를 띄우면 입력바가 비활성인데도 권하는 꼴이 된다. */
test('끝난 챌린지의 빈 방에는 말을 걸라고 권하지 않는다', () => {
    const chatView = source('src/views/challenge/group/GroupChatView.vue');

    const emptyBlock = chatView.match(/chat-view__empty"[\s\S]*?<\/div>/);
    assert.ok(emptyBlock, '빈 상태 블록을 찾지 못했다');
    assert.match(emptyBlock[0], /store\.isEnded/, '종료 여부로 문구를 갈라야 한다');
});

/* ── 금액 단위 ────────────────────────────────────────── */

/*
 * 홈 순자산이 「1,244,200」으로 나갔다. 같은 화면의 다른 금액은 전부 「0원」이라
 * 무엇을 세는 숫자인지 알 수 없었다.
 */
test('홈 순자산 금액에 원 단위를 붙인다', () => {
    const homeView = source('src/views/HomeView.vue');

    const amountBlock = homeView.match(/<strong class="summary-card__amount">[\s\S]*?<\/strong>/);
    assert.ok(amountBlock, 'summary-card__amount 블록을 찾지 못했다');
    assert.match(amountBlock[0], /summary-card__unit">원</, '단위 span 이 있어야 한다');
});

/*
 * 숫자는 mono + tabular-nums 로 두는 것이 의도된 디자인이다.
 * 단위까지 mono 로 끌려가면 한글이 폴백 서체로 떨어져 어색해진다.
 */
test('단위는 본문 서체로 되돌린다 - 숫자만 mono 로 남긴다', () => {
    const homeCss = source('src/views/HomeView.css');

    const unitRule = homeCss.match(/\.summary-card__unit\s*\{[\s\S]*?\}/);
    assert.ok(unitRule, 'summary-card__unit 규칙이 없다');
    assert.match(unitRule[0], /font-family:\s*var\(--tt-font-sans\)/);

    assert.doesNotMatch(
        unitRule[0],
        /--tt-font-base/,
        '존재하지 않는 토큰이다. 폴백이 mono 를 상속해 조용히 어긋난다',
    );
});

test('금액 숫자는 여전히 mono 와 tabular-nums 를 쓴다', () => {
    const homeCss = source('src/views/HomeView.css');

    const amountRule = homeCss.match(/\.summary-card__amount\s*\{\s*font-family[\s\S]*?\}/);
    assert.ok(amountRule, 'summary-card__amount 의 서체 규칙이 없다');
    assert.match(amountRule[0], /var\(--tt-font-mono\)/);
    assert.match(amountRule[0], /tabular-nums/);
});
