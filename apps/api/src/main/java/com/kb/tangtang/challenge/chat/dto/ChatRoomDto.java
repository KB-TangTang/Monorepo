package com.kb.tangtang.challenge.chat.dto;

/** 채팅방 헤더에 필요한 최소 정보 */
public class ChatRoomDto {

    private final long groupId;
    private final String groupName;
    private final String status;
    private final int memberCount;
    private final int unreadCount;

    public ChatRoomDto(long groupId, String groupName, String status,
                       int memberCount, int unreadCount) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.status = status;
        this.memberCount = memberCount;
        this.unreadCount = unreadCount;
    }

    public long getGroupId() { return groupId; }
    public String getGroupName() { return groupName; }
    public String getStatus() { return status; }
    public int getMemberCount() { return memberCount; }
    public int getUnreadCount() { return unreadCount; }
}
