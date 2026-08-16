package com.kb.tangtang.challenge.chat.controller;

import com.kb.tangtang.challenge.chat.dto.ChatMessagePageDto;
import com.kb.tangtang.challenge.chat.dto.ChatRoomDto;
import com.kb.tangtang.challenge.chat.service.ChatQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    private static final long GROUP_ID = 7L;
    private static final long USER_ID = 3L;

    @Mock private ChatQueryService queryService;

    @Test
    @DisplayName("방 조회 응답을 ApiResponse 로 감싼다")
    void wrapsRoomInApiResponse() {
        ChatController controller = new ChatController(queryService);
        ChatRoomDto dto = new ChatRoomDto(GROUP_ID, "절약단", "ACTIVE", 4, 2, 3, 4);
        when(queryService.room(GROUP_ID, USER_ID)).thenReturn(dto);

        var response = controller.room(GROUP_ID, USER_ID);

        assertTrue(response.isSuccess());
        assertEquals(dto, response.getData());
    }

    @Test
    @DisplayName("limit 을 주지 않으면 50 으로 조회한다")
    void defaultsLimitTo50() {
        ChatController controller = new ChatController(queryService);
        when(queryService.messages(GROUP_ID, USER_ID, null, null, 50))
                .thenReturn(new ChatMessagePageDto(List.of(), false));

        controller.messages(GROUP_ID, null, null, 50, USER_ID);

        verify(queryService).messages(GROUP_ID, USER_ID, null, null, 50);
    }

    @Test
    @DisplayName("읽음 처리는 data 없는 성공 응답이다")
    void markReadReturnsEmptyOk() {
        ChatController controller = new ChatController(queryService);

        var response = controller.markRead(GROUP_ID, USER_ID);

        assertTrue(response.isSuccess());
        verify(queryService).markRead(GROUP_ID, USER_ID);
    }
}
