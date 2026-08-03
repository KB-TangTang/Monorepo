package com.kb.tangtang.user.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.user.dto.RefreshTokenDto;
import com.kb.tangtang.user.mapper.RefreshTokenMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 리프레시 토큰 발급 · 회전 · 재사용 감지.
 *
 * 원문 토큰은 DB 에 저장하지 않는다. SHA-256 해시만 저장하고, 검증할 때 다시 해시해 비교한다.
 * (tbl_refresh_token.token_hash 컬럼 주석의 설계 의도)
 *
 * 회전: consume() 이 기존 토큰을 폐기하고 userId 만 돌려준다.
 *       호출자가 이어서 issue() 로 새 토큰을 받는다.
 * 재사용 감지: 이미 폐기된 토큰이 다시 들어오면 탈취로 간주하고 해당 사용자 토큰을 전부 폐기한다.
 */
@Service
@Log4j2
public class RefreshTokenService {

    private final RefreshTokenMapper refreshTokenMapper;
    private final long refreshTokenValiditySeconds;

    public RefreshTokenService(RefreshTokenMapper refreshTokenMapper,
                               @Value("${jwt.refresh-token-validity}") long refreshTokenValiditySeconds) {
        this.refreshTokenMapper = refreshTokenMapper;
        this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
    }

    /** 새 토큰을 발급한다. 반환값은 쿠키에 실어 보낼 원문이며, DB 에는 해시만 남는다. */
    public String issue(Long userId) {
        String raw = UUID.randomUUID().toString() + UUID.randomUUID();
        refreshTokenMapper.insert(RefreshTokenDto.builder()
                .userId(userId)
                .tokenHash(sha256Hex(raw))
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenValiditySeconds))
                .build());
        return raw;
    }

    /**
     * 토큰을 검증하고 폐기한 뒤 userId 를 돌려준다.
     * 이미 폐기된 토큰이면 탈취로 보고 해당 사용자의 전체 토큰을 폐기한다.
     */
    public Long consume(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessException("INVALID_TOKEN", "리프레시 토큰이 없습니다.");
        }

        RefreshTokenDto token = refreshTokenMapper.findByHash(sha256Hex(rawToken));
        if (token == null) {
            throw new BusinessException("INVALID_TOKEN", "유효하지 않은 리프레시 토큰입니다.");
        }

        if (token.isRevoked()) {
            // 폐기된 토큰이 다시 왔다 = 누군가 탈취본을 쓰고 있다는 신호
            log.warn("리프레시 토큰 재사용 감지 — userId={} 전체 폐기", token.getUserId());
            refreshTokenMapper.revokeAllByUserId(token.getUserId());
            throw new BusinessException("REFRESH_TOKEN_REUSED",
                    "보안을 위해 로그아웃되었습니다. 다시 로그인해 주세요.");
        }

        if (token.getExpiresAt() == null || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("INVALID_TOKEN", "리프레시 토큰이 만료되었습니다.");
        }

        refreshTokenMapper.revokeById(token.getId());
        return token.getUserId();
    }

    /** 로그아웃. 이미 없거나 폐기된 토큰이어도 오류로 만들지 않는다. */
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        RefreshTokenDto token = refreshTokenMapper.findByHash(sha256Hex(rawToken));
        if (token != null && !token.isRevoked()) {
            refreshTokenMapper.revokeById(token.getId());
        }
    }

    /** SHA-256 hex 64자. CHAR(64) 컬럼과 길이가 맞는다. */
    public static String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 은 모든 JVM 이 반드시 지원한다. 여기 오면 환경이 깨진 것이다.
            throw new IllegalStateException("SHA-256 을 사용할 수 없습니다.", ex);
        }
    }
}
