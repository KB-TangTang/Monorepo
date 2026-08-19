import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { registerHooks } from 'node:module';
import { createPinia, setActivePinia } from 'pinia';

/*
 * 맞춤 미션 개시 안내(이슈 #129, PR #311). 시트는 개인 미션 홈에 붙어 동작한다.
 *
 * 영속 상태는 서버가 쥔다 - tbl_user.personal_mission_unlock_status 가
 * UNTRACKED → INSUFFICIENT → PENDING → SEEN 으로만 움직이고,
 * showUnlock 은 PENDING 일 때만 true 다. 자격 판정도 서버가 한다(이슈 #315 (1)).
 *
 * 그래서 검증할 것은 두 가지다.
 *   1) store 가 서버 상태를 그대로 받아 노출 여부를 정하는가 (아래 store 전이 테스트)
 *   2) 화면이 그 결과를 안전하게 다루는가 (#313 · #314 소스 검사)
 *
 * 프론트에 있던 순수 규칙 shouldShowPersonalMissionUnlock 은 지웠다(이슈 #315 (3)).
 * 아무 실행 경로도 타지 않는데 테스트만 그것을 붙들고 있어, 정작 진짜 전이는 무검증이었다.
 */

/*
 * store 는 `@/api/personalMission`(→ axios + import.meta.env)과 탕이 png 를 끌고 온다.
 * Vite 밖(node --test)에서는 둘 다 죽으므로 이 파일 안에서만 대역으로 갈아끼운다
 * (tests/groupChatStore.test.js 와 같은 방식).
 */
const API_STUB_URL = new URL('./stubs/personalMissionApiStub.js', import.meta.url).href;
const IMAGE_STUB_URL = new URL('./stubs/imageStub.js', import.meta.url).href;
const HTTP_STUB_URL = new URL('./stubs/httpStub.js', import.meta.url).href;

registerHooks({
    resolve(specifier, context, nextResolve) {
        if (specifier === '@/api/personalMission') {
            return { url: API_STUB_URL, shortCircuit: true };
        }
        /* store 는 @/api/user 도 끌고 오는데 그쪽은 진짜 http.js 로 내려간다. */
        if (specifier === '@/api/http') {
            return { url: HTTP_STUB_URL, shortCircuit: true };
        }
        if (specifier.endsWith('.png')) {
            return { url: IMAGE_STUB_URL, shortCircuit: true };
        }
        return nextResolve(specifier, context);
    },
});

const apiStub = await import(API_STUB_URL);
const { usePersonalMissionChallengeStore } = await import('../src/stores/personalMission.js');
const { CHALLENGE_CONSENT_STATE } = await import('../src/services/challengeConsent.js');

/*
 * categoryAnalysis 가 hasEnoughData 의 원본이다. null 이면 목 프로필로 떨어져 항상 true 다.
 * consentState 는 화면에 도달한 사용자(동의함)를 가정한다 - 없으면 screenState 가 'loading' 이다.
 */
function newStore({ relativeEligible } = { relativeEligible: true }) {
    setActivePinia(createPinia());
    apiStub.reset();
    const store = usePersonalMissionChallengeStore();
    store.consentState = CHALLENGE_CONSENT_STATE.ACTIVE;
    store.categoryAnalysis = { relativeEligible, topCategories: [] };
    return store;
}

/* ── 서버 상태 기반 전이 (이슈 #315 (3)) ────────────────────── */

test('동기화는 자격 판정을 서버에 맡긴다 - 보내는 인자가 없다', async () => {
    const store = newStore();
    apiStub.setSyncResponse({ status: 'INSUFFICIENT', showUnlock: false });

    await store.syncMissionUnlock();

    assert.equal(apiStub.syncCalls.length, 1);
    assert.deepEqual(
        apiStub.syncCalls[0],
        [],
        '클라이언트가 충족 여부를 보내면 그것을 위조해 안내를 열 수 있다',
    );
});

test('서버가 INSUFFICIENT 를 주면 안내를 띄우지 않는다', async () => {
    const store = newStore({ relativeEligible: false });
    apiStub.setSyncResponse({ status: 'INSUFFICIENT', showUnlock: false });

    assert.equal(await store.syncMissionUnlock(), false);
    assert.equal(store.missionUnlockStatus, 'INSUFFICIENT');
});

test('서버가 PENDING 을 주면 안내를 띄운다', async () => {
    const store = newStore();
    apiStub.setSyncResponse({ status: 'PENDING', showUnlock: true });

    assert.equal(await store.syncMissionUnlock(), true);
    assert.equal(store.missionUnlockStatus, 'PENDING');
});

test('UNTRACKED 는 안내 대상이 아니다 - 기능 도입 전부터 자격이 있던 사용자', async () => {
    const store = newStore();
    apiStub.setSyncResponse({ status: 'UNTRACKED', showUnlock: false });

    assert.equal(await store.syncMissionUnlock(), false);
    assert.equal(store.missionUnlockStatus, 'UNTRACKED');
});

test('확인하면 SEEN 이 되고 다시 뜨지 않는다', async () => {
    const store = newStore();
    apiStub.setSyncResponse({ status: 'PENDING', showUnlock: true });
    assert.equal(await store.syncMissionUnlock(), true);

    await store.acknowledgeMissionUnlock();
    assert.equal(store.missionUnlockStatus, 'SEEN');

    apiStub.setSyncResponse({ status: 'SEEN', showUnlock: false });
    assert.equal(await store.syncMissionUnlock(), false);
});

