/*
 * 그룹챌린지 화면의 복귀 규칙 (이슈 #303).
 *
 * 상세는 진입로가 둘이다 — 그룹챌린지 **홈**의 진행 중 카드와 **목록**의 카드. 그런데
 * 뒤로가기가 홈으로 하드코딩돼 있어 목록에서 들어와도 홈으로 튕겼다. 초대 화면도 같다
 * (상세·목록·생성완료 셋에서 들어오는데 언제나 홈으로 나갔다).
 *
 * **그렇다고 `router.back()` 으로 바꾸면 안 된다.** 판결 플로우 화면들이 상세로 돌아올 때
 * `router.replace` 를 쓰는데(이슈 #172), 그러면 히스토리가 이렇게 남는다.
 *
 *     [홈, 상세, 재판현황, 투표, 상세]   ← 마지막 상세는 「투표완료」 자리를 replace 한 것
 *                              ↑ back() 하면 여기(투표 화면)로 되돌아간다
 *
 * 그래서 **「바로 앞이 내가 온 곳이라고 확인된 경우에만」** back 을 쓴다. 그 확인은 진입시키는
 * 쪽이 `history.state` 에 남긴다 — push 하는 그 순간에만 앞 항목이 진입 화면이라고 단정할 수
 * 있기 때문이다. replace 로 들어온 판결 플로우와 생성 완료 플로우는 이 표시가 없어 종전대로
 * 지정된 화면으로 나간다.
 *
 * 쿼리스트링(`?from=list`)을 쓰지 않은 이유는 주소가 지저분해지는 것도 있지만, 사용자가 그 URL 을
 * 복사해 새 탭에서 열면 **앞 항목이 없는데도 back 을 시도하게** 되기 때문이다. history.state 는
 * 주소를 복사해도 따라오지 않아 그 경우 자동으로 기본값으로 떨어진다.
 */

/** 진입 표시 키. 다른 history.state 키(`ttOverlay` 등)와 섞이지 않게 접두를 붙인다 */
export const BACK_STATE_KEY = 'ttBackTo';

/**
 * 진입할 때 실어 보낼 history state.
 *
 * **`push` 에만 쓴다.** `replace` 로 들어가면서 이 표시를 남기면 앞 항목이 진입 화면이 아닌데도
 * back 을 하게 돼, 고치려던 그 증상(방금 본 재판 화면으로 되돌아감)이 그대로 재현된다.
 *
 * @param routeName 지금 화면(=돌아올 곳)의 라우트 이름
 */
export function entryState(routeName) {
    return { [BACK_STATE_KEY]: routeName };
}

/**
 * 뒤로가기가 무엇을 해야 하는지 정한다.
 *
 * @param historyState `window.history.state` (없을 수 있다)
 * @param fallbackName 표시가 없을 때 이동할 라우트 이름
 * @returns `{ type: 'back' }` 이면 히스토리를 한 칸 되돌린다 — 스크롤 위치까지 복원된다.
 *          `{ type: 'route', name }` 이면 그 화면으로 이동한다
 */
export function resolveBack(historyState, fallbackName) {
    const entry = historyState?.[BACK_STATE_KEY];

    /* 값의 내용은 보지 않는다. 이 키를 넣는 곳이 entryState 하나뿐이라 「있다」가 곧 「push 로 들어왔다」다 */
    if (typeof entry === 'string' && entry.length > 0) {
        return { type: 'back' };
    }
    return { type: 'route', name: fallbackName };
}
