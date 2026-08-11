package com.kb.tangtang.user.service;

import com.kb.tangtang.common.auth.JwtProvider;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.user.dto.AuthResultDto;
import com.kb.tangtang.user.dto.GoogleProfileDto;
import com.kb.tangtang.user.dto.LoginResponseDto;
import com.kb.tangtang.user.dto.UserDto;
import com.kb.tangtang.user.dto.UserMeDto;
import com.kb.tangtang.user.mapper.UserMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 오케스트레이션. 트랜잭션 경계는 여기다(Controller·Mapper 에 두지 않는다).
 */
@Service
@Log4j2
public class AuthService {

    private static final String PROVIDER_GOOGLE = "GOOGLE";
    /** 가입 시 부여하는 난이도. db/seed.sql 의 tbl_mission_difficulty EASY 행 id. */
    private static final long DEFAULT_DIFFICULTY_ID = 1L;
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final GoogleOAuthClient googleOAuthClient;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;
    private final JwtProvider jwtProvider;
    private final ConsentService consentService;

    public AuthService(GoogleOAuthClient googleOAuthClient,
                       RefreshTokenService refreshTokenService,
                       UserMapper userMapper,
                       JwtProvider jwtProvider,
                       ConsentService consentService) {
        this.googleOAuthClient = googleOAuthClient;
        this.refreshTokenService = refreshTokenService;
        this.userMapper = userMapper;
        this.jwtProvider = jwtProvider;
        this.consentService = consentService;
    }

    /** 구글 콜백에서 받은 code 로 로그인/가입을 마치고 토큰 쌍을 만든다. */
    @Transactional
    public AuthResultDto loginWithGoogleCode(String code) {
        GoogleProfileDto profile = googleOAuthClient.exchangeCodeForProfile(code);

        UserDto user = userMapper.findBySocialId(PROVIDER_GOOGLE, profile.getProviderUserId());
        if (user == null) {
            user = createUser(profile);
        } else if (!STATUS_ACTIVE.equals(user.getStatus())) {
            throw new BusinessException("USER_WITHDRAWN", "이용할 수 없는 계정입니다.");
        }

        return buildResult(user);
    }

    /** 리프레시 토큰을 회전시키고 새 토큰 쌍을 만든다. */
    @Transactional
    public AuthResultDto refresh(String rawRefreshToken) {
        Long userId = refreshTokenService.consume(rawRefreshToken);

        UserDto user = userMapper.findById(userId);
        if (user == null || !STATUS_ACTIVE.equals(user.getStatus())) {
            throw new BusinessException("USER_WITHDRAWN", "이용할 수 없는 계정입니다.");
        }

        return buildResult(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    private UserDto createUser(GoogleProfileDto profile) {
        UserDto user = UserDto.builder()
                .socialProvider(PROVIDER_GOOGLE)
                .providerUserId(profile.getProviderUserId())
                .email(profile.getEmail())
                .nickname(profile.getName())   // 실명(name)은 계좌 인증 단계에서 채운다
                .status(STATUS_ACTIVE)
                .difficultyId(DEFAULT_DIFFICULTY_ID)
                .build();
        userMapper.insert(user);
        log.info("신규 가입 — userId={}", user.getId());
        return user;
    }

    private AuthResultDto buildResult(UserDto user) {
        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = refreshTokenService.issue(user.getId());
        boolean needsConsent = consentService.needsConsent(user.getId());

        return AuthResultDto.builder()
                .response(LoginResponseDto.builder()
                        .accessToken(accessToken)
                        .user(UserMeDto.builder()
                                .id(user.getId())
                                .nickname(user.getNickname())
                                .email(user.getEmail())
                                .build())
                        .needsConsent(needsConsent)
                        .build())
                .refreshToken(refreshToken)
                .build();
    }
}
