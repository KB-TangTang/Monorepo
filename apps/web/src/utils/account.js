/**
 * 계좌 연동의 도메인 상수와 순수 로직 (이슈 #12).
 *
 * 뷰에서 분리해 둔 이유는 두 가지다.
 *   1. 컴포넌트를 마운트하지 않고 `node --test` 로 검증할 수 있어야 한다.
 *   2. 단계 구성(LINK_STEPS)이 바뀔 가능성이 큰데, 뷰마다 흩어져 있으면 한 곳에서 못 고친다.
 *
 * ⚠ **이 파일에는 `@/` 별칭 import 를 쓰지 않는다.** 별칭은 Vite 가 푸는 것이라
 * `node --test` 는 해석하지 못해 테스트 파일이 통째로 죽는다. 도메인 상수를 여기 둔 이유도 그것이다.
 */

/**
 * 기관 분류 라벨. 화면 섹션 제목과 순서를 정한다.
 * key 는 백엔드 `GET /accounts/institutions` 응답의 필드명과 같아야 한다.
 */
export const INSTITUTION_GROUPS = [
    { key: 'banks', label: '은행' },
    { key: 'cards', label: '카드' },
    { key: 'securities', label: '증권' },
    { key: 'loans', label: '대출' },
    { key: 'payMoney', label: '페이머니' },
];

/** 계좌 종류. 목서버가 은행 자산을 이 두 값으로 구분한다. */
export const ACCOUNT_TYPE = {
    DEMAND_DEPOSIT: 'DEMAND_DEPOSIT',
    SAVINGS: 'SAVINGS',
};

/** 동기화 상태 5종. tbl_connected_account.sync_status 와 같은 값이다. */
export const SYNC_STATUS = {
    NORMAL: 'NORMAL',
    SYNCING: 'SYNCING',
    NEED_SYNC: 'NEED_SYNC',
    FAILED: 'FAILED',
    NEED_RECONNECT: 'NEED_RECONNECT',
};

/**
 * 인증 수단 종류. 백엔드 auth-methods 응답의 type 값과 같다.
 *
 * ⚠ **프론트는 자기가 목 모드인지 CODEF 모드인지 몰라야 한다.**
 * 어느 수단을 쓰는지는 서버가 내려주는 목록이 정한다. 여기에 'mock'·'codef' 같은 값을 두면
 * "설정 교체만으로 전환 가능"이라는 전제가 깨진다. (DECISIONS.md 2026-08-05)
 */
export const AUTH_METHOD = {
    SIMPLE_AUTH: 'SIMPLE_AUTH',
    INSTITUTION_LOGIN: 'INSTITUTION_LOGIN',
};

/** 기관별 조회 진행 상태. 백엔드 progress 응답의 status 값과 같다. */
export const PROGRESS_STATUS = {
    WAITING: 'WAITING',
    FETCHING: 'FETCHING',
    DONE: 'DONE',
    FAILED: 'FAILED',
};

/**
 * 연결 플로우 단계 순서.
 *
 * 'auth' 는 수단 중립적인 이름이다 — 목 모드는 간편인증, CODEF 모드는 기관 로그인이
 * **같은 자리**에 들어가고 나머지 단계는 공유한다. 화면이 갈리는 곳은 이 단계 하나뿐이다.
 * 뷰는 서로를 router.push 로 직접 부르지 않고 store 의 goNextStep()/goPrevStep() 만 쓴다.
 * (DECISIONS.md 2026-08-05 계좌 연동 항목)
 */
export const LINK_STEPS = ['institutions', 'auth', 'progress', 'select', 'done'];

/** 단계 → 라우트 이름. 라우터에 등록된 name 과 짝이 맞아야 한다. */
export const LINK_STEP_ROUTES = {
    institutions: 'accountLinkInstitutions',
    auth: 'accountLinkAuth',
    progress: 'accountLinkProgress',
    select: 'accountLinkSelect',
    done: 'accountLinkDone',
};

/** 다음 단계. 마지막 단계면 null 을 돌려준다. */
export function nextLinkStep(current) {
    const index = LINK_STEPS.indexOf(current);
    if (index === -1 || index === LINK_STEPS.length - 1) {
        return null;
    }
    return LINK_STEPS[index + 1];
}

