<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useGroupChatStore } from '@/stores/groupChat';
import GroupChatHeader from '@/components/challenge/group/chat/GroupChatHeader.vue';
import GroupChatDateDivider from '@/components/challenge/group/chat/GroupChatDateDivider.vue';
import GroupChatBubble from '@/components/challenge/group/chat/GroupChatBubble.vue';
import GroupChatSystemCard from '@/components/challenge/group/chat/GroupChatSystemCard.vue';
import GroupChatSystemPill from '@/components/challenge/group/chat/GroupChatSystemPill.vue';
import GroupChatInput from '@/components/challenge/group/chat/GroupChatInput.vue';
import GroupChatStickerPicker from '@/components/challenge/group/chat/GroupChatStickerPicker.vue';
import GroupChatToast from '@/components/challenge/group/chat/GroupChatToast.vue';

const route = useRoute();
const router = useRouter();
const store = useGroupChatStore();

const groupId = computed(() => route.params.id);

/* ── refs ──────────────────────────────────────────────── */
const chatScrollEl = ref(null);
const showSticker = ref(false);
const toastText = ref('');
let toastTimer = null;

/* ── 메시지 그룹핑 (날짜 구분 삽입) ────────────────────── */
const groupedMessages = computed(() => {
    const items = [];
    let lastDateKey = '';

    for (const msg of store.messages) {
        const d = new Date(msg.createdAt);
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
        const prevHeight = chatScrollEl.value.scrollHeight;
        store.loadOlderMessages(groupId.value).then(() => {
            nextTick(() => {
                if (chatScrollEl.value) {
                    chatScrollEl.value.scrollTop =
                        chatScrollEl.value.scrollHeight - prevHeight;
                }
            });
        });
    }
}

/* ── 토스트 ────────────────────────────────────────────── */
function showToast(msg) {
    clearTimeout(toastTimer);
    toastText.value = msg;
    toastTimer = setTimeout(() => { toastText.value = ''; }, 1800);
}

/* ── 전송 ──────────────────────────────────────────────── */
function handleSendText(text) {
    store.sendText(groupId.value, text);
    showSticker.value = false;
    scrollToBottom();
}

function handleSelectSticker(stickerId) {
    store.sendSticker(groupId.value, stickerId);
    showSticker.value = false;
    scrollToBottom();
}

function toggleSticker() {
    showSticker.value = !showSticker.value;
}

/* ── 시스템 CTA ────────────────────────────────────────── */
function handleOpenTrial(indictmentId) {
    router.push({
        name: 'defenseViolation',
        params: { id: groupId.value, indictmentId },
    });
}

function handleVote(indictmentId) {
    router.push({
        name: 'voteVerdict',
        params: { id: groupId.value, indictmentId },
    });
}

function handleOpenVerdict(indictmentId) {
    router.push({
        name: 'groupAiVerdictDetail',
        params: { id: groupId.value, indictmentId },
    });
}

/* ── 메시지 판별 헬퍼 ──────────────────────────────────── */
function isMine(msg) {
    return msg.senderId === store.currentUserId;
}

function isSystemCard(msg) {
    return msg.messageType === 'SYSTEM' && msg.systemSubType === 'TRIAL_STARTED';
}

function isSystemPill(msg) {
    return (
        msg.messageType === 'SYSTEM' &&
        ['VOTE_OPENED', 'VERDICT_CONFIRMED', 'CONFESSION'].includes(msg.systemSubType)
    );
}

function isUserMessage(msg) {
    return msg.messageType === 'USER';
}

/* ── 라이프사이클 ──────────────────────────────────────── */
onMounted(async () => {
    await store.enterRoom(groupId.value);
    scrollToBottom();
});

onUnmounted(() => {
    clearTimeout(toastTimer);
    store.leaveRoom();
});

// 메시지가 추가되면 자동 스크롤
watch(
    () => store.messages.length,
    () => {
        scrollToBottom();
    },
);
</script>

<template>
    <div class="chat-view">
        <!-- 헤더 -->
        <GroupChatHeader :room-info="store.roomInfo" />

        <!-- 대화 영역 -->
        <div
            ref="chatScrollEl"
            class="chat-view__scroll"
            @scroll="handleScroll"
        >
            <!-- 로딩 -->
            <div v-if="store.loading && store.messages.length === 0" class="chat-view__loading">
                메시지를 불러오는 중...
            </div>

            <template v-for="item in groupedMessages" :key="item.key">
                <!-- 날짜 구분 -->
                <GroupChatDateDivider
                    v-if="item.type === 'date'"
                    :label="item.label"
                />

                <!-- 시스템: 재판 게시 (Ink 카드) -->
                <GroupChatSystemCard
                    v-else-if="isSystemCard(item.data)"
                    :message="item.data"
                    @open-trial="handleOpenTrial"
                />

                <!-- 시스템: 투표/판결/혐의인정 (pill) -->
                <GroupChatSystemPill
                    v-else-if="isSystemPill(item.data)"
                    :message="item.data"
                    @vote="handleVote"
                    @open-verdict="handleOpenVerdict"
                />

                <!-- 사용자 메시지 -->
                <GroupChatBubble
                    v-else-if="isUserMessage(item.data)"
                    :message="item.data"
                    :is-mine="isMine(item.data)"
                />
            </template>
        </div>

        <!-- 토스트 -->
        <GroupChatToast :message="toastText" />

        <!-- 스티커 패널 -->
        <GroupChatStickerPicker
            v-if="showSticker"
            @select="handleSelectSticker"
            @close="showSticker = false"
        />

        <!-- 입력바 -->
        <GroupChatInput
            :disabled="store.isEnded"
            @send-text="handleSendText"
            @toggle-sticker="toggleSticker"
        />
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
</style>
