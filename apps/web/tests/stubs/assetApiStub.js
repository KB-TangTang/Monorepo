/*
 * `./http` 대역(stub). api/asset.js 는 `@/api/http` 가 아니라 상대경로 `./http` 를 쓴다
 * (다른 api/* 파일들과의 패턴 차이는 fixedExpenseApiStub.js 참고 주석과 같은 이유 — axios·
 * import.meta.env 가 node --test 밖에서는 동작하지 않는다).
 */
export const calls = [];

const getResponses = [];

export function reset() {
    calls.length = 0;
    getResponses.length = 0;
}

export function setGetResponses(responses) {
    getResponses.push(...responses);
}

function nextResponse(responses) {
    const response = responses.shift();
    return response instanceof Error ? Promise.reject(response) : Promise.resolve(response);
}

export default {
    get: (...args) => {
        calls.push({ method: 'get', args });
        return nextResponse(getResponses);
    },
};
