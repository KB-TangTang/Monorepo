-- =====================================================================
-- db/seed_closed_group_challenge_demo.sql
--
-- 종료된 그룹 챌린지 이력 시드. 기존 seed_local_demo.sql을 대체하지 않는다.
--
-- 선행 실행:
--   1. schema.sql, seed.sql, seed_category.sql, seed_mission_pool.sql
--   2. migration/ 전체 (특히 20260809, 20260814 이후)
--   3. seed_local_demo.sql
--
-- 전제:
--   - user_id=1은 ACTIVE 상태이며 활성 연동 계좌를 하나 이상 가진다.
--   - seed_local_demo.sql이 만든 ACTIVE DEMO 사용자 중, user_id=1 이외에
--     활성 연동 계좌를 가진 사용자가 3명 이상 있다.
--   - 카테고리 ID 14(배달앱), 21(택시/모빌리티)이 존재한다.
--
-- 이 파일의 소유 표식:
--   - 그룹 초대 코드: ENDG2, ENDG3, ENDG4
--   - 거래 키: GCLOSE-2026-%
-- 재실행 시 위 표식의 행만 삭제하고 다시 넣는다. 기존 로컬 시드는 건드리지 않는다.
-- 되돌리기: 아래 초기화 DELETE 두 문장만 같은 순서로 실행한다.
-- =====================================================================

USE tangtang;
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 0. 선행 조건 확인
-- ---------------------------------------------------------------------
SET @closed_seed_user_1 = (
    SELECT u.id
      FROM tbl_user u
     WHERE u.id = 1
       AND u.status = 'ACTIVE'
);

SET @closed_seed_user_2 = (
    SELECT u.id
      FROM tbl_user u
      JOIN tbl_connected_account ca
        ON ca.user_id = u.id
       AND ca.is_active = 1
     WHERE u.social_provider = 'DEMO'
       AND u.status = 'ACTIVE'
       AND u.id <> 1
     ORDER BY u.id
     LIMIT 1
);

SET @closed_seed_user_3 = (
    SELECT u.id
      FROM tbl_user u
      JOIN tbl_connected_account ca
        ON ca.user_id = u.id
       AND ca.is_active = 1
     WHERE u.social_provider = 'DEMO'
       AND u.status = 'ACTIVE'
       AND u.id NOT IN (1, COALESCE(@closed_seed_user_2, -1))
     ORDER BY u.id
     LIMIT 1
);

SET @closed_seed_user_4 = (
    SELECT u.id
      FROM tbl_user u
      JOIN tbl_connected_account ca
        ON ca.user_id = u.id
       AND ca.is_active = 1
     WHERE u.social_provider = 'DEMO'
       AND u.status = 'ACTIVE'
       AND u.id NOT IN (1, COALESCE(@closed_seed_user_2, -1), COALESCE(@closed_seed_user_3, -1))
     ORDER BY u.id
     LIMIT 1
);

SET @closed_seed_account_1 = (
    SELECT ca.id FROM tbl_connected_account ca
     WHERE ca.user_id = @closed_seed_user_1 AND ca.is_active = 1
     ORDER BY ca.id LIMIT 1
);
SET @closed_seed_account_2 = (
    SELECT ca.id FROM tbl_connected_account ca
     WHERE ca.user_id = @closed_seed_user_2 AND ca.is_active = 1
     ORDER BY ca.id LIMIT 1
);
SET @closed_seed_account_3 = (
    SELECT ca.id FROM tbl_connected_account ca
     WHERE ca.user_id = @closed_seed_user_3 AND ca.is_active = 1
     ORDER BY ca.id LIMIT 1
);
SET @closed_seed_account_4 = (
    SELECT ca.id FROM tbl_connected_account ca
     WHERE ca.user_id = @closed_seed_user_4 AND ca.is_active = 1
     ORDER BY ca.id LIMIT 1
);

SET @closed_seed_category_ready = (
    SELECT COUNT(*) = 2
      FROM tbl_category
     WHERE id IN (14, 21)
);
SET @closed_seed_verdict_schema_ready = (
    SELECT COUNT(*) = 4
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND ((table_name = 'tbl_group_challenge_daily_result'
             AND column_name = 'verdict_deduction_amount')
         OR (table_name = 'tbl_group_challenge_daily_result'
             AND column_name = 'effective_amount')
         OR (table_name = 'tbl_defense'
             AND column_name = 'actual_burden_amount')
         OR (table_name = 'tbl_defense'
             AND column_name = 'deduction_amount'))
);

