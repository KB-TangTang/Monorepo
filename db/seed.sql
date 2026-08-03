-- =====================================================================
--  db/seed.sql — 개발용 초기 데이터
--
--  schema.sql 실행 후에 넣는다.
--    mysql -u tangtang -p tangtang < db/seed.sql
--
--  tbl_mission_difficulty 는 tbl_user.difficulty_id 가 NOT NULL FK 로 참조한다.
--  이 3행이 없으면 회원가입 INSERT 자체가 실패하므로 반드시 선행되어야 한다.
-- =====================================================================

USE tangtang;

INSERT INTO tbl_mission_difficulty (id, difficulty_name, min_reduction_rate, max_reduction_rate, score) VALUES
  (1, 'EASY',   10.00, 20.00, 10),
  (2, 'NORMAL', 20.00, 40.00, 20),
  (3, 'HARD',   40.00, 50.00, 35);

INSERT INTO tbl_fixed_expense_rule (rule_name, min_repeat_count, amount_tolerance_pct, cycle_tolerance_days, is_active)
VALUES ('기본 룰 v1', 3, 10.00, 7, 1);

-- 카테고리·미션 풀 시드는 팀 seed 스크립트에서 관리한다.
