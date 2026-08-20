/*
 * 용도: BaseModal · BaseBottomSheet 가 공유하는 오버레이 동작(스크롤 잠금 · ESC · 포커스 트랩 · 뒤로가기 닫기).
 * 언제 쓰는지: 화면 위에 겹쳐 뜨는 레이어를 새로 만들 때. 이 파일 하나만 고치면 모든 오버레이 동작이 함께 바뀐다.
 * 쓰면 안 되는 경우: 겹치지 않는 인라인 패널·아코디언. 스크롤을 잠그면 안 되는 UI 에는 쓰지 말 것.
 */
import { nextTick, onBeforeUnmount, watch } from 'vue';

/* 스크롤 잠금은 참조 카운트로 관리한다. 모달 위에 시트가 겹쳐 열려도 마지막 하나가 닫힐 때만 풀린다. */
let scrollLockCount = 0;
let savedPaddingRight = '';
let savedOverflow = '';

/*
 * 여백을 「예상」하지 않고 **잠근 뒤에 실제로 넓어진 만큼**을 잰다.
 *
 * 예전에는 `window.innerWidth - clientWidth` 로 스크롤바 너비를 재서 그만큼 padding 을 줬다.
 * 그런데 base.css 의 `html { scrollbar-gutter: stable }` 이 스크롤바 자리를 항상 예약해 두기
 * 때문에, overflow:hidden 을 걸어도 그 자리는 돌아오지 않는다. 결국 돌려받지도 않은 15px 을
 * 보정한 셈이 되어, 가운데 정렬된 .tt-app(max-width 480px)이 시트가 열리는 순간 그만큼
 * 좁아졌다가 닫으면 되돌아왔다 — 화면이 「뭉개지는」 것처럼 보인 원인이다.
 *
 * clientWidth 를 읽으면 강제 리플로우가 일어나므로 잠금 직후 값은 정확하다.
 * scrollbar-gutter 가 없는 환경에서는 예전과 똑같이 스크롤바 너비가 잡힌다.
 */
function lockScroll() {
    if (scrollLockCount === 0) {
        savedOverflow = document.body.style.overflow;
        savedPaddingRight = document.body.style.paddingRight;
        const widthBefore = document.documentElement.clientWidth;
        document.body.style.overflow = 'hidden';
        const reclaimed = document.documentElement.clientWidth - widthBefore;
        if (reclaimed > 0) {
            document.body.style.paddingRight = `${reclaimed}px`;
        }
    }
    scrollLockCount += 1;
}

function unlockScroll() {
    scrollLockCount = Math.max(0, scrollLockCount - 1);
    if (scrollLockCount === 0) {
        document.body.style.overflow = savedOverflow;
        document.body.style.paddingRight = savedPaddingRight;
    }
}

/*
 * iOS 키보드 대응.
 *
 * 안드로이드는 키보드가 올라오면 뷰포트 자체가 줄어들어 position: fixed 인 바텀시트가
 * 저절로 키보드 위로 올라온다. iOS 는 레이아웃 뷰포트를 그대로 두고 보이는 영역(visual
 * viewport)만 줄이기 때문에, 시트가 키보드 밑에 깔린 채 남는다.
 *
 * 그래서 키보드가 가린 높이를 직접 재서 --tt-keyboard-inset 에 넣는다.
 * 오버레이(.tt-sheet)가 이 값만큼 아래 여백을 두면 패널이 키보드 위로 떠오른다.
 * visualViewport 가 없는 환경(구형 브라우저·테스트)에서는 아무것도 하지 않는다.
 */
let keyboardWatchCount = 0;

/**
 * 키보드가 화면 아래를 가린 높이(px). 계산만 하는 순수 함수라 단위 테스트가 붙어 있다.
 *
 * innerHeight 는 레이아웃 뷰포트(키보드가 올라와도 그대로), viewport.height 는 실제 보이는
 * 높이다. offsetTop 은 iOS 가 포커스된 입력창을 보이려고 화면을 밀어올린 양이라 함께 빼야
 * 시트가 정확히 키보드 바로 위에 선다.
 *
 * @param {number} innerHeight window.innerHeight
 * @param {{height: number, offsetTop: number}|undefined} viewport window.visualViewport
 * @returns {number} 0 이상의 정수
 */
export function keyboardInsetOf(innerHeight, viewport) {
    if (!viewport) {
        return 0;
    }
    return Math.max(0, Math.round(innerHeight - viewport.height - viewport.offsetTop));
}

function syncKeyboardInset() {
    if (!window.visualViewport) {
        return;
    }
    const inset = keyboardInsetOf(window.innerHeight, window.visualViewport);
    document.documentElement.style.setProperty('--tt-keyboard-inset', `${inset}px`);
}