SET @closed_seed_ready = (
       @closed_seed_user_1 IS NOT NULL
   AND @closed_seed_user_2 IS NOT NULL
   AND @closed_seed_user_3 IS NOT NULL
   AND @closed_seed_user_4 IS NOT NULL
   AND @closed_seed_account_1 IS NOT NULL
   AND @closed_seed_account_2 IS NOT NULL
   AND @closed_seed_account_3 IS NOT NULL
   AND @closed_seed_account_4 IS NOT NULL
   AND @closed_seed_category_ready = 1
   AND @closed_seed_verdict_schema_ready = 1
);

SELECT @closed_seed_ready AS seed_ready,
       @closed_seed_user_1 AS user_1,
       @closed_seed_user_2 AS demo_user_2,
       @closed_seed_user_3 AS demo_user_3,
       @closed_seed_user_4 AS demo_user_4,
       @closed_seed_category_ready AS category_ready,
       @closed_seed_verdict_schema_ready AS verdict_schema_ready;

-- ---------------------------------------------------------------------
-- 1. 초기화 — 이 파일이 만든 그룹을 먼저 지워 연관 재판 데이터를 CASCADE로 정리한다.
-- ---------------------------------------------------------------------
DELETE FROM tbl_challenge_group
 WHERE invite_code IN ('ENDG2', 'ENDG3', 'ENDG4')
   AND @closed_seed_ready = 1;

DELETE FROM tbl_transaction
 WHERE codef_tr_key LIKE 'GCLOSE-2026-%'
   AND @closed_seed_ready = 1;

-- ---------------------------------------------------------------------
-- 2. 종료된 그룹 3개와 참여자
--    max_members=6은 실제 생성 서비스의 고정 정원이다. 실제 참여자는 각각 4/3/2명이다.
-- ---------------------------------------------------------------------
INSERT INTO tbl_challenge_group
  (admin_id, group_name, category_id, limit_amount, eval_type,
   max_members, start_date, end_date, invite_code, status, memo, created_at, updated_at)
SELECT @closed_seed_user_1, '데모그룹01', 21, 10000, 'DAILY',
       6, '2026-06-29', '2026-07-03', 'ENDG2', 'JUDGING',
       '종료 이력 시드: 일일결산·목숨 소진 사례', '2026-06-25 09:00:00', '2026-07-05 12:00:00'
 WHERE @closed_seed_ready = 1
UNION ALL
SELECT @closed_seed_user_1, '데모그룹02', NULL, 50000, 'PERIOD',
       6, '2026-07-07', '2026-07-10', 'ENDG3', 'JUDGING',
       '종료 이력 시드: 기간결산·종료 후 1회 재판 사례', '2026-07-03 09:00:00', '2026-07-12 12:00:00'
 WHERE @closed_seed_ready = 1
UNION ALL
SELECT @closed_seed_user_1, '데모그룹03', 14, 10000, 'DAILY',
       6, '2026-06-30', '2026-07-05', 'ENDG4', 'JUDGING',
       '종료 이력 시드: 일일결산 무죄 감액 사례', '2026-06-26 09:00:00', '2026-07-07 12:00:00'
 WHERE @closed_seed_ready = 1;

SET @closed_seed_group_1 = (SELECT id FROM tbl_challenge_group WHERE invite_code = 'ENDG2');
SET @closed_seed_group_2 = (SELECT id FROM tbl_challenge_group WHERE invite_code = 'ENDG3');
SET @closed_seed_group_3 = (SELECT id FROM tbl_challenge_group WHERE invite_code = 'ENDG4');

INSERT INTO tbl_group_member
  (group_id, user_id, lives_count, joined_at, updated_at)
SELECT @closed_seed_group_1, @closed_seed_user_1, 5, '2026-06-25 09:00:00', '2026-07-05 12:00:00'
 WHERE @closed_seed_ready = 1
UNION ALL
SELECT @closed_seed_group_1, @closed_seed_user_2, 5, '2026-06-25 09:10:00', '2026-07-05 12:00:00'
 WHERE @closed_seed_ready = 1
UNION ALL
SELECT @closed_seed_group_1, @closed_seed_user_3, 5, '2026-06-25 09:20:00', '2026-07-05 12:00:00'
 WHERE @closed_seed_ready = 1
