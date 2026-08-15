package com.kb.tangtang.challenge.chat.controller;

import com.kb.tangtang.challenge.chat.dto.ChatSendRequestDto;
import com.kb.tangtang.challenge.chat.service.ChatMessageService;
import com.kb.tangtang.user.mapper.UserMapper;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * STOMP 발행 진입점. REST 컨트롤러가 아니므로 ApiResponse 로 감싸지 않는다.
 *
 * <p>Principal#getName() 이 userId 문자열이라는 규약은 StompAuthChannelInterceptor 가 만든다.
 */
@Controller
public class ChatStompController {

    private final ChatMessageService chatMessageService;
    private final UserMapper userMapper;

    public ChatStompController(ChatMessageService chatMessageService, UserMapper userMapper) {
        this.chatMessageService = chatMessageService;
        this.userMapper = userMapper;
    }

    @MessageMapping("/chat/{groupId}")
    public void send(@DestinationVariable long groupId, ChatSendRequestDto request, Principal principal) {
        long userId = Long.parseLong(principal.getName());
        chatMessageService.send(groupId, userId, nicknameOf(userId), request.getContent());
    }

    private String nicknameOf(long userId) {
        return userMapper.findById(userId).getNickname();
    }
}
