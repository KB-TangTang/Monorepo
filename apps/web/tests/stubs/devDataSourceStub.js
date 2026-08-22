/*
 * `@/services/devDataSource` 대역(stub).
 *
 * 진짜 모듈은 `import.meta.env.DEV` 를 읽어 Vite 밖(node --test)에서는 import 만 해도 터진다.
 * 테스트는 언제나 **실데이터 경로**를 봐야 하므로 목 토글을 끈 상태로 고정한다 —
 * 목 경로로 새면 서버 응답 모양을 검사하는 의미가 없어진다.
 */
export const isMockMode = { value: false };

export function setMockMode() {}