/*
 * 이슈 #315 (2) - 서버 상태와 화면 상태가 어긋나는 구간.
 *
 * 자격 래치(relative_mission_qualified_at)는 한 번 박히면 안 풀려 서버는 PENDING 을 유지하는데,
 * 최근 28일 소비가 비면 relativeEligible 이 false 로 내려와 화면은 「증거 부족」이 된다.
 * 그 위에 「맞춤 미션이 열렸어요」를 겹쳐 띄우면 두 화면이 정면으로 모순된다.
 */
test('증거 부족 화면에서는 PENDING 이어도 안내를 겹쳐 띄우지 않는다', async () => {
    const store = newStore({ relativeEligible: false });
    apiStub.setSyncResponse({ status: 'PENDING', showUnlock: true });

    assert.equal(await store.syncMissionUnlock(), false);
    assert.equal(store.screenState, 'insufficient');
});

test('띄우지 않은 안내는 버리지 않고 미룬다 - 데이터가 다시 차면 그때 뜬다', async () => {
    const store = newStore({ relativeEligible: false });
    apiStub.setSyncResponse({ status: 'PENDING', showUnlock: true });
    await store.syncMissionUnlock();

    assert.equal(store.missionUnlockStatus, 'PENDING', '서버 상태를 SEEN 으로 소비하면 안 된다');

    store.categoryAnalysis = { relativeEligible: true, topCategories: [] };
    assert.equal(await store.syncMissionUnlock(), true);
});

/* ── 지운 규칙이 되살아나지 않는지 (이슈 #315 (3)) ──────────── */

test('노출 판단은 store 한 곳에만 있다 - 프론트에 병행 규칙을 두지 않는다', () => {
    const flow = readFileSync(
        new URL('../src/services/personalMissionFlow.js', import.meta.url),
        'utf8',
    );

    assert.doesNotMatch(
        flow,
        /export function shouldShowPersonalMissionUnlock/,
        '서버가 게이트를 쥐고 있다 - 프론트에 두 번째 판단 규칙이 생기면 둘이 갈린다',
    );
});

/*
 * ── #311 후속 (이슈 #313 · #314) ──────────────────────────────────
 *
 * 렌더링 하네스가 없어 소스를 검사한다. 두 결함 모두 "코드가 그 자리에 있는가" 로 잡히는 종류다.
 */

function homeSource() {
    return readFileSync(
        new URL('../src/views/challenge/personal/PersonalMissionHomeView.vue', import.meta.url),
        'utf8',
    );
}

/*
 * #313 — 데이터 부족 화면이 고정 픽스처를 실사용자에게 보여줬다.
 *
 * hasEnoughData 가 목 프로필(항상 true)을 보던 시절엔 이 화면이 도달 불가능한 분기였는데,
 * #311 이 서버의 relativeEligible 을 보게 되면서 열렸다. 그 순간부터 MOCK_DATA_REQUIREMENTS 의
 * 「19일째」·「12 / 50」이 자기 데이터인 줄 알고 읽히게 됐다.
 */
test('개인 미션 홈은 데이터 요건 목데이터를 화면에 그리지 않는다', () => {
    const src = homeSource();

    assert.doesNotMatch(
        src,
        /store\.dataRequirements/,
        'dataRequirements 는 MOCK_DATA_REQUIREMENTS 고정값이다 - 서버가 주지 않는 진행도를 지어내지 않는다',
    );
    assert.doesNotMatch(src, /calculateDataProgress/, '진행 막대도 같은 목데이터로 계산된다');
});

test('맞춤 사건이 열리는 조건은 값 없이 조건만 보여준다', () => {
    const src = homeSource();

    assert.match(src, /unlockConditions/, '조건 목록이 있어야 한다');
    assert.match(src, /전체 소비 50건 확보/, '조건 문구 자체는 남긴다');
});

/*
 * #314 — 안내 API 하나가 페이지 전체를 지웠다.
 *
 * 진입 경로의 바깥 try 는 실패 시 consentState='ERROR' 로 StateError 를 띄운다.
 * 동의 경로의 catch 는 「동의를 저장하지 못했어요」를 띄운다 - 동의는 이미 저장된 뒤인데도.
 * 그래서 동기화는 자기 자리에서 실패를 삼켜야 한다.
 */
test('맞춤 미션 안내 동기화는 실패를 삼켜 페이지를 지우지 않는다', () => {
    const src = homeSource();

    const helper = src.match(/async function syncMissionUnlockQuietly\(\)\s*\{[\s\S]*?\n\}/);
    assert.ok(helper, 'syncMissionUnlockQuietly 헬퍼가 없다');
    assert.match(helper[0], /try\s*\{/, '헬퍼가 자기 try/catch 를 가져야 한다');
    assert.match(helper[0], /catch/, '실패를 삼키는 catch 가 있어야 한다');
    assert.match(
        helper[0],
        /hasPendingMissionUnlock\.value = false/,
        '실패하면 안내를 띄우지 않는 쪽으로 확정해야 한다',
    );
});

test('안내 동기화를 store 에서 직접 부르는 곳이 남아 있지 않다', () => {
    const src = homeSource();

    const directCalls = src.match(/await store\.syncMissionUnlock\(\)/g) ?? [];
    assert.equal(
        directCalls.length,
        1,
        '헬퍼 안의 한 번만 남아야 한다 - 바깥에서 직접 부르면 그 자리의 catch 가 다시 페이지를 지운다',
    );
});
