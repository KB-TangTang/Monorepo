package com.kb.tangtang.common.auth;

import com.kb.tangtang.common.exception.BusinessException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 액세스 토큰(JWT) 발급·검증. jjwt 를 감싸는 유일한 지점이다.
 *
 * 생성자 주입만 쓰므로 Spring 없이 new 로 만들어 단위 테스트할 수 있다.
 * 서명키는 application-local.properties 의 jwt.secret (커밋 금지).
 */
@Component
public class JwtProvider {

    private final SecretKey key;
    private final long accessTokenValiditySeconds;

    public JwtProvider(@Value("${jwt.secret}") String secret,
                       @Value("${jwt.access-token-validity}") long accessTokenValiditySeconds) {
        // HS256 은 256비트 이상을 요구한다. 짧으면 여기서 WeakKeyException 이 나며 기동이 실패한다.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValiditySeconds = accessTokenValiditySeconds;
    }

    public String createAccessToken(Long userId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date(now))
                .expiration(new Date(now + accessTokenValiditySeconds * 1000L))
                .signWith(key)
                .compact();
    }

    /**
     * 토큰에서 userId 를 꺼낸다.
     * 만료는 TOKEN_EXPIRED, 그 밖의 모든 실패(서명 위조·형식 오류·null)는 INVALID_TOKEN 이다.
     * 호출자(JwtAuthInterceptor)가 잡아서 401 로 변환한다.
     */
    public Long parseUserId(String token) {
        try {
            String subject = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            return Long.valueOf(subject);
        } catch (ExpiredJwtException ex) {
            throw new BusinessException("TOKEN_EXPIRED", "액세스 토큰이 만료되었습니다.");
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException("INVALID_TOKEN", "유효하지 않은 토큰입니다.");
        }
    }
}
