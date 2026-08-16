-- [업그레이드/신규 설치 공통] 기소는 일별 결과 1행당 최대 1건임을 DB가 강제한다.
--
-- 신규 설치 순서:
--   1. db/schema.sql 실행
--   2. 필요한 seed SQL 실행
--   3. db/migration/ 파일을 날짜순으로 실행
--
-- 기존 설치 순서:
--   1. 20260815_add_challenge_monthly_report_metrics.sql 까지 적용된 DB에서 이 파일을 실행한다.
--   2. schema.sql은 수정하지 않는다.
--
-- ── 배경 ────────────────────────────────────────────────────────────────
-- 평가·기소 배치(#168)는 5분마다 돌면서 "한도를 넘긴 사람"을 찾아 기소를 만든다.
-- 그런데 **한도를 넘긴 상태는 그날이 끝날 때까지 계속 참**이다. 중복 방지가 없으면
-- 같은 위반에 대해 기소가 틱마다 하나씩, 하루 최대 288건 쌓인다.
--
-- 배치는 INSERT ... WHERE NOT EXISTS 로 거르지만 이것은 SELECT → INSERT 2단계라
-- 두 실행이 동시에 SELECT 하면 둘 다 "없음"을 보고 둘 다 INSERT 한다.
-- 실제로 겹치는 경로는 **DEV 수동 트리거(POST /api/dev/batches/group-challenge-evaluation)를
-- 누른 순간 스케줄러도 도는 경우**다. 둘은 다른 스레드다.
-- (@Scheduled(fixedDelay) 는 자기 자신끼리만 중첩을 막아 준다)
--
-- 중복 기소가 나면 피해가 조용하지 않다.
--   · 같은 건으로 알림이 두 번 나간다
--   · 재판 목록에 같은 위반이 두 줄로 뜬다
--   · 변론이 갈라진다 (uk_def_indictment 가 기소당 1건으로 묶여 있다)
--   · 투표가 두 건으로 나뉜다 (PK(group_id, user_id, indictment_id))
--
-- 재기소 없음(A안: 무죄로 한도 이하가 되어도 기소를 지우지 않는다)이 확정 정책이므로
-- "daily_result 1행 = 기소 최대 1건" 이 도메인 규칙이고, UNIQUE 가 그 규칙과 정확히 일치한다.
-- 애플리케이션 조건문 대신 DB 가 막으면 경합 자체가 성립하지 않는다.
--
-- 기간평가(PERIOD) 기소도 end_date 행 하나에만 붙으므로 이 제약과 충돌하지 않는다.
-- 한 그룹의 eval_type 은 DAILY / PERIOD 중 하나로 고정이라 같은 행에 두 종류가 섞일 수 없다.
--
-- ⚠ 배치는 INSERT 실패(DuplicateKeyException)를 **정상 흐름으로 삼켜야** 한다.
--   이 제약은 "이미 기소된 건" 을 걸러 내는 정상 경로지 오류가 아니다.
--
-- ⚠ tbl_indictment 에 미리 넣어 둔 중복 행이 없어야 이 ALTER 가 통과한다.
--   현재 이 테이블에 행을 넣는 것은 seed_closed_group_challenge_demo.sql 하나뿐이고,
--   그 시드는 daily_result 1행당 기소 1건만 만들어(DAILY 는 r.id 당 1행, PERIOD 는 GROUP BY r.id)
--   이 제약과 충돌하지 않는다. 오히려 그 시드의 `JOIN ... ON t.result_id = i.result_id` 가
--   result_id 유일성을 전제로 하므로 이 UNIQUE 가 그 전제를 보장해 준다.
--   수동으로 넣어 둔 테스트 행 때문에 Duplicate entry 로 실패하면,
--   아래 조회로 중복을 확인한 뒤 직접 지우고 다시 실행한다.
--     SELECT result_id, COUNT(*) c FROM tbl_indictment GROUP BY result_id HAVING c > 1;

-- ── 1. 중복 기소 방지 ───────────────────────────────────────────────────
ALTER TABLE tbl_indictment
    ADD CONSTRAINT uk_ind_result UNIQUE (result_id);

-- ── 2. 중복이 된 기존 인덱스 정리 ───────────────────────────────────────
-- schema.sql 의 idx_ind_result (result_id) 는 위 UNIQUE 와 선행 컬럼이 같아
-- 탐색 능력이 완전히 겹친다. 남겨 두면 INSERT 마다 인덱스 두 벌을 갱신하고
-- 옵티마이저가 둘 중 무엇을 골랐는지 실행계획을 읽기 어려워진다.
--
-- fk_ind_result (result_id 참조) 가 인덱스를 요구하지만 uk_ind_result 가 그 역할을
-- 대신하므로 FK 는 깨지지 않는다. 순서상 UNIQUE 를 먼저 만든 뒤에 지워야 한다 —
-- 반대로 하면 MySQL 이 "Cannot drop index needed in a foreign key constraint" 로 거부한다.
ALTER TABLE tbl_indictment
    DROP INDEX idx_ind_result;

-- ── 적용 확인 ───────────────────────────────────────────────────────────
-- SHOW CREATE TABLE tbl_indictment\G
--   기대: UNIQUE KEY `uk_ind_result` (`result_id`) 있음, KEY `idx_ind_result` 없음
--
-- 롤백은 수동으로 수행한다. 위 순서를 거꾸로 밟는다.
--   1) ALTER TABLE tbl_indictment ADD INDEX idx_ind_result (result_id);
--   2) ALTER TABLE tbl_indictment DROP INDEX uk_ind_result;
