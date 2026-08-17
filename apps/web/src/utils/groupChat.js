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
