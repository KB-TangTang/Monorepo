import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { registerHooks } from 'node:module';

/*
 * 지방법원(그룹챌린지) 홈 「내 챌린지」 섹션 회귀 방지 (이슈 #423).
 *
 * 서버가 시작일을 **반드시 내일 이후**로 막는다(ChallengeGroupService.validateCreate).
 * 그래서 그룹을 만들거나 참여코드로 들어온 직후에는 ACTIVE 가 하나도 없다 —
 * ACTIVE 만 부르던 예전 홈에서는 방금 한 행동이 흔적도 없이 사라지고
 * 판사 탕이가 「새로운 챌린지에 참여해보세요」라고 되물었다.
 *
 * 렌더링 하네스가 없어(`node:test` + 순수 JS) 목데이터 경로는 실제로 호출해 보고,
 * 화면 쪽은 소스에 규칙이 들어 있는지를 검사한다.
 */

const HTTP_STUB = new URL('./stubs/httpStub.js', import.meta.url).href;
/* devDataSource 는 import.meta.env.DEV 를 읽는다 — Vite 밖에서는 import 만 해도 터진다 */
const DEV_STUB = new URL('./stubs/devDataSourceStub.js', import.meta.url).href;

registerHooks({
    resolve(specifier, context, nextResolve) {
        if (specifier === '@/api/http') {
            return { url: HTTP_STUB, shortCircuit: true };
        }
        if (specifier === '@/services/devDataSource') {
            return { url: DEV_STUB, shortCircuit: true };
        }
        return nextResolve(specifier, context);
    },
});

const { isMockMode } = await import(DEV_STUB);
const { fetchMyGroupChallenges } = await import('../src/api/groupChallenge.js');
const { MOCK_PRE_START_CHALLENGES, MOCK_ACTIVE_LIST_CHALLENGES, MOCK_TODO_ITEMS } =
    await import('../src/fixtures/groupChallenge.js');

