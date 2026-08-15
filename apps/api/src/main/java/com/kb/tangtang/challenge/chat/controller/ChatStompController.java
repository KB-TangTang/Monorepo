package com.kb.tangtang.challenge.chat.controller;

import com.kb.tangtang.challenge.chat.dto.ChatSendRequestDto;
import com.kb.tangtang.challenge.chat.service.ChatMessageService;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.user.dto.UserDto;
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

    /**
     * 인증된 세션인데 사용자가 사라진 상황(탈퇴·삭제)은 업무 오류로 처리한다.
     * 닉네임 자체(null 가능 — 온보딩 미완료)는 그대로 둔다. 빈 값 처리는
     * ChatMessageService 가 알림 페이로드를 만드는 지점 한 곳에서 맡는다.
     */
    private String nicknameOf(long userId) {
        UserDto user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("CHAT_SENDER_NOT_FOUND", "사용자 정보를 찾을 수 없어요.");
        }
        return user.getNickname();
    }
}