/** 이전 단계. 첫 단계면 null 을 돌려준다. */
export function prevLinkStep(current) {
    const index = LINK_STEPS.indexOf(current);
    if (index <= 0) {
        return null;
    }
    return LINK_STEPS[index - 1];
}

/**
 * 진입점을 모를 때 플로우를 빠져나갈 **기본** 착지점.
 *
 * 연결 계좌 관리는 이 플로우의 실질적인 출발지다 — `기관 추가`·`재연동` 모두 거기서 들어온다.
 * 어디서 들어왔는지 기록이 없을 때(새로고침·주소 직접 입력·온보딩 강제 이동) 여기로 내보낸다.
 * 기록이 있으면 그쪽이 우선이다 — `linkExitRoute()` 참고.
 */
export const LINK_EXIT_ROUTE = 'connectedAccounts';

/** 연결 플로우 5단계에 속한 라우트인지. 진입점 기록 판정이 이 함수 하나만 본다. */
export function isLinkFlowRoute(routeName) {
    return Object.values(LINK_STEP_ROUTES).includes(routeName);
}

/**
 * 진입점으로 **기록하면 안 되는** 라우트.
 *
 * 온보딩(금융 동의 → 기관 선택)은 강제 단계다. 여기를 진입점으로 기록해두면 플로우에서 나갈 때
 * 동의 화면으로 되돌아가는데, 온보딩 게이트(resolveOnboardingRedirect)가 곧바로 다시 앞으로 보내
 * 두 화면 사이를 왕복한다. 기록하지 않으면 기본 착지점으로 나가고, 그다음 갈 곳은 게이트가 정한다.
 * (라우트 이름을 문자열로 적는 이유: utils/user.js 가 이 파일을 import 하므로 역참조하면 순환이 된다)
 */
const LINK_ENTRY_IGNORED_ROUTES = ['financialConsent'];

/**
 * 플로우 **밖**에서 첫 단계로 들어올 때 기록할 진입점 라우트 이름. 기록하지 않아야 하면 `null`.
 *
 * 라우터 가드가 이 결과만 보고 스토어에 기록한다 — 가드 본문에 조건을 늘어놓지 말 것.
 * 진입 화면(연결 계좌 관리·재연동·개인챌린지 …)을 일일이 고치지 않아도 새 진입점이 자동으로 잡힌다.
 *
 * 기록하지 않는 경우는 셋이다.
 *   - `redirected` — 가드가 되돌려보낸 진입이다. 사용자가 그 화면에서 온 게 아니라
 *     온보딩 게이트·단계 가드가 강제로 보낸 것이라 "돌아갈 곳"이 될 수 없다.
 *   - `fromName` 이 없다 — 새로고침·주소 직접 진입. 돌아갈 화면 자체가 없다.
 *   - 플로우 내부에서 왔거나(단계 이동일 뿐이다) 기록 금지 목록에 있다.
 *
 * @param {string|undefined} fromName 직전 라우트 이름
 * @param {{redirected?: boolean}} [options] 가드 리다이렉트로 도착했는지
 * @returns {string|null}
 */
export function resolveLinkEntryRoute(fromName, { redirected = false } = {}) {
    if (redirected || !fromName) {
        return null;
    }
    if (isLinkFlowRoute(fromName) || LINK_ENTRY_IGNORED_ROUTES.includes(fromName)) {
        return null;
    }
    return fromName;
}

/**
 * 플로우를 완전히 빠져나갈 때 갈 화면. 기록된 진입점이 있으면 그곳, 없으면 기본 착지점.
 * 완료 화면의 뒤로가기와 첫 단계의 "뒤로"가 같은 곳으로 나가야 해서 판정을 여기 하나로 둔다.
 */
export function linkExitRoute(entryRoute) {
    return entryRoute || LINK_EXIT_ROUTE;
}

