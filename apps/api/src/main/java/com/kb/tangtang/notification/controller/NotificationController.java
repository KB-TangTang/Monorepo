package com.kb.tangtang.notification.controller;

import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.notification.dto.NotificationListDto;
import com.kb.tangtang.notification.dto.UnreadCountDto;
import com.kb.tangtang.notification.service.NotificationService;
import com.kb.tangtang.notification.service.SseEmitterRegistry;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * 알림 API (NT_01_03).
 *
 * 사용자 식별은 @LoginUser 로만 한다. 요청으로 userId 를 받으면 남의 알림에 접근할 수 있다.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitterRegistry registry;

    public NotificationController(NotificationService notificationService, SseEmitterRegistry registry) {
        this.notificationService = notificationService;
        this.registry = registry;
    }

    @GetMapping
    public ApiResponse<NotificationListDto> list(@LoginUser Long userId,
                                                 @RequestParam(required = false) Long cursor,
                                                 @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(notificationService.list(userId, cursor, size));
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountDto> unreadCount(@LoginUser Long userId) {
        return ApiResponse.ok(notificationService.unreadCount(userId));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<UnreadCountDto> read(@LoginUser Long userId, @PathVariable long id) {
        return ApiResponse.ok(notificationService.markRead(userId, id));
    }

    @PostMapping("/read-all")
    public ApiResponse<UnreadCountDto> readAll(@LoginUser Long userId) {
        return ApiResponse.ok(notificationService.markAllRead(userId));
    }

    /**
     * SSE 스트림. 스트림이라 ApiResponse 로 감싸지 않는다.
     *
     * 연결 직후 connected 이벤트를 한 번 보낸다 — 프록시 버퍼링을 깨고
     * 프론트가 "연결됨" 을 판정할 근거를 준다. 이걸 못 받으면 폴링으로 강등한다.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@LoginUser Long userId) {
        SseEmitter emitter = registry.register(userId);
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            registry.remove(userId, emitter);
        }
        return emitter;
    }
}
