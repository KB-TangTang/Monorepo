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
                /*
                 * 구글 이름은 socialName 으로 들어간다. nickname 은 **비워 둔다** —
                 * 그래야 "닉네임을 설정했는지"를 nickname IS NULL 하나로 판별할 수 있다.
                 * 미리 채워두면 설정한 값과 구분할 별도 플래그 컬럼이 필요해진다.
                 * (DECISIONS.md 2026-08-11 닉네임 온보딩)
                 * 실명(name)은 계좌 인증 단계에서 채운다.
                 */
                .socialName(profile.getName())
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
                        /*
                         * ⚠ 프론트는 부팅 시 이 응답 하나로 스토어를 채우고, 이후 화면들은
                         * 추가 조회 없이 여기 실린 값만 본다 — 튜토리얼 노출(seenAt 이 null 인가)과
                         * 닉네임 온보딩 가드(nickname 이 null 인가)가 둘 다 그렇다.
                         * 그래서 /users/me 와 **같은 모양**을 내려준다. 필드가 빠지면
                         * 그 경로에서만 튜토리얼이 다시 뜨거나 온보딩으로 튕기는 버그가 난다.
                         */
                        .user(UserMeDto.from(user))
                        .needsConsent(needsConsent)
                        .build())
                .refreshToken(refreshToken)
                .build();
    }
}
