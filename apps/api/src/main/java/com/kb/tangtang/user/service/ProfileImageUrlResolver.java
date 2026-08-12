package com.kb.tangtang.user.service;

import com.kb.tangtang.common.storage.ImageStorage;
import com.kb.tangtang.user.dto.UserDto;
import org.springframework.stereotype.Component;

/**
 * 사용자 행의 프로필 이미지 키를 URL 로 푼다.
 *
 * UserService 와 AuthService 가 각자 저장소를 부르면 조립 규칙이 두 벌이 된다.
 * 한 곳에 모아 두 서비스가 이것만 주입받는다.
 */
@Component
public class ProfileImageUrlResolver {

    private final ImageStorage imageStorage;

    public ProfileImageUrlResolver(ImageStorage imageStorage) {
        this.imageStorage = imageStorage;
    }

    /** 키가 없으면 null 이다 — 화면은 null 을 보고 이니셜 아바타를 그린다. */
    public String resolve(UserDto user) {
        return user == null ? null : imageStorage.urlOf(user.getProfileImageKey());
    }
}
