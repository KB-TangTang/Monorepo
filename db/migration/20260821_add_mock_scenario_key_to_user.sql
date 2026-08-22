-- 신규 설치 시 실행: 예. schema.sql → seed.sql 뒤 migration 순서에 따라 실행한다.
--
-- 시연용 유저에게 목서버 시나리오 키를 수동으로 고정하기 위한 컬럼이다. 기존
-- mock.server.scenario-keys 풀-나머지 로직(PooledScenarioKeyProvider, #334)은 더미 유저용으로
-- 그대로 두고, 이 컬럼이 채워진 유저만 UserOverridingScenarioKeyProvider 가 우선 적용한다.
-- 값은 API 없이 수동 SQL 로 지정한다: UPDATE tbl_user SET mock_scenario_key='...' WHERE id=...;
--
-- 되돌리는 방법: ALTER TABLE tbl_user DROP COLUMN mock_scenario_key;
ALTER TABLE tbl_user
    ADD COLUMN mock_scenario_key VARCHAR(50) NULL
    COMMENT '목서버 시나리오 키 수동 오버라이드. NULL이면 mock.server.scenario-keys 풀-나머지 로직을 따른다 (#334)'
    AFTER difficulty_id;
