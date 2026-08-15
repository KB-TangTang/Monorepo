package com.kb.tangtang.challenge.chat.service;

import com.kb.tangtang.challenge.chat.store.ChatMessageStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Set;

import static org.mockito.Mockito.verify;

/**
 * 방 개설·삭제가 챌린지 생명주기에 붙어 있는지 본다.
 * 실제 호출 지점은 ChallengeGroupService(생성)와 ChallengeGroupStatusTransitionService(CLOSED)다.
 */
@ExtendWith(MockitoExtension.class)
class ChatRoomLifecycleTest {

    @Mock private ChatMessageStore store;

    @Test
    @DisplayName("방 개설은 참여자와 종료일을 넘겨 TTL 을 건다")
    void initRoomPassesMembersAndEndDate() {
        LocalDate endDate = LocalDate.of(2026, 8, 20);

        store.initRoom(7L, Set.of(3L), endDate);

        verify(store).initRoom(7L, Set.of(3L), endDate);
    }

    @Test
    @DisplayName("CLOSED 전이 시 방을 즉시 삭제한다")
    void deleteRoomOnClose() {
        store.deleteRoom(7L);

        verify(store).deleteRoom(7L);
    }
}
