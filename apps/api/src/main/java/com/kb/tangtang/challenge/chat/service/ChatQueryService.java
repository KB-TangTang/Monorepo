package com.kb.tangtang.challenge.chat.service;

import com.kb.tangtang.challenge.chat.domain.ChatMessage;
import com.kb.tangtang.challenge.chat.dto.ChatMessageDto;
import com.kb.tangtang.challenge.chat.dto.ChatMessagePageDto;
import com.kb.tangtang.challenge.chat.dto.ChatRoomDto;
import com.kb.tangtang.challenge.chat.store.ChatMessageStore;
import com.kb.tangtang.challenge.domain.ChallengeGroup;
import com.kb.tangtang.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
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

    /** 조회 개수 상한. Redis LRANGE 로 그대로 전달되므로 방 전체 유출(limit<=0)·과도한 조회를 막는다 */
    private static final int MAX_LIMIT = 100;
    private static final int MIN_LIMIT = 1;

    private final ChatRoomAccessService access;
    private final ChatMessageStore store;
    private final Clock clock;

    /*
     * 생성자가 둘이면 스프링이 어느 쪽을 쓸지 스스로 고르지 못한다 — 표시가 없으면 기본 생성자를
     * 찾다가 NoSuchMethodException 으로 루트 컨텍스트 전체가 죽는다. 실제로 그렇게 배포가 깨졌다.
     */
    @Autowired
    public ChatQueryService(ChatRoomAccessService access, ChatMessageStore store) {
        this(access, store, Clock.system(ZoneId.of("Asia/Seoul")));
    }

    ChatQueryService(ChatRoomAccessService access, ChatMessageStore store, Clock clock) {
        this.access = access;
        this.store = store;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ChatRoomDto room(long groupId, long userId) {
        ChallengeGroup group = access.verifyCanEnter(groupId, userId);
        LocalDate today = LocalDate.now(clock);
        return new ChatRoomDto(
                groupId,
                group.getGroupName(),
                group.getStatus(),
                access.memberIdsOf(groupId).size(),
                store.unreadOf(groupId, userId),
                dayIndexOf(group.getStartDate(), today),
                daysLeftOf(group.getEndDate(), today));
    }

    /** 시작일이 1일차. 아직 시작 전이면 0 을 주고 화면이 일차 표기를 생략한다 */
    private int dayIndexOf(LocalDate startDate, LocalDate today) {
        if (startDate == null || today.isBefore(startDate)) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(startDate, today) + 1;
    }

    /** 종료일 당일이면 0(D-day). 이미 지났으면 음수 — 화면이 "종료" 로 바꿔 표시한다 */
    private long daysLeftOf(LocalDate endDate, LocalDate today) {
        if (endDate == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(today, endDate);
    }

    @Transactional(readOnly = true)
    public ChatMessagePageDto messages(long groupId, long userId, Long before, Long after, int limit) {
        if (before != null && after != null) {
            throw new BusinessException("INVALID_REQUEST", "before 와 after 는 함께 쓸 수 없어요.");
        }
        if (limit < MIN_LIMIT || limit > MAX_LIMIT) {
            throw new BusinessException("INVALID_REQUEST", "조회 개수는 1~100 사이여야 해요.");
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