UNION ALL
SELECT @closed_seed_group_1, @closed_seed_user_4, 5, '2026-06-25 09:30:00', '2026-07-05 12:00:00'
 WHERE @closed_seed_ready = 1
UNION ALL
SELECT @closed_seed_group_2, @closed_seed_user_1, 1, '2026-07-03 09:00:00', '2026-07-12 12:00:00'
 WHERE @closed_seed_ready = 1
UNION ALL
SELECT @closed_seed_group_2, @closed_seed_user_2, 1, '2026-07-03 09:10:00', '2026-07-12 12:00:00'
 WHERE @closed_seed_ready = 1
UNION ALL
SELECT @closed_seed_group_2, @closed_seed_user_3, 1, '2026-07-03 09:20:00', '2026-07-12 12:00:00'
 WHERE @closed_seed_ready = 1
UNION ALL
SELECT @closed_seed_group_3, @closed_seed_user_1, 6, '2026-06-26 09:00:00', '2026-07-07 12:00:00'
 WHERE @closed_seed_ready = 1
UNION ALL
SELECT @closed_seed_group_3, @closed_seed_user_2, 6, '2026-06-26 09:10:00', '2026-07-07 12:00:00'
 WHERE @closed_seed_ready = 1;

-- ---------------------------------------------------------------------
-- 3. 재판의 근거가 되는 거래
--    일별 결과는 아래 거래와 기존 거래 전체를 같은 조건으로 집계해 생성한다.
-- ---------------------------------------------------------------------
INSERT INTO tbl_transaction
  (user_id, account_id, codef_tr_key, merchant_name, merchant_name_normalized,
   amount, direction, tr_date, tr_time, classification, category_id,
   is_refund, is_excluded_from_summary, created_at, updated_at)
SELECT @closed_seed_user_1, @closed_seed_account_1, 'GCLOSE-2026-G01-U1-0629', '택시 이동', '택시 이동',
       12000, 'OUT', '2026-06-29', '12:00:00', 'CONSUMPTION', 21, 0, 0, '2026-06-29 12:00:00', '2026-06-29 12:00:00'
 WHERE @closed_seed_ready = 1
UNION ALL SELECT @closed_seed_user_1, @closed_seed_account_1, 'GCLOSE-2026-G01-U1-0630', '택시 이동', '택시 이동',
       13000, 'OUT', '2026-06-30', '12:00:00', 'CONSUMPTION', 21, 0, 0, '2026-06-30 12:00:00', '2026-06-30 12:00:00' WHERE @closed_seed_ready = 1
UNION ALL SELECT @closed_seed_user_1, @closed_seed_account_1, 'GCLOSE-2026-G01-U1-0701', '택시 이동', '택시 이동',
       14000, 'OUT', '2026-07-01', '12:00:00', 'CONSUMPTION', 21, 0, 0, '2026-07-01 12:00:00', '2026-07-01 12:00:00' WHERE @closed_seed_ready = 1
UNION ALL SELECT @closed_seed_user_1, @closed_seed_account_1, 'GCLOSE-2026-G01-U1-0702', '택시 이동', '택시 이동',
       15000, 'OUT', '2026-07-02', '12:00:00', 'CONSUMPTION', 21, 0, 0, '2026-07-02 12:00:00', '2026-07-02 12:00:00' WHERE @closed_seed_ready = 1
UNION ALL SELECT @closed_seed_user_1, @closed_seed_account_1, 'GCLOSE-2026-G01-U1-0703', '택시 이동', '택시 이동',
       16000, 'OUT', '2026-07-03', '12:00:00', 'CONSUMPTION', 21, 0, 0, '2026-07-03 12:00:00', '2026-07-03 12:00:00' WHERE @closed_seed_ready = 1
UNION ALL SELECT @closed_seed_user_2, @closed_seed_account_2, 'GCLOSE-2026-G01-U2-0630', '택시 이동', '택시 이동',
       13500, 'OUT', '2026-06-30', '13:00:00', 'CONSUMPTION', 21, 0, 0, '2026-06-30 13:00:00', '2026-06-30 13:00:00' WHERE @closed_seed_ready = 1
UNION ALL SELECT @closed_seed_user_1, @closed_seed_account_1, 'GCLOSE-2026-G03-U1-0630', '배달 주문', '배달 주문',
       12000, 'OUT', '2026-06-30', '18:00:00', 'CONSUMPTION', 14, 0, 0, '2026-06-30 18:00:00', '2026-06-30 18:00:00' WHERE @closed_seed_ready = 1
