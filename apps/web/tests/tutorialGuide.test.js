import test from 'node:test';
import assert from 'node:assert/strict';
import { registerHooks } from 'node:module';
import { createPinia, setActivePinia } from 'pinia';
import { useAuthStore } from '../src/stores/auth.js';

const STUB_URL = new URL('./stubs/tutorialApiStub.js', import.meta.url).href;

/*
 * tutorialGuide 는 `@/api/tutorial` 을 import 하고, 그 모듈은 `@/api/http`(axios + import.meta.env)를 탄다.
 * import.meta.env 는 Vite 밖에서 undefined 라 import 만으로 터지므로 대역으로 갈아끼운다.
 *
 * node:test 의 mock.module 은 아직 --experimental-test-module-mocks 플래그가 필요해
 * 팀 공용 스크립트(package.json 의 test)를 건드려야 한다. 대신 플래그가 필요 없는
 * module.registerHooks 로 이 파일 안에서만 해석 경로를 바꿨다.
 * 훅은 등록 이후 로드되는 모듈에만 적용되므로 대상 모듈은 동적 import 로 나중에 불러온다.
 */
registerHooks({
    resolve(specifier, context, nextResolve) {
        if (specifier === '@/api/tutorial') {
            return { url: STUB_URL, shortCircuit: true };
        }
        return nextResolve(specifier, context);
    },
});

const api = await import(STUB_URL);
const {
    hasSeenPersonalTutorial,
    hasSeenGroupTutorial,
    markPersonalTutorialSeen,
    markGroupTutorialSeen,
    resetPersonalTutorial,
    resetGroupTutorial,
} = await import('../src/services/tutorialGuide.js');

function setUser(user) {
    setActivePinia(createPinia());
    api.reset();
    const auth = useAuthStore();
    auth.setSession({ accessToken: 'token', user });
    return auth;
}

/* 실패 경로에서 나오는 console.warn 이 테스트 출력을 더럽히지 않게 잠시 막는다. */
async function silencingWarn(fn) {
    const original = console.warn;
    const warnings = [];
    console.warn = (...args) => warnings.push(args);
    try {
        await fn();
    } finally {
        console.warn = original;
    }
    return warnings;
}

test('tutorialSeenAt 이 있으면 개인 튜토리얼을 본 것이다', () => {
    setUser({ id: 1, tutorialSeenAt: '2026-08-11T09:30:00' });
    assert.equal(hasSeenPersonalTutorial(), true);
});

/*
 * 서버는 아직 안 본 사용자에게 null 을 주지만, 필드가 빠진 옛 응답에서는 undefined 가 온다.
 * 둘 다 "안 봄"이어야 한다 — undefined 를 "봤음"으로 읽으면 튜토리얼이 영영 안 뜬다.
 */
test('null·undefined 는 모두 아직 안 본 것으로 본다', () => {
    setUser({ id: 1, tutorialSeenAt: null, groupTutorialSeenAt: null });
    assert.equal(hasSeenPersonalTutorial(), false);
    assert.equal(hasSeenGroupTutorial(), false);

    setUser({ id: 1 });
    assert.equal(hasSeenPersonalTutorial(), false);
    assert.equal(hasSeenGroupTutorial(), false);
});

test('로그인 정보가 없으면 아직 안 본 것으로 본다', () => {
    setUser(null);
    assert.equal(hasSeenPersonalTutorial(), false);
    assert.equal(hasSeenGroupTutorial(), false);
});

/* 대법원(개인)과 지방법원(그룹)은 별개 튜토리얼이라 한쪽만 봐도 다른 쪽은 떠야 한다. */
test('개인·그룹 플래그는 서로 간섭하지 않는다', () => {
    setUser({ id: 1, tutorialSeenAt: '2026-08-11T09:30:00', groupTutorialSeenAt: null });
    assert.equal(hasSeenPersonalTutorial(), true);
    assert.equal(hasSeenGroupTutorial(), false);

    setUser({ id: 1, tutorialSeenAt: null, groupTutorialSeenAt: '2026-08-11T09:30:00' });
    assert.equal(hasSeenPersonalTutorial(), false);
    assert.equal(hasSeenGroupTutorial(), true);
});

