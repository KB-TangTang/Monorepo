package com.kb.tangtang.common.docs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 배포 환경에서 Swagger 문서를 HTTP Basic 인증으로 가린다 (2026-08-14 보안 점검).
 *
 * <p><b>왜 필요한가.</b> 인증 인터셉터({@code JwtAuthInterceptor})는 {@code /api/**} 에만 걸려 있어
 * {@code /swagger-ui.html} · {@code /v2/api-docs} 는 무인증으로 열린다. 배포 서버가 공인 IP 로
 * 떠 있으므로 <b>누구나 API 52개의 경로·파라미터·응답 구조를 그대로 읽을 수 있었다.</b>
 * 실제로 {@code http://<공인IP>:8080/swagger-ui.html} 이 200 을 반환하는 것을 확인했다.
 *
 * <p><b>왜 차단이 아니라 Basic 인증인가.</b> 팀이 배포 주소로 문서를 보고 있어 완전히 막으면
 * 공유가 끊긴다. 비밀번호 한 겹이면 팀은 그대로 쓰고 외부에는 닫힌다.
 *
 * <p><b>동작</b>
 * <ul>
 *   <li>로컬({@code app.env=local})은 그대로 통과한다 — 개발 중에 매번 로그인하지 않는다</li>
 *   <li>배포 환경에서 비밀번호가 설정돼 있지 않으면 <b>404 로 숨긴다.</b> 설정을 빠뜨린 채
 *       배포됐을 때 문서가 열린 채로 남는 것이 최악이므로, 기본값은 "닫힘" 이어야 한다</li>
 *   <li>자격 증명이 틀리면 401 + {@code WWW-Authenticate} → 브라우저가 로그인 창을 띄운다</li>
 * </ul>
 *
 * <p>적용 경로는 {@code ServletConfig#addInterceptors} 에서 지정한다.
 * {@code DevEnvironmentGuard} 와 같은 이유로 루트 컨텍스트의 빈이다 —
 * 서블릿 컨텍스트에는 PropertySourcesPlaceholderConfigurer 가 없어 {@code @Value} 가 풀리지 않는다.
 */
@Component
public class SwaggerAccessInterceptor implements HandlerInterceptor {

    private static final String LOCAL = "local";
    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Basic ";
    private static final String REALM = "tangtang API docs";

    private final String appEnv;
    private final String username;
    private final String password;

    public SwaggerAccessInterceptor(@Value("${app.env:local}") String appEnv,
                                    @Value("${swagger.access.username:}") String username,
                                    @Value("${swagger.access.password:}") String password) {
        this.appEnv = appEnv;
        this.username = username;
        this.password = password;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (LOCAL.equalsIgnoreCase(appEnv)) {
            return true;
        }
        if (password == null || password.isBlank()) {
            /*
             * 설정 누락 = 닫힘. 401 이 아니라 404 인 이유는 "여기에 문서가 있다" 는 사실 자체를
             * 알리지 않기 위해서다. 팀이 못 보게 됐다면 SWAGGER_ACCESS_PASSWORD 를 넣으면 된다.
             */
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return false;
        }
        if (matches(request.getHeader(HEADER))) {
            return true;
        }
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setHeader("WWW-Authenticate", "Basic realm=\"" + REALM + "\", charset=\"UTF-8\"");
        return false;
    }

    private boolean matches(String header) {
        if (header == null || !header.startsWith(PREFIX)) {
            return false;
        }
        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(header.substring(PREFIX.length()).trim()),
                    StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        int separator = decoded.indexOf(':');
        if (separator < 0) {
            return false;
        }
        /* 비교 시간으로 비밀번호 길이가 새지 않도록 MessageDigest.isEqual 을 쓴다. */
        return equalsConstantTime(decoded.substring(0, separator), username)
                && equalsConstantTime(decoded.substring(separator + 1), password);
    }

    private boolean equalsConstantTime(String actual, String expected) {
        return MessageDigest.isEqual(actual.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }
}
