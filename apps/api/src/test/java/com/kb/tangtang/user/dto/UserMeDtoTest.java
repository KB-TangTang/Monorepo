package com.kb.tangtang.user.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserMeDtoTest {

    @Test
    @DisplayName("소셜 제공자를 함께 내려준다 — 마이페이지 프로필 카드가 쓴다")
    void carriesSocialProvider() {
        UserMeDto dto = UserMeDto.builder()
                .id(1L).nickname("김지갑").email("a@b.com").socialProvider("GOOGLE")
                .build();

        assertEquals("GOOGLE", dto.getSocialProvider());
    }

    @Test
    @DisplayName("프로필 이미지 URL 을 그대로 싣는다")
    void carriesProfileImageUrl() {
        UserDto user = UserDto.builder()
                .id(7L)
                .nickname("지윤")
                .socialName("JH Jang")
                .profileImageKey("profile/7/abc.jpg")
                .build();

        UserMeDto result = UserMeDto.from(user, "/uploads/profile/7/abc.jpg");

        assertEquals("/uploads/profile/7/abc.jpg", result.getProfileImageUrl());
    }

    @Test
    @DisplayName("이미지가 없으면 URL 은 null 이다 — 화면이 이니셜 아바타로 폴백한다")
    void nullUrlWhenUnset() {
        UserDto user = UserDto.builder().id(7L).nickname("지윤").build();

        UserMeDto result = UserMeDto.from(user, null);

        assertNull(result.getProfileImageUrl());
    }
}
