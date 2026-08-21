<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useGroupChatStore } from '@/stores/groupChat';
import { useAuthStore } from '@/stores/auth';
import { createChatSocket } from '@/api/chatSocket';
import { isGroupedMessage, shouldShowTime } from '@/utils/groupChat';
import GroupChatHeader from '@/components/challenge/group/chat/GroupChatHeader.vue';
import GroupChatDateDivider from '@/components/challenge/group/chat/GroupChatDateDivider.vue';
import GroupChatUnreadDivider from '@/components/challenge/group/chat/GroupChatUnreadDivider.vue';
import GroupChatBubble from '@/components/challenge/group/chat/GroupChatBubble.vue';
import GroupChatSystemLabel from '@/components/challenge/group/chat/GroupChatSystemLabel.vue';
import GroupChatRecordCard from '@/components/challenge/group/chat/GroupChatRecordCard.vue';
import GroupChatVerdictCard from '@/components/challenge/group/chat/GroupChatVerdictCard.vue';
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
/* 입장 시점의 안 읽은 경계. 이후 새 메시지가 들어와도 선이 따라 내려가지 않게 id 로 고정한다 */
const unreadBoundaryId = ref(null);
const unreadCount = ref(0);
let toastTimer = null;
let isLoadingOlder = false; // 스크롤-업 페이징 중 scrollToBottom 방지용
let socket = null;
let unmounted = false; // enterRoom 대기 중 라우트 이탈 시 소켓을 만들지 않기 위한 가드

/* ── 시스템 메시지 카드 선택 ───────────────────────────── */
/*
 * 서버가 systemType 을 준다(ChatMessageDto). 문구를 파싱해 카드를 고르지 않는다 — 문구가 한 글자만
 * 바뀌어도 화면이 조용히 깨지던 방식이다. systemType 이 없는 메시지(이 필드가 생기기 전에 저장된 것)는
 * 문구만 있는 pill 로 떨어진다.
 */
const RECORD_TYPES = ['VIOLATION_DETECTED', 'TRIAL_OPENED', 'DEFENSE_REGISTERED'];

function systemView(msg) {
    if (msg.systemType === 'VERDICT_CONFIRMED') return 'verdict';
    if (RECORD_TYPES.includes(msg.systemType)) return 'record';
    return 'pill';
}

/* 라벨을 붙일 대상은 "카드로 그려지는" 시스템 메시지다. 폴백 pill 은 라벨 없이 조용히 흐른다 */
function isRecordCard(msg) {
    return Boolean(msg?.isSystem) && systemView(msg) !== 'pill';
}

/* ── 메시지 그룹핑 (날짜 · 안 읽은 경계 · 연속 발화) ───── */
/* msg.sentAt 은 어댑터(api/groupChatAdapter.js)가 만든 Date 다. 해석 못 한 값은 null 이라
   그때는 구분선을 넣지 않는다 — 예전엔 NaN 이 흘러들어 "NaN월 NaN일" 이 찍혔다.
   묶기 규칙 두 가지는 utils/groupChat.js 에 있다(이름·아바타는 5분 창, 시간은 분 단위). */

const groupedMessages = computed(() => {
    const items = [];
    let lastDateKey = '';
    let prev = null;

    for (const msg of store.messages) {
        if (msg.messageId === unreadBoundaryId.value) {
            items.push({ type: 'unread', key: `unread-${msg.messageId}` });
        }

        const d = msg.sentAt;
        if (d) {
            const dateKey = `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`;
            if (dateKey !== lastDateKey) {
                lastDateKey = dateKey;
                items.push({ type: 'date', label: dateLabel(d), key: `date-${dateKey}` });
                prev = null; // 날짜가 바뀌면 연속 발화도 끊는다
            }
        }

        items.push({
            type: 'message',
            data: msg,
            key: msg.messageId,
            grouped: isGroupedMessage(prev, msg),
            /* 재판 기록이 잇달아 오면 "재판 시스템" 라벨은 첫 장에만 붙인다 */
            showSystemLabel: isRecordCard(msg) && !isRecordCard(prev),
        });
        prev = msg;
    }

    /*
     * 시간 표시는 "다음 줄"을 봐야 정해진다. 바로 아래가 같은 사람의 같은 분 메시지면 이 줄의
     * 시간은 숨기고 묶음의 마지막 줄에만 남긴다. 사이에 날짜 구분선이나 안 읽은 경계가 끼면
     * next 를 넘기지 않아 구분선 위 줄에는 시간이 남는다.
     */
    for (let i = 0; i < items.length; i += 1) {
        if (items[i].type !== 'message') continue;
        const next = items[i + 1];
        items[i].showTime = shouldShowTime(
            items[i].data,
            next?.type === 'message' ? next.data : null,
        );
    }
    return items;
});

function dateLabel(d) {
    const today = new Date();
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);

    if (isSameDay(d, today)) return '오늘';
    if (isSameDay(d, yesterday)) return '어제';
    return `${d.getMonth() + 1}월 ${d.getDate()}일`;
}

function isSameDay(a, b) {
    return (
        a.getFullYear() === b.getFullYear() &&
        a.getMonth() === b.getMonth() &&
        a.getDate() === b.getDate()
    );
}

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

    markUnreadBoundary();
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

