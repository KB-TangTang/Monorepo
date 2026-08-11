package com.kb.tangtang.user.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.user.domain.TutorialType;
import com.kb.tangtang.user.dto.UserDto;
import com.kb.tangtang.user.dto.UserMeDto;
import com.kb.tangtang.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * 로그인한 사용자 본인 정보 조회·수정.
 *
 * 컨트롤러가 매퍼를 직접 쓰던 것을 이 서비스로 옮겼다 — 쓰기(실명 갱신)가 생겨
 * 트랜잭션 경계가 필요해졌기 때문이다. (apps/api/AGENTS.md: 트랜잭션 경계는 Service)
 *
 * 실명(name)은 간편인증 화면이 채운다. 표시명(nickname)과 다른 값이다.
 * (DECISIONS.md 2026-08-11 (4))
 */
@Service
public class UserService {

    /** tbl_user.name 이 VARCHAR(50) 이다. 넘치면 DB 가 자르기 전에 여기서 막는다. */
    private static final int NAME_MAX_LENGTH = 50;

    /** 실명이 한 글자인 경우는 없다. 오타·실수 입력을 걸러내는 하한이다. */
    private static final int NAME_MIN_LENGTH = 2;

    /**
     * 한글·영문·공백만 허용한다.
     * 자모(ㄱ, ㅏ)는 '가-힣' 범위 밖이라 자동으로 걸러진다 — 조합이 덜 된 입력을 막는다.
     */
    private static final Pattern NAME_PATTERN = Pattern.compile("^[가-힣a-zA-Z ]+$");

    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public UserMeDto me(long userId) {
        return toMeDto(findActive(userId));
    }

    /**
     * 실명 갱신 (간편인증 화면).
     *
     * ⚠ 여기서 저장하는 값은 **이름 하나뿐이다.** 같은 화면에서 받는 생년월일·통신사·휴대폰은
     *   공급자에 전달만 하고 저장하지 않는다 — 그쪽은 account 모듈이 다루며 DB 에 남기지 않는다.
     *   (DECISIONS.md 2026-08-05 계좌 연동 · 2026-08-11 (4))
     *
     * @return 갱신된 사용자 정보. 프론트가 스토어를 바로 갱신할 수 있게 돌려준다.
     */
    @Transactional
    public UserMeDto updateName(long userId, String rawName) {
        String name = normalizeName(rawName);
        if (userMapper.updateName(userId, name) == 0) {
            throw new BusinessException("NOT_FOUND", "사용자를 찾을 수 없습니다.");
        }
        return toMeDto(findActive(userId));
    }

    /**
     * 실명 형식 검증. 프론트도 같은 규칙으로 검사하지만
     * (utils/account.js validateSimpleAuthForm) 화면을 거치지 않는 요청이 들어올 수 있어
     * 서버에서 다시 본다.
     */
    private static String normalizeName(String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty()) {
            throw new BusinessException("INVALID_NAME", "이름을 입력해 주세요.");
        }
        if (name.length() < NAME_MIN_LENGTH || name.length() > NAME_MAX_LENGTH) {
            throw new BusinessException("INVALID_NAME",
                    "이름을 " + NAME_MIN_LENGTH + "~" + NAME_MAX_LENGTH + "자로 입력해 주세요.");
        }
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new BusinessException("INVALID_NAME", "이름은 한글 또는 영문으로 입력해 주세요.");
        }
        return name;
    }

    /**
     * 튜토리얼을 봤다고 표시한다 (이슈 #128).
     *
     * 시각을 서버에서 찍는다 — 클라이언트 시계를 믿으면 기기 시간만 바꿔도 값이 어긋난다.
     * 이 프로젝트에는 Clock 빈이 없어 LocalDateTime.now() 를 그대로 쓴다.
     */
    @Transactional
    public UserMeDto markTutorialSeen(long userId, TutorialType type) {
        return writeTutorialSeenAt(userId, type, LocalDateTime.now());
    }

    /**
     * 「튜토리얼 다시 보기」 (마이페이지).
     * 완료 시각을 지우면 다음 진입 때 다시 뜬다 — 별도 플래그를 두지 않는다.
     */
    @Transactional
    public UserMeDto clearTutorialSeen(long userId, TutorialType type) {
        return writeTutorialSeenAt(userId, type, null);
    }

    private UserMeDto writeTutorialSeenAt(long userId, TutorialType type, LocalDateTime seenAt) {
        if (userMapper.updateTutorialSeenAt(userId, type.name(), seenAt) == 0) {
            throw new BusinessException("NOT_FOUND", "사용자를 찾을 수 없습니다.");
        }
        return toMeDto(findActive(userId));
    }

    private UserDto findActive(long userId) {
        UserDto user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("NOT_FOUND", "사용자를 찾을 수 없습니다.");
        }
        return user;
    }

    private static UserMeDto toMeDto(UserDto user) {
        return UserMeDto.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .name(user.getName())
                .email(user.getEmail())
                .socialProvider(user.getSocialProvider())
                .tutorialSeenAt(user.getTutorialSeenAt())
                .groupTutorialSeenAt(user.getGroupTutorialSeenAt())
                .build();
    }
}