UNION ALL SELECT @closed_seed_user_2, @closed_seed_account_2, 'GCLOSE-2026-G03-U2-0702', '배달 주문', '배달 주문',
       13000, 'OUT', '2026-07-02', '18:00:00', 'CONSUMPTION', 14, 0, 0, '2026-07-02 18:00:00', '2026-07-02 18:00:00' WHERE @closed_seed_ready = 1
UNION ALL SELECT @closed_seed_user_1, @closed_seed_account_1, 'GCLOSE-2026-G02-U1-0707', '생활 소비', '생활 소비',
       10000, 'OUT', '2026-07-07', '12:00:00', 'CONSUMPTION', 15, 0, 0, '2026-07-07 12:00:00', '2026-07-07 12:00:00' WHERE @closed_seed_ready = 1
UNION ALL SELECT @closed_seed_user_1, @closed_seed_account_1, 'GCLOSE-2026-G02-U1-0708', '생활 소비', '생활 소비',
       10000, 'OUT', '2026-07-08', '12:00:00', 'CONSUMPTION', 15, 0, 0, '2026-07-08 12:00:00', '2026-07-08 12:00:00' WHERE @closed_seed_ready = 1
UNION ALL SELECT @closed_seed_user_1, @closed_seed_account_1, 'GCLOSE-2026-G02-U1-0709', '생활 소비', '생활 소비',
       10000, 'OUT', '2026-07-09', '12:00:00', 'CONSUMPTION', 15, 0, 0, '2026-07-09 12:00:00', '2026-07-09 12:00:00' WHERE @closed_seed_ready = 1
UNION ALL SELECT @closed_seed_user_1, @closed_seed_account_1, 'GCLOSE-2026-G02-U1-0710', '생활 소비', '생활 소비',
       10000, 'OUT', '2026-07-10', '12:00:00', 'CONSUMPTION', 15, 0, 0, '2026-07-10 12:00:00', '2026-07-10 12:00:00' WHERE @closed_seed_ready = 1
UNION ALL SELECT @closed_seed_user_2, @closed_seed_account_2, 'GCLOSE-2026-G02-U2-0710', '여행 소비', '여행 소비',
       65000, 'OUT', '2026-07-10', '13:00:00', 'CONSUMPTION', 17, 0, 0, '2026-07-10 13:00:00', '2026-07-10 13:00:00' WHERE @closed_seed_ready = 1
UNION ALL SELECT @closed_seed_user_3, @closed_seed_account_3, 'GCLOSE-2026-G02-U3-0710', '여행 소비', '여행 소비',
       60000, 'OUT', '2026-07-10', '14:00:00', 'CONSUMPTION', 17, 0, 0, '2026-07-10 14:00:00', '2026-07-10 14:00:00' WHERE @closed_seed_ready = 1;

-- ---------------------------------------------------------------------
-- 4. 거래 원장을 기준으로 일별 소비 결과를 생성한다.
--    환불·집계 제외·비소비 거래는 실제 집계와 같은 조건으로 제외한다.
-- ---------------------------------------------------------------------
INSERT INTO tbl_group_challenge_daily_result
  (group_id, user_id, challenge_date, daily_amount, target_count, verdict_deduction_amount, created_at, updated_at)
WITH RECURSIVE challenge_calendar (group_id, user_id, challenge_date) AS (
    SELECT m.group_id, m.user_id, g.start_date
      FROM tbl_group_member m
      JOIN tbl_challenge_group g ON g.id = m.group_id
     WHERE g.id IN (@closed_seed_group_1, @closed_seed_group_2, @closed_seed_group_3)
       AND @closed_seed_ready = 1
    UNION ALL
    SELECT cc.group_id, cc.user_id, DATE_ADD(cc.challenge_date, INTERVAL 1 DAY)
      FROM challenge_calendar cc
      JOIN tbl_challenge_group g ON g.id = cc.group_id
     WHERE cc.challenge_date < g.end_date
)
SELECT cc.group_id,
       cc.user_id,
       cc.challenge_date,
       COALESCE(SUM(t.amount), 0),
       COUNT(t.id),
       0,
       DATE_ADD(cc.challenge_date, INTERVAL 1 DAY),
       DATE_ADD(cc.challenge_date, INTERVAL 1 DAY)
  FROM challenge_calendar cc
  JOIN tbl_challenge_group g ON g.id = cc.group_id
  LEFT JOIN tbl_transaction t
    ON t.user_id = cc.user_id
   AND t.tr_date = cc.challenge_date
   AND t.classification = 'CONSUMPTION'
   AND t.direction = 'OUT'
   AND t.is_refund = 0
   AND t.is_excluded_from_summary = 0
   AND (g.category_id IS NULL OR t.category_id = g.category_id)
 GROUP BY cc.group_id, cc.user_id, cc.challenge_date
