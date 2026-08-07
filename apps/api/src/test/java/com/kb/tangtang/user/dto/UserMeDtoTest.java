package com.kb.tangtang.user.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserMeDtoTest {

    @Test
    @DisplayName("소셜 제공자를 함께 내려준다 — 마이페이지 프로필 카드가 쓴다")
    void carriesSocialProvider() {
        UserMeDto dto = UserMeDto.builder()
                .id(1L).nickname("김지갑").email("a@b.com").socialProvider("GOOGLE")
                .build();

        assertEquals("GOOGLE", dto.getSocialProvider());
    }
}
