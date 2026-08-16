package com.kb.tangtang.challenge.chat.dto;

/**
 * 채팅방 헤더에 필요한 최소 정보.
 *
 * <p>{@code dayIndex} · {@code daysLeft} 는 화면이 "3일차 · D-4" 를 그리기 위한 값이다. 날짜 계산을
 * 프론트로 넘기면 사용자 기기의 시간대·시계에 따라 값이 달라진다 — 챌린지 판정은 서버의 Asia/Seoul
 * 기준이므로 표시도 서버가 계산한다.
 */
public class ChatRoomDto {

    private final long groupId;
    private final String groupName;
    private final String status;
    private final int memberCount;
    private final int unreadCount;
    /** 시작일이면 1. 아직 시작 전이면 0 */
    private final int dayIndex;
    /** 종료일까지 남은 날. 종료일 당일이면 0, 이미 지났으면 음수 */
    private final long daysLeft;

    public ChatRoomDto(long groupId, String groupName, String status,
                       int memberCount, int unreadCount, int dayIndex, long daysLeft) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.status = status;
        this.memberCount = memberCount;
        this.unreadCount = unreadCount;
        this.dayIndex = dayIndex;
        this.daysLeft = daysLeft;
    }

    public long getGroupId() { return groupId; }
    public String getGroupName() { return groupName; }
    public String getStatus() { return status; }
    public int getMemberCount() { return memberCount; }
    public int getUnreadCount() { return unreadCount; }
    public int getDayIndex() { return dayIndex; }
    /** 이름을 `getDDay()` 로 두면 Jackson 이 `DDay` 로 직렬화한다. 프론트가 읽을 이름은 평범해야 한다 */
    public long getDaysLeft() { return daysLeft; }
}