ON DUPLICATE KEY UPDATE
    daily_amount = VALUES(daily_amount),
    target_count = VALUES(target_count),
    verdict_deduction_amount = 0,
    updated_at = VALUES(updated_at);

-- ---------------------------------------------------------------------
-- 5. 기소 후보
--    DAILY는 초과한 날짜별 1건, PERIOD는 종료일 결과를 연결점으로 참여자별 1건만 만든다.
-- ---------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_closed_seed_trial;
CREATE TEMPORARY TABLE tmp_closed_seed_trial (
    result_id       BIGINT       NOT NULL PRIMARY KEY,
    group_id        BIGINT       NOT NULL,
    user_id         BIGINT       NOT NULL,
    challenge_date  DATE         NOT NULL,
    trial_scope     VARCHAR(10)  NOT NULL,
    basis_amount    DECIMAL(15,2) NOT NULL,
    verdict         VARCHAR(10)  NOT NULL
);

INSERT INTO tmp_closed_seed_trial
  (result_id, group_id, user_id, challenge_date, trial_scope, basis_amount, verdict)
SELECT r.id,
       r.group_id,
       r.user_id,
       r.challenge_date,
       'DAILY',
       r.daily_amount,
       CASE
           -- 데모그룹01의 user_id=1은 5일 모두 유죄: 목숨 5개를 전부 잃는다.
           WHEN r.group_id = @closed_seed_group_1 AND r.user_id = @closed_seed_user_1 THEN 'GUILTY'
           -- 데모그룹01의 두 번째 참여자는 무죄 감액 사례를 하나 남긴다.
           WHEN r.group_id = @closed_seed_group_1 AND r.user_id = @closed_seed_user_2
                AND r.challenge_date = '2026-06-30' THEN 'INNOCENT'
           -- 데모그룹03의 user_id=1은 무죄 판결을 받는다.
           WHEN r.group_id = @closed_seed_group_3 AND r.user_id = @closed_seed_user_1 THEN 'INNOCENT'
           ELSE 'GUILTY'
       END
  FROM tbl_group_challenge_daily_result r
  JOIN tbl_challenge_group g ON g.id = r.group_id
 WHERE g.id IN (@closed_seed_group_1, @closed_seed_group_3)
   AND g.eval_type = 'DAILY'
   AND r.daily_amount > g.limit_amount
   AND @closed_seed_ready = 1;

INSERT INTO tmp_closed_seed_trial
  (result_id, group_id, user_id, challenge_date, trial_scope, basis_amount, verdict)
SELECT r.id,
       r.group_id,
       r.user_id,
       r.challenge_date,
       'PERIOD',
       SUM(all_days.daily_amount),
       CASE
           -- 기간결산의 user_2는 유죄가 확정돼 1개뿐인 목숨을 잃고 탈락한다.
           WHEN r.user_id = @closed_seed_user_2 THEN 'GUILTY'
           ELSE 'INNOCENT'
       END
  FROM tbl_group_challenge_daily_result r
  JOIN tbl_challenge_group g ON g.id = r.group_id
  JOIN tbl_group_challenge_daily_result all_days
    ON all_days.group_id = r.group_id
   AND all_days.user_id = r.user_id
 WHERE g.id = @closed_seed_group_2
   AND g.eval_type = 'PERIOD'
   AND r.challenge_date = g.end_date
   AND @closed_seed_ready = 1
 GROUP BY r.id, r.group_id, r.user_id, r.challenge_date
HAVING SUM(all_days.daily_amount) > MAX(g.limit_amount);

INSERT INTO tbl_indictment
  (result_id, group_id, user_id, status, message, result, verdict_method, created_at, updated_at)
