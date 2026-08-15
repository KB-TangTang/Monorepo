package com.kb.tangtang.challenge.chat.service;

import com.kb.tangtang.challenge.chat.store.ChatMessageStore;
import com.kb.tangtang.challenge.domain.ChallengeGroup;
import com.kb.tangtang.challenge.domain.ChallengeGroupStatus;
import com.kb.tangtang.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatQueryServiceTest {

    private static final long GROUP_ID = 7L;
    private static final long USER_ID = 3L;

    @Mock private ChatRoomAccessService access;
    @Mock private ChatMessageStore store;

    @InjectMocks private ChatQueryService service;

    @BeforeEach
    void setUp() {
        // 판정 1: ChallengeGroup 은 setter 가 없다(id 제외). 빌더로 만든다.
        // 판정 2: status 필드는 String 이다. ChallengeGroupStatus.name() 을 넣는다.
        ChallengeGroup group = ChallengeGroup.builder()
                .id(GROUP_ID)
                .groupName("절약단")
                .status(ChallengeGroupStatus.ACTIVE.name())
                .endDate(LocalDate.of(2026, 9, 30))
                .build();
        lenient().when(access.verifyCanEnter(GROUP_ID, USER_ID)).thenReturn(group);
    }

    @Test
    @DisplayName("before 도 after 도 없으면 최근 구간을 읽는다")
    void readsRecentWhenNoCursor() {
        when(store.findRecent(GROUP_ID, 50)).thenReturn(List.of());

        service.messages(GROUP_ID, USER_ID, null, null, 50);

        verify(store).findRecent(GROUP_ID, 50);
    }

    @Test
    @DisplayName("before 가 있으면 그 앞 구간을 읽는다")
    void readsBeforeCursor() {
        when(store.findBefore(GROUP_ID, 21L, 50)).thenReturn(List.of());

        service.messages(GROUP_ID, USER_ID, 21L, null, 50);

        verify(store).findBefore(GROUP_ID, 21L, 50);
    }

    @Test
    @DisplayName("after 가 있으면 그 뒤 구간을 읽는다 (재연결 보충)")
    void readsAfterCursor() {
        when(store.findAfter(GROUP_ID, 10L, 50)).thenReturn(List.of());

        service.messages(GROUP_ID, USER_ID, null, 10L, 50);

        verify(store).findAfter(GROUP_ID, 10L, 50);
    }

    @Test
    @DisplayName("before 와 after 를 동시에 주면 INVALID_REQUEST 다")
    void rejectsBothCursors() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.messages(GROUP_ID, USER_ID, 21L, 10L, 50));

        assertEquals("INVALID_REQUEST", ex.getCode());
    }

    @Test
    @DisplayName("limit=0 이면 INVALID_REQUEST 고 store 를 전혀 호출하지 않는다")
    void rejectsZeroLimit() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.messages(GROUP_ID, USER_ID, null, null, 0));

        assertEquals("INVALID_REQUEST", ex.getCode());
        verifyNoInteractions(store);
    }

    @Test
    @DisplayName("limit 이 음수면 INVALID_REQUEST 고 store 를 전혀 호출하지 않는다")
    void rejectsNegativeLimit() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.messages(GROUP_ID, USER_ID, null, null, -1));

        assertEquals("INVALID_REQUEST", ex.getCode());
        verifyNoInteractions(store);
    }

    @Test
    @DisplayName("limit 이 100 을 넘으면 INVALID_REQUEST 고 store 를 전혀 호출하지 않는다")
    void rejectsLimitOverMax() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.messages(GROUP_ID, USER_ID, null, null, 101));

        assertEquals("INVALID_REQUEST", ex.getCode());
        verifyNoInteractions(store);
    }

    @Test
    @DisplayName("limit=1 은 하한 경계로 정상 조회된다")
    void allowsMinLimit() {
        when(store.findRecent(GROUP_ID, 1)).thenReturn(List.of());

        service.messages(GROUP_ID, USER_ID, null, null, 1);

        verify(store).findRecent(GROUP_ID, 1);
    }

    @Test
    @DisplayName("limit=100 은 상한 경계로 정상 조회된다")
    void allowsMaxLimit() {
        when(store.findRecent(GROUP_ID, 100)).thenReturn(List.of());

        service.messages(GROUP_ID, USER_ID, null, null, 100);

        verify(store).findRecent(GROUP_ID, 100);
    }

    @Test
    @DisplayName("읽음 처리는 권한 검증 후 카운터를 지운다")
    void marksReadAfterVerifying() {
        service.markRead(GROUP_ID, USER_ID);

        verify(access).verifyCanEnter(GROUP_ID, USER_ID);
        verify(store).clearUnread(GROUP_ID, USER_ID);
    }

    @Test
    @DisplayName("방 조회는 내 안 읽은 수를 함께 준다")
    void roomIncludesMyUnread() {
        when(store.unreadOf(GROUP_ID, USER_ID)).thenReturn(4);

        assertEquals(4, service.room(GROUP_ID, USER_ID).getUnreadCount());
    }
}
