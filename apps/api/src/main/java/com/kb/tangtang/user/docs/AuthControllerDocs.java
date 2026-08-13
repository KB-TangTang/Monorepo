package com.kb.tangtang.user.docs;

import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.user.dto.LoginResponseDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * {@code AuthController} 의 Swagger 문서.
 *
 * <p>문서 애노테이션을 컨트롤러에서 떼어내 여기에 모은다. 컨트롤러는 이 인터페이스를
 * {@code implements} 하기만 하면 되고, 로직을 읽을 때 문서 텍스트가 시야를 가리지 않는다.
 * Spring 5.3 의 {@code HandlerMethod} 가 인터페이스 쪽 파라미터 애노테이션까지 합쳐 주므로
 * {@code @ApiIgnore}·{@code @ApiParam} 이 그대로 반영된다.
 */
@Api(tags = "01. 인증 — 구글 OAuth 로그인 · 토큰 재발급")
public interface AuthControllerDocs {

    @ApiOperation(
            value = "구글 로그인 시작 (브라우저 전체 이동)",
            notes = "구글 동의 화면으로 **302 리다이렉트**한다. JSON 을 반환하지 않으므로 "
                    + "Swagger 의 Try it out 으로는 확인할 수 없다 — 주소창에 직접 열어야 한다.\n\n"
                    + "CSRF 방지용 `state` 를 만들어 쿠키에도 남긴다.")
    void googleLogin(HttpServletResponse response) throws IOException;

    @ApiOperation(
            value = "구글 로그인 콜백 (브라우저 전체 이동)",
            notes = "구글이 호출하는 경로다. **프론트에서 직접 부르지 않는다.**\n\n"
                    + "성공하면 리프레시 토큰을 httpOnly 쿠키로 심고 프론트 `/auth/callback` 으로 보낸다.\n"
                    + "실패해도 JSON 을 뿌리지 않고 `/login?error=...` 로 되돌린다 "
                    + "(주소창에 JSON 이 뜨는 것을 막기 위함).\n\n"
                    + "`error` 값: `cancelled`(사용자 취소) · `invalid`(state 불일치, CSRF 의심) · "
                    + "`withdrawn`(탈퇴한 계정) · `failed`(그 밖)")
    void googleCallback(@ApiParam(value = "구글이 돌려주는 인가 코드") String code,
                        @ApiParam(value = "CSRF 방지용 state. 쿠키에 저장한 값과 일치해야 한다") String state,
                        @ApiParam(value = "구글이 실패를 알릴 때만 채워진다") String error,
                        HttpServletRequest request,
                        HttpServletResponse response) throws IOException;

    @ApiOperation(
            value = "액세스 토큰 재발급",
            notes = "리프레시 토큰은 **쿠키로 받는다** — 요청 본문이 없다.\n\n"
                    + "재발급할 때마다 리프레시 토큰이 회전하며 새 쿠키로 덮어쓴다.\n"
                    + "이미 쓴 토큰이 다시 오면 `REFRESH_TOKEN_REUSED` 로 실패하고 쿠키를 지운다 — "
                    + "지우지 않으면 브라우저가 계속 stale 쿠키를 보내 무한 리다이렉트가 된다.")
    ApiResponse<LoginResponseDto> refresh(HttpServletRequest request, HttpServletResponse response);

    @ApiOperation(
            value = "로그아웃",
            notes = "서버의 리프레시 토큰을 폐기하고 쿠키를 지운다. "
                    + "쿠키가 없어도 성공이다 — 없는 것을 지우는 것은 오류가 아니다.")
    ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response);
}
