/**
 * 그룹 채팅 스토어
 *
 * 채팅방 진입 → 메시지 로드 → 실시간 송수신 → 퇴장 흐름을 관리한다.
 * 실전송·실수신은 STOMP(api/chatSocket.js)가 맡는다 — 이 스토어는 REST 로 읽은
 * 상태(방 정보 · 메시지 목록)와 소켓이 넘겨주는 메시지를 합치는 역할만 한다.
 *
 * **서버 DTO → 화면 모델 정규화는 이 스토어의 진입점에서만 한다**(api/groupChatAdapter.js).
 * REST 로 받은 것과 소켓으로 받은 것이 같은 어댑터를 지나야 두 경로가 갈라지지 않는다.
 */
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { fetchChatRoomInfo, fetchChatMessages, resetUnreadCount } from '@/api/groupChat';
import { toChatMessage, toChatMessagePage, toChatRoom } from '@/api/groupChatAdapter';

export const useGroupChatStore = defineStore('groupChat', () => {
    /* ── 상태 ──────────────────────────────────────────── */
    const groupId = ref(null);
    const messages = ref([]);
    const roomInfo = ref(null);
    const hasMore = ref(false);
    const loading = ref(false);
    /** 종료된 챌린지(CHAT_ROOM_CLOSED) — 대화가 이미 삭제돼 안내만 보여준다 */
    const closed = ref(false);
    /** closed 가 아닌 그 외 진입 실패(예: CHAT_NOT_MEMBER, 네트워크 오류) */
    const error = ref(null);

    /* ── 파생 ──────────────────────────────────────────── */
    const currentUserId = computed(() => {
        const auth = useAuthStore();
        return auth.user?.id ?? 1; // 목업 기본값 = 유저 1
    });

    /* 서버 상태값은 RECRUITING · ACTIVE · JUDGING · CLOSED 다 (목업의 'ENDED' 는 없다) */
    const isEnded = computed(() => roomInfo.value?.isEnded === true);

    /* ── 액션 ──────────────────────────────────────────── */

    /** 채팅방 진입: 방 정보 + 최초 메시지 로드 + 읽음 처리 */
    async function enterRoom(id) {
        groupId.value = id;
        loading.value = true;
        closed.value = false;
        error.value = null;
        try {
            roomInfo.value = toChatRoom(await fetchChatRoomInfo(id));
            const page = toChatMessagePage(await fetchChatMessages(id, {}));
            messages.value = page.messages;
            hasMore.value = page.hasMore;
            await resetUnreadCount(id);
        } catch (e) {
            // 종료된 챌린지는 안내 화면을 보여준다. 대화는 챌린지가 CLOSED 되는 즉시 삭제된다
            // (ChatMessageStore, 이슈 #174). http.js 인터셉터는 실패를 ApiError(code, message,
            // status) 로 정규화해 던지므로 e.code 를 본다 — e.response.data.code 가 아니다.
            if (e?.code === 'CHAT_ROOM_CLOSED') {
                closed.value = true;
            } else {
                error.value = e;
            }
        } finally {
            loading.value = false;
        }
    }

    /**
     * 스크롤-업 페이징: 더 오래된 메시지 앞에 삽입.
     *
     * fetchChatMessages 는 개수(offset)가 아니라 커서를 받는다(이슈 #174 Task 11).
     * 서버는 messageId 오름차순으로 페이지를 내려주고(ChatMessageStore: messageId N ↔
     * 인덱스 N-1), 이 스토어는 초기 로드·loadOlderMessages·appendMessage 어디서도 그 순서를
     * 어지럽히지 않는다 — 그래서 messages 는 항상 오래된 순이고, 맨 앞(0번)이 현재 갖고 있는
     * 것 중 가장 오래된 메시지다. 그 messageId 를 before 로 넘겨야 그보다 이전 것을 받는다.
     */
    async function loadOlderMessages(id) {
        if (!hasMore.value || loading.value) return;
        loading.value = true;
        try {
            const oldest = messages.value[0];
            const page = toChatMessagePage(
                await fetchChatMessages(id, oldest ? { before: oldest.messageId } : {}),
            );
            messages.value = [...page.messages, ...messages.value];
            hasMore.value = page.hasMore;
        } finally {
            loading.value = false;
        }
    }

    /**
     * 소켓으로 수신한 메시지를 뒤에 붙인다. 이미 있는 messageId 는 무시한다(중복 수신 방지).
     * REST 경로와 같은 어댑터를 통과시켜 두 경로의 모양을 일치시킨다.
     */
    function appendMessage(raw) {
        const message = toChatMessage(raw);
        if (messages.value.some((m) => m.messageId === message.messageId)) return;
        messages.value.push(message);
    }

    /**
     * 재연결 직후 끊긴 동안의 메시지를 메운다. STOMP 만으로는 그 구간이 사라진다.
     * chatSocket 의 onReconnect 콜백에서 인자 없이 호출되므로, store 가 들고 있는
     * groupId·마지막 messageId 로 스스로 보충 조회한다.
     */
    async function catchUp() {
        if (!groupId.value || messages.value.length === 0) return;
        const lastId = messages.value[messages.value.length - 1].messageId;
        const page = await fetchChatMessages(groupId.value, { after: lastId });
        page.messages.forEach((m) => appendMessage(m));
    }

    /** 채팅방 퇴장: 상태 초기화 */
    function leaveRoom() {
        groupId.value = null;
        messages.value = [];
        roomInfo.value = null;
        hasMore.value = false;
        loading.value = false;
        closed.value = false;
        error.value = null;
    }

    return {
        groupId,
        messages,
        roomInfo,
        hasMore,
        loading,
        closed,
        error,
        currentUserId,
        isEnded,
        enterRoom,
        loadOlderMessages,
        appendMessage,
        catchUp,
        leaveRoom,
    };
});
