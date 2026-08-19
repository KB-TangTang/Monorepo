package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.chat.store.ChatMessageStore;
import com.kb.tangtang.challenge.domain.ChallengeGroup;
import com.kb.tangtang.challenge.domain.GroupMember;
import com.kb.tangtang.challenge.mapper.ChallengeGroupMapper;
import com.kb.tangtang.challenge.mapper.GroupMemberMapper;
import com.kb.tangtang.notification.domain.NotificationRequestedEvent;
import com.kb.tangtang.notification.domain.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 그룹 하나의 시작 전이 (이슈 #152).
 *
 * <p>발행된 이벤트를 리스트에 모아 검증한다 — 알림이 실제로 저장되는지까지 보려면 notification
 * 모듈과 DB 가 함께 떠야 하므로, 여기서는 "무엇을 누구에게 발행했는가" 까지만 본다.
 * 발행자는 Mock 대신 람다다. {@code ApplicationEventPublisher} 가 함수형 인터페이스라
 * 그게 더 짧고, 검증도 리스트를 그대로 보면 된다.
 */
@ExtendWith(MockitoExtension.class)
class ChallengeGroupStatusTransitionServiceTest {

    private static final long GROUP_ID = 7L;
    private static final long OWNER_ID = 1L;
    private static final LocalDate START = LocalDate.of(2026, 8, 13);

    @Mock private ChallengeGroupMapper groupMapper;
    @Mock private GroupMemberMapper memberMapper;
    @Mock private ChatMessageStore chatMessageStore;

    private final List<NotificationRequestedEvent> published = new ArrayList<>();
    private ChallengeGroupStatusTransitionService service;

    @BeforeEach
    void setUp() {
        ApplicationEventPublisher publisher =
                event -> published.add((NotificationRequestedEvent) event);
        service = new ChallengeGroupStatusTransitionService(groupMapper, memberMapper, chatMessageStore, publisher);
    }

    @Test
    @DisplayName("참여자가 2명 이상이면 ACTIVE 로 바뀌고 전원에게 시작 알림이 간다")
    void activatesAndNotifiesEveryMember() {
        when(memberMapper.findByGroupIds(anyList()))
                .thenReturn(List.of(member(OWNER_ID), member(2L), member(3L)));
        when(groupMapper.updateStatusIfCurrent(GROUP_ID, "RECRUITING", "ACTIVE")).thenReturn(1);

        assertTrue(service.startOrCancel(group()));

        verify(groupMapper).updateStatusIfCurrent(GROUP_ID, "RECRUITING", "ACTIVE");
        assertEquals(3, published.size(), "방장도 tbl_group_member 에 있으므로 따로 챙기지 않는다");
        assertEquals(List.of(OWNER_ID, 2L, 3L),
                published.stream().map(NotificationRequestedEvent::userId).toList());

        NotificationRequestedEvent first = published.get(0);
        assertEquals(NotificationType.GROUP_CHALLENGE_STARTED, first.type());
        assertEquals("커피값 줄이기", first.params().get("groupName"));
        assertEquals("3", first.params().get("days"), "8/13~8/15 는 양끝을 포함해 3일이다");
        assertEquals("/group-challenges/7", first.deepLinkUrl());

        verify(chatMessageStore, never()).deleteRoom(anyLong());
    }

    /**
     * <b>#350 회귀 방지.</b> 「시작일 23:59까지 초대」를 맞추려고 이 삭제를 하루 미뤘던 적이 있다.
     * 미루면 시작일 하루가 RECRUITING 인 채 흘러가고, 그 날 늦게 누가 들어오면 다음 날 ACTIVE 가
     * 되면서 {@code findGroupsToEvaluate} 가 <b>이미 지나간 시작일을 소급 기소</b>한다.
     * 성립 못 한 그룹은 시작일 당일에 지운다 — 「23:59까지」는 성립한 그룹에만 적용된다.
     */
    @Test
    @DisplayName("시작일 당일에 방장 1명뿐이면 미루지 않고 그대로 삭제한다")
    void cancelsWhenAloneAndNotifiesOwnerOnly() {
        when(memberMapper.findByGroupIds(anyList())).thenReturn(List.of(member(OWNER_ID)));
        when(groupMapper.deleteIfCurrent(GROUP_ID, "RECRUITING")).thenReturn(1);

        assertTrue(service.startOrCancel(group()));

        verify(groupMapper).deleteIfCurrent(GROUP_ID, "RECRUITING");
        verify(groupMapper, never()).updateStatusIfCurrent(anyLong(), anyString(), anyString());

        assertEquals(1, published.size());
        assertEquals(OWNER_ID, published.get(0).userId());
        assertEquals(NotificationType.GROUP_CHALLENGE_CANCELED, published.get(0).type());
        assertEquals("커피값 줄이기", published.get(0).params().get("groupName"));
        assertEquals("/group-challenges", published.get(0).deepLinkUrl(),
                "그룹이 사라졌으므로 상세가 아니라 홈으로 보낸다");

        verify(chatMessageStore).deleteRoom(GROUP_ID);
    }

    @Test
    @DisplayName("채팅방 삭제가 실패해도 그룹 삭제·알림은 정상 처리된다 — TTL 이 백스톱이다")
    void cancelSucceedsEvenWhenChatRoomDeleteFails() {
        when(memberMapper.findByGroupIds(anyList())).thenReturn(List.of(member(OWNER_ID)));
        when(groupMapper.deleteIfCurrent(GROUP_ID, "RECRUITING")).thenReturn(1);
        doThrow(new RuntimeException("Redis 연결 실패")).when(chatMessageStore).deleteRoom(GROUP_ID);

        assertTrue(service.startOrCancel(group()));

        assertEquals(1, published.size(), "채팅방 삭제 실패가 알림 발행을 막으면 안 된다");
    }

    @Test
    @DisplayName("이미 전이된 그룹은 알림 없이 건너뛴다 — 배치를 두 번 돌려도 중복 발송이 없다")
    void skipsWhenAlreadyTransitioned() {
        when(memberMapper.findByGroupIds(anyList()))
                .thenReturn(List.of(member(OWNER_ID), member(2L)));
        when(groupMapper.updateStatusIfCurrent(GROUP_ID, "RECRUITING", "ACTIVE")).thenReturn(0);

        assertFalse(service.startOrCancel(group()));

        assertTrue(published.isEmpty(), "compare-and-set 이 0 을 냈으면 알림도 없어야 한다");
        verifyNoInteractions(chatMessageStore);
    }

    /**
     * 조회 시점에는 혼자였지만 그 사이 누가 들어와 다른 실행이 ACTIVE 로 전이시킨 경우.
     * DELETE 의 {@code status = 'RECRUITING'} 조건이 없으면 방금 시작된 그룹이 CASCADE 로 통째로 사라진다.
     */
    @Test
    @DisplayName("삭제 대상이 이미 RECRUITING 이 아니면 아무 일도 하지 않는다 — 정상 시작한 그룹 오삭제 방지")
    void skipsCancelWhenGroupAlreadyStarted() {
        when(memberMapper.findByGroupIds(anyList())).thenReturn(List.of(member(OWNER_ID)));
        when(groupMapper.deleteIfCurrent(GROUP_ID, "RECRUITING")).thenReturn(0);

        assertFalse(service.startOrCancel(group()));

        assertTrue(published.isEmpty(), "0 행이면 이미 다른 쪽이 처리한 것이라 알림도 없어야 한다");
        verifyNoInteractions(chatMessageStore);
    }

    private ChallengeGroup group() {
        return ChallengeGroup.builder()
                .id(GROUP_ID)
                .adminId(OWNER_ID)
                .groupName("커피값 줄이기")
                .maxMembers(6)
                .startDate(START)
                .endDate(START.plusDays(2))
                .status("RECRUITING")
                .build();
    }

    private GroupMember member(long userId) {
        return GroupMember.builder().groupId(GROUP_ID).userId(userId).livesCount(3).build();
    }
}
