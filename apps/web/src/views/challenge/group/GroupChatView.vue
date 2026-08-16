<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useGroupChatStore } from '@/stores/groupChat';
import { useAuthStore } from '@/stores/auth';
import { createChatSocket } from '@/api/chatSocket';
import GroupChatHeader from '@/components/challenge/group/chat/GroupChatHeader.vue';
import GroupChatDateDivider from '@/components/challenge/group/chat/GroupChatDateDivider.vue';
import GroupChatBubble from '@/components/challenge/group/chat/GroupChatBubble.vue';
import GroupChatSystemPill from '@/components/challenge/group/chat/GroupChatSystemPill.vue';
import GroupChatInput from '@/components/challenge/group/chat/GroupChatInput.vue';
import GroupChatToast from '@/components/challenge/group/chat/GroupChatToast.vue';
import StateError from '@/components/common/StateError.vue';

const route = useRoute();
const store = useGroupChatStore();

const groupId = computed(() => route.params.id);

/* ── refs ──────────────────────────────────────────────── */
const chatScrollEl = ref(null);
const toastText = ref('');
let toastTimer = null;
let isLoadingOlder = false; // 스크롤-업 페이징 중 scrollToBottom 방지용
let socket = null;
let unmounted = false; // enterRoom 대기 중 라우트 이탈 시 소켓을 만들지 않기 위한 가드

/* ── 메시지 그룹핑 (날짜 구분 삽입) ────────────────────── */
/* msg.sentAt 은 어댑터(api/groupChatAdapter.js)가 만든 Date 다. 해석 못 한 값은 null 이라
   그때는 구분선을 넣지 않는다 — 예전엔 NaN 이 흘러들어 "NaN월 NaN일" 이 찍혔다. */
const groupedMessages = computed(() => {
    const items = [];
    let lastDateKey = '';

    for (const msg of store.messages) {
        const d = msg.sentAt;
        if (!d) {
            items.push({ type: 'message', data: msg, key: msg.messageId });
            continue;
        }
        const dateKey = `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`;

        if (dateKey !== lastDateKey) {
            lastDateKey = dateKey;
            const today = new Date();
            const yesterday = new Date();
            yesterday.setDate(yesterday.getDate() - 1);

            let label;
            if (
                d.getFullYear() === today.getFullYear() &&
                d.getMonth() === today.getMonth() &&
                d.getDate() === today.getDate()
            ) {
                label = '오늘';
            } else if (
                d.getFullYear() === yesterday.getFullYear() &&
                d.getMonth() === yesterday.getMonth() &&
                d.getDate() === yesterday.getDate()
            ) {
                label = '어제';
            } else {
                label = `${d.getMonth() + 1}월 ${d.getDate()}일`;
            }
            items.push({ type: 'date', label, key: `date-${dateKey}` });
        }

        items.push({ type: 'message', data: msg, key: msg.messageId });
    }
    return items;
});

/* ── 스크롤 ────────────────────────────────────────────── */
function scrollToBottom() {
    nextTick(() => {
        if (chatScrollEl.value) {
            chatScrollEl.value.scrollTop = chatScrollEl.value.scrollHeight;
        }
    });
}

function handleScroll() {
    if (!chatScrollEl.value) return;
    if (chatScrollEl.value.scrollTop < 40 && store.hasMore && !store.loading) {
        isLoadingOlder = true;
        const prevHeight = chatScrollEl.value.scrollHeight;
        store.loadOlderMessages(groupId.value).then(() => {
            nextTick(() => {
                if (chatScrollEl.value) {
                    chatScrollEl.value.scrollTop = chatScrollEl.value.scrollHeight - prevHeight;
                }
                isLoadingOlder = false;
            });
        });
    }
}

/* ── 토스트 ────────────────────────────────────────────── */
function showToast(msg) {
    clearTimeout(toastTimer);
    toastText.value = msg;
    toastTimer = setTimeout(() => {
        toastText.value = '';
    }, 1800);
}

/* ── 전송 ──────────────────────────────────────────────── */
/*
 * 실전송은 STOMP(socket.send)가 맡는다(이슈 #174 Task 11). 내가 보낸 메시지도 서버가
 * /sub/chat/{groupId} 로 다시 내려보내 store.appendMessage 로 들어오므로, 화면에 미리
 * 낙관적으로 붙이지 않는다 — 아래 watch(messages.length) 가 그때 스크롤을 내린다.
 */
function handleSendText(text) {
    try {
        socket?.send(text);
    } catch {
        showToast('메시지 전송에 실패했어요. 다시 시도해 주세요.');
    }
}

/* ── 메시지 판별 헬퍼 ──────────────────────────────────── */
/*
 * 서버가 주는 구분은 type: 'TEXT' | 'SYSTEM' 둘뿐이다(ChatMessageDto). 목업에 있던
 * 시스템 서브타입·메타데이터(재판 딥링크 CTA)는 서버 계약에 없으므로 화면에서도 만들지 않는다.
 * 시스템 메시지는 본문 그대로 pill 로 보여준다.
 */
