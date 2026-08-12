package com.kb.tangtang.user.service;

import com.kb.tangtang.common.auth.JwtProvider;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.user.domain.ConsentScope;
import com.kb.tangtang.user.dto.AuthResultDto;
import com.kb.tangtang.user.dto.GoogleProfileDto;
import com.kb.tangtang.user.dto.LoginResponseDto;
import com.kb.tangtang.user.dto.UserDto;
import com.kb.tangtang.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private GoogleOAuthClient googleOAuthClient;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private UserMapper userMapper;
    @Mock private JwtProvider jwtProvider;
    @Mock private ConsentService consentService;
    @Mock private com.kb.tangtang.account.service.ConnectedAccountQuery connectedAccountQuery;
    @Mock private ProfileImageUrlResolver profileImageUrlResolver;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(googleOAuthClient, refreshTokenService, userMapper, jwtProvider,
                consentService, connectedAccountQuery, profileImageUrlResolver);
    }

    private static GoogleProfileDto profile() {
        return GoogleProfileDto.builder()
                .providerUserId("google-sub-1")
                .email("jiyoon@example.com")
                .name("지윤")
                .build();
    }

    /*
     * 온보딩 게이트 3단 (DECISIONS.md 2026-08-11 (7)).
     * 프론트 가드는 부팅 시 이 응답만 보고 다음 단계를 정한다 — 하나라도 빠지면 그 단계가 통째로 건너뛰어진다.
     */
    @Test
    @DisplayName("세션 응답에 온보딩 게이트 3단이 모두 실린다")
    void sessionCarriesOnboardingGates() {
        when(refreshTokenService.consume("refresh-raw")).thenReturn(11L);
        when(userMapper.findById(11L)).thenReturn(UserDto.builder()
                .id(11L).socialProvider("GOOGLE").providerUserId("sub-1")
                .socialName("지윤").status("ACTIVE").difficultyId(1L).build());
        when(jwtProvider.createAccessToken(11L)).thenReturn("access-jwt");
        when(refreshTokenService.issue(11L)).thenReturn("refresh-new");
        when(consentService.needsConsent(11L)).thenReturn(false);
        when(consentService.needsConsent(11L, ConsentScope.FINANCIAL)).thenReturn(true);
        when(connectedAccountQuery.hasActiveAccount(11L)).thenReturn(false);

        LoginResponseDto response = authService.refresh("refresh-raw").getResponse();

        assertFalse(response.isNeedsConsent(), "가입 동의는 마쳤다");
        assertTrue(response.isNeedsFinancialConsent(), "제3자 제공 동의가 남았다");
        assertTrue(response.isNeedsAccountLink(), "계좌가 0개면 연동이 남았다");
        assertNull(response.getUser().getNickname(), "닉네임 단계는 nickname 이 null 인지로 본다");
    }

    @Test
    @DisplayName("계좌가 있으면 계좌연동 단계는 요구하지 않는다")
    void noAccountLinkWhenAccountExists() {
        when(refreshTokenService.consume("refresh-raw")).thenReturn(11L);
        when(userMapper.findById(11L)).thenReturn(UserDto.builder()
                .id(11L).socialProvider("GOOGLE").providerUserId("sub-1")
                .nickname("탕탕이").socialName("지윤").status("ACTIVE").difficultyId(1L).build());
        when(jwtProvider.createAccessToken(11L)).thenReturn("access-jwt");
        when(refreshTokenService.issue(11L)).thenReturn("refresh-new");
        when(connectedAccountQuery.hasActiveAccount(11L)).thenReturn(true);

        LoginResponseDto response = authService.refresh("refresh-raw").getResponse();

        assertFalse(response.isNeedsAccountLink());
        assertEquals("탕탕이", response.getUser().getDisplayName());
    }

    @Test
    @DisplayName("처음 로그인하는 사용자는 NORMAL 난이도로 새로 만든다")
    void firstLoginCreatesUser() {
        when(googleOAuthClient.exchangeCodeForProfile("code")).thenReturn(profile());
        when(userMapper.findBySocialId("GOOGLE", "google-sub-1")).thenReturn(null);
        // insert 가 PK 를 채우는 동작을 흉내낸다
        doAnswer(invocation -> {
            UserDto arg = invocation.getArgument(0);
            arg.setId(11L);
            return null;
        }).when(userMapper).insert(any(UserDto.class));
        when(jwtProvider.createAccessToken(11L)).thenReturn("access-jwt");
        when(refreshTokenService.issue(11L)).thenReturn("refresh-raw");
        when(consentService.needsConsent(11L)).thenReturn(true);

        AuthResultDto result = authService.loginWithGoogleCode("code");

        ArgumentCaptor<UserDto> captor = ArgumentCaptor.forClass(UserDto.class);
        verify(userMapper).insert(captor.capture());
        UserDto created = captor.getValue();
        assertEquals("GOOGLE", created.getSocialProvider());
        assertEquals("google-sub-1", created.getProviderUserId());
        /*
         * 구글 이름은 socialName 으로 들어가고 nickname 은 비어 있어야 한다 —
         * nickname IS NULL 이 "닉네임 온보딩 미완료" 판별 기준이라, 여기서 미리 채우면
         * 모든 신규 사용자가 온보딩 화면을 영영 못 본다.
         * (DECISIONS.md 2026-08-11 닉네임 온보딩)
         */
        assertEquals("지윤", created.getSocialName());
        assertNull(created.getNickname(), "가입 시 닉네임은 비어 있어야 온보딩이 뜬다");
        assertEquals("ACTIVE", created.getStatus());
        assertEquals(2L, created.getDifficultyId(), "가입 시 NORMAL(2) 를 부여한다");

        assertEquals("access-jwt", result.getResponse().getAccessToken());
        assertEquals("refresh-raw", result.getRefreshToken());
        assertEquals(11L, result.getResponse().getUser().getId());
        assertTrue(result.getResponse().isNeedsConsent(), "동의 이력이 없으면 true");
    }

    @Test
    @DisplayName("이미 있는 사용자는 새로 만들지 않는다")
    void existingUserIsReused() {
        when(googleOAuthClient.exchangeCodeForProfile("code")).thenReturn(profile());
        when(userMapper.findBySocialId("GOOGLE", "google-sub-1")).thenReturn(
                UserDto.builder().id(11L).nickname("지윤").email("jiyoon@example.com")
                        .status("ACTIVE").difficultyId(2L).build());
        when(jwtProvider.createAccessToken(11L)).thenReturn("access-jwt");
        when(refreshTokenService.issue(11L)).thenReturn("refresh-raw");
        when(consentService.needsConsent(11L)).thenReturn(false);

        AuthResultDto result = authService.loginWithGoogleCode("code");

        verify(userMapper, never()).insert(any(UserDto.class));
        assertEquals("지윤", result.getResponse().getUser().getNickname());
        assertFalse(result.getResponse().isNeedsConsent(), "동의 이력이 있으면 false");
    }

    @Test
    @DisplayName("탈퇴한 사용자는 USER_WITHDRAWN 으로 막는다")
    void withdrawnUserRejected() {
        when(googleOAuthClient.exchangeCodeForProfile("code")).thenReturn(profile());
        when(userMapper.findBySocialId("GOOGLE", "google-sub-1")).thenReturn(
                UserDto.builder().id(11L).status("WITHDRAWN").difficultyId(1L).build());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.loginWithGoogleCode("code"));

        assertEquals("USER_WITHDRAWN", ex.getCode());
        verify(jwtProvider, never()).createAccessToken(anyLong());
    }

    @Test
    @DisplayName("차단된 사용자도 USER_WITHDRAWN 으로 막는다")
    void blockedUserRejected() {
        when(googleOAuthClient.exchangeCodeForProfile("code")).thenReturn(profile());
        when(userMapper.findBySocialId("GOOGLE", "google-sub-1")).thenReturn(
                UserDto.builder().id(11L).status("BLOCKED").difficultyId(1L).build());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.loginWithGoogleCode("code"));

        assertEquals("USER_WITHDRAWN", ex.getCode());
    }

    @Test
    @DisplayName("refresh 는 기존 토큰을 소비하고 새 토큰 쌍을 만든다")
    void refreshRotates() {
        when(refreshTokenService.consume("old-raw")).thenReturn(11L);
        when(userMapper.findById(11L)).thenReturn(
                UserDto.builder().id(11L).nickname("지윤").email("jiyoon@example.com")
                        .status("ACTIVE").difficultyId(1L).build());
        when(jwtProvider.createAccessToken(11L)).thenReturn("new-access");
        when(refreshTokenService.issue(11L)).thenReturn("new-raw");
        when(consentService.needsConsent(11L)).thenReturn(true);

        AuthResultDto result = authService.refresh("old-raw");

        verify(refreshTokenService).consume("old-raw");
        assertEquals("new-access", result.getResponse().getAccessToken());
        assertEquals("new-raw", result.getRefreshToken());
    }

    @Test
    @DisplayName("logout 은 토큰을 폐기한다")
    void logoutRevokes() {
        authService.logout("raw");

        verify(refreshTokenService).revoke("raw");
    }
}
