package com.kb.tangtang.user.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.common.storage.ImageProcessor;
import com.kb.tangtang.common.storage.ImageStorage;
import com.kb.tangtang.user.domain.TutorialType;
import com.kb.tangtang.user.dto.UserDto;
import com.kb.tangtang.user.dto.UserMeDto;
import com.kb.tangtang.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
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

    /** tbl_user.nickname 도 VARCHAR(50) 이다. 하한은 두지 않는다 — 표시명은 한 글자여도 된다. */
    private static final int NICKNAME_MAX_LENGTH = 50;

    private final UserMapper userMapper;
    private final ProfileImageUrlResolver profileImageUrlResolver;
    private final ImageStorage imageStorage;
    private final ImageProcessor imageProcessor;

    public UserService(UserMapper userMapper, ProfileImageUrlResolver profileImageUrlResolver,
                       ImageStorage imageStorage, ImageProcessor imageProcessor) {
        this.userMapper = userMapper;
        this.profileImageUrlResolver = profileImageUrlResolver;
        this.imageStorage = imageStorage;
        this.imageProcessor = imageProcessor;
    }

    @Transactional(readOnly = true)
    public UserMeDto me(long userId) {
        return meOf(userId);
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
        return meOf(userId);
    }

    /**
     * 닉네임(표시명) 설정·수정 — 온보딩(AU_03_01)과 마이페이지(MY_01_03)가 함께 쓴다.
     *
     * ⚠ **중복 검사를 하지 않는다.** 닉네임 중복 허용이 팀 결정이라 UNIQUE 제약도 없다.
     *   유니크를 걸면 흔한 이름이 전부 선점돼 가입 직후가 이탈 지점이 된다.
     *   그룹챌린지는 초대 기반 소규모라 중복이 실제 문제가 되지 않는다.
     *   (DECISIONS.md 2026-08-11 닉네임 온보딩)
     *
     * 빈 값 저장은 허용하지 않는다 — 온보딩에서 강제로 받은 값이라
     * 미설정(null) 상태로 되돌릴 경로를 두지 않는다.
     */
    @Transactional
    public UserMeDto updateNickname(long userId, String rawNickname) {
        String nickname = normalizeNickname(rawNickname);
        if (userMapper.updateNickname(userId, nickname) == 0) {
            throw new BusinessException("NOT_FOUND", "사용자를 찾을 수 없습니다.");
        }
        return meOf(userId);
    }

    /**
     * 프로필 이미지 업로드 (온보딩 AU_03_01 · 마이페이지 MY_01_03 공용).
     *
     * ⚠ **새로 저장한 뒤에 옛 파일을 지운다.** 순서를 뒤집으면 저장이 실패했을 때
     *   기존 사진까지 잃는다. 키에 UUID 를 넣어 매번 새 파일이 되게 한다 —
     *   같은 이름을 덮어쓰면 브라우저·CDN 캐시가 옛 사진을 계속 보여준다.
     */
    @Transactional
    public UserMeDto updateProfileImage(long userId, byte[] content) {
        UserDto user = findActive(userId);
        String oldKey = user.getProfileImageKey();

        byte[] jpeg = imageProcessor.toSquareJpeg(content);
        String newKey = "profile/" + userId + "/" + UUID.randomUUID() + ".jpg";
        imageStorage.store(jpeg, newKey);

        if (userMapper.updateProfileImageKey(userId, newKey) == 0) {
            throw new BusinessException("NOT_FOUND", "사용자를 찾을 수 없습니다.");
        }
        if (oldKey != null) {
            imageStorage.delete(oldKey);
        }
        return meOf(userId);
    }

    /**
     * 프로필 이미지 삭제 — 기본(이니셜) 아바타로 되돌린다.
     * 이미 미설정이어도 성공으로 처리한다. 없는 것을 지우는 것은 오류가 아니다.
     */
    @Transactional
    public UserMeDto deleteProfileImage(long userId) {
        UserDto user = findActive(userId);
        String oldKey = user.getProfileImageKey();

        if (userMapper.updateProfileImageKey(userId, null) == 0) {
            throw new BusinessException("NOT_FOUND", "사용자를 찾을 수 없습니다.");
        }
        if (oldKey != null) {
            imageStorage.delete(oldKey);
        }
        return meOf(userId);
    }

    /**
     * 닉네임 형식 검증.
     *
     * 실명(name)보다 규칙이 느슨하다 — 표시명이라 한 글자도, 이모지·기호도 막을 이유가 없다.
     * 막는 건 **빈 값과 길이 초과** 둘뿐이다. (API 연동규격 No.87)
     */
    private static String normalizeNickname(String rawNickname) {
        String nickname = rawNickname == null ? "" : rawNickname.trim();
        if (nickname.isEmpty()) {
            throw new BusinessException("INVALID_REQUEST", "닉네임을 입력해 주세요.");
        }
        if (nickname.length() > NICKNAME_MAX_LENGTH) {
            throw new BusinessException("INVALID_REQUEST",
                    "닉네임은 " + NICKNAME_MAX_LENGTH + "자 이하로 입력해 주세요.");
        }
        return nickname;
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
        return meOf(userId);
    }

    /** 조회 → URL 조립 → DTO. 네 곳이 같은 일을 하므로 여기 모은다. */
    private UserMeDto meOf(long userId) {
        UserDto user = findActive(userId);
        return UserMeDto.from(user, profileImageUrlResolver.resolve(user));
    }

    private UserDto findActive(long userId) {
        UserDto user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("NOT_FOUND", "사용자를 찾을 수 없습니다.");
        }
        return user;
    }

}
