package com.kb.tangtang.account.client.sync;

import java.util.List;

/**
 * 시나리오 키 풀에서 userId 를 인덱스로 하나를 고른다.
 *
 * 로그인 사용자의 실제 ID를 목서버에 그대로 전달하지 않기 위한 설계다 — userId 는 나눗셈 인덱스로만
 * 쓰인다. 풀에 키가 하나뿐이면(기본값 "1") 전원 같은 목데이터를 받는다. 더미 사용자를 늘릴 때는
 * mock.server.scenario-keys 프로퍼티에 키를 추가하기만 하면 되고, 이 클래스는 바뀌지 않는다.
 */
public class PooledScenarioKeyProvider implements ScenarioKeyProvider {

    private final List<String> scenarioKeys;

    public PooledScenarioKeyProvider(List<String> scenarioKeys) {
        if (scenarioKeys == null || scenarioKeys.isEmpty()) {
            throw new IllegalArgumentException(
                    "mock.server.scenario-keys 가 비어 있어요. 최소 1개는 설정해야 합니다.");
        }
        this.scenarioKeys = List.copyOf(scenarioKeys);
    }

    @Override
    public String resolve(Long userId) {
        int index = (int) Math.floorMod(userId, (long) scenarioKeys.size());
        return scenarioKeys.get(index);
    }
}
