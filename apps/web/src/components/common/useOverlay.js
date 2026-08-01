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

function lockScroll() {
    if (scrollLockCount === 0) {
        const scrollbar = window.innerWidth - document.documentElement.clientWidth;
        savedOverflow = document.body.style.overflow;
        savedPaddingRight = document.body.style.paddingRight;
        document.body.style.overflow = 'hidden';
        if (scrollbar > 0) {
            document.body.style.paddingRight = `${scrollbar}px`;
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
        /* 내부 UI 로 닫은 경우에는 쌓아둔 히스토리 항목을 되돌린다.
         * popstate 로 닫힌 경우엔 이미 소비됐으므로 건드리지 않는다. */
        if (pushedHistory) {
            pushedHistory = false;
            window.history.back();
        }
        lastFocused?.focus?.();
        lastFocused = null;
    }

    watch(isOpen, (open) => (open ? activate() : deactivate()), { immediate: true });
    onBeforeUnmount(deactivate);
}
