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
import { fetchGroupDetail, fetchMyGroupChallenges } from '@/api/groupChallenge';
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
    /**
     * 발신자 id → { nickname, profileImage }.
     *
     * **메시지에 프로필 값을 실어 두지 않는 이유가 여기 있다.** 채팅 메시지는 Redis 에
     * 저장돼 발송 시점 값이 그대로 굳는다. 실제로 `senderNickname` 이 그렇게 저장돼 있어,
     * 닉네임을 바꿔도 과거 메시지는 옛 이름으로 남았다(이슈 #414). 이미지도 메시지에 넣었다면
     * 같은 증상이 났을 것이다.
     *
     * 그래서 메시지는 `senderId` 만 신뢰하고, 보이는 값(이름·사진)은 그릴 때마다 이 표에서 찾는다.
     */
    const membersById = ref({});

    /* ── 방 밖에서 보는 채팅 요약 (앱 단위) ─────────────── */
    /*
     * 위의 상태는 **지금 열어 둔 방 하나**의 것이라 leaveRoom() 이 전부 비운다.
     * 아래 둘은 반대다 — 홈 목록과 지방법원 토글 점이 **방 밖에서** 읽으므로
     * 앱이 살아 있는 동안 유지된다. leaveRoom() 이 건드리지 않는 것이 핵심이다.
     */

    /** groupId → 안 읽은 개수 */
    const unreadByGroup = ref({});

    /** groupId → 마지막 메시지 한 줄(`'닉네임: 내용'`). 실시간으로 받은 것이 목록 값보다 최신이다. */
    const lastMessageByGroup = ref({});

    /* ── 파생 ──────────────────────────────────────────── */
    const currentUserId = computed(() => {
        const auth = useAuthStore();
        return auth.user?.id ?? 1; // 목업 기본값 = 유저 1
    });

    /* 서버 상태값은 RECRUITING · ACTIVE · JUDGING · CLOSED 다 (목업의 'ENDED' 는 없다) */
    const isEnded = computed(() => roomInfo.value?.isEnded === true);

    /** 어느 방이든 안 읽은 게 있는가. 지방법원 토글의 점이 이 값 하나만 본다. */
    const hasUnreadChat = computed(() =>
        Object.values(unreadByGroup.value).some((count) => count > 0),
    );

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
            /* 서버 카운터를 0 으로 만들었으니 화면이 보는 사본도 같이 내린다 */
            clearUnread(id);
            await loadMembers(id);
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
     * 멤버 프로필 이미지를 받아 발신자 id 로 찾을 수 있게 표로 만든다.
     *
     * 채팅 API 는 이미지를 주지 않는다(`ChatMessageDto` 에 그 필드가 없다). 그룹 상세가
     * 이미 `members[].profileImage` 를 내려주므로 그걸 쓴다.
     *
     * ⚠ **실패해도 채팅은 그대로 뜬다.** 이미지는 없으면 이니셜 원으로 떨어지는 장식이라,
     *   여기서 던지면 곁가지 때문에 대화 전체가 안 보이게 된다. 그래서 enterRoom 의
     *   try 안에 있으면서도 자기 오류는 자기가 삼킨다.
     */
    async function loadMembers(id) {
        try {
            const detail = await fetchGroupDetail(id);
            membersById.value = Object.fromEntries(
                (detail?.members ?? [])
                    .filter((m) => m?.userId != null)
                    .map((m) => [
                        String(m.userId),
                        { nickname: m.nickname ?? null, profileImage: m.profileImage ?? null },
                    ]),
            );
        } catch {
            membersById.value = {};
        }
    }

    /** 발신자 id 로 프로필 이미지를 찾는다. 없으면 null — UserAvatar 가 이니셜로 그린다. */
    function imageOf(senderId) {
        if (senderId == null) return null;
        return membersById.value[String(senderId)]?.profileImage ?? null;
    }

    /**
     * 발신자 id 로 **지금** 닉네임을 찾는다.
     *
     * 못 찾으면 null 이다 — 그때는 화면이 메시지에 실려 온 발송 시점 이름으로 되돌아간다.
     * 나간 참여자·삭제된 계정처럼 멤버 목록에 없는 사람의 과거 메시지가 「익명」이 되지 않게
     * 하려는 것이다. 지금 있는 사람이면 언제나 이 표가 이긴다.
     */
    function nicknameOf(senderId) {
        if (senderId == null) return null;
        return membersById.value[String(senderId)]?.nickname ?? null;
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

    /* ── 방 밖에서 보는 채팅 요약 ───────────────────────── */

    /**
     * 그룹 목록 응답으로 요약을 덮어쓴다. **서버 값이 언제나 이긴다.**
     *
     * 놓친 SSE(연결이 끊겨 있던 동안 온 것)를 여기서 메우는 것이 이 함수의 본래 일이다.
     * 목록에 없는 그룹(종료·탈퇴)은 통째로 사라져야 하므로 병합이 아니라 교체다.
     */
    function syncChatSummary(challenges = []) {
        const unread = {};
        const last = {};
        for (const ch of challenges) {
            if (ch?.id == null) continue;
            unread[ch.id] = ch.unreadChatCount ?? 0;
            if (ch.lastChatMessage) last[ch.id] = ch.lastChatMessage;
        }
        unreadByGroup.value = unread;
        lastMessageByGroup.value = last;
    }

    /**
     * 알림 SSE 의 `chat` 이벤트를 받는다 (서버: ChatMessageService.pushChatAlert).
     *
     * **이 알림은 알림함에 저장되지 않는다**(팀 결정 2026-08-15). 여기서 흘리면 영영 사라지므로
     * 화면이 열려 있든 아니든 스토어가 받아 둔다 — 그래서 채팅방 컴포넌트가 아니라 여기에 있다.
     *
     * 서버가 **메시지마다 빠짐없이** 쏘므로 `+1` 이 곧 정확한 수다. 한때 30초 쿨다운이 있어
     * 연타로 온 열 통이 이벤트 하나로 뭉쳤고 그래서 이 값이 어림값이었다 — 이슈 #423 에서
     * 서버 쿨다운을 없애며 해소했다(`ChatMessageService#pushChatAlert` 주석 참고).
     * 그래도 SSE 가 끊겨 있던 동안 온 것은 못 받으므로 목록을 받을 때 syncChatSummary 가 메운다.
     *
     * 미리보기 문자열은 **서버의 목록 응답과 같은 모양(`'닉네임: 내용'`)으로 조립한다.**
     * 서버 쪽 짝은 `ChallengeGroupService#preview` 다. 한쪽만 바꾸면 새로고침 한 번에 형식이 바뀐다.
     */
    function receiveChatAlert(payload) {
        const id = Number(payload?.groupId);
        if (!Number.isFinite(id)) return;
        /* 이미 그 방에 들어와 읽고 있다. 서버도 제외하지만 입장 직후 경합을 한 겹 더 막는다 */
        if (groupId.value === id) return;

        unreadByGroup.value = {
            ...unreadByGroup.value,
            [id]: (unreadByGroup.value[id] ?? 0) + 1,
        };
        if (payload.content) {
            const who = payload.senderNickname;
            lastMessageByGroup.value = {
                ...lastMessageByGroup.value,
                [id]: who ? `${who}: ${payload.content}` : payload.content,
            };
        }
    }

    /**
     * 로그인 직후 씨를 뿌린다. 실패는 삼킨다 — 점 하나 때문에 첫 화면이 막히면 안 된다.
     *
     * **시작 전(RECRUITING)까지 묻는 것이 중요하다.** 채팅에는 상태 제한이 없어서
     * 방을 만든 직후부터 대화가 된다. 게다가 서버가 시작일을 반드시 내일 이후로 막으므로
     * 만들고 나서 처음 떠드는 구간이 통째로 RECRUITING 이다 — `['ACTIVE']` 만 물으면
     * 정작 대화가 오가는 방이 배지 계산에서 통째로 빠진다. 홈이 부르는 것과 같은 조합이다.
     */
    async function refreshChatSummary() {
        try {
            syncChatSummary(await fetchMyGroupChallenges(['ACTIVE', 'RECRUITING']));
        } catch {
            /* 무시 */
        }
    }

    function clearUnread(id) {
        if (id == null || unreadByGroup.value[id] == null) return;
        unreadByGroup.value = { ...unreadByGroup.value, [id]: 0 };
    }

    /** 로그아웃 등 세션 종료. 다음 사람에게 앞사람의 대화가 보이면 안 된다. */
    function clearChatSummary() {
        unreadByGroup.value = {};
        lastMessageByGroup.value = {};
    }

    /** 채팅방 퇴장: 상태 초기화 */
    function leaveRoom() {
        groupId.value = null;
        messages.value = [];
        roomInfo.value = null;
        membersById.value = {};
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
        membersById,
        imageOf,
        nicknameOf,
        enterRoom,
        loadOlderMessages,
        appendMessage,
        catchUp,
        leaveRoom,
        /* 방 밖 요약 — leaveRoom() 이 지우지 않는다 */
        unreadByGroup,
        lastMessageByGroup,
        hasUnreadChat,
        syncChatSummary,
        receiveChatAlert,
        refreshChatSummary,
        clearUnread,
        clearChatSummary,
    };
});