/*
 * 입장과 동시에 서버에서 읽음 처리가 끝나므로, "어디부터 새 메시지였는지" 는 이 순간에만 알 수 있다.
 * 불러온 메시지 전부가 새 메시지면 선을 그리지 않는다 — 맨 위에 붙어 아무것도 구분해 주지 못한다.
 */
function markUnreadBoundary() {
    const count = store.roomInfo?.unreadCount ?? 0;
    const list = store.messages;
    if (count > 0 && count < list.length) {
        unreadBoundaryId.value = list[list.length - count].messageId;
        unreadCount.value = count;
    }
}

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
        <!--
          우상단 「더보기」 버튼은 2026-08-19 에 걷어냈다(이슈 #331).
          #125 목업 UI 시절의 자리였는데 연결된 메뉴가 없어 누르면 「준비 중인 기능입니다」만 떴다.
          방 나가기는 GroupFinalizeService 의 deleteRoom 순서 문제와 얽혀 있어 따로 다룬다.
        -->
        <GroupChatHeader :room-info="store.roomInfo" />

        <!-- 종료된 챌린지: 대화가 챌린지 종료와 함께 이미 삭제됐다 -->
        <div v-if="store.closed" class="chat-view__closed">
            <p class="chat-view__closed-title">종료된 챌린지예요</p>
            <p class="chat-view__closed-desc">대화 내용은 챌린지가 끝나면서 사라졌어요.</p>
        </div>

        <!-- 그 외 진입 실패 (예: 참여자가 아님) -->
        <div v-else-if="store.error" class="chat-view__error">
            <StateError :message="store.error.message" @retry="retryEnter" />
        </div>

        <template v-else>
            <div ref="chatScrollEl" class="chat-view__scroll" @scroll="handleScroll">
                <div v-if="store.loading && store.messages.length === 0" class="chat-view__loading">
                    메시지를 불러오는 중...
                </div>

                <!--
                  로드에 성공했는데 메시지가 0건인 방(이슈 #323).
                  이 분기가 없어 새로 만든 방이 아무 문구 없는 흰 화면이었다.
                -->
                <div v-else-if="store.messages.length === 0" class="chat-view__empty">
                    <p class="chat-view__empty-title">아직 오간 말이 없어요</p>
                    <p class="chat-view__empty-desc">
                        {{
                            store.isEnded
                                ? '이 챌린지는 끝났어요.'
                                : '첫 마디를 남겨 재판을 열어보세요.'
                        }}
                    </p>
                </div>

                <template v-for="item in groupedMessages" :key="item.key">
                    <GroupChatDateDivider v-if="item.type === 'date'" :label="item.label" />

                    <GroupChatUnreadDivider
                        v-else-if="item.type === 'unread'"
                        :count="unreadCount"
                    />

                    <!-- 재판 기록 (적발 · 개시 · 변론) -->
                    <template v-else-if="item.data.isSystem && systemView(item.data) === 'record'">
                        <GroupChatSystemLabel v-if="item.showSystemLabel" />
                        <GroupChatRecordCard :message="item.data" />
                    </template>

                    <!-- 판결 확정 -->
                    <template v-else-if="item.data.isSystem && systemView(item.data) === 'verdict'">
                        <GroupChatSystemLabel v-if="item.showSystemLabel" />
                        <GroupChatVerdictCard :message="item.data" />
                    </template>

                    <!-- systemType 이 없는 옛 시스템 메시지 -->
                    <GroupChatSystemPill v-else-if="item.data.isSystem" :message="item.data" />

                    <!-- 참여자 메시지. v-else 라 어떤 type 이든 화면에서 사라지지 않는다 -->
                    <GroupChatBubble
                        v-else
                        :message="item.data"
                        :is-mine="isMine(item.data)"
                        :grouped="item.grouped"
                        :show-time="item.showTime"
                        :avatar-url="store.imageOf(item.data.senderId)"
                    />
                </template>
            </div>

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
    padding: var(--tt-space-2) var(--tt-screen-padding) var(--tt-space-5);
    display: flex;
    flex-direction: column;
    gap: 10px;
    scrollbar-width: none;
}

.chat-view__scroll::-webkit-scrollbar {
    display: none;
}

/*
 * 세로 flex 컨테이너의 자식은 기본적으로 줄어든다. 시스템 카드는 overflow:hidden 이라
 * 줄어든 만큼 내용이 잘려 나갔다 — 실제로 카드가 30px 높이로 눌린 채 배포됐다.
 */
.chat-view__scroll > * {
    flex: none;
}

.chat-view__loading {
    text-align: center;
    color: var(--tt-text-muted);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-semibold);
    padding: var(--tt-space-6) 0;
}

/* 빈 방(이슈 #323). 스크롤 영역이 flex 라 auto 마진으로 가운데에 세운다. */
.chat-view__empty {
    margin: auto 0;
    text-align: center;
    padding: var(--tt-space-6) var(--tt-screen-padding);
}

.chat-view__empty-title {
    color: var(--tt-text);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
}

.chat-view__empty-desc {
    margin-top: var(--tt-space-2);
    color: var(--tt-text-muted);
    font-size: var(--tt-fs-caption);
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
}

.chat-view__closed-title {
    margin: 0;
    font-size: var(--tt-fs-subtitle);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.chat-view__closed-desc {
    margin: 0;
    font-size: var(--tt-fs-body);
    color: var(--tt-text-muted);
}

.chat-view__error {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
}
</style>
