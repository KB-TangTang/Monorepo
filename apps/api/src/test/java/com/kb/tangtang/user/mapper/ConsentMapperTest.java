package com.kb.tangtang.user.mapper;

import com.kb.tangtang.config.RootConfig;
import com.kb.tangtang.user.dto.ConsentRecordDto;
import com.kb.tangtang.user.dto.MyConsentRowDto;
import com.kb.tangtang.user.dto.UserDto;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 매퍼 XML 이 실제 스키마와 맞는지 확인한다.
 * 실DB 연결이 필요하므로 기본 비활성. 매퍼를 고친 뒤 한 번 돌려보고 다시 켜서 커밋한다.
 */
@Disabled("실DB 연결이 필요할 때만 임시로 해제")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RootConfig.class})
@Transactional
@Rollback
class ConsentMapperTest {

    @Autowired private ConsentMapper consentMapper;
    @Autowired private UserMapper userMapper;

    private Long newUser(String sub) {
        UserDto user = UserDto.builder()
                .socialProvider("GOOGLE").providerUserId(sub)
                .nickname("동의테스트").status("ACTIVE").difficultyId(1L)
                .build();
        userMapper.insert(user);
        return user.getId();
    }

    private static ConsentRecordDto agreed(Long userId, String type, boolean required) {
        return ConsentRecordDto.builder()
                .userId(userId).consentType(type).required(required)
                .termsVersion("v1.0").status(1).withdrawnAt(null).expiresAt(null)
                .build();
    }

    @Test
    @DisplayName("동의를 넣고 사용자로 다시 읽는다")
    void insertAndFind() {
        Long userId = newUser("consent-sub-0001");
        consentMapper.upsert(agreed(userId, "TERMS", true));

        List<MyConsentRowDto> rows = consentMapper.findByUserId(userId);
        assertEquals(1, rows.size());
        assertEquals("TERMS", rows.get(0).getConsentType());
        assertEquals("v1.0", rows.get(0).getTermsVersion());
        assertEquals(1, rows.get(0).getStatus());
        assertNotNull(rows.get(0).getCreatedAt());
    }

    @Test
    @DisplayName("같은 타입을 다시 저장하면 행이 늘지 않고 갱신된다")
    void upsertIsIdempotent() {
        Long userId = newUser("consent-sub-0002");
        consentMapper.upsert(agreed(userId, "MARKETING", false));

        ConsentRecordDto withdrawn = ConsentRecordDto.builder()
                .userId(userId).consentType("MARKETING").required(false)
                .termsVersion("v1.0").status(0).withdrawnAt(LocalDateTime.now()).expiresAt(null)
                .build();
        consentMapper.upsert(withdrawn);

        List<MyConsentRowDto> rows = consentMapper.findByUserId(userId);
        assertEquals(1, rows.size(), "유니크 제약으로 행이 하나여야 한다");
        assertEquals(0, rows.get(0).getStatus());
        assertNotNull(rows.get(0).getWithdrawnAt());
    }

    @Test
    @DisplayName("활성 건수는 status·withdrawn_at·expires_at 을 모두 본다")
    void countActiveRespectsAllConditions() {
        Long userId = newUser("consent-sub-0003");
        LocalDateTime now = LocalDateTime.now();

        consentMapper.upsert(agreed(userId, "TERMS", true));
        consentMapper.upsert(agreed(userId, "PRIVACY", true));
        // 만료된 금융정보 동의
        consentMapper.upsert(ConsentRecordDto.builder()
                .userId(userId).consentType("FINANCIAL_DATA").required(true)
                .termsVersion("v1.0").status(1).withdrawnAt(null)
                .expiresAt(now.minusDays(1))
                .build());

        List<String> required = List.of("TERMS", "PRIVACY", "FINANCIAL_DATA");
        assertEquals(2, consentMapper.countActive(userId, required, now),
                "만료된 FINANCIAL_DATA 는 세지 않는다");
    }

    @Test
    @DisplayName("철회하면 status 0 · withdrawn_at 이 채워지고 행은 남는다")
    void withdrawUpdatesRow() {
        Long userId = newUser("consent-sub-0004");
        consentMapper.upsert(agreed(userId, "AI_USAGE", false));

        int affected = consentMapper.withdraw(userId, List.of("AI_USAGE"), LocalDateTime.now());
        assertEquals(1, affected);

        List<MyConsentRowDto> rows = consentMapper.findByUserId(userId);
        assertEquals(1, rows.size(), "철회는 행을 지우지 않는다");
        assertEquals(0, rows.get(0).getStatus());
        assertNotNull(rows.get(0).getWithdrawnAt());
    }

    @Test
    @DisplayName("이미 철회한 항목을 다시 철회하면 0건이다")
    void withdrawTwice() {
        Long userId = newUser("consent-sub-0005");
        consentMapper.upsert(agreed(userId, "MARKETING", false));
        consentMapper.withdraw(userId, List.of("MARKETING"), LocalDateTime.now());

        assertEquals(0, consentMapper.withdraw(userId, List.of("MARKETING"), LocalDateTime.now()));
    }
}