/**
 * "뒤로"를 눌렀을 때 어디로 갈지 판정한다.
 *
 * ⚠ 첫 단계에서 `router.back()` 을 쓰면 안 된다.
 * `restartFlow()` 로 1단계에 되돌아온 경우 히스토리 **뒤쪽에 auth/progress/select 가 남아 있는데**,
 * 그 화면들은 `meta.linkStep` 가드가 붙어 있고 restartFlow 가 상태를 비워둔 터라
 * `canEnterLinkStep` 이 false → 가드가 곧바로 institutions 로 되돌려보낸다.
 * 결과적으로 뒤로가기를 눌러도 **화면이 그대로인 것처럼 보인다**(2026-08-06 실제 발생).
 * 그래서 히스토리에 기대지 않고 나갈 곳을 명시한다.
 *
 * @param {string} current 지금 단계
 * @param {string} [entryRoute] 플로우에 들어온 화면(스토어가 기록한다). 없으면 기본 착지점으로 나간다
 * @returns {{type: 'step', step: string} | {type: 'route', name: string}}
 */
export function prevLinkDestination(current, entryRoute) {
    const step = prevLinkStep(current);
    return step ? { type: 'step', step } : { type: 'route', name: linkExitRoute(entryRoute) };
}

/** 진행 표시용. 1부터 센다. */
export function linkStepPosition(current) {
    const index = LINK_STEPS.indexOf(current);
    return { current: index + 1, total: LINK_STEPS.length };
}

/**
 * 그 단계에 들어갈 자격이 있는지. 라우터 가드가 쓴다.
 *
 * 북마크·새로고침으로 중간 단계에 바로 들어오면 이전 단계 결과가 없어 빈 화면이 된다.
 * 스토어를 직접 읽지 않고 값만 받는 순수 함수라 마운트 없이 검증할 수 있다.
 *
 * @param {string} step 검사할 단계
 * @param {{selectedCount: number, hasConnection: boolean, linkedCount: number, progressDone: boolean,
 *          directAssetsPending?: boolean}} state
 */
export function canEnterLinkStep(step, state) {
    switch (step) {
        case 'auth':
            return state.selectedCount > 0;
        case 'progress':
            return state.hasConnection;
        case 'select':
            /* 계좌 목록은 인증이 아니라 **조회**가 끝나야 생긴다. */
            return state.hasConnection && state.progressDone === true;
        case 'done':
            /*
             * 은행 계좌 없이 대출·페이머니만 고르면 linkedCount 는 0 그대로다(#334) — 그 업권은
             * link() 가 아니라 완료 화면의 최초 동기화가 저장한다. directAssetsPending 이 그 사실을
             * 대신 말해준다.
             */
            return state.linkedCount > 0 || state.directAssetsPending === true;
        default:
            return true;
    }
}

/**
 * 인증 수단 목록 → 어떤 화면을 보여줄지.
 *
 * 선택지가 하나뿐인 선택 화면은 헛걸음이라 건너뛰고 바로 그 수단의 패널로 들어간다.
 * @param {Array<{type: string}>} methods auth-methods 응답의 methods
 * @returns {{needsPicker: boolean, method: string|null}}
 */
export function resolveAuthView(methods) {
    const list = Array.isArray(methods) ? methods : [];
    if (list.length === 1) {
        return { needsPicker: false, method: list[0].type };
    }
    return { needsPicker: list.length > 1, method: null };
}

/**
 * 기관별 조회 상태 → 진행률(%).
 * 실패도 '끝난 것'으로 센다 — 한 기관이 실패했다고 진행률이 멈춰 있으면 화면이 죽은 것처럼 보인다.
 */
export function calcLinkProgress(institutions) {
    const list = Array.isArray(institutions) ? institutions : [];
    if (!list.length) {
        return 0;
    }
    const finished = list.filter(
        (item) => item.status === PROGRESS_STATUS.DONE || item.status === PROGRESS_STATUS.FAILED,
    ).length;
    return Math.floor((finished / list.length) * 100);
}

/** 진행 상태 → 아이콘·라벨·색. 모르는 값은 대기로 본다. */
export function resolveProgressRow(status) {
    switch (status) {
        case PROGRESS_STATUS.DONE:
            return { icon: '✓', label: '완료', variant: 'innocent' };
        case PROGRESS_STATUS.FETCHING:
            return { icon: '⟳', label: '조회중', variant: 'progress' };
        case PROGRESS_STATUS.FAILED:
            return { icon: '✕', label: '실패', variant: 'guilty' };
        default:
            return { icon: '·', label: '대기', variant: 'default' };
    }
}