test('개인 완료를 저장하면 응답이 스토어에 반영된다', async () => {
    const auth = setUser({ id: 1, tutorialSeenAt: null, groupTutorialSeenAt: null });
    api.setResponse({
        id: 1,
        tutorialSeenAt: '2026-08-11T10:00:00',
        groupTutorialSeenAt: null,
    });

    await markPersonalTutorialSeen();

    assert.deepEqual(api.calls, ['completeMainTutorial']);
    assert.equal(auth.user.tutorialSeenAt, '2026-08-11T10:00:00');
    /* 저장 직후 /users/me 를 다시 부르지 않아도 판정이 맞아야 한다. */
    assert.equal(hasSeenPersonalTutorial(), true);
    assert.equal(hasSeenGroupTutorial(), false);
});

test('그룹 완료를 저장하면 그룹 플래그만 바뀐다', async () => {
    const auth = setUser({ id: 1, tutorialSeenAt: null, groupTutorialSeenAt: null });
    api.setResponse({ id: 1, tutorialSeenAt: null, groupTutorialSeenAt: '2026-08-11T10:00:00' });

    await markGroupTutorialSeen();

    assert.deepEqual(api.calls, ['completeGroupTutorial']);
    assert.equal(hasSeenGroupTutorial(), true);
    assert.equal(hasSeenPersonalTutorial(), false);
    assert.equal(auth.user.id, 1);
});

/*
 * 완료 저장 실패는 사용자에게 알릴 일이 아니다 — 튜토리얼을 다 본 사람에게 오류를 띄울 이유가 없고,
 * 최악의 결과는 다음에 한 번 더 뜨는 것뿐이다. 그래서 예외를 삼키고 경고만 남긴다.
 */
test('완료 저장이 실패해도 예외를 던지지 않는다', async () => {
    setUser({ id: 1, tutorialSeenAt: null });
    api.failing.add('completeMainTutorial');
    api.failing.add('completeGroupTutorial');

    const warnings = await silencingWarn(async () => {
        await markPersonalTutorialSeen();
        await markGroupTutorialSeen();
    });

    assert.equal(warnings.length, 2);
    /* 실패했으니 플래그도 그대로여야 한다 — 다음 진입에서 다시 뜬다. */
    assert.equal(hasSeenPersonalTutorial(), false);
    assert.equal(hasSeenGroupTutorial(), false);
});

test('다시 보기는 플래그를 되돌린다', async () => {
    const auth = setUser({
        id: 1,
        tutorialSeenAt: '2026-08-11T10:00:00',
        groupTutorialSeenAt: '2026-08-11T10:00:00',
    });

    api.setResponse({ id: 1, tutorialSeenAt: null, groupTutorialSeenAt: '2026-08-11T10:00:00' });
    await resetPersonalTutorial();
    assert.deepEqual(api.calls, ['resetMainTutorial']);
    assert.equal(hasSeenPersonalTutorial(), false);
    assert.equal(hasSeenGroupTutorial(), true);

    api.setResponse({ id: 1, tutorialSeenAt: null, groupTutorialSeenAt: null });
    await resetGroupTutorial();
    assert.deepEqual(api.calls, ['resetMainTutorial', 'resetGroupTutorial']);
    assert.equal(hasSeenGroupTutorial(), false);
    assert.equal(auth.user.id, 1);
});

/*
 * 반대로 다시 보기는 사용자가 방금 누른 버튼이다. 조용히 실패하면 튜토리얼이 안 뜨는 화면으로
 * 이동해 고장으로 보이므로 예외를 그대로 던져 화면이 알리게 한다.
 */
test('다시 보기 실패는 호출부로 예외를 던진다', async () => {
    setUser({ id: 1, tutorialSeenAt: '2026-08-11T10:00:00' });
    api.failing.add('resetMainTutorial');
    api.failing.add('resetGroupTutorial');

    await assert.rejects(() => resetPersonalTutorial(), /resetMainTutorial 실패/);
    await assert.rejects(() => resetGroupTutorial(), /resetGroupTutorial 실패/);
    /* 실패했으면 스토어도 건드리지 않는다. */
    assert.equal(hasSeenPersonalTutorial(), true);
});
