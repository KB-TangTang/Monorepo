package com.kb.tangtang.account.client.sync;

import com.kb.tangtang.user.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserOverridingScenarioKeyProviderTest {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final PooledScenarioKeyProvider fallback = new PooledScenarioKeyProvider(List.of("1", "2"));
    private final UserOverridingScenarioKeyProvider provider =
            new UserOverridingScenarioKeyProvider(userMapper, fallback);

    @Test
    @DisplayName("컬럼에 오버라이드 값이 있으면 풀 계산 없이 그 값을 돌려준다")
    void returnsOverrideWhenPresent() {
        when(userMapper.findMockScenarioKeyById(101L)).thenReturn("demo-vip-user");

        assertEquals("demo-vip-user", provider.resolve(101L));
    }

    @Test
    @DisplayName("컬럼이 NULL이면 풀-나머지 로직으로 폴백한다")
    void fallsBackWhenNull() {
        when(userMapper.findMockScenarioKeyById(4L)).thenReturn(null);

        assertEquals(fallback.resolve(4L), provider.resolve(4L));
    }

    @Test
    @DisplayName("컬럼이 빈 문자열이면 풀-나머지 로직으로 폴백한다")
    void fallsBackWhenBlank() {
        when(userMapper.findMockScenarioKeyById(5L)).thenReturn("   ");

        assertEquals(fallback.resolve(5L), provider.resolve(5L));
    }
}