SELECT t.result_id,
       t.group_id,
       t.user_id,
       t.verdict,
       CASE t.trial_scope
           WHEN 'DAILY' THEN CONCAT('일일 소비 ', FORMAT(t.basis_amount, 0), '원이 제한 금액을 초과했습니다.')
           ELSE CONCAT('기간 누적 소비 ', FORMAT(t.basis_amount, 0), '원이 제한 금액을 초과했습니다.')
       END,
       (t.verdict = 'GUILTY'),
       'VOTE',
       CASE t.trial_scope
           WHEN 'DAILY' THEN TIMESTAMP(t.challenge_date, '18:00:00')
           ELSE TIMESTAMP(DATE_ADD(t.challenge_date, INTERVAL 1 DAY), '10:00:00')
       END,
       CASE t.trial_scope
           WHEN 'DAILY' THEN TIMESTAMP(DATE_ADD(t.challenge_date, INTERVAL 2 DAY), '00:30:00')
           ELSE TIMESTAMP(DATE_ADD(t.challenge_date, INTERVAL 2 DAY), '16:00:00')
       END
  FROM tmp_closed_seed_trial t;

-- 무죄 변론은 실제 부담액과 인정 감액을 함께 남긴다.
-- PERIOD는 기간 초과분을 종료일 소비에서 먼저 감액하며, 감액이 당일 소비액을 넘지 않게 막는다.
INSERT INTO tbl_defense
  (indictment_id, content, actual_burden_amount, deduction_amount, created_at, updated_at)
SELECT i.id,
       '실제 부담한 금액을 반영해 달라고 요청합니다.',
       r.daily_amount - CASE t.trial_scope
           WHEN 'DAILY' THEN GREATEST(r.daily_amount - g.limit_amount, 0)
           ELSE LEAST(r.daily_amount, GREATEST(t.basis_amount - g.limit_amount, 0))
       END,
       CASE t.trial_scope
           WHEN 'DAILY' THEN GREATEST(r.daily_amount - g.limit_amount, 0)
           ELSE LEAST(r.daily_amount, GREATEST(t.basis_amount - g.limit_amount, 0))
       END,
       i.created_at,
       i.updated_at
  FROM tbl_indictment i
  JOIN tmp_closed_seed_trial t ON t.result_id = i.result_id
  JOIN tbl_group_challenge_daily_result r ON r.id = i.result_id
  JOIN tbl_challenge_group g ON g.id = i.group_id
 WHERE i.status = 'INNOCENT';

-- 피고인을 제외한 같은 그룹 참여자가 모두 같은 결론으로 투표한다.
INSERT INTO tbl_vote
  (group_id, user_id, indictment_id, verdict, comment, created_at, updated_at)
SELECT i.group_id,
       voter.user_id,
       i.id,
       i.status,
       CASE i.status WHEN 'GUILTY' THEN '기준 초과가 확인됐어요.' ELSE '사정이 인정됐어요.' END,
       DATE_ADD(i.created_at, INTERVAL 8 HOUR),
       DATE_ADD(i.created_at, INTERVAL 8 HOUR)
  FROM tbl_indictment i
  JOIN tmp_closed_seed_trial t ON t.result_id = i.result_id
  JOIN tbl_group_member voter
    ON voter.group_id = i.group_id
   AND voter.user_id <> i.user_id;

-- 무죄 감액은 판결 전용 컬럼에만 적재한다. daily_amount는 거래 집계 원본으로 보존한다.
UPDATE tbl_group_challenge_daily_result r
JOIN tbl_indictment i ON i.result_id = r.id
JOIN tbl_defense d ON d.indictment_id = i.id
JOIN tmp_closed_seed_trial t ON t.result_id = i.result_id
   SET r.verdict_deduction_amount = d.deduction_amount,
       r.updated_at = i.updated_at
 WHERE i.status = 'INNOCENT';

-- ---------------------------------------------------------------------
-- 6. 최종 목숨·부담금·순위·상태 확정
-- ---------------------------------------------------------------------
UPDATE tbl_group_member m
JOIN tbl_challenge_group g ON g.id = m.group_id
LEFT JOIN (
    SELECT i.group_id, i.user_id, COUNT(*) AS guilty_count
      FROM tbl_indictment i
      JOIN tmp_closed_seed_trial t ON t.result_id = i.result_id
     WHERE i.status = 'GUILTY'
     GROUP BY i.group_id, i.user_id
) guilty ON guilty.group_id = m.group_id AND guilty.user_id = m.user_id
   SET m.lives_count = CASE g.eval_type
       WHEN 'DAILY' THEN GREATEST(DATEDIFF(g.end_date, g.start_date) + 1 - COALESCE(guilty.guilty_count, 0), 0)
       ELSE CASE WHEN COALESCE(guilty.guilty_count, 0) > 0 THEN 0 ELSE 1 END
   END
 WHERE g.id IN (@closed_seed_group_1, @closed_seed_group_2, @closed_seed_group_3);