function source(path) {
    return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

/** 목 토글을 켠 채로 한 번만 돌린다. 스텁은 실데이터 경로 고정이라 반드시 되돌린다. */
async function withMockMode(fn) {
    isMockMode.value = true;
    try {
        return await fn();
    } finally {
        isMockMode.value = false;
    }
}

const HOME = 'src/views/challenge/group/GroupChallengeHomeView.vue';

test('홈은 세 상태를 부르고 재판 중만 카드에서 뺀다', () => {
    /*
     * 여기가 `['ACTIVE']` 로 되돌아가면 만들기·참여 직후 홈이 다시 빈 화면이 된다.
     * 화면에서는 「그냥 아무것도 안 생겼네」로 보여 원인을 찾기 어렵다.
     *
     * 조회는 셋, 표시는 둘이라 **두 줄을 함께 고정해야** 위 보장이 유지된다. 조회 리터럴만
     * 검사하면 필터가 `=== 'ACTIVE'` 로 잘못 바뀌어 RECRUITING 이 사라져도 초록으로 지나간다.
     */
    assert.match(source(HOME), /fetchMyGroupChallenges\(\['ACTIVE', 'RECRUITING', 'JUDGING'\]\)/);
    assert.match(source(HOME), /\.filter\(\(ch\) => ch\.status !== 'JUDGING'\)/);
});

test('목모드에서도 두 상태가 모두 나온다 — 앞의 하나만 돌려주지 않는다', async () => {
    /*
     * 예전 `mockGroupsByStatus` 는 `includes` 를 순서대로 검사해 **처음 걸린 상태 하나만**
     * 돌려줬다. 실서버는 `status IN (...)` 이라 멀쩡하고 DEV 목 토글을 켤 때만 진행 중 그룹이
     * 통째로 사라진다 — 실서버에서 재현이 안 돼 디버깅이 오래 걸리는 종류의 버그다.
     */
    const list = await withMockMode(() => fetchMyGroupChallenges(['ACTIVE', 'RECRUITING']));

    assert.equal(
        list.length,
        MOCK_ACTIVE_LIST_CHALLENGES.length + MOCK_PRE_START_CHALLENGES.length,
    );
    for (const set of [MOCK_ACTIVE_LIST_CHALLENGES, MOCK_PRE_START_CHALLENGES]) {
        for (const mock of set) {
            assert.ok(
                list.some((ch) => ch.groupId === mock.groupId),
                `${mock.groupName} 이 목록에서 빠졌다`,
            );
        }
    }
});

test('목데이터에 없는 상태를 물어도 터지지 않는다', async () => {
    /* 「종료됨」 탭이 JUDGING 을 함께 넘긴다. 목데이터가 없는 상태라 빈 배열로 통과해야 한다 */
    const list = await withMockMode(() => fetchMyGroupChallenges(['JUDGING']));
    assert.deepEqual(list, []);
});

test('시작 전 판별은 status 하나에만 기대지 않는다', () => {
    /*
     * 목데이터의 진행 중 항목에는 `status` 가 없다. `status === 'RECRUITING'` 만 보면
     * 목모드에서 시작 전 그룹이 진행 중으로 섞인다. 서버는 시작 후 `daysUntilStart` 를
     * NULL 로 내려주므로 두 조건을 함께 봐야 실데이터·목데이터 양쪽에서 맞는다.
     */
    assert.match(
        source(HOME),
        /function isUpcoming\(challenge\) \{[\s\S]*?daysUntilStart != null[\s\S]*?\}/,
    );
});

test('홈 목록에는 목숨 판사봉을 그리지 않는다', () => {
    /*
     * 한때 행마다 판사봉을 찍었는데 두 가지가 걸렸다. RECRUITING 은 `livesCount` 가 NULL 이라
     * 전부 회색으로 찍혀 「이미 다 잃었다」로 읽혔고, 값이 멀쩡할 때도 홈에서 그걸로 할 일이
     * 없는데 행 높이만 먹었다. 상세 화면이 그대로 보여주므로 사라지는 게 아니다.
     */
    const src = source(HOME);
    assert.doesNotMatch(src, /gc-group-row__lives/);
    assert.doesNotMatch(src, /gavelAlive|gavelDepleted/);
});

test('챌린지는 한 카드에 쌓지 않고 카드마다 띄운다', () => {
    /*
     * 구분선으로 쌓아 두었더니 서로 이어진 한 덩어리로 읽혔는데 실제로는 각각 독립된
     * 챌린지다. 목록 화면(GroupChallengeListView)도 카드를 띄워 나열하므로 gap 으로
     * 맞춘다 — 여기서 `+ .gc-group-row { border-top }` 이 되살아나면 두 화면이 어긋난다.
     */
    const src = source(HOME);
    assert.match(src, /\.gc-group-row \{[^}]*background: var\(--tt-bg\)/);
    assert.match(src, /\.gc-groups \{[^}]*gap: var\(--tt-space-3\)/);
    assert.doesNotMatch(src, /\.gc-group-row \+ \.gc-group-row/);
});

test('만들기·참여코드 두 진입로는 챌린지 카드와 섞지 않는다', () => {
    /* 카드를 띄운 뒤로 이 둘까지 각자 카드가 되면 「챌린지가 두 개 더 있다」로 읽힌다 */
    const src = source(HOME);
    assert.match(src, /class="gc-groups__actions"/);
    assert.match(src, /\.gc-groups__cta \+ \.gc-groups__cta \{[^}]*border-top/);
});

test('시작일이 지났는데 RECRUITING 인 그룹에 「시작 D-null」 을 찍지 않는다', () => {
    /*
     * 서버는 `daysUntilStart` 를 status 가 아니라 **시작일만 보고** 계산한다
     * (ChallengeGroupService.daysUntilStart — `today >= startDate` 면 NULL).
     * 그런데 RECRUITING → ACTIVE 전이는 하루 한 번 도는 배치라(cron `0 1 0 * * *`)
     * 그 시각에 서버가 꺼져 있으면 status 가 그대로 남는다. 로컬에서는 거의 항상 이 상태다.
     * → `시작 D-${daysUntilStart}` 를 무조건 끼우면 화면에 「시작 D-null」 이 뜬다.
     */
    assert.match(source(HOME), /daysUntilStart == null\s*\?\s*'시작 대기'/);
});

test('배치 cron 이 하루 한 번인 것을 전제로 둔다', () => {
    /*
     * 위 폴백의 근거다. 이 배치가 잦아지거나 없어지면 전제가 바뀌므로 같이 본다.
     * 값 자체를 고정하려는 게 아니라 **하루 한 번 도는 성격**이 유지되는지를 본다.
     */
    assert.match(
        source('../api/src/main/resources/application.properties'),
        /challenge\.group\.status-transition\.cron=/,
    );
});

test('카테고리 없는 그룹은 「기타」가 아니라 「총 소비」로 그린다', () => {
    /*
     * `categoryId` 가 NULL 인 그룹은 전체 소비를 합산하는 정상 선택지다(GroupCategoryPicker).
     * 그런데 `resolveCategoryIcon` 의 폴백 아이콘이 하필 「기타」 카테고리 아이콘과 **같은 모양**이라
     * 그대로 두면 뜻이 정반대로 읽힌다. 이름도 빼 버리면 카테고리 챌린지와 구별이 안 된다.
     */
    const src = source(HOME);
    assert.match(src, /challenge\.categoryName \? resolveCategoryIcon\(.+\) : 'Wallet'/);
    assert.match(src, /challenge\.categoryName \?\? GROUP_CATEGORY_ALL_LABEL/);
});

test('「총 소비」 표기는 만들기 화면과 목록이 같은 상수를 쓴다', async () => {
    /* 한쪽만 고치면 만들 때는 「총 소비」, 목록에서는 다른 말이 되는 어긋남이 조용히 생긴다 */
    const { GROUP_CATEGORY_ALL_LABEL } = await import('../src/utils/groupCategory.js');
    assert.equal(GROUP_CATEGORY_ALL_LABEL, '총 소비');
    assert.match(
        source('src/components/challenge/group/GroupCategoryPicker.vue'),
        /const ALL_LABEL = GROUP_CATEGORY_ALL_LABEL;/,
    );
    /* 「기타」 카테고리는 그룹챌린지에서 고를 수 없다 — 폴백이 그 이름으로 새면 안 된다 */
    const { GROUP_CATEGORY_NAMES } = await import('../src/utils/groupCategory.js');
    assert.ok(!GROUP_CATEGORY_NAMES.includes('기타'));
});

/* ── TO-DO 투표 진행바 ─────────────────────────────── */

const TODO_ITEM = 'src/components/challenge/group/GroupTodoItem.vue';

test('투표 건에는 진행바를 그리고 변론 건에는 그리지 않는다', () => {
    const src = source(TODO_ITEM);
    assert.match(src, /if \(isAccuse\.value \|\| !totalVoters\) return null;/);
    assert.match(src, /v-if="votePercent !== null"/);
});

test('투표 목데이터는 tally 와 함께 숫자도 갖는다', () => {
    /*
     * 진행바는 `tally`('3/5 투표') 문자열이 아니라 `voteCount`/`totalVoters` 를 읽는다.
     * 목데이터에 숫자가 없으면 목모드에서 바가 통째로 사라진다.
     */
    for (const item of MOCK_TODO_ITEMS.filter((i) => i.type === 'vote')) {
        assert.equal(typeof item.voteCount, 'number', `${item.title}: voteCount 가 없다`);
        assert.equal(typeof item.totalVoters, 'number', `${item.title}: totalVoters 가 없다`);
    }
});

/* ── 채팅 접근성 (이슈 #423) ─────────────────────────── */

/*
 * 서버는 채팅이 오면 알림 SSE 로 `chat` 이벤트를 쏘고 있었는데(ChatMessageService.pushChatAlert)
 * 프론트가 그 이름을 받는 분기를 두지 않아 **전부 버려지고 있었다.** 게다가 채팅 알림은
 * 알림함에 저장하지도 않으므로(팀 결정 2026-08-15) 놓치면 되찾을 방법이 없다.
 * 아래 셋은 그 경로가 다시 끊기는 것을 막는다.
 */

test('알림 스트림은 chat 이벤트를 받는다', () => {
    const src = source('src/stores/notification.js');
    assert.match(src, /event\.event === 'chat'/);
    /* 채팅은 알림함에 저장하지 않는다 — items 에 넣으면 새로고침 한 번에 사라져 목록이 거짓말을 한다 */
    assert.match(src, /useGroupChatStore\(\)\.receiveChatAlert/);
});

test('로그인하면 채팅 배지에도 씨를 뿌린다', () => {
    /*
     * 이걸 그룹 홈에만 두면 앱을 켜자마자 대법원에 있는 사람은 안 읽은 채팅이 쌓여 있어도
     * 지방법원 점을 못 본다. 종 배지(refreshBadge)와 같은 자리에서 같이 한다.
     */
    const src = source('src/App.vue');
    assert.match(src, /groupChat\.refreshChatSummary\(\)/);
    assert.match(src, /groupChat\.clearChatSummary\(\)/);
});

test('홈은 목록을 받을 때마다 채팅 요약을 걸러내기 전 값으로 정정한다', () => {
    /*
     * 서버는 메시지마다 SSE 를 쏘지만 **연결이 끊겨 있던 동안 온 것은 못 받는다.**
     * 이 한 줄이 빠지면 그 구간의 누락이 영영 정정되지 않아 배지가 계속 작게 뜬다.
     * (예전에는 서버 30초 쿨다운 때문에 상시로 어림값이었다 — 이슈 #423 에서 제거)
     *
     * **넘기는 값과 순서가 계약이다.** 카드에서 JUDGING 을 빼느라 `myChallenges.value` 를
     * 넘기면 배지가 다시 두 상태만 세게 되고, 그래도 조회 리터럴 테스트는 초록으로 지나간다.
     * 세 줄을 통째로 고정해 그 조합을 막는다.
     */
    assert.match(
        source(HOME),
        /const raw = await fetchMyGroupChallenges\([\s\S]*?\);[\s\S]*?groupChat\.syncChatSummary\(raw\);[\s\S]*?myChallenges\.value = raw\.filter\(/,
    );
});

test('대화가 있으면 메타 줄 대신 마지막 메시지를 그린다', () => {
    /* 둘을 함께 그리면 행이 한 줄 높아져 카드가 화면 밖으로 밀린다 */
    const src = source(HOME);
    assert.match(src, /v-if="chatLine\(ch\)"/);
    assert.match(src, /v-else class="gc-group-row__meta"/);
});

test('안 읽은 배지는 D-day 칩 아래에 여백을 두고 쌓는다', () => {
    /*
     * 배지를 대화 줄 옆에 두면 이름·메시지 길이에 따라 자리가 춤춘다. 오른쪽 열에
     * 칩과 세로로 쌓되 `gap` 으로 떼어 놓는다 — 붙여 두면 칩의 일부처럼 보인다.
     */
    const src = source(HOME);
    assert.match(
        src,
        /class="gc-group-row__aside">[\s\S]*?gc-group-row__chip[\s\S]*?gc-group-row__badge/,
    );
    assert.match(src, /\.gc-group-row__aside \{[^}]*flex-direction: column[^}]*gap:/);
});

test('대화 줄만 채팅방으로 보낸다 — 행 전체는 상세로 간다', () => {
    /* `.stop` 이 빠지면 부모 @click 까지 함께 타서 채팅방을 열자마자 상세로 새어 나간다 */
    assert.match(source(HOME), /@click\.stop="goToChat\(ch\)"/);
});

test('시작 전 그룹도 대화 줄을 그린다', () => {
    /*
     * 한때 `if (isUpcoming(challenge)) return null;` 로 걸러냈고 **그것 때문에 채팅이 안 보였다.**
     * `ChatMessageService` 에는 상태 검사가 없어 방을 만든 직후부터 대화가 되는데,
     * 서버가 시작일을 반드시 내일 이후로 막아서(validateCreate) 만들고 나서 처음 떠드는
     * 구간이 통째로 RECRUITING 이다 — 정작 대화가 가장 활발할 때가 전부 잘려 나갔다.
     */
    assert.doesNotMatch(
        source(HOME),
        /function chatLine\(challenge\) \{[\s\S]*?isUpcoming\(challenge\)[\s\S]*?\n\}/,
    );
});

test('채팅 배지 씨 뿌리기는 홈 목록과 같은 상태 조합을 쓴다', () => {
    /* 한쪽만 고치면 지방법원 점과 홈 배지가 서로 다른 방을 세게 된다 */
    assert.match(
        source('src/stores/groupChat.js'),
        /fetchMyGroupChallenges\(\['ACTIVE', 'RECRUITING', 'JUDGING'\]\)/,
    );
});

test('안 읽은 채팅 점은 지방법원 세그먼트에만 찍는다', () => {
    /*
     * 하단 5탭(TheTabBar)까지 번지면 「재판 탭 어딘가에 뭔가 있다」는 뜻이 되어
     * 개인 미션 때문인지 채팅 때문인지 구별할 수 없다.
     */
    assert.match(
        source('src/components/challenge/ChallengeModeTabBar.vue'),
        /v-if="hasUnreadChat"/,
    );
    assert.doesNotMatch(source('src/components/common/TheTabBar.vue'), /hasUnreadChat/);
});
