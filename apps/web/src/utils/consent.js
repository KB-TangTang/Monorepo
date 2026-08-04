/**
 * 동의 화면의 체크 상태 계산. 화면에서 떼어내 순수 함수로 둔다.
 *
 * 상태는 { [type]: boolean } 형태의 평평한 객체다.
 * 항목 목록(items)은 서버 카탈로그에서 받은 그대로를 쓴다 —
 * 필수 여부를 프론트에 하드코딩하면 서버 정의와 어긋난다.
 */

export function buildAgreementState(items) {
    return items.reduce((state, item) => {
        state[item.type] = false;
        return state;
    }, {});
}

export function toggleAll(items, checked) {
    return items.reduce((state, item) => {
        state[item.type] = checked;
        return state;
    }, {});
}

export function isAllChecked(items, state) {
    return items.every((item) => state[item.type] === true);
}

/** 제출 가능 조건: 필수 항목이 전부 체크. 선택 항목은 무관하다. */
export function canSubmit(items, state) {
    return items.filter((item) => item.required).every((item) => state[item.type] === true);
}

export function toAgreements(state) {
    return Object.entries(state).map(([type, agreed]) => ({ type, agreed }));
}
