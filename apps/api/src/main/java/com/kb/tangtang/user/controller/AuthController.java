package com.kb.tangtang.user.controller;

import com.kb.tangtang.common.auth.AuthCookieWriter;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.user.docs.AuthControllerDocs;
import com.kb.tangtang.user.dto.AuthResultDto;
import com.kb.tangtang.user.dto.LoginResponseDto;
import com.kb.tangtang.user.service.AuthService;
import com.kb.tangtang.user.service.GoogleOAuthClient;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 구글 OAuth 로그인 엔드포인트.
 *
 * /google 과 /google/callback 은 브라우저 전체 이동(302)이라 JSON 을 반환하지 않는다.
 * 콜백에서 예외가 나면 JSON 을 뿌리는 대신 프론트 로그인 화면으로 error 쿼리를 달아 되돌린다.
 * (브라우저 주소창에 JSON 이 뜨는 상황을 막는다)
 */
@RestController
@RequestMapping("/api/auth")
@Log4j2
public class AuthController implements AuthControllerDocs {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthService authService;
    private final GoogleOAuthClient googleOAuthClient;
    private final AuthCookieWriter cookieWriter;
    private final String frontUrl;

    public AuthController(AuthService authService,
                          GoogleOAuthClient googleOAuthClient,
                          AuthCookieWriter cookieWriter,
                          @Value("${app.front-url}") String frontUrl) {
        this.authService = authService;
        this.googleOAuthClient = googleOAuthClient;
        this.cookieWriter = cookieWriter;
        this.frontUrl = frontUrl;
    }

    /** 구글 동의 화면으로 보낸다. state 를 만들어 쿠키에도 남긴다(CSRF 방지). */
    @GetMapping("/google")
    public void googleLogin(HttpServletResponse response) throws IOException {
        String state = randomState();
        cookieWriter.writeState(response, state);
        response.sendRedirect(googleOAuthClient.buildAuthorizationUrl(state));
    }

    @GetMapping("/google/callback")
    public void googleCallback(@RequestParam(required = false) String code,
                               @RequestParam(required = false) String state,
                               @RequestParam(required = false) String error,
                               HttpServletRequest request,
                               HttpServletResponse response) throws IOException {

        String savedState = cookieWriter.read(request, AuthCookieWriter.OAUTH_STATE).orElse(null);
        cookieWriter.clearState(response);   // state 는 1회용이다

        if (error != null) {
            // 사용자가 구글 동의 화면에서 취소한 경우가 대부분이다
            log.info("구글 인증 취소/실패 — {}", error);
            response.sendRedirect(frontUrl + "/login?error=cancelled");
            return;
        }

        if (code == null || state == null || savedState == null || !savedState.equals(state)) {
            log.warn("OAuth state 불일치 — CSRF 의심");
            response.sendRedirect(frontUrl + "/login?error=invalid");
            return;
        }

        AuthResultDto result;
        try {
            result = authService.loginWithGoogleCode(code);
        } catch (BusinessException ex) {
            log.warn("로그인 실패 [{}] {}", ex.getCode(), ex.getMessage());
            response.sendRedirect(frontUrl + "/login?error=" + toFrontErrorCode(ex.getCode()));
            return;
        } catch (Exception ex) {
            // 브라우저 전체 이동이라 예외가 새어나가면 주소창에 JSON 500 이 뜬다.
            // 원인은 서버 로그에만 남기고 사용자에게는 일반 실패로 되돌린다.
            log.error("로그인 처리 중 예기치 못한 오류", ex);
            response.sendRedirect(frontUrl + "/login?error=failed");
            return;
        }

        cookieWriter.writeRefreshToken(response, result.getRefreshToken());
        response.sendRedirect(frontUrl + "/auth/callback");
    }

    /**
     * 액세스 토큰 재발급. 리프레시 토큰은 회전하며 새 쿠키로 덮어쓴다.
     *
     * REFRESH_TOKEN_REUSED 시 쿠키를 지워야 한다.
     * httpOnly 쿠키는 서버의 Set-Cookie 로만 삭제할 수 있으므로,
     * 여기서 지우지 않으면 브라우저가 매 요청마다 stale 쿠키를 보내
     * 프론트엔드 세션 복원(main.js) 과 맞물려 무한 리다이렉트가 발생한다.
     */
    @PostMapping("/refresh")
    public ApiResponse<LoginResponseDto> refresh(HttpServletRequest request, HttpServletResponse response) {
        String rawToken = cookieWriter.read(request, AuthCookieWriter.REFRESH_TOKEN).orElse(null);

        try {
            AuthResultDto result = authService.refresh(rawToken);
            cookieWriter.writeRefreshToken(response, result.getRefreshToken());
            return ApiResponse.ok(result.getResponse());
        } catch (BusinessException ex) {
            if ("REFRESH_TOKEN_REUSED".equals(ex.getCode())) {
                cookieWriter.clearRefreshToken(response);
            }
            throw ex;
        }
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        cookieWriter.read(request, AuthCookieWriter.REFRESH_TOKEN).ifPresent(authService::logout);
        cookieWriter.clearRefreshToken(response);
        return ApiResponse.ok();
    }

    private static String randomState() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** 백엔드 에러 코드를 프론트 쿼리스트링용 짧은 값으로 바꾼다. */
    private static String toFrontErrorCode(String code) {
        switch (code) {
            case "USER_WITHDRAWN":
                return "withdrawn";
            case "OAUTH_TOKEN_EXCHANGE_FAILED":
                return "failed";
            default:
                return "failed";
        }
    }
}
