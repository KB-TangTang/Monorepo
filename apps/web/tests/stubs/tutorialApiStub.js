/*
 * `@/api/tutorial` 대역(stub).
 *
 * 진짜 모듈은 `@/api/http` 를 거쳐 axios 와 `import.meta.env` 에 닿는데, 둘 다 Vite 밖(node --test)에서는
 * 동작하지 않는다(`import.meta.env` 가 undefined 라 import 만 해도 터진다).
 * 그래서 tutorialGuide 테스트는 이 파일을 대신 로드시킨다 (tests/tutorialGuide.test.js 의 resolve 훅 참고).
 *
 * 호출 기록과 실패 여부를 밖에서 조작할 수 있게 상태를 여기 둔다.
 */
export const calls = [];

/** 다음 호출부터 실패시킬 함수 이름들. 실패 경로(mark 는 삼키고 reset 은 던진다)를 검증할 때 쓴다. */
export const failing = new Set();

/** 각 호출이 돌려줄 사용자 정보. 테스트가 갈아끼운다. */
export let response = {};

export function setResponse(next) {
    response = next;
}

export function reset() {
    calls.length = 0;
    failing.clear();
    response = {};
}

async function call(name) {
    calls.push(name);
    if (failing.has(name)) {
        throw new Error(`${name} 실패`);
    }
    return response;
}

export function completeMainTutorial() {
    return call('completeMainTutorial');
}

export function resetMainTutorial() {
    return call('resetMainTutorial');
}

export function completeGroupTutorial() {
    return call('completeGroupTutorial');
}

export function resetGroupTutorial() {
    return call('resetGroupTutorial');
}