/**
 * 폴링 제한 시간을 넘겼는지.
 *
 * ⚠ 기본값은 **서버가 주는 인증 유효시간(5분)** 과 같아야 한다.
 *   예전에는 60초로 고정돼 있어, 화면은 5분을 세는데 폴링은 1분에 멈췄다 —
 *   사용자가 앱에서 승인해도 화면이 반응하지 않는 정지 상태가 됐다.
 *   호출부는 서버가 준 expiresInSeconds 를 넘긴다.
 *
 * @param {Date|string|number} startedAt 폴링 시작 시각
 * @param {Date|string|number} [now] 테스트에서 기준 시각을 고정하기 위한 인자
 */
export function isPollExpired(startedAt, now = new Date(), limitSeconds = 300) {
    const start = new Date(startedAt).getTime();
    if (Number.isNaN(start)) {
        /* 시작 시각을 모르면 계속 도는 것보다 만료로 보는 편이 안전하다. */
        return true;
    }
    return (new Date(now).getTime() - start) / 1000 >= limitSeconds;
}

/**
 * 휴대폰번호 입력 포맷.
 *
 * 자리수에 맞춰 하이픈을 넣어준다 — 플레이스홀더가 `010-0000-0000` 인데 입력은 자유형이면
 * 화면이 스스로 말한 형식을 지키지 않는 셈이다. 사용자가 하이픈을 치든 말든 결과가 같아진다.
 *
 * 숫자만 남기고 11자리에서 자른다.
 * **`010` 으로 시작하면 3-4-4 로 고정**한다 — 타이핑 도중 10자리를 지날 때 하이픈이 한 칸 튀지 않는다.
 * 그 외(011·016 같은 구 번호)는 10자리 3-3-4 규칙을 쓴다.
 */