DROP TEMPORARY TABLE IF EXISTS tmp_closed_seed_final;
CREATE TEMPORARY TABLE tmp_closed_seed_final (
    group_id             BIGINT        NOT NULL,
    user_id              BIGINT        NOT NULL,
    final_charge_amount  DECIMAL(15,2) NOT NULL,
    final_outcome        VARCHAR(20)   NOT NULL,
    final_rank           INT           NOT NULL,
    PRIMARY KEY (group_id, user_id)
);

INSERT INTO tmp_closed_seed_final
  (group_id, user_id, final_charge_amount, final_outcome, final_rank)
SELECT x.group_id,
       x.user_id,
       x.final_charge_amount,
       x.final_outcome,
       CASE
           -- 탈락자는 스키마 정책대로 공동 최하위다.
           WHEN x.final_outcome = 'ELIMINATED' THEN x.member_count
           ELSE RANK() OVER (
               PARTITION BY x.group_id
               ORDER BY CASE WHEN x.final_outcome = 'SURVIVED'
                             THEN x.final_charge_amount ELSE 9999999999999 END,
                        x.user_id
           )
       END
  FROM (
      SELECT m.group_id,
             m.user_id,
             COALESCE(SUM(r.effective_amount), 0) AS final_charge_amount,
             CASE WHEN m.lives_count = 0 THEN 'ELIMINATED' ELSE 'SURVIVED' END AS final_outcome,
             COUNT(*) OVER (PARTITION BY m.group_id) AS member_count
        FROM tbl_group_member m
        JOIN tbl_group_challenge_daily_result r
          ON r.group_id = m.group_id
         AND r.user_id = m.user_id
       WHERE m.group_id IN (@closed_seed_group_1, @closed_seed_group_2, @closed_seed_group_3)
       GROUP BY m.group_id, m.user_id, m.lives_count
  ) x;

UPDATE tbl_group_member m
JOIN tmp_closed_seed_final f
  ON f.group_id = m.group_id
 AND f.user_id = m.user_id
JOIN tbl_challenge_group g ON g.id = m.group_id
   SET m.final_charge_amount = f.final_charge_amount,
       m.final_outcome = f.final_outcome,
       m.final_rank = f.final_rank,
       m.updated_at = CASE g.invite_code
           WHEN 'ENDG2' THEN '2026-07-05 12:00:00'
           WHEN 'ENDG3' THEN '2026-07-12 12:00:00'
           ELSE '2026-07-07 12:00:00'
       END;

UPDATE tbl_challenge_group
   SET status = 'CLOSED',
       updated_at = CASE invite_code
           WHEN 'ENDG2' THEN '2026-07-05 12:00:00'
           WHEN 'ENDG3' THEN '2026-07-12 12:00:00'
           ELSE '2026-07-07 12:00:00'
       END
 WHERE id IN (@closed_seed_group_1, @closed_seed_group_2, @closed_seed_group_3);

DROP TEMPORARY TABLE IF EXISTS tmp_closed_seed_final;
DROP TEMPORARY TABLE IF EXISTS tmp_closed_seed_trial;

-- ---------------------------------------------------------------------
-- 7. 검증 — 결과가 없으면 모두 0행이 정상이다.
-- ---------------------------------------------------------------------

-- 7-1. 일별 결과와 실제 거래 집계가 일치하는지 확인한다.
SELECT r.group_id,
       r.user_id,
       r.challenge_date,
       r.daily_amount AS stored_daily_amount,
       COALESCE(SUM(t.amount), 0) AS calculated_daily_amount,
       r.target_count AS stored_target_count,
       COUNT(t.id) AS calculated_target_count
  FROM tbl_group_challenge_daily_result r
  JOIN tbl_challenge_group g ON g.id = r.group_id
  LEFT JOIN tbl_transaction t
    ON t.user_id = r.user_id
   AND t.tr_date = r.challenge_date
   AND t.classification = 'CONSUMPTION'
   AND t.direction = 'OUT'
   AND t.is_refund = 0
   AND t.is_excluded_from_summary = 0
   AND (g.category_id IS NULL OR t.category_id = g.category_id)
 WHERE g.invite_code IN ('ENDG2', 'ENDG3', 'ENDG4')
 GROUP BY r.id, r.group_id, r.user_id, r.challenge_date, r.daily_amount, r.target_count
