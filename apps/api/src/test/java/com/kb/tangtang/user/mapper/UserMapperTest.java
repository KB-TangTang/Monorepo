package com.kb.tangtang.user.mapper;

import com.kb.tangtang.config.RootConfig;
import com.kb.tangtang.user.dto.RefreshTokenDto;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
                .socialName("테스트지윤")
                .status("ACTIVE")
                .difficultyId(1L)
                .build();

        userMapper.insert(user);
        assertNotNull(user.getId(), "useGeneratedKeys 가 PK 를 채워야 한다");

        UserDto found = userMapper.findBySocialId("GOOGLE", "test-sub-0001");
        assertNotNull(found);
        /*
         * [정정 2026-08-13] 이 테스트는 nickname 을 넣고 다시 읽히기를 기대했으나,
         * insert 는 2026-08-11 닉네임 온보딩 결정 이후 **nickname 을 넣지 않는다**
         * (가입 시점에 NULL 이어야 「닉네임 미설정」으로 판별된다).
         * 형제 테스트 insertLeavesNicknameNull 이 그 동작을 검증하고 있어 기대값이 서로 모순이었다.
         * @Disabled 클래스라 드러나지 않았을 뿐 계속 실패하던 단언이다 — 실제 동작에 맞춘다.
         */
        assertEquals("테스트지윤", found.getSocialName());
        assertNull(found.getNickname(), "가입 시 nickname 은 NULL 이다");
        assertEquals("ACTIVE", found.getStatus());
        assertEquals(1L, found.getDifficultyId());
    }

    @Test
    @DisplayName("없는 소셜 ID 는 null 을 돌려준다")
    void findMissing() {
        assertNull(userMapper.findBySocialId("GOOGLE", "no-such-sub"));
    }

    @Test
    @DisplayName("실명을 저장하면 findById 로 다시 읽힌다")
    void updateName() {
        UserDto user = UserDto.builder()
                .socialProvider("GOOGLE").providerUserId("test-sub-0002")
                .email("name@example.com").nickname("이름없음")
                .status("ACTIVE").difficultyId(1L)
                .build();
        userMapper.insert(user);
        assertNull(userMapper.findById(user.getId()).getName(), "가입 직후 실명은 비어 있다");

        assertEquals(1, userMapper.updateName(user.getId(), "장재한"));
        assertEquals("장재한", userMapper.findById(user.getId()).getName());
    }

    @Test
    @DisplayName("없는 사용자의 실명 갱신은 0행을 돌려준다")
    void updateNameMissing() {
        assertEquals(0, userMapper.updateName(-1L, "장재한"));
    }

    @Test
    @DisplayName("가입 시 닉네임은 비고 소셜 이름만 들어간다 — 온보딩 판별 기준")
    void insertLeavesNicknameNull() {
        UserDto user = UserDto.builder()
                .socialProvider("GOOGLE").providerUserId("test-sub-0005")
                .email("nick@example.com").socialName("JH Jang")
                .status("ACTIVE").difficultyId(1L)
                .build();
        userMapper.insert(user);

        UserDto found = userMapper.findById(user.getId());
        assertNull(found.getNickname(), "nickname IS NULL 이어야 온보딩 화면이 뜬다");
        assertEquals("JH Jang", found.getSocialName());

        assertEquals(1, userMapper.updateNickname(user.getId(), "탕탕이"));
        UserDto after = userMapper.findById(user.getId());
        assertEquals("탕탕이", after.getNickname());
        assertEquals("JH Jang", after.getSocialName(), "닉네임 설정이 소셜 이름을 덮으면 안 된다");
    }

    @Test
    @DisplayName("없는 사용자의 닉네임 갱신은 0행을 돌려준다")
    void updateNicknameMissing() {
        assertEquals(0, userMapper.updateNickname(-1L, "탕탕이"));
    }

    @Test
    @DisplayName("튜토리얼 완료 시각은 개인·그룹이 서로 간섭하지 않는다")
    void updateTutorialSeenAt() {
        UserDto user = UserDto.builder()
                .socialProvider("GOOGLE").providerUserId("test-sub-0004")
                .email("tutorial@example.com").nickname("튜토리얼")
                .status("ACTIVE").difficultyId(1L)
                .build();
        userMapper.insert(user);
        assertNull(userMapper.findById(user.getId()).getTutorialSeenAt());
        assertNull(userMapper.findById(user.getId()).getGroupTutorialSeenAt());

        LocalDateTime seen = LocalDateTime.now().withNano(0);

        assertEquals(1, userMapper.updateTutorialSeenAt(user.getId(), "MAIN", seen));
        UserDto afterMain = userMapper.findById(user.getId());
        assertEquals(seen, afterMain.getTutorialSeenAt());
        assertNull(afterMain.getGroupTutorialSeenAt(), "MAIN 갱신이 그룹을 건드리면 안 된다");

        assertEquals(1, userMapper.updateTutorialSeenAt(user.getId(), "GROUP", seen));
        UserDto afterGroup = userMapper.findById(user.getId());
        assertEquals(seen, afterGroup.getTutorialSeenAt(), "GROUP 갱신이 개인을 지우면 안 된다");
        assertEquals(seen, afterGroup.getGroupTutorialSeenAt());

        // 다시 보기 — null 이 실제로 들어가는지 (jdbcType 누락 시 여기서 터진다)
        assertEquals(1, userMapper.updateTutorialSeenAt(user.getId(), "MAIN", null));
        UserDto afterReset = userMapper.findById(user.getId());
        assertNull(afterReset.getTutorialSeenAt());
        assertEquals(seen, afterReset.getGroupTutorialSeenAt(), "개인만 지워져야 한다");
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
        assertFalse(found.isRevoked(), "새로 발급한 토큰은 폐기 상태가 아니어야 한다");

        refreshTokenMapper.revokeById(found.getId());

        RefreshTokenDto revoked = refreshTokenMapper.findByHash("a".repeat(64));
        assertTrue(revoked.isRevoked(), "폐기 후에도 조회는 되어야 재사용 감지가 가능하다");
    }

    @Test
    @DisplayName("탈퇴하면 식별정보가 지워지고 provider_user_id 에 접미사가 붙는다")
    void 탈퇴_익명화() {
        UserDto user = UserDto.builder()
                .socialProvider("GOOGLE").providerUserId("sub-withdraw-1")
                .email("a@b.com").socialName("홍길동")
                .status("ACTIVE").difficultyId(2L)
                .build();
        userMapper.insert(user);

        assertEquals(1, userMapper.withdraw(user.getId(), LocalDateTime.now()));

        UserDto after = userMapper.findById(user.getId());
        assertEquals("WITHDRAWN", after.getStatus());
        assertNull(after.getEmail());
        assertNull(after.getSocialName());
        assertEquals("sub-withdraw-1_withdrawn_" + user.getId(), after.getProviderUserId());
    }

    @Test
    @DisplayName("탈퇴 후 같은 소셜 ID 로 조회되지 않는다 — 재가입이 가능해진다")
    void 탈퇴후_소셜조회_미스() {
        UserDto user = UserDto.builder()
                .socialProvider("GOOGLE").providerUserId("sub-withdraw-2")
                .email("c@d.com").status("ACTIVE").difficultyId(2L)
                .build();
        userMapper.insert(user);
        userMapper.withdraw(user.getId(), LocalDateTime.now());

        assertNull(userMapper.findBySocialId("GOOGLE", "sub-withdraw-2"));
    }

    @Test
    @DisplayName("두 번 탈퇴해도 접미사가 두 번 붙지 않는다 — 멱등")
    void 탈퇴_멱등() {
        UserDto user = UserDto.builder()
                .socialProvider("GOOGLE").providerUserId("sub-withdraw-3")
                .status("ACTIVE").difficultyId(2L)
                .build();
        userMapper.insert(user);

        assertEquals(1, userMapper.withdraw(user.getId(), LocalDateTime.now()));
        assertEquals(0, userMapper.withdraw(user.getId(), LocalDateTime.now()));

        assertEquals("sub-withdraw-3_withdrawn_" + user.getId(),
                userMapper.findById(user.getId()).getProviderUserId());
    }
}