export function formatPhoneNumber(value) {
    const digits = String(value ?? '')
        .replace(/\D/g, '')
        .slice(0, 11);
    if (digits.length <= 3) {
        return digits;
    }
    if (digits.length <= 7) {
        return `${digits.slice(0, 3)}-${digits.slice(3)}`;
    }
    if (digits.startsWith('010') || digits.length > 10) {
        return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`;
    }
    return `${digits.slice(0, 3)}-${digits.slice(3, 6)}-${digits.slice(6)}`;
}

/** 생년월일 입력 포맷. 숫자 6자리만 남긴다 — 화면이 "6자리"를 요구하므로 그 이상은 받지 않는다. */
export function formatBirthDate(value) {
    return String(value ?? '')
        .replace(/\D/g, '')
        .slice(0, 6);
}

/**
 * 간편인증 입력 형식 검증.
 *
 * ⚠ **형식만 본다.** 목 모드에는 대조할 인증기관이 없다.
 * 생년월일·통신사·휴대폰은 서버로 보내지 않고 저장하지도 않는다.
 * 실제 마이데이터에서 이 값들은 "누구에게 인증 푸시를 보낼지"를 정하는 용도이고,
 * 우리 목 모드에는 보낼 곳이 없다. (DECISIONS.md 2026-08-05)
 *
 * ⚠ **이름만은 예외다 — `tbl_user.name` 에 저장한다.**
 * 구글 계정 이름이 실명과 달라 인증에 실패하는 사용자가 화면에서 바로 고칠 수 있어야 하고,
 * 고친 값은 `PATCH /api/users/me/name` 으로 남는다. 매번 다시 입력하게 만들지 않기 위해서다.
 * (DECISIONS.md 2026-08-11)
 *
 * @returns {{valid: boolean, errors: Object<string, string>}}
 */
export function validateSimpleAuthForm({ name, birthDate, carrier, phone } = {}) {
    const errors = {};
    const trimmedName = String(name ?? '').trim();
    if (!trimmedName) {
        errors.name = '이름을 입력해주세요';
    } else if (trimmedName.length < 2 || trimmedName.length > 50) {
        /* 50자 상한은 저장 대상 컬럼이 `tbl_user.name VARCHAR(50)` 이기 때문이다. */
        errors.name = '이름을 2~50자로 입력해주세요';
    } else if (!/^[가-힣a-zA-Z ]+$/.test(trimmedName)) {
        /*
         * `가-힣` 은 완성형만 포함하므로 자모만 친 입력(ㄱ, ㅏ)도 여기서 걸러진다.
         * 공백은 \s 가 아니라 진짜 공백만 허용한다 — 서버(UserService.NAME_PATTERN)와 같은 규칙이라야
         * 화면은 통과했는데 서버가 INVALID_NAME 으로 되돌려주는 어긋남이 안 생긴다.
         */
        errors.name = '한글 또는 영문으로 입력해주세요';
    }
    if (!/^\d{6}$/.test(String(birthDate ?? ''))) {
        errors.birthDate = '생년월일 6자리를 입력해주세요';
    }
    if (!carrier) {
        errors.carrier = '통신사를 선택해주세요';
    }
    /* 하이픈은 허용한다 — 사용자가 넣는 걸 막을 이유가 없다. */
    const digits = String(phone ?? '').replace(/-/g, '');
    if (!/^010\d{7,8}$/.test(digits)) {
        errors.phone = '010으로 시작하는 휴대폰번호를 입력해주세요';
    }
    return { valid: Object.keys(errors).length === 0, errors };
}

/**
 * 동기화 상태 5종 → 배지 라벨·색.
 * tbl_connected_account.sync_status 값과 1:1 대응한다.
 */
export function resolveSyncBadge(syncStatus) {
    switch (syncStatus) {
        case SYNC_STATUS.NORMAL:
            return { label: '정상', variant: 'innocent' };
        case SYNC_STATUS.SYNCING:
            return { label: '갱신 중', variant: 'progress' };
        case SYNC_STATUS.NEED_SYNC:
            return { label: '갱신 필요', variant: 'default' };
        case SYNC_STATUS.FAILED:
            return { label: '실패', variant: 'guilty' };
        case SYNC_STATUS.NEED_RECONNECT:
            return { label: '재연동 필요', variant: 'guilty' };
        default:
            return { label: '알 수 없음', variant: 'default' };
    }
}

/** 재연동 버튼을 보여줘야 하는 상태인지. */
export function needsReconnect(syncStatus) {
    return syncStatus === SYNC_STATUS.NEED_RECONNECT || syncStatus === SYNC_STATUS.FAILED;
}

/**
 * 쿨다운 남은 시간 → `MM:SS`.
 * 참고화면의 `방금 갱신함 · 02:30 후 다시 조회` 표기를 만든다.
 */
export function formatCooldown(seconds) {
    const safe = Math.max(0, Math.floor(Number(seconds) || 0));
    const mm = String(Math.floor(safe / 60)).padStart(2, '0');
    const ss = String(safe % 60).padStart(2, '0');
    return `${mm}:${ss}`;
}

/** 같은 날인지. 시간대 계산 없이 연·월·일만 본다. */
function isSameDay(a, b) {
    return (
        a.getFullYear() === b.getFullYear() &&
        a.getMonth() === b.getMonth() &&
        a.getDate() === b.getDate()
    );
}

/**
 * 동기화 시각 표기. 오늘이면 `오늘 09:32`, 아니면 `8월 3일 18:00`.
 * @param {string|null} isoString ISO-8601. 최초 연결 직후엔 null 이 올 수 있다.
 * @param {Date} [now] 테스트에서 기준 시각을 고정하기 위한 인자.
 */
export function formatSyncTime(isoString, now = new Date()) {
    if (!isoString) {
        return '동기화 이력 없음';
    }
    const target = new Date(isoString);
    if (Number.isNaN(target.getTime())) {
        return '동기화 이력 없음';
    }
    const hh = String(target.getHours()).padStart(2, '0');
    const mm = String(target.getMinutes()).padStart(2, '0');
    if (isSameDay(target, now)) {
        return `오늘 ${hh}:${mm}`;
    }
    return `${target.getMonth() + 1}월 ${target.getDate()}일 ${hh}:${mm}`;
}

/**
 * 동의 만료까지 남은 일수 → `D-12`.
 * 명세 AC_01_04 가 "만료 30일 전 안내"를 요구하므로, 30일 이내일 때만 값을 돌려준다.
 * @returns {string|null} 안내가 필요 없으면 null
 */
export function consentExpiryLabel(expiresAt, now = new Date()) {
    if (!expiresAt) {
        return null;
    }
    const target = new Date(expiresAt);
    if (Number.isNaN(target.getTime())) {
        return null;
    }
    const days = Math.ceil((target.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
    if (days > 30) {
        return null;
    }
    return days <= 0 ? '동의 만료' : `동의 D-${days}`;
}

/**
 * 금액 표기.
 *
 * 참고화면(`doc/개발참고화면/탕탕/0-4[계좌관리]조회`, `0-5[계좌연결]연결계좌선택`)은
 * 계좌 금액을 **모노스페이스 숫자로만** 쓴다 — `8,340,000` 처럼 단위 '원'을 붙이지 않는다.
 * 통화가 KRW 하나뿐이라 단위가 반복 정보이고, 모노 숫자는 자릿수가 세로로 정렬돼 비교하기 쉽다.
 *
 * 글꼴(`--tt-font-mono`)은 화면이 지정한다. 표기 규칙만 여기서 정한다.
 */
export function formatAmount(value) {
    return Number(value ?? 0).toLocaleString('ko-KR');
}

/**
 * 기관 코드 → 로고 색조.
 *
 * Figma 확정본(`금융기관 선택`)은 기관마다 로고 배경색이 다르다 —
 * KB·카카오는 골드, 신한·토스는 블루, 우리는 그린, 하나는 로즈.
 * 브랜드 색을 그대로 쓰면 HEX 하드코딩이 되므로(DESIGN_SYSTEM.md 절대 규칙 1)
 * **디자인시스템의 의미 토큰 4계열에 배정**해 같은 인상을 만든다.
 *
 * 매핑에 없는 기관은 코드 모양으로 갈린다 — CODEF `organization` 은 숫자 4자리이고 앞 두 자리가
 * 업권이라 `03xx` 는 카드사, `02xx` 는 증권사로 본다.
 * 반면 대출·페이머니 기관은 숫자가 아니라 `CP_`(캐피탈) · `SB_`(저축은행) · `PAY_`(페이머니)
 * 접두사를 쓴다. 앞 두 자리 규칙에 걸리지 않아 전부 기본값 gold 로 떨어지므로,
 * **14곳을 아래 매핑에 직접 적어 둔다** — 적지 않으면 두 업권 화면이 통째로 단색이 된다.
 *
 * @returns {'gold'|'blue'|'green'|'rose'} 컴포넌트가 이 값으로 클래스를 고른다
 */
const INSTITUTION_TONES = {
    '0004': 'gold', // KB국민은행
    '0090': 'gold', // 카카오뱅크
    '0088': 'blue', // 신한은행
    '0092': 'blue', // 토스뱅크
    '0089': 'blue', // 케이뱅크
    '0020': 'green', // 우리은행
    '0011': 'green', // NH농협은행
    '0003': 'green', // IBK기업은행
    '0081': 'rose', // 하나은행

    /* 대출 — 캐피탈(CP_) · 저축은행(SB_). 로고 PNG 가 없어 전부 배경색 + 약칭으로 그린다. */
    CP_KB: 'gold', // KB캐피탈
    CP_HYUNDAI: 'blue', // 현대캐피탈
    CP_SHINHAN: 'blue', // 신한캐피탈
    CP_HANA: 'rose', // 하나캐피탈
    CP_WOORI: 'green', // 우리금융캐피탈
    SB_SBI: 'blue', // SBI저축은행
    SB_OK: 'rose', // OK저축은행
    SB_WELCOME: 'gold', // 웰컴저축은행

    /* 페이머니(PAY_) */
    PAY_KAKAO: 'gold', // 카카오페이
    PAY_NAVER: 'green', // 네이버페이
    PAY_TOSS: 'blue', // 토스페이
    PAY_PAYCO: 'rose', // 페이코
    PAY_KB: 'gold', // KB페이
    PAY_CPANG: 'rose', // 쿠팡페이
};

export function resolveInstitutionTone(code) {
    const key = String(code ?? '');
    if (INSTITUTION_TONES[key]) {
        return INSTITUTION_TONES[key];
    }
    const prefix = key.slice(0, 2);
    if (prefix === '03') {
        return 'blue'; // 카드
    }
    if (prefix === '02') {
        return 'green'; // 증권
    }
    return 'gold';
}
