package com.kb.tangtang.common.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 파라미터에 로그인 사용자 ID 를 주입한다.
 *
 *   @GetMapping("/me")
 *   public ApiResponse<UserMeDto> me(@LoginUser Long userId) { ... }
 *
 * JwtAuthInterceptor 를 통과한 요청에서만 값이 채워진다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginUser {
}
