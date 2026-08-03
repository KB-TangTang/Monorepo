package com.kb.tangtang.common.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Optional;

/**
 * 인증 쿠키(리프레시 토큰 · OAuth state) 읽기/쓰기.
 *
 * javax.servlet 4.0 의 Cookie 에는 SameSite 세터가 없어 Set-Cookie 헤더를 직접 만든다.
 * SameSite=None 은 Secure 없이는 브라우저가 쿠키를 버리므로 설정 조합을 검증한다.
 */
@Component
public class AuthCookieWriter {

    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String OAUTH_STATE = "oauth_state";

    /** 리프레시 쿠키는 인증 경로에서만 필요하다. 다른 API 요청에 실려 나가지 않게 좁힌다. */
    private static final String PATH = "/api/auth";
    private static final int STATE_MAX_AGE_SECONDS = 300;

    private final String sameSite;
    private final boolean secure;
    private final long refreshTokenValiditySeconds;

    public AuthCookieWriter(@Value("${auth.cookie.same-site}") String sameSite,
                            @Value("${auth.cookie.secure}") boolean secure,
                            @Value("${jwt.refresh-token-validity}") long refreshTokenValiditySeconds) {
        if ("None".equalsIgnoreCase(sameSite) && !secure) {
            throw new IllegalStateException(
                    "auth.cookie.same-site=None 은 auth.cookie.secure=true 가 있어야 한다. "
                            + "Secure 없는 SameSite=None 쿠키는 브라우저가 버린다.");
        }
        this.sameSite = sameSite;
        this.secure = secure;
        this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
    }

    public void writeRefreshToken(HttpServletResponse response, String rawToken) {
        response.addHeader("Set-Cookie",
                build(REFRESH_TOKEN, rawToken, (int) refreshTokenValiditySeconds));
    }

    public void clearRefreshToken(HttpServletResponse response) {
        response.addHeader("Set-Cookie", build(REFRESH_TOKEN, "", 0));
    }

    public void writeState(HttpServletResponse response, String state) {
        response.addHeader("Set-Cookie", build(OAUTH_STATE, state, STATE_MAX_AGE_SECONDS));
    }

    public void clearState(HttpServletResponse response) {
        response.addHeader("Set-Cookie", build(OAUTH_STATE, "", 0));
    }

    public Optional<String> read(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .map(javax.servlet.http.Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }

    private String build(String name, String value, int maxAgeSeconds) {
        StringBuilder sb = new StringBuilder()
                .append(name).append('=').append(value)
                .append("; Max-Age=").append(maxAgeSeconds)
                .append("; Path=").append(PATH)
                .append("; HttpOnly")
                .append("; SameSite=").append(sameSite);
        if (secure) {
            sb.append("; Secure");
        }
        return sb.toString();
    }
}
