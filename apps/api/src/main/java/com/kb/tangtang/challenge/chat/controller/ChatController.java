package com.kb.tangtang.challenge.chat.controller;

import com.kb.tangtang.challenge.chat.dto.ChatMessagePageDto;
import com.kb.tangtang.challenge.chat.dto.ChatRoomDto;
import com.kb.tangtang.challenge.chat.service.ChatQueryService;
import com.kb.tangtang.challenge.docs.ChatControllerDocs;
import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 그룹 채팅방 조회 — 방 정보 · 메시지 목록 · 읽음 처리. 발송(쓰기)은 STOMP 가 맡는다. */
@RestController
@RequestMapping("/api/groups/{groupId}/chat")
public class ChatController implements ChatControllerDocs {

    private final ChatQueryService queryService;

    public ChatController(ChatQueryService queryService) {
        this.queryService = queryService;
    }

    @Override
    @GetMapping("/room")
    public ApiResponse<ChatRoomDto> room(@PathVariable long groupId, @LoginUser Long userId) {
        return ApiResponse.ok(queryService.room(groupId, userId));
    }

    @Override
    @GetMapping("/messages")
    public ApiResponse<ChatMessagePageDto> messages(@PathVariable long groupId,
                                                    @RequestParam(required = false) Long before,
                                                    @RequestParam(required = false) Long after,
                                                    @RequestParam(defaultValue = "50") int limit,
                                                    @LoginUser Long userId) {
        return ApiResponse.ok(queryService.messages(groupId, userId, before, after, limit));
    }

    @Override
    @PostMapping("/read")
    public ApiResponse<Void> markRead(@PathVariable long groupId, @LoginUser Long userId) {
        queryService.markRead(groupId, userId);
        return ApiResponse.ok();
    }
}
