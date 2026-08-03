package com.kb.tangtang.common.auth;

import com.kb.tangtang.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * JwtProvider 단위 테스트. Spring 컨텍스트도 DB 도 필요 없다.
 */
class JwtProviderTest {

    private static final String SECRET = "tangtang-test-secret-key-must-be-32-bytes-or-longer";

    @Test
    @DisplayName("발급한 토큰에서 userId 를 다시 꺼낸다")
    void createAndParse() {
        JwtProvider provider = new JwtProvider(SECRET, 900);

        String token = provider.createAccessToken(42L);

        assertEquals(42L, provider.parseUserId(token));
    }

    @Test
    @DisplayName("만료된 토큰은 TOKEN_EXPIRED 로 거부한다")
    void expiredToken() {
        // 유효기간을 음수로 주면 발급 시점에 이미 만료된 토큰이 나온다
        JwtProvider expiredProvider = new JwtProvider(SECRET, -60);
        String token = expiredProvider.createAccessToken(42L);

        JwtProvider provider = new JwtProvider(SECRET, 900);
        BusinessException ex = assertThrows(BusinessException.class, () -> provider.parseUserId(token));

        assertEquals("TOKEN_EXPIRED", ex.getCode());
    }

    @Test
    @DisplayName("다른 키로 서명된 토큰은 INVALID_TOKEN 으로 거부한다")
    void forgedSignature() {
        JwtProvider attacker = new JwtProvider("attacker-secret-key-that-is-also-32-bytes-long!!", 900);
        String forged = attacker.createAccessToken(42L);

        JwtProvider provider = new JwtProvider(SECRET, 900);
        BusinessException ex = assertThrows(BusinessException.class, () -> provider.parseUserId(forged));

        assertEquals("INVALID_TOKEN", ex.getCode());
    }

    @Test
    @DisplayName("형식이 깨진 문자열은 INVALID_TOKEN 으로 거부한다")
    void malformedToken() {
        JwtProvider provider = new JwtProvider(SECRET, 900);

        BusinessException ex = assertThrows(BusinessException.class, () -> provider.parseUserId("not-a-jwt"));

        assertEquals("INVALID_TOKEN", ex.getCode());
    }
}
