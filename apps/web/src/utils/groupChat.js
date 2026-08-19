/*
 * 그룹 채팅 말풍선 묶기 규칙. GroupChatView 가 목록을 만들 때 쓴다.
 *
 * 규칙이 두 개인데 기준이 서로 다르다. 한 곳에 모아 둔 이유가 그것이다.
 *   - 이름·아바타 숨김: 같은 사람이 5분 안에 이어 보내면 반복하지 않는다 (isGroupedMessage)
 *   - 시간 숨김:        같은 사람이 같은 "분"에 이어 보내면 마지막 줄에만 남긴다 (shouldShowTime)
 *
 * 시간까지 5분 창으로 묶으면 10:01 에 보낸 줄의 시간이 사라지고 10:05 만 남아 정보가 준다.
 * 분 단위로 끊으면 연달아 친 줄만 합쳐지고 분이 바뀌면 다시 보인다 — 카카오톡과 같은 방식이다.
 *
 * 메시지는 api/groupChatAdapter.js 가 정규화한 모양이다
 * ({ messageId, type, isSystem, senderId, senderName, content, sentAt: Date|null }).
 */

/** 이름·아바타를 반복하지 않을 연속 발화 판정 기준 */
export const GROUPING_WINDOW_MS = 5 * 60 * 1000;

/** 같은 사람이 5분 안에 잇달아 보낸 메시지면 이름·아바타를 반복하지 않는다 */
export function isGroupedMessage(prev, msg) {
    if (!prev || prev.isSystem || msg.isSystem) return false;
    if (Number(prev.senderId) !== Number(msg.senderId)) return false;
    if (!prev.sentAt || !msg.sentAt) return false;
    return msg.sentAt - prev.sentAt < GROUPING_WINDOW_MS;
}

/**
 * 이 메시지에 시간을 찍을지 정한다.
 *
 * `next` 는 화면에서 바로 아래에 오는 참여자 메시지다. 사이에 날짜 구분선이나
 * "여기부터 새 메시지" 경계가 끼면 호출부가 null 을 넘긴다 — 구분선 위 줄에는 시간을 남긴다.
 */
export function shouldShowTime(msg, next) {
    /* 서버가 시각을 안 줬거나 해석에 실패한 메시지다. 빈 자리를 만들지 않는다 */
    if (!msg?.sentAt) return false;
    if (!next || msg.isSystem || next.isSystem) return true;
    if (Number(next.senderId) !== Number(msg.senderId)) return true;
    if (!next.sentAt) return true;
    return !isSameMinute(msg.sentAt, next.sentAt);
}

function isSameMinute(a, b) {
    return (
        a.getFullYear() === b.getFullYear() &&
        a.getMonth() === b.getMonth() &&
        a.getDate() === b.getDate() &&
        a.getHours() === b.getHours() &&
        a.getMinutes() === b.getMinutes()
    );
}

/*
 * ── 판결 카드 표시 규칙 (이슈 #304) ───────────────────────────────────
 *
 * 카드 안에 두지 않고 여기로 뺐다. 이 저장소에는 SFC 렌더링 하네스가 없어
 * 컴포넌트 안의 computed 는 소스를 정규식으로 훑는 방식으로밖에 검사하지 못한다 —
 * 「0:0 을 숨긴다」 같은 규칙은 그 방식으로 지켜지지 않는다.
 *
 * 입력은 groupChatAdapter 가 정규화한 모양이다
 * ({ outcome: 'GUILTY'|'INNOCENT', guiltyVotes: number|null, innocentVotes: number|null,
 *    livesLost: number }) 또는 null.
 */

/**
 * 「투표 4:2 · 목숨 1 차감」 을 이루는 조각들.
 *
 * 표를 숨기는 경우가 둘이다.
 *   - null : 투표라는 절차가 없었다(혐의 인정). 없는 절차를 지어내지 않는다
 *   - 0:0  : 아무도 던지지 않아 무죄 추정으로 끝났다. "투표 0:0" 은 정보가 아니라 오해다
 *
 * 목숨은 실제로 깎였을 때만 적는다. 남은 목숨이 0이던 유죄는 서버가 livesLost 0 을 주고,
 * 그때 「목숨 1 차감」을 적으면 문구("남은 목숨이 없어요")와 정반대가 된다.
 *
 * @returns {string[]} 그릴 조각이 없으면 빈 배열 — 카드는 줄 자체를 그리지 않는다
 */
export function verdictMetaParts(verdict) {
    if (!verdict) return [];

    const parts = [];
    const { guiltyVotes, innocentVotes, livesLost } = verdict;
    const hasVotes =
        guiltyVotes !== null &&
        guiltyVotes !== undefined &&
        innocentVotes !== null &&
        innocentVotes !== undefined;

    if (hasVotes && guiltyVotes + innocentVotes > 0) {
        parts.push(`투표 ${guiltyVotes}:${innocentVotes}`);
    }
    if (Number(livesLost) > 0) {
        parts.push(`목숨 ${livesLost} 차감`);
    }
    return parts;
}

/**
 * 도장 종류. 문구에서 "유죄" 를 찾아 고르지 않는다 — 문구를 한 글자만 고쳐도 무죄 판결에
 * 붉은 도장이 찍힌다.
 *
 * @returns {'GUILTY'|'INNOCENT'|null} null 이면 중립 도장을 그린다.
 *          이 필드가 생기기 전에 Redis 에 쌓인 판결 메시지가 그 경우다
 */
export function verdictStampKind(verdict) {
    if (verdict?.outcome === 'GUILTY' || verdict?.outcome === 'INNOCENT') {
        return verdict.outcome;
    }
    return null;
}
