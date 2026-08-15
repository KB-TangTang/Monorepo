package com.kb.tangtang.account.client.sync;

/** 로그인 사용자를 목서버 scenarioKey 로 바꾼다. userId 를 그대로 넘기지 않는다. */
public interface ScenarioKeyProvider {
    String resolve(Long userId);
}