HAVING r.daily_amount <> COALESCE(SUM(t.amount), 0)
    OR r.target_count <> COUNT(t.id);

-- 7-2. DAILY는 같은 참여자·같은 날짜에 기소가 2건 이상이면 안 된다.
SELECT i.group_id, i.user_id, r.challenge_date, COUNT(*) AS indictment_count
  FROM tbl_indictment i
  JOIN tbl_group_challenge_daily_result r ON r.id = i.result_id
  JOIN tbl_challenge_group g ON g.id = i.group_id
 WHERE g.invite_code IN ('ENDG2', 'ENDG4')
 GROUP BY i.group_id, i.user_id, r.challenge_date
HAVING COUNT(*) > 1;

-- 7-3. PERIOD는 참여자별 기소가 1건을 넘으면 안 된다.
SELECT i.group_id, i.user_id, COUNT(*) AS indictment_count
  FROM tbl_indictment i
  JOIN tbl_challenge_group g ON g.id = i.group_id
 WHERE g.invite_code = 'ENDG3'
 GROUP BY i.group_id, i.user_id
HAVING COUNT(*) > 1;

-- 7-4. 최종 부담금·목숨·결과가 판결 및 일별 결과와 일치하는지 확인한다.
SELECT m.group_id,
       m.user_id,
       m.lives_count AS stored_lives,
       CASE g.eval_type
           WHEN 'DAILY' THEN GREATEST(DATEDIFF(g.end_date, g.start_date) + 1 - COALESCE(SUM(i.status = 'GUILTY'), 0), 0)
           ELSE CASE WHEN COALESCE(SUM(i.status = 'GUILTY'), 0) > 0 THEN 0 ELSE 1 END
       END AS calculated_lives,
       m.final_charge_amount AS stored_final_charge_amount,
       COALESCE(SUM(r.effective_amount), 0) AS calculated_final_charge_amount,
       m.final_outcome
  FROM tbl_group_member m
  JOIN tbl_challenge_group g ON g.id = m.group_id
  JOIN tbl_group_challenge_daily_result r
    ON r.group_id = m.group_id
   AND r.user_id = m.user_id
  LEFT JOIN tbl_indictment i ON i.result_id = r.id
 WHERE g.invite_code IN ('ENDG2', 'ENDG3', 'ENDG4')
 GROUP BY m.group_id, m.user_id, m.lives_count, m.final_charge_amount, m.final_outcome, g.eval_type, g.start_date, g.end_date
HAVING m.lives_count <> CASE g.eval_type
           WHEN 'DAILY' THEN GREATEST(DATEDIFF(g.end_date, g.start_date) + 1 - COALESCE(SUM(i.status = 'GUILTY'), 0), 0)
           ELSE CASE WHEN COALESCE(SUM(i.status = 'GUILTY'), 0) > 0 THEN 0 ELSE 1 END
       END
    OR m.final_charge_amount <> COALESCE(SUM(r.effective_amount), 0)
    OR m.final_outcome <> CASE WHEN m.lives_count = 0 THEN 'ELIMINATED' ELSE 'SURVIVED' END;

-- 7-5. 최종 확인용 요약.
SELECT g.group_name,
       g.status,
       g.eval_type,
       g.start_date,
       g.end_date,
       COUNT(DISTINCT m.user_id) AS member_count,
       MAX(m.user_id = 1) AS contains_user_1,
       COUNT(DISTINCT CASE WHEN m.final_outcome = 'ELIMINATED' THEN m.user_id END) AS eliminated_count,
       COUNT(DISTINCT CASE WHEN i.status = 'GUILTY' THEN i.id END) AS guilty_count,
       COUNT(DISTINCT CASE WHEN i.status = 'INNOCENT' THEN i.id END) AS innocent_count
  FROM tbl_challenge_group g
  JOIN tbl_group_member m ON m.group_id = g.id
  LEFT JOIN tbl_indictment i ON i.group_id = g.id
 WHERE g.invite_code IN ('ENDG2', 'ENDG3', 'ENDG4')
 GROUP BY g.id, g.group_name, g.status, g.eval_type, g.start_date, g.end_date
 ORDER BY g.start_date, g.id;
