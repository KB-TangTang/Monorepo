/**
 * 서버 DTO → 화면 모델 정규화 (이슈 #174).
 *
 * **정규화는 여기 한 곳에서만 한다.** 스토어의 REST 진입점(enterRoom · loadOlderMessages)과
 * 소켓 진입점(appendMessage · catchUp)이 <b>같은 함수</b>를 통과해야 두 경로의 필드 이름·형식이
 * 갈라지지 않는다. 실제로 갈라진 적이 있다 — 화면이 목업 스키마(messageType · createdAt ·
 * senderName · challengeName)를 보는 동안 서버는 {type, sentAt, senderNickname, groupName} 을
 * 보내고 있었고, 그 결과 텍스트 메시지가 한 건도 렌더링되지 않았다.
 *
 * 서버가 주지 않는 값(스티커 등)은 <b>지어내지 않는다.</b> 화면에서 생략하거나 서버가
 * 주는 값으로 대체한다. 판결 승패도 오래 그랬는데, 이제는 서버가 verdict 로 준다(이슈 #304).
 *
 * 서버 계약 (docs/API_SPEC.md 「그룹 채팅」):
 *   메시지 { messageId, type: 'TEXT'|'SYSTEM', senderId, senderNickname, content, sentAt,
 *            systemType, deepLink, caseNo, verdict }   ← 뒤 넷은 SYSTEM 메시지에만 있다
 *   verdict { outcome: 'GUILTY'|'INNOCENT', guiltyVotes, innocentVotes, livesLost }
 *            ← 판결 확정 메시지에만 있다. 표는 투표를 거치지 않은 판결(혐의 인정)에서 null 이다
 *   방     { groupId, groupName, status: 'RECRUITING'|'ACTIVE'|'JUDGING'|'CLOSED', memberCount,
 *            unreadCount, dayIndex, daysLeft }
 */

/** 대화가 이미 삭제된 상태. 서버도 이 상태의 방은 조회 자체를 막는다(CHAT_ROOM_CLOSED) */
const STATUS_CLOSED = 'CLOSED';
/** 재판 중. 대화가 가장 활발한 구간이라 입장은 허용된다 */
const STATUS_JUDGING = 'JUDGING';

/**
 * sentAt 파싱.
 *
 * 서버는 ISO-8601 문자열을 보낸다(REST·STOMP 동일 — WebSocketConfig#jsonConverter).
 * 과거 STOMP 경로가 [2026,8,16,...] 숫자 배열을 보내던 시기가 있어 그 모양도 받아 준다.
 *
 * @returns {Date|null} 해석할 수 없으면 null — 화면은 날짜 구분선·시각을 생략한다
 */
function parseSentAt(value) {
    if (value instanceof Date) {
        return Number.isNaN(value.getTime()) ? null : value;
    }
    if (Array.isArray(value)) {
        const [year, month, day, hour = 0, minute = 0, second = 0] = value;
        if (year === undefined || month === undefined || day === undefined) return null;
        return new Date(year, month - 1, day, hour, minute, second);
    }
    if (typeof value === 'string' && value.length > 0) {
        const parsed = new Date(value);
        return Number.isNaN(parsed.getTime()) ? null : parsed;
    }
    return null;
}

/**
 * 메시지 한 건.
 *
 * 이미 정규화된 객체를 다시 넣어도 같은 결과가 나오도록(idempotent) 두 이름을 모두 받는다 —
 * catchUp 처럼 REST 응답을 appendMessage 로 흘려보내는 경로가 있어서다.
 */
export function toChatMessage(dto) {
    const type = dto.type ?? 'TEXT';
    return {
        messageId: Number(dto.messageId),
        type,
        isSystem: type === 'SYSTEM',
        senderId: dto.senderId ?? null,
        senderName: dto.senderNickname ?? dto.senderName ?? '',
        content: dto.content ?? '',
        sentAt: parseSentAt(dto.sentAt),
        /*
         * 시스템 메시지 전용. systemType 이 화면의 카드 모양을 정한다.
         * 이 필드들이 생기기 전에 저장된 메시지는 null 이라, 화면은 그때도 그려져야 한다.
         */
        systemType: dto.systemType ?? null,
        deepLink: dto.deepLink ?? null,
        caseNo: dto.caseNo ?? null,
        verdict: toVerdict(dto.verdict),
    };
}

/**
 * 판결 결과.
 *
 * outcome 이 없으면 통째로 null 로 만든다 — 이 필드가 생기기 전(#304)에 Redis 에 쌓인 판결
 * 메시지가 그렇다. 카드는 그때 예전처럼 중립 도장을 찍는다.
 *
 * 표는 **0 과 null 을 구분해서** 넘긴다. 0:0 은 「아무도 투표하지 않았다」(무죄 추정)이고
 * null 은 「투표라는 절차가 없었다」(혐의 인정)라서, 둘을 뭉개면 카드가 없는 표를 지어낸다.
 */
function toVerdict(dto) {
    if (!dto || (dto.outcome !== 'GUILTY' && dto.outcome !== 'INNOCENT')) return null;
    const votes = (value) => (value === null || value === undefined ? null : Number(value));
    return {
        outcome: dto.outcome,
        guiltyVotes: votes(dto.guiltyVotes),
        innocentVotes: votes(dto.innocentVotes),
        livesLost: Number(dto.livesLost ?? 0),
    };
}

/** 메시지 페이지 (`{ messages, hasMore }`) */
export function toChatMessagePage(page) {
    return {
        messages: (page?.messages ?? []).map(toChatMessage),
        hasMore: page?.hasMore === true,
    };
}

/** 채팅방 헤더 정보 */
export function toChatRoom(dto) {
    const status = dto.status ?? '';
    return {
        groupId: dto.groupId ?? null,
        groupName: dto.groupName ?? '',
        status,
        memberCount: Number(dto.memberCount ?? 0),
        unreadCount: Number(dto.unreadCount ?? 0),
        /* 서버(Asia/Seoul)가 계산해 준다. 시작 전이면 dayIndex 0, 종료 후면 daysLeft 가 음수다 */
        dayIndex: Number(dto.dayIndex ?? 0),
        daysLeft: Number(dto.daysLeft ?? 0),
        isEnded: status === STATUS_CLOSED,
        isJudging: status === STATUS_JUDGING,
    };
}