function watchKeyboard() {
    if (keyboardWatchCount === 0 && window.visualViewport) {
        window.visualViewport.addEventListener('resize', syncKeyboardInset);
        window.visualViewport.addEventListener('scroll', syncKeyboardInset);
        syncKeyboardInset();
    }
    keyboardWatchCount += 1;
}

function unwatchKeyboard() {
    keyboardWatchCount = Math.max(0, keyboardWatchCount - 1);
    if (keyboardWatchCount === 0) {
        window.visualViewport?.removeEventListener('resize', syncKeyboardInset);
        window.visualViewport?.removeEventListener('scroll', syncKeyboardInset);
        document.documentElement.style.removeProperty('--tt-keyboard-inset');
    }
}

const FOCUSABLE = [
    'a[href]',
    'button:not([disabled])',
    'input:not([disabled])',
    'textarea:not([disabled])',
    'select:not([disabled])',
    '[tabindex]:not([tabindex="-1"])',
].join(',');

function focusableIn(root) {
    if (!root) {
        return [];
    }
    return Array.from(root.querySelectorAll(FOCUSABLE)).filter(
        (el) => el.offsetParent !== null || el === document.activeElement,
    );
}

/**
 * @param {object} options
 * @param {import('vue').Ref<boolean>} options.isOpen       열림 상태 (v-model)
 * @param {import('vue').Ref<HTMLElement|null>} options.panelRef  포커스를 가둘 패널 엘리먼트
 * @param {() => boolean} options.canCloseOnEsc             ESC 로 닫아도 되는지
 * @param {() => void} options.requestClose                 닫기 요청(부모가 modelValue 를 false 로 바꾼다)
 */
export function useOverlay({ isOpen, panelRef, canCloseOnEsc, requestClose }) {
    let active = false;
    let pushedHistory = false;
    let lastFocused = null;

    function onKeydown(event) {
        if (event.key === 'Escape' && canCloseOnEsc()) {
            event.stopPropagation();
            requestClose();
            return;
        }
        if (event.key !== 'Tab') {
            return;
        }
        const items = focusableIn(panelRef.value);
        if (items.length === 0) {
            event.preventDefault();
            panelRef.value?.focus();
            return;
        }
        const first = items[0];
        const last = items[items.length - 1];
        if (event.shiftKey && document.activeElement === first) {
            event.preventDefault();
            last.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
            event.preventDefault();
            first.focus();
        }
    }

    /* 뒤로가기로 닫기: 열릴 때 히스토리 항목을 하나 쌓아두고, popstate 가 오면 닫는다. */
    function onPopState() {
        pushedHistory = false;
        requestClose();
    }

    function activate() {
        if (active) {
            return;
        }
        active = true;
        lastFocused = document.activeElement;
        lockScroll();
        watchKeyboard();
        document.addEventListener('keydown', onKeydown, true);
        window.history.pushState({ ttOverlay: true }, '');
        pushedHistory = true;
        window.addEventListener('popstate', onPopState);
        nextTick(() => {
            const items = focusableIn(panelRef.value);
            (items[0] ?? panelRef.value)?.focus();
        });
    }

    function deactivate() {
        if (!active) {
            return;
        }
        active = false;
        document.removeEventListener('keydown', onKeydown, true);
        window.removeEventListener('popstate', onPopState);
        unlockScroll();
        unwatchKeyboard();
        /* 내부 UI 로 닫은 경우에는 쌓아둔 히스토리 항목을 되돌린다.
         * popstate 로 닫힌 경우엔 이미 소비됐으므로 건드리지 않는다.
         * releaseHistory() 로 소유권을 넘긴 경우에도 건드리지 않는다. */
        if (pushedHistory) {
            pushedHistory = false;
            window.history.back();
        }
        lastFocused?.focus?.();
        lastFocused = null;
    }

    /*
     * 오버레이 안에서 다른 화면으로 이동할 때 쓴다. 쌓아둔 히스토리 항목의 소유권을 호출부에 넘긴다.
     *
     * 이걸 부르지 않고 오버레이 안에서 router.push 를 하면 이동이 취소된다.
     * 닫히면서 실행되는 history.back() 이 라우터가 방금 쌓은 항목을 그대로 되감기 때문이다.
     * 라우터의 pushState 는 라우트 컴포넌트의 동적 import 가 끝난 뒤에 일어나서
     * 항상 back() 보다 늦다 — 순서로는 절대 피할 수 없다.
     *
     * 호출한 쪽은 push 가 아니라 **router.replace** 로 이동한다.
     * 그래야 남겨진 오버레이 항목을 새 화면이 덮어써서 뒤로가기가 한 번에 원래 화면으로 간다.
     */
    function releaseHistory() {
        pushedHistory = false;
    }

    watch(isOpen, (open) => (open ? activate() : deactivate()), { immediate: true });
    onBeforeUnmount(deactivate);

    return { releaseHistory };
}
