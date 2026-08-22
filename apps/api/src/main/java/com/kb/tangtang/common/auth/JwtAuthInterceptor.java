package com.kb.tangtang.common.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.common.exception.BusinessException;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Authorization: Bearer <jwt> 검증.
 *
 * 인증 실패는 401 이라 CommonExceptionAdvice(BusinessException → 400)를 태우지 않고
 * 여기서 직접 ApiResponse 형태의 JSON 을 쓴다. 응답 포맷은 다른 실패 응답과 동일하다.
 *
 * 적용 범위는 ServletConfig 에서 지정한다 (/api/** 중 /api/health · /api/auth/** 제외).
 */
@Component
@Log4j2
public class JwtAuthInterceptor implements HandlerInterceptor {

    /** 통과한 요청의 request attribute 키. LoginUserArgumentResolver 가 읽는다. */
    public static final String LOGIN_USER_ID = "loginUserId";

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    public JwtAuthInterceptor(JwtProvider jwtProvider, ObjectMapper objectMapper) {
        this.jwtProvider = jwtProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {

        // CORS preflight 에는 Authorization 헤더가 실리지 않는다. 막으면 본 요청이 아예 못 온다.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String header = request.getHeader(HEADER);
        if (header == null || !header.startsWith(PREFIX)) {
            writeError(response, "UNAUTHORIZED", "로그인이 필요합니다.");
            return false;
        }

        try {
            Long userId = jwtProvider.parseUserId(header.substring(PREFIX.length()));
            request.setAttribute(LOGIN_USER_ID, userId);
            return true;
        } catch (BusinessException ex) {
            writeError(response, ex.getCode(), ex.getMessage());
            return false;
        }
    }

    private void writeError(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(code, message));
    }
}
