package com.kb.tangtang.notification.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.common.exception.CommonExceptionAdvice;
import com.kb.tangtang.notification.dto.NotificationDto;
import com.kb.tangtang.notification.dto.NotificationListDto;
import com.kb.tangtang.notification.dto.UnreadCountDto;
import com.kb.tangtang.notification.mapper.NotificationMapper;
import com.kb.tangtang.notification.service.NotificationService;
import com.kb.tangtang.notification.service.SseEmitterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class NotificationControllerTest {

    private static final long USER_ID = 1L;

    /**
     * standaloneSetup 에는 LoginUserArgumentResolver 가 없고 인터셉터도 타지 않는다.
     * @LoginUser 자리에 고정 사용자 ID 를 넣어주는 리졸버를 직접 단다.
     * (account/controller/AccountLinkControllerTest 52~74행과 같은 방식 — 그 파일을 먼저 읽을 것)
     */
    private HandlerMethodArgumentResolver loginUserResolver() {
        return new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(com.kb.tangtang.common.auth.LoginUser.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return USER_ID;
            }
        };
    }

    /**
     * userId 를 상수로 넘기는 얇은 서비스 스텁으로 검증한다.
     * 리졸버 자체는 account 모듈의 기존 컨트롤러 테스트가 이미 덮는다.
     */
    private static class StubService extends NotificationService {
        Integer capturedSize;
        Long capturedCursor;
        StubService() { super((NotificationMapper) null); }
        @Override public NotificationListDto list(long userId, Long cursor, Integer size) {
            capturedCursor = cursor;
            capturedSize = size;
            return NotificationListDto.builder()
                    .items(List.of(NotificationDto.builder().id(1L).type("ACCOUNT_RECONNECT")
                            .title("계좌 재연동이 필요해요").content("국민은행")
                            .deepLinkUrl("/asset/accounts/9/reconnect").isRead(false)
                            .createdAt("2026-08-06T09:00:00").build()))
                    .nextCursor(null).unreadCount(3).build();
        }
        @Override public UnreadCountDto unreadCount(long userId) {
            return UnreadCountDto.builder().unreadCount(3).build();
        }
    }

    @Test
    @DisplayName("목록은 ApiResponse 로 감싸 내려간다")
    void listIsWrapped() throws Exception {
        StubService service = new StubService();
        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new NotificationController(service, new SseEmitterRegistry()))
                .setCustomArgumentResolvers(loginUserResolver())
                .setControllerAdvice(new CommonExceptionAdvice())
                .build();

        String body = mvc.perform(get("/api/notifications"))
                .andReturn().getResponse().getContentAsString();

        JsonNode json = new ObjectMapper().readTree(body);
        assertTrue(json.get("success").asBoolean());
        assertEquals(3, json.get("data").get("unreadCount").asInt());
        assertEquals(1, json.get("data").get("items").size());
    }

    /**
     * ⚠ 이 테스트가 지키는 것은 **JSON 키 이름 자체**다.
     *   boolean isRead 는 Lombok getter(isRead())를 Jackson 이 "read" 로 읽어버려
     *   프론트의 item.isRead 가 계속 undefined 였다. 값이 아니라 이름을 검사해야 잡힌다.
     */
    @Test
    @DisplayName("목록 항목의 JSON 키는 프론트·API_SPEC 과 정확히 같다 — isRead 는 read 가 아니다")
    void listItemFieldNames() throws Exception {
        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new NotificationController(new StubService(), new SseEmitterRegistry()))
                .setCustomArgumentResolvers(loginUserResolver())
                .build();

        /* MockHttpServletResponse 의 기본 charset 은 ISO-8859-1 이라 한글이 깨진다. 명시할 것 */
        String body = mvc.perform(get("/api/notifications"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        JsonNode item = new ObjectMapper().readTree(body).get("data").get("items").get(0);

        for (String field : List.of("id", "type", "title", "content", "deepLinkUrl", "isRead", "createdAt")) {
            assertTrue(item.has(field), field + " 키가 응답에 없다 — 프론트가 읽지 못한다");
        }
        assertFalse(item.has("read"), "read 로 나가면 프론트의 item.isRead 가 undefined 가 된다");

        assertEquals(1L, item.get("id").asLong());
        assertEquals("ACCOUNT_RECONNECT", item.get("type").asText());
        assertEquals("계좌 재연동이 필요해요", item.get("title").asText());
        assertEquals("국민은행", item.get("content").asText());
        assertEquals("/asset/accounts/9/reconnect", item.get("deepLinkUrl").asText());
        assertFalse(item.get("isRead").asBoolean());
        assertEquals("2026-08-06T09:00:00", item.get("createdAt").asText());
    }

    @Test
    @DisplayName("cursor·size 쿼리를 서비스로 그대로 넘긴다")
    void passesCursorAndSize() throws Exception {
        StubService service = new StubService();
        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new NotificationController(service, new SseEmitterRegistry()))
                .setCustomArgumentResolvers(loginUserResolver())
                .build();

        mvc.perform(get("/api/notifications?cursor=40&size=10"));

        assertEquals(40L, service.capturedCursor);
        assertEquals(10, service.capturedSize);
    }
}
