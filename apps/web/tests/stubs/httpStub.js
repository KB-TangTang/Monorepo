/*
 * `@/api/http` 대역(stub).
 *
 * 진짜 모듈은 axios 와 `import.meta.env` 에 닿는데 둘 다 Vite 밖(node --test)에서는 동작하지 않는다
 * (`import.meta.env` 가 undefined 라 import 만 해도 터진다).
 * 호출 인자를 그대로 기록해 두고 테스트가 검사한다.
 */
export const calls = [];

export function reset() {
    calls.length = 0;
}

const http = {
    post: (...args) => {
        calls.push({ method: 'post', args });
        return Promise.resolve({});
    },
    delete: (...args) => {
        calls.push({ method: 'delete', args });
        return Promise.resolve({});
    },
    patch: (...args) => {
        calls.push({ method: 'patch', args });
        return Promise.resolve({});
    },
};

export default http;
