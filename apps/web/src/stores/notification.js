import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import {
    NOTIFICATION_STREAM_URL,
    fetchNotifications,
    fetchUnreadCount,
    markAllNotificationsRead,
    markNotificationRead,
} from '@/api/notification';
import { parseSseChunk } from '@/utils/sseStream';
import { useAuthStore } from '@/stores/auth';

const POLL_INTERVAL_MS = 60_000;
const STREAM_RETRY_MS = 120_000;

/**
 * 알림 도메인 스토어 (이슈 #58). 도메인당 1개 규칙이라 목록과 배지를 함께 둔다.
 *
 * SSE 가 되면 LIVE, 안 되면 POLLING 으로 강등한다.
 * 토큰 만료는 따로 다루지 않는다 — 폴링 요청이 http.js 를 타면서 401 인터셉터가 재발급하고,
 * 다음 SSE 재시도가 새 토큰을 쓴다.
 */
export const useNotificationStore = defineStore('notification', () => {
    const items = ref([]);
    const nextCursor = ref(null);
    const unreadCount = ref(0);
    const loading = ref(false);
    const error = ref('');
    const streamState = ref('IDLE'); // IDLE | CONNECTING | LIVE | POLLING

    const hasMore = computed(() => nextCursor.value !== null);

    let controller = null;
    let pollTimer = null;
    let retryTimer = null;
    let stopping = false; // disconnect() 로 인한 의도적 중단인지 — abort 로 인한 catch/루프종료를 실패로 오인하지 않기 위함

    async function load() {
        loading.value = true;
        error.value = '';
        try {
            const data = await fetchNotifications(null, 20);
            items.value = data.items;
            nextCursor.value = data.nextCursor;
            unreadCount.value = data.unreadCount;
        } catch (err) {
            error.value = err.message ?? '알림을 불러오지 못했어요.';
        } finally {
            loading.value = false;
        }
    }

    async function loadMore() {
        if (!nextCursor.value || loading.value) {
            return;
        }
        loading.value = true;
        try {
            const data = await fetchNotifications(nextCursor.value, 20);
            items.value = [...items.value, ...data.items];
            nextCursor.value = data.nextCursor;
            unreadCount.value = data.unreadCount;
        } catch (err) {
            error.value = err.message ?? '알림을 더 불러오지 못했어요.';
        } finally {
            loading.value = false;
        }
    }

    /** 배지 조회 실패는 조용히 넘긴다 — 종 하나 때문에 홈이 깨지면 안 된다 */
    async function refreshBadge() {
        try {
            const data = await fetchUnreadCount();
            unreadCount.value = data.unreadCount;
        } catch {
            /* 무시 */
        }
    }

    async function markRead(id) {
        const target = items.value.find((item) => item.id === id);
        if (!target || target.isRead) {
            return;
        }
        target.isRead = true; // 낙관적 업데이트
        try {
            const data = await markNotificationRead(id);
            unreadCount.value = data.unreadCount;
        } catch (err) {
            target.isRead = false; // 롤백
            error.value = err.message ?? '읽음 처리에 실패했어요.';
        }
    }

    async function markAllRead() {
        const before = items.value.map((item) => item.isRead);
        items.value.forEach((item) => {
            item.isRead = true;
        });
        try {
            const data = await markAllNotificationsRead();
            unreadCount.value = data.unreadCount;
        } catch (err) {
            items.value.forEach((item, index) => {
                item.isRead = before[index];
            });
            error.value = err.message ?? '읽음 처리에 실패했어요.';
        }
    }

    function startPolling() {
        streamState.value = 'POLLING';
        stopPolling();
        pollTimer = setInterval(refreshBadge, POLL_INTERVAL_MS);
        retryTimer = setTimeout(connect, STREAM_RETRY_MS);
    }

    function stopPolling() {
        clearInterval(pollTimer);
        clearTimeout(retryTimer);
        pollTimer = null;
        retryTimer = null;
    }

    async function connect() {
        disconnect();
        stopping = false; // 새 연결을 시작하므로 직전 disconnect() 의 중단 표시를 해제한다
        const auth = useAuthStore();
        if (!auth.isLoggedIn) {
            return;
        }
        streamState.value = 'CONNECTING';
        controller = new AbortController();

        try {
            const response = await fetch(NOTIFICATION_STREAM_URL, {
                headers: { Authorization: `Bearer ${auth.accessToken}` },
                credentials: 'include',
                signal: controller.signal,
            });
            if (!response.ok || !response.body) {
                throw new Error('스트림 연결 실패');
            }

            const reader = response.body.getReader();
            const decoder = new TextDecoder();
            let buffer = '';

            for (;;) {
                const { value, done } = await reader.read();
                if (done) {
                    break;
                }
                const parsed = parseSseChunk(buffer, decoder.decode(value, { stream: true }));
                buffer = parsed.rest;
                for (const event of parsed.events) {
                    if (event.event === 'connected') {
                        streamState.value = 'LIVE';
                        stopPolling();
                    } else if (event.event === 'notification') {
                        onPushed(event.data);
                    }
                }
            }
            if (!stopping) {
                startPolling(); // 서버가 끊었다
            }
        } catch (err) {
            /* 의도적인 disconnect() 는 실패가 아니다. 여기서 폴링을 다시 걸면 방금 끊은 타이머가 되살아난다 */
            if (stopping || err?.name === 'AbortError') {
                return;
            }
            startPolling(); // 연결 자체가 안 됐다 (프록시 차단 포함)
        }
    }

    function onPushed(raw) {
        try {
            const notification = JSON.parse(raw);
            items.value = [notification, ...items.value];
            unreadCount.value += 1;
        } catch {
            refreshBadge(); // 본문을 못 읽어도 개수는 맞춘다
        }
    }

    function disconnect() {
        stopping = true;
        controller?.abort();
        controller = null;
        stopPolling();
        streamState.value = 'IDLE';
    }

    /**
     * 로그아웃 등 세션이 끝날 때 호출한다. 연결을 끊고 사용자 단위 데이터까지 비운다.
     *
     * ⚠ disconnect() 와 나눠 둔 이유: connect() 가 시작할 때 disconnect() 를 부르므로,
     *   여기에 데이터 비우기를 넣으면 스트림이 끊겼다 재연결될 때마다 목록이 지워진다.
     */
    function clearSession() {
        disconnect();
        items.value = [];
        nextCursor.value = null;
        unreadCount.value = 0;
        error.value = '';
    }

    return {
        items,
        nextCursor,
        unreadCount,
        loading,
        error,
        streamState,
        hasMore,
        load,
        loadMore,
        refreshBadge,
        markRead,
        markAllRead,
        connect,
        disconnect,
        clearSession,
    };
});
