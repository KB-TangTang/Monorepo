package com.kb.tangtang.user.mapper;

import com.kb.tangtang.config.RootConfig;
import com.kb.tangtang.user.dto.RefreshTokenDto;
import com.kb.tangtang.user.dto.UserDto;
import lombok.extern.log4j.Log4j2;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 매퍼 XML 이 실제 스키마와 맞는지 확인하는 통합 테스트.
 *
 * 실제 MySQL 연결이 필요하므로 기본은 비활성화다.
 * 매퍼를 고친 뒤 한 번씩 @Disabled 를 주석 처리하고 돌려 확인한 다음,
 * 반드시 다시 활성화해서 커밋한다. (켜둔 채 커밋하면 팀원 빌드가 깨진다)
 */
@Disabled("실DB 연결이 필요할 때만 임시로 해제")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RootConfig.class})
@Transactional
@Rollback
@Log4j2
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RefreshTokenMapper refreshTokenMapper;

    @Test
    @DisplayName("사용자를 넣고 소셜 ID 로 다시 찾는다")
    void insertAndFind() {
        UserDto user = UserDto.builder()
                .socialProvider("GOOGLE")
                .providerUserId("test-sub-0001")
                .email("test@example.com")
                .nickname("테스트지윤")
                .status("ACTIVE")
                .difficultyId(1L)
                .build();

        userMapper.insert(user);
        assertNotNull(user.getId(), "useGeneratedKeys 가 PK 를 채워야 한다");

        UserDto found = userMapper.findBySocialId("GOOGLE", "test-sub-0001");
        assertNotNull(found);
        assertEquals("테스트지윤", found.getNickname());
        assertEquals("ACTIVE", found.getStatus());
        assertEquals(1L, found.getDifficultyId());
    }

    @Test
    @DisplayName("없는 소셜 ID 는 null 을 돌려준다")
    void findMissing() {
        assertNull(userMapper.findBySocialId("GOOGLE", "no-such-sub"));
    }

    @Test
    @DisplayName("동의 이력이 없는 사용자는 0 건이다")
    void countConsentsEmpty() {
        UserDto user = UserDto.builder()
                .socialProvider("GOOGLE").providerUserId("test-sub-0002")
                .nickname("동의없음").status("ACTIVE").difficultyId(1L)
                .build();
        userMapper.insert(user);

        assertEquals(0, userMapper.countActiveConsents(user.getId()));
    }

    @Test
    @DisplayName("리프레시 토큰을 넣고 해시로 찾은 뒤 폐기한다")
    void refreshTokenLifecycle() {
        UserDto user = UserDto.builder()
                .socialProvider("GOOGLE").providerUserId("test-sub-0003")
                .nickname("토큰유저").status("ACTIVE").difficultyId(1L)
                .build();
        userMapper.insert(user);

        RefreshTokenDto token = RefreshTokenDto.builder()
                .userId(user.getId())
                .tokenHash("a".repeat(64))
                .expiresAt(LocalDateTime.now().plusDays(14))
                .build();
        refreshTokenMapper.insert(token);

        RefreshTokenDto found = refreshTokenMapper.findByHash("a".repeat(64));
        assertNotNull(found);
        assertEquals(user.getId(), found.getUserId());
        assertTrue(!found.isRevoked(), "새로 발급한 토큰은 폐기 상태가 아니어야 한다");

        refreshTokenMapper.revokeById(found.getId());

        RefreshTokenDto revoked = refreshTokenMapper.findByHash("a".repeat(64));
        assertTrue(revoked.isRevoked(), "폐기 후에도 조회는 되어야 재사용 감지가 가능하다");
    }
}
