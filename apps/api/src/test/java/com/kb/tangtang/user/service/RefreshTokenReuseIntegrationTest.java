package com.kb.tangtang.user.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.config.RootConfig;
import com.kb.tangtang.user.dto.RefreshTokenDto;
import com.kb.tangtang.user.dto.UserDto;
import com.kb.tangtang.user.mapper.RefreshTokenMapper;
import com.kb.tangtang.user.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 재사용 감지로 인한 전체 폐기가 바깥 트랜잭션 롤백을 견디는지 검증한다.
 *
 * 이 테스트에는 @Transactional 을 붙이지 않는다. 붙이면 테스트 자체가 롤백돼
 * "커밋이 살아남았는가" 를 관찰할 수 없어 검증하려는 성질이 사라진다.
 * 대신 @AfterEach 에서 직접 지운다.
 *
 * 실DB 연결이 필요하므로 기본은 비활성화다.
 */
@Disabled("실DB 연결이 필요할 때만 임시로 해제")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RootConfig.class})
class RefreshTokenReuseIntegrationTest {

    private static final String TEST_SUB = "reuse-rollback-test-sub";

    @Autowired private UserMapper userMapper;
    @Autowired private RefreshTokenMapper refreshTokenMapper;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private AuthService authService;
    @Autowired private DataSource dataSource;

    @AfterEach
    void cleanUp() {
        // tbl_refresh_token 은 tbl_user 에 ON DELETE CASCADE 로 걸려 있어 함께 지워진다
        new JdbcTemplate(dataSource).update(
                "DELETE FROM tbl_user WHERE provider_user_id = ?", TEST_SUB);
    }

    @Test
    @DisplayName("재사용 감지 시 전체 폐기는 바깥 트랜잭션이 롤백돼도 커밋된 채 남는다")
    void revokeAllSurvivesRollback() {
        UserDto user = UserDto.builder()
                .socialProvider("GOOGLE").providerUserId(TEST_SUB)
                .nickname("재사용테스트").status("ACTIVE").difficultyId(1L)
                .build();
        userMapper.insert(user);

        String stolen = refreshTokenService.issue(user.getId());
        String alive = refreshTokenService.issue(user.getId());

        // stolen 을 한 번 정상 사용해 폐기 상태로 만든다 (= 회전이 일어난 상태)
        refreshTokenService.consume(stolen);

        // 탈취자가 폐기된 토큰을 다시 쓴다. AuthService.refresh 는 @Transactional 이라
        // 여기서 던져지는 예외가 바깥 트랜잭션을 롤백시킨다.
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refresh(stolen));
        assertEquals("REFRESH_TOKEN_REUSED", ex.getCode());

        // 핵심 단언: 살아 있던 다른 토큰까지 폐기돼 있어야 한다.
        // REQUIRES_NEW 가 아니면 롤백돼 alive 가 그대로 살아남고 이 단언이 깨진다.
        RefreshTokenDto aliveRow =
                refreshTokenMapper.findByHash(RefreshTokenService.sha256Hex(alive));
        assertTrue(aliveRow.isRevoked(),
                "재사용 감지 시 전체 폐기가 독립 트랜잭션으로 커밋돼 있어야 한다");
    }
}
