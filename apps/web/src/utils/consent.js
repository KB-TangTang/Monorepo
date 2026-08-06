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

/**
 * 재동의 화면 진입 시 초기 체크 상태를 사용자의 기존 동의로 채운다.
 * (GET /api/consents/me 응답 items 를 그대로 넘기면 된다.)
 *
 * 기존 동의 행이 없거나(최초 사용자) 카탈로그 항목에 매칭되는 행이 없으면
 * 미체크(false) 로 시작한다 — 안전한 기본값이다. 이렇게 시드해두지 않으면
 * 재동의 시 선택 항목(AI_USAGE, MARKETING 등)이 사용자가 건드리지 않아도
 * 전부 미동의로 제출돼버린다(백엔드 규칙: agreed:true 로 안 온 항목은 미동의 처리).
 */
export function buildAgreementStateFromConsents(items, myConsents) {
    const agreedTypes = new Set(
        myConsents.filter((consent) => consent.agreed).map((consent) => consent.type),
    );
    return items.reduce((state, item) => {
        state[item.type] = agreedTypes.has(item.type);
        return state;
    }, {});
}

/**
 * 동의 관리 화면(ConsentManageView) 전용 헬퍼.
 *
 * GET /api/consents/me 의 MyConsentDto 에는 동의·철회 시각이 없다
 * (백엔드 필드: type/required/label/termsUrl/agreed/withdrawable/termsVersion/expiresAt).
 * 그래서 날짜 문구는 만료 예정일(expiresAt, FINANCIAL_DATA 동의 시에만 채워짐)만 쓴다.
 */

/** 토글 가능 여부. 동의 중이면서 철회 가능한 항목만 끌 수 있다. */
export function canWithdraw(item) {
    return Boolean(item?.agreed && item?.withdrawable);
}

/** 카드 하단에 보여줄 상태 문구. */
export function consentStatusText(item) {
    if (!item?.agreed) {
        return '철회함';
    }
    const parts = ['동의함'];
    if (item.termsVersion) {
        parts.push(item.termsVersion);
    }
    if (item.expiresAt) {
        parts.push(`만료 ${String(item.expiresAt).slice(0, 10)}`);
    }
    return parts.join(' · ');
}
