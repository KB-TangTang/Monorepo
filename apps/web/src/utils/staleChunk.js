/*
 * 재배포로 사라진 청크에서 복구한다 (이슈 #320)
 *
 * 화면은 라우트 단위로 lazy import 된다. 탭을 열어 둔 사이에 재배포가 되면
 * 이미 로드된 화면은 멀쩡히 보이지만, 그 뒤 **처음 들어가는 화면**이 옛 빌드의
 * 청크 이름을 요청하다가 실패한다. `dev` 머지마다 자동 배포가 도는 구조라
 * 시연·녹화 중에도 일어날 수 있다.
 *
 * 처리를 두 겹으로 둔다.
 *   1) `vite:preloadError` - Vite 가 이 상황 하나를 위해 제공하는 이벤트다. 1차 수단.
 *   2) `router.onError`    - preload 를 타지 않고 실패하는 경우를 받는다.
 *
 * 훅이 하나도 없으면 실패가 처리되지 않은 Promise 거부로 끝나고 화면은 반응이 없다.
 * 에러 화면조차 뜨지 않아 사용자에겐 「버튼이 안 눌린다」로만 보인다.
 */

const RELOAD_FLAG = 'tt-stale-chunk-reloaded';

/*
 * 브라우저마다 문구가 다르다. 실측 기준으로 넷을 본다.
 *   Chrome  Failed to fetch dynamically imported module
 *   Safari  Importing a module script failed
 *   Firefox error loading dynamically imported module
 *   Vite    Unable to preload CSS for ...
 */
const STALE_CHUNK_PATTERNS = [
    /failed to fetch dynamically imported module/i,
    /importing a module script failed/i,
    /error loading dynamically imported module/i,
    /unable to preload css/i,
];

/** 이 오류가 「청크가 사라졌다」인지 판정한다. 다른 오류를 새로고침으로 덮으면 안 된다. */
export function isStaleChunkError(error) {
    const message = typeof error === 'string' ? error : (error?.message ?? '');
    return STALE_CHUNK_PATTERNS.some((pattern) => pattern.test(message));
}

/**
 * 청크 오류면 한 번만 새로고침한다.
 *
 * 가드가 없으면 **진짜로 파일이 없을 때 무한 새로고침**이 돈다.
 * 성공한 내비게이션이 한 번이라도 끝나면 `clearStaleChunkFlag` 가 가드를 푼다.
 *
 * @returns 새로고침을 걸었으면 true. 호출부가 이벤트를 삼킬지 판단하는 데 쓴다.
 */
export function recoverFromStaleChunk(error, targetPath, { storage, reload }) {
    if (!isStaleChunkError(error)) {
        return false;
    }
    if (storage.getItem(RELOAD_FLAG)) {
        return false;
    }

    storage.setItem(RELOAD_FLAG, '1');
    reload(targetPath);
    return true;
}

export function clearStaleChunkFlag(storage) {
    storage.removeItem(RELOAD_FLAG);
}

/**
 * 라우터와 window 에 복구 훅을 건다.
 *
 * 의존을 인자로 받는 이유는 테스트에서 진짜 새로고침을 걸 수 없어서다.
 */
export function installStaleChunkRecovery(router, options = {}) {
    const {
        storage = window.sessionStorage,
        reload = (path) => window.location.assign(path),
        target = window,
    } = options;

    /* Vite 1차. preventDefault 로 기본 throw 를 막아 콘솔을 어지럽히지 않는다. */
    target.addEventListener('vite:preloadError', (event) => {
        const error = event?.payload ?? event;
        /* preloadError 는 목적지를 알려주지 않는다. 지금 있는 자리를 그대로 다시 연다. */
        if (recoverFromStaleChunk(error, currentPath(target), { storage, reload })) {
            event?.preventDefault?.();
        }
    });

    /* 2차. preload 를 타지 않고 실패하는 경로를 받는다. */
    router.onError((error, to) => {
        recoverFromStaleChunk(error, to?.fullPath ?? currentPath(target), { storage, reload });
    });

    /* 한 번이라도 정상 이동에 성공하면 가드를 푼다. 실패한 이동은 여기 오지 않는다. */
    router.afterEach(() => clearStaleChunkFlag(storage));
}

function currentPath(target) {
    const { pathname = '/', search = '', hash = '' } = target?.location ?? {};
    return `${pathname}${search}${hash}`;
}