function isMine(msg) {
    return Number(msg.senderId) === Number(store.currentUserId);
}

/* ── 오류 재시도 ───────────────────────────────────────── */
function retryEnter() {
    store.enterRoom(groupId.value);
}

/* ── 라이프사이클 ──────────────────────────────────────── */
onMounted(async () => {
    await store.enterRoom(groupId.value);
    if (unmounted) return; // enterRoom 대기 중 화면을 벗어났으면 소켓을 만들지 않는다
    scrollToBottom();

    // 종료된 챌린지(store.closed)·비참여자 등 진입 실패(store.error) 시에는 연결하지 않는다
    if (store.closed || store.error) return;

    socket = createChatSocket({
        groupId: groupId.value,
        getToken: () => useAuthStore().accessToken,
        onMessage: (message) => store.appendMessage(message),
        onReconnect: () => store.catchUp(),
    });
    socket.connect();
});

onUnmounted(() => {
    unmounted = true;
    clearTimeout(toastTimer);
    /*
     * 구독 해제만으로는 서버의 접속 세션 추적(ChatSessionRegistry)이 UNSUBSCRIBE 를 인식하지
     * 못해 "방에 접속 중"으로 계속 남고, 그러면 이 사용자에게는 알림이 안 나간다. 그래서 화면을
     * 벗어날 때는 반드시 STOMP 연결 자체를 끊는다(뒤로가기·탭 전환 전부 이 훅을 탄다).
     */
    socket?.disconnect();
    store.leaveRoom();
});

// 새 메시지가 뒤에 추가될 때만 자동 스크롤 (페이징으로 앞에 삽입될 때는 스킵)
watch(
    () => store.messages.length,
    () => {
        if (!isLoadingOlder) {
            scrollToBottom();
        }
    },
);
</script>

<template>
    <div class="chat-view">
        <!-- 헤더 -->
        <GroupChatHeader
            :room-info="store.roomInfo"
            @open-menu="showToast('준비 중인 기능입니다')"
        />

        <!-- 종료된 챌린지: 대화가 챌린지 종료와 함께 이미 삭제됐다 -->
        <div v-if="store.closed" class="chat-view__closed">
            <p>종료된 챌린지예요.</p>
            <p>대화 내용은 챌린지가 끝나면서 사라졌어요.</p>
        </div>

        <!-- 그 외 진입 실패 (예: 참여자가 아님) -->
        <div v-else-if="store.error" class="chat-view__error">
            <StateError :message="store.error.message" @retry="retryEnter" />
        </div>

        <template v-else>
            <!-- 대화 영역 -->
            <div ref="chatScrollEl" class="chat-view__scroll" @scroll="handleScroll">
                <!-- 로딩 -->
                <div v-if="store.loading && store.messages.length === 0" class="chat-view__loading">
                    메시지를 불러오는 중...
                </div>

                <template v-for="item in groupedMessages" :key="item.key">
                    <!-- 날짜 구분 -->
                    <GroupChatDateDivider v-if="item.type === 'date'" :label="item.label" />

                    <!-- 시스템(재판 봇) 메시지 -->
                    <GroupChatSystemPill v-else-if="item.data.isSystem" :message="item.data" />

                    <!-- 참여자 메시지. v-else 라 어떤 type 이든 화면에서 사라지지 않는다 -->
                    <GroupChatBubble v-else :message="item.data" :is-mine="isMine(item.data)" />
                </template>
            </div>

            <!-- 토스트 -->
            <GroupChatToast :message="toastText" />

            <!-- 입력바 (스티커 버튼은 이번 범위 밖이라 GroupChatInput 기본값으로 숨겨져 있다) -->
            <GroupChatInput :disabled="store.isEnded" @send-text="handleSendText" />
        </template>
    </div>
</template>

<style scoped>
.chat-view {
    display: flex;
    flex-direction: column;
    height: 100vh;
    height: 100dvh;
    background: var(--tt-bg-subtle);
    position: relative;
}

.chat-view__scroll {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    padding: 14px var(--tt-space-4) var(--tt-space-4);
    display: flex;
    flex-direction: column;
    gap: 10px;
    scrollbar-width: none;
}

.chat-view__scroll::-webkit-scrollbar {
    display: none;
}

.chat-view__loading {
    text-align: center;
    color: var(--tt-text-muted);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-semibold);
    padding: var(--tt-space-6) 0;
}

.chat-view__closed {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: var(--tt-space-2);
    padding: var(--tt-space-10) var(--tt-space-5);
    text-align: center;
    color: var(--tt-text-muted);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-semibold);
}

.chat-view__error {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
}
</style>
