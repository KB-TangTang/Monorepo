package com.kb.tangtang.account.client.sync;

import com.kb.tangtang.user.mapper.UserMapper;

/**
 * tbl_user.mock_scenario_key 가 채워진 유저는 그 값을 그대로 쓰고, 비어 있으면(NULL) fallback
 * (PooledScenarioKeyProvider) 으로 넘긴다. 시연용 유저에게 특정 시나리오를 고정하기 위한 것으로,
 * 값은 API 없이 수동 SQL 로 지정한다(UPDATE tbl_user SET mock_scenario_key=... WHERE id=...).
 *
 * account.client.sync 가 user 모듈의 UserMapper 를 직접 참조하는 것은 모듈 경계 원칙(Service 간
 * 직접 호출 최소화)의 예외다. ChatStompController 가 같은 패턴(다른 모듈 Mapper 직접 주입)을 이미
 * 쓰고 있어 그 선례를 따른다 — 시나리오 키 조회 하나만을 위해 별도 이벤트·Service 계층을 두는 것은
 * 과하다고 판단했다.
 */
public class UserOverridingScenarioKeyProvider implements ScenarioKeyProvider {

    private final UserMapper userMapper;
    private final ScenarioKeyProvider fallback;

    public UserOverridingScenarioKeyProvider(UserMapper userMapper, ScenarioKeyProvider fallback) {
        this.userMapper = userMapper;
        this.fallback = fallback;
    }

    @Override
    public String resolve(Long userId) {
        String override = userMapper.findMockScenarioKeyById(userId);
        if (override != null && !override.isBlank()) {
            return override;
        }
        return fallback.resolve(userId);
    }
}
