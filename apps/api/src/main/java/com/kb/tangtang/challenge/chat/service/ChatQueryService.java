package com.kb.tangtang.challenge.chat.service;

import com.kb.tangtang.challenge.chat.domain.ChatMessage;
import com.kb.tangtang.challenge.chat.dto.ChatMessageDto;
import com.kb.tangtang.challenge.chat.dto.ChatMessagePageDto;
import com.kb.tangtang.challenge.chat.dto.ChatRoomDto;
import com.kb.tangtang.challenge.chat.store.ChatMessageStore;
import com.kb.tangtang.challenge.domain.ChallengeGroup;
import com.kb.tangtang.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 채팅 조회 전용. 쓰기(발송)는 ChatMessageService 가 맡는다.
 *
 * <p>판정 2: {@code ChallengeGroup.status} 는 {@code String} 컬럼이라 {@code .name()} 없이
 * 그대로 {@code ChatRoomDto} 에 넣는다.
 */
@Service
public class ChatQueryService {

    private final ChatRoomAccessService access;
    private final ChatMessageStore store;

    public ChatQueryService(ChatRoomAccessService access, ChatMessageStore store) {
        this.access = access;
        this.store = store;
    }

    @Transactional(readOnly = true)
    public ChatRoomDto room(long groupId, long userId) {
        ChallengeGroup group = access.verifyCanEnter(groupId, userId);
        return new ChatRoomDto(
                groupId,
                group.getGroupName(),
                group.getStatus(),
                access.memberIdsOf(groupId).size(),
                store.unreadOf(groupId, userId));
    }

    @Transactional(readOnly = true)
    public ChatMessagePageDto messages(long groupId, long userId, Long before, Long after, int limit) {
        if (before != null && after != null) {
            throw new BusinessException("INVALID_REQUEST", "before 와 after 는 함께 쓸 수 없어요.");
        }
        access.verifyCanEnter(groupId, userId);

        List<ChatMessage> found;
        if (before != null) {
            found = store.findBefore(groupId, before, limit);
        } else if (after != null) {
            found = store.findAfter(groupId, after, limit);
        } else {
            found = store.findRecent(groupId, limit);
        }

        boolean hasMore = !found.isEmpty() && found.get(0).getMessageId() > 1;
        return new ChatMessagePageDto(
                found.stream().map(ChatMessageDto::from).collect(Collectors.toList()),
                hasMore);
    }

    @Transactional(readOnly = true)
    public void markRead(long groupId, long userId) {
        access.verifyCanEnter(groupId, userId);
        store.clearUnread(groupId, userId);
    }
}
