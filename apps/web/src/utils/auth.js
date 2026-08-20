/**
 * 인증 흐름에서 "실패의 종류"를 가르는 판정 함수들.
 *
 * 부팅 시 세션 복원(main.js)은 실패를 모두 "비로그인"으로 처리하면 안 된다.
 * 백엔드가 내려가 있는 동안에도 로그인 화면이 뜨는데, 사용자 입장에서는
 * 멀쩡하던 세션이 갑자기 풀린 것처럼 보이고 구글 로그인을 다시 누르게 된다.
 * (그마저도 백엔드가 없으니 502 페이지로 떨어진다 — 2026-08-06 실제 발생)
 */

/**
 * 백엔드에 닿지 못한 실패인지 판정한다.
 *
 * `ApiError.status` 기준:
 *   0    — 네트워크 오류 · 타임아웃 (응답 자체가 없음)
 *   5xx  — 게이트웨이/서버 장애. 배포 중이면 Vercel 이 502 를 돌려준다.
 *   401  — 정상적인 "비로그인". 여기 해당하지 않는다.
 *
 * @param {{ status?: number }} error 정규화된 ApiError
 * @returns {boolean} 서버에 닿지 못했으면 true
 */
export function isBackendUnreachable(error) {
    const status = error?.status;
    if (typeof status !== 'number') {
        return false;
    }
    return status === 0 || status >= 500;
}

/*
 * 로그인 화면의 사건번호 조판. 화면에서 직접 만들지 않고 여기 두는 이유는
 * 월·일 0 채움을 빠뜨리면 `2026-소환-83` 같은 값이 그대로 나가기 때문이다 — 테스트로 막는다.
 */

/** 2자리 0 채움. `String.padStart` 를 매번 쓰면 조판 코드가 길어진다 */
function pad2(value) {
    return String(value).padStart(2, '0');
}

/**
 * 로그인 화면에 찍는 사건번호.
 *
 * 앱의 기존 사건번호 조판(`2026-재판-0619` · `2026-고정-0729`)과 같은 꼴을 쓴다.
 * 여기만 다른 모양을 쓰면 로그인 후 화면과 어휘가 어긋난다.
 *
 * @param {Date} date 기준 날짜
 * @returns {string} 예: `2026-소환-0820`
 */
export function toSummonsCaseNumber(date) {
    return `${date.getFullYear()}-소환-${pad2(date.getMonth() + 1)}${pad2(date.getDate())}`;
}
