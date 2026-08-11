/**
 * 그룹 채팅 스토어
 *
 * 채팅방 진입 → 메시지 로드 → 전송 → 퇴장 흐름을 관리한다.
 * 실시간 수신은 추후 WebSocket 연동 시 subscribeRoom 액션으로 추가.
 */
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { useAuthStore } from '@/stores/auth';
import {
    fetchChatRoomInfo,
    fetchChatMessages,
    sendChatMessage,
    resetUnreadCount,
} from '@/api/groupChat';

export const useGroupChatStore = defineStore('groupChat', () => {
    /* ── 상태 ──────────────────────────────────────────── */
    const messages = ref([]);
    const roomInfo = ref(null);
    const hasMore = ref(false);
    const loading = ref(false);
    const sending = ref(false);

    /* ── 파생 ──────────────────────────────────────────── */
    const currentUserId = computed(() => {
        const auth = useAuthStore();
        return auth.user?.id ?? 1; // 목업 기본값 = 유저 1
    });

    const isEnded = computed(() => roomInfo.value?.status === 'ENDED');

    /* ── 액션 ──────────────────────────────────────────── */

    /** 채팅방 진입: 방 정보 + 최초 메시지 로드 + 읽음 처리 */
    async function enterRoom(groupId) {
        loading.value = true;
        try {
            const [room, page] = await Promise.all([
                fetchChatRoomInfo(groupId),
                fetchChatMessages(groupId),
            ]);
            roomInfo.value = room;
            messages.value = page.messages;
            hasMore.value = page.hasMore;
            await resetUnreadCount(groupId);
        } finally {
            loading.value = false;
        }
    }

    /** 스크롤-업 페이징: 더 오래된 메시지 앞에 삽입 */
    async function loadOlderMessages(groupId) {
        if (!hasMore.value || loading.value) return;
        loading.value = true;
        try {
            const page = await fetchChatMessages(groupId, messages.value.length);
            messages.value = [...page.messages, ...messages.value];
            hasMore.value = page.hasMore;
        } finally {
            loading.value = false;
        }
    }

    /** 텍스트 메시지 전송 */
    async function sendText(groupId, text) {
        if (!text.trim() || sending.value) return;
        sending.value = true;
        try {
            const { messageId, createdAt } = await sendChatMessage(groupId, {
                contentType: 'TEXT',
                content: text.trim(),
            });
            messages.value.push({
                messageId,
                groupId: Number(groupId),
                senderId: currentUserId.value,
                senderName: '나',
                senderColor: '#232842',
                messageType: 'USER',
                contentType: 'TEXT',
                content: text.trim(),
                systemSubType: null,
                metadata: null,
                createdAt,
            });
        } finally {
            sending.value = false;
        }
    }

    /** 스티커 전송 */
    async function sendSticker(groupId, stickerId) {
        if (sending.value) return;
        sending.value = true;
        try {
            const { messageId, createdAt } = await sendChatMessage(groupId, {
                contentType: 'STICKER',
                content: stickerId,
            });
            messages.value.push({
                messageId,
                groupId: Number(groupId),
                senderId: currentUserId.value,
                senderName: '나',
                senderColor: '#232842',
                messageType: 'USER',
                contentType: 'STICKER',
                content: stickerId,
                systemSubType: null,
                metadata: null,
                createdAt,
            });
        } finally {
            sending.value = false;
        }
    }

    /** 채팅방 퇴장: 상태 초기화 */
    function leaveRoom() {
        messages.value = [];
        roomInfo.value = null;
        hasMore.value = false;
        loading.value = false;
        sending.value = false;
    }

    return {
        messages,
        roomInfo,
        hasMore,
        loading,
        sending,
        currentUserId,
        isEnded,
        enterRoom,
        loadOlderMessages,
        sendText,
        sendSticker,
        leaveRoom,
    };
});
