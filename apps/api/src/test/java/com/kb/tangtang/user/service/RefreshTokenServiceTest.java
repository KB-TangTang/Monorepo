package com.kb.tangtang.user.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.user.dto.RefreshTokenDto;
import com.kb.tangtang.user.mapper.RefreshTokenMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenMapper refreshTokenMapper;

    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        // 리프레시 토큰 유효기간 14일(초)
        service = new RefreshTokenService(refreshTokenMapper, 1209600);
    }

    @Test
    @DisplayName("발급하면 원문을 돌려주고 DB 에는 해시만 저장한다")
    void issueStoresHashOnly() {
        String raw = service.issue(7L);

        ArgumentCaptor<RefreshTokenDto> captor = ArgumentCaptor.forClass(RefreshTokenDto.class);
        verify(refreshTokenMapper).insert(captor.capture());
        RefreshTokenDto saved = captor.getValue();

        assertEquals(7L, saved.getUserId());
        assertEquals(64, saved.getTokenHash().length(), "SHA-256 hex 는 64자다");
        assertNotEquals(raw, saved.getTokenHash(), "원문을 그대로 저장하면 안 된다");
        assertEquals(RefreshTokenService.sha256Hex(raw), saved.getTokenHash());
    }

    @Test
    @DisplayName("정상 토큰을 소비하면 폐기하고 userId 를 돌려준다")
    void consumeRevokesAndReturnsUserId() {
        String raw = "raw-token-value";
        when(refreshTokenMapper.findByHash(RefreshTokenService.sha256Hex(raw)))
                .thenReturn(RefreshTokenDto.builder()
                        .id(100L).userId(7L)
                        .tokenHash(RefreshTokenService.sha256Hex(raw))
                        .expiresAt(LocalDateTime.now().plusDays(1))
                        .revoked(false)
                        .build());

        Long userId = service.consume(raw);

        assertEquals(7L, userId);
        verify(refreshTokenMapper).revokeById(100L);
        verify(refreshTokenMapper, never()).revokeAllByUserId(7L);
    }

    @Test
    @DisplayName("이미 폐기된 토큰이 다시 오면 전체 폐기하고 REFRESH_TOKEN_REUSED 를 던진다")
    void consumeDetectsReuse() {
        String raw = "stolen-token";
        when(refreshTokenMapper.findByHash(RefreshTokenService.sha256Hex(raw)))
                .thenReturn(RefreshTokenDto.builder()
                        .id(100L).userId(7L)
                        .tokenHash(RefreshTokenService.sha256Hex(raw))
                        .expiresAt(LocalDateTime.now().plusDays(1))
                        .revoked(true)
                        .build());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.consume(raw));

        assertEquals("REFRESH_TOKEN_REUSED", ex.getCode());
        verify(refreshTokenMapper).revokeAllByUserId(7L);
    }

    @Test
    @DisplayName("만료된 토큰은 INVALID_TOKEN 으로 거부한다")
    void consumeRejectsExpired() {
        String raw = "expired-token";
        when(refreshTokenMapper.findByHash(RefreshTokenService.sha256Hex(raw)))
                .thenReturn(RefreshTokenDto.builder()
                        .id(100L).userId(7L)
                        .tokenHash(RefreshTokenService.sha256Hex(raw))
                        .expiresAt(LocalDateTime.now().minusMinutes(1))
                        .revoked(false)
                        .build());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.consume(raw));

        assertEquals("INVALID_TOKEN", ex.getCode());
    }

    @Test
    @DisplayName("존재하지 않는 토큰은 INVALID_TOKEN 으로 거부한다")
    void consumeRejectsUnknown() {
        when(refreshTokenMapper.findByHash(anyString())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.consume("nope"));

        assertEquals("INVALID_TOKEN", ex.getCode());
    }

    @Test
    @DisplayName("null 토큰은 INVALID_TOKEN 으로 거부한다 — 쿠키가 없는 경우")
    void consumeRejectsNull() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.consume(null));

        assertEquals("INVALID_TOKEN", ex.getCode());
    }

    @Test
    @DisplayName("로그아웃은 없는 토큰이어도 조용히 넘어간다")
    void revokeIgnoresUnknown() {
        when(refreshTokenMapper.findByHash(anyString())).thenReturn(null);

        service.revoke("nope");

        verify(refreshTokenMapper, never()).revokeById(org.mockito.ArgumentMatchers.anyLong());
    }
}
