-- =====================================================================
-- db/seed_transaction_mock_local.sql
-- 월간 소비 리포트·거래내역 API 개발용 로컬 거래 목데이터
--
-- 선행 조건
--   1. db/seed_category.sql 적용
--   2. tbl_user.id = 1 존재
--   3. 지정한 로컬 테스트 계좌가 활성 상태로 존재
--
-- 이 파일은 MOCK-LOCAL-U1- 접두어 거래만 초기화한 뒤 같은 결과를 재생성한다.
-- 실제 거래와 tbl_fixed_expense_candidate는 변경하지 않는다.
-- =====================================================================

USE tangtang;
SET NAMES utf8mb4;

SET @target_user_id = 1;
SET @mock_account_key = 'MOCK-SEED-LOCAL-USER-1-KB-001';

SET @target_user_exists = (
  SELECT COUNT(*)
    FROM tbl_user
   WHERE id = @target_user_id
);

SET @target_account_id = (
  SELECT id
    FROM tbl_connected_account
   WHERE user_id = @target_user_id
     AND account_no_encrypted = @mock_account_key
     AND is_active = 1
   LIMIT 1
);

SET @required_category_count = (
  SELECT COUNT(*)
    FROM tbl_category
   WHERE category_name IN (
     '음식점/외식', '배달앱', '카페/간식', '편의점', '장보기/마트',
     '생활용품', '대중교통', '택시/모빌리티', '온라인쇼핑', '패션',
     '뷰티', '영화·공연·전시', '도서', '취미', '병원', '약국',
     '운동시설', '수수료', '통신비', 'OTT', '소프트웨어·클라우드'
   )
);

SET @seed_ready = (
  @target_user_exists = 1
  AND @target_account_id IS NOT NULL
  AND @required_category_count = 21
);

START TRANSACTION;

-- ---------------------------------------------------------------------
-- 1. 주요 정규화 가맹점의 공용 카테고리 매핑
-- ---------------------------------------------------------------------
INSERT INTO tbl_merchant_category_map (
  merchant_name_normalized,
  category_id,
  source,
  created_at,
  updated_at
)
SELECT merchant_seed.merchant_name_normalized,
       category.id,
       'KEYWORD',
       '2026-08-11 00:00:00',
       '2026-08-11 00:00:00'
  FROM (
    SELECT '두잇' AS merchant_name_normalized, '배달앱' AS category_name
    UNION ALL SELECT '배달의민족', '배달앱'
    UNION ALL SELECT '쿠팡이츠', '배달앱'
    UNION ALL SELECT 'SK텔레콤', '통신비'
    UNION ALL SELECT '유튜브프리미엄', 'OTT'
    UNION ALL SELECT '티빙', 'OTT'
    UNION ALL SELECT '캘린더구독', '소프트웨어·클라우드'
    UNION ALL SELECT '스타벅스', '카페/간식'
    UNION ALL SELECT '메가커피', '카페/간식'
    UNION ALL SELECT '서울교통공사', '대중교통'
    UNION ALL SELECT 'CU', '편의점'
    UNION ALL SELECT 'GS25', '편의점'
    UNION ALL SELECT '이마트', '장보기/마트'
    UNION ALL SELECT '홈플러스', '장보기/마트'
    UNION ALL SELECT '쿠팡', '온라인쇼핑'
    UNION ALL SELECT '무신사', '패션'
    UNION ALL SELECT '올리브영', '뷰티'
    UNION ALL SELECT 'CGV', '영화·공연·전시'
    UNION ALL SELECT '교보문고', '도서'
    UNION ALL SELECT '카카오택시', '택시/모빌리티'
    UNION ALL SELECT '동네음식점', '음식점/외식'
    UNION ALL SELECT '동네병원', '병원'
    UNION ALL SELECT '동네약국', '약국'
    UNION ALL SELECT '다이소', '생활용품'
    UNION ALL SELECT '취미공방', '취미'
    UNION ALL SELECT '동네헬스장', '운동시설'
    UNION ALL SELECT 'KB수수료', '수수료'
  ) AS merchant_seed
  JOIN tbl_category AS category
    ON category.category_name = merchant_seed.category_name
 WHERE @seed_ready = 1
ON DUPLICATE KEY UPDATE
  category_id = VALUES(category_id),
  source = VALUES(source),
  updated_at = VALUES(updated_at);

-- ---------------------------------------------------------------------
-- 2. 이전 실행에서 만든 목거래만 환불 → 원거래 순서로 초기화
-- ---------------------------------------------------------------------
DELETE FROM tbl_transaction
 WHERE user_id = @target_user_id
   AND codef_tr_key LIKE 'MOCK-LOCAL-U1-%'
   AND is_refund = 1
   AND @seed_ready = 1;

DELETE FROM tbl_transaction
 WHERE user_id = @target_user_id
   AND codef_tr_key LIKE 'MOCK-LOCAL-U1-%'
   AND @seed_ready = 1;

-- ---------------------------------------------------------------------
-- 3. 2026-01-01~2026-08-11 원거래
--    월별 최종 목표: 82, 76, 91, 85, 96, 79, 94, 28건
--    아래에서는 환불 행 수를 제외한 613건을 먼저 생성한다.
-- ---------------------------------------------------------------------
INSERT INTO tbl_transaction (
  user_id,
  account_id,
  codef_tr_key,
  merchant_name,
  merchant_name_normalized,
  amount,
  direction,
  tr_date,
  tr_time,
  description1,
  description2,
  cancel_yn,
  is_refund,
  original_transaction_id,
  refunded_amount,
  classification,
  category_id,
  is_excluded_from_summary,
  created_at,
  updated_at
)
WITH RECURSIVE
month_plan AS (
  SELECT 1 AS month_num, '202601' AS month_code, DATE('2026-01-01') AS month_start,
         82 AS total_count, 2 AS refund_count, 31 AS day_span, 100 AS spend_pct, 10 AS transfer_end
  UNION ALL SELECT 2, '202602', DATE('2026-02-01'), 76, 2, 28, 88, 10
  UNION ALL SELECT 3, '202603', DATE('2026-03-01'), 91, 3, 31, 112, 10
  UNION ALL SELECT 4, '202604', DATE('2026-04-01'), 85, 2, 30, 94, 10
  UNION ALL SELECT 5, '202605', DATE('2026-05-01'), 96, 3, 31, 124, 10
  UNION ALL SELECT 6, '202606', DATE('2026-06-01'), 79, 2, 30, 82, 10
  UNION ALL SELECT 7, '202607', DATE('2026-07-01'), 94, 3, 31, 118, 10
  UNION ALL SELECT 8, '202608', DATE('2026-08-01'), 28, 1, 11, 102, 9
),
seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 96
),
typed_rows AS (
  SELECT plan.*,
         seq.n,
         CASE
           WHEN seq.n = 1 AND plan.month_num <= 7 THEN 'MOBILE'
           WHEN seq.n = 2 THEN 'YOUTUBE'
           WHEN seq.n = 3 AND plan.month_num BETWEEN 3 AND 7 THEN 'TVING'
           WHEN seq.n = 4 AND plan.month_num >= 4 THEN 'CALENDAR'
           WHEN seq.n = 5 AND (plan.month_num BETWEEN 1 AND 5 OR plan.month_num = 7) THEN 'DOIT'
           WHEN seq.n = 6 AND plan.month_num <= 7 THEN 'INCOME'
           WHEN seq.n BETWEEN 7 AND plan.transfer_end THEN 'TRANSFER'
           ELSE 'CONSUMPTION'
         END AS row_type
    FROM month_plan AS plan
    JOIN seq
      ON seq.n <= plan.total_count - plan.refund_count
),
decorated_rows AS (
  SELECT typed.*,
         CASE typed.row_type
           WHEN 'MOBILE' THEN 'SK텔레콤'
           WHEN 'YOUTUBE' THEN '유튜브프리미엄'
           WHEN 'TVING' THEN '티빙'
           WHEN 'CALENDAR' THEN '캘린더구독'
           WHEN 'DOIT' THEN '두잇'
           WHEN 'INCOME' THEN 'KB급여'
           WHEN 'TRANSFER' THEN '계좌이체'
           ELSE CASE MOD(typed.n, 22)
             WHEN 0 THEN '스타벅스'
             WHEN 1 THEN '메가커피'
             WHEN 2 THEN '서울교통공사'
             WHEN 3 THEN '배달의민족'
             WHEN 4 THEN '쿠팡이츠'
             WHEN 5 THEN 'CU'
             WHEN 6 THEN 'GS25'
             WHEN 7 THEN '이마트'
             WHEN 8 THEN '홈플러스'
             WHEN 9 THEN '쿠팡'
             WHEN 10 THEN '무신사'
             WHEN 11 THEN '올리브영'
             WHEN 12 THEN 'CGV'
             WHEN 13 THEN '교보문고'
             WHEN 14 THEN '카카오택시'
             WHEN 15 THEN '동네음식점'
             WHEN 16 THEN '동네병원'
             WHEN 17 THEN '동네약국'
             WHEN 18 THEN '다이소'
             WHEN 19 THEN '취미공방'
             WHEN 20 THEN '동네헬스장'
             ELSE 'KB수수료'
           END
         END AS merchant,
         CASE typed.row_type
           WHEN 'MOBILE' THEN '통신비'
           WHEN 'YOUTUBE' THEN 'OTT'
           WHEN 'TVING' THEN 'OTT'
           WHEN 'CALENDAR' THEN '소프트웨어·클라우드'
           WHEN 'DOIT' THEN '배달앱'
           WHEN 'INCOME' THEN NULL
           WHEN 'TRANSFER' THEN NULL
           ELSE CASE MOD(typed.n, 22)
             WHEN 0 THEN '카페/간식'
             WHEN 1 THEN '카페/간식'
             WHEN 2 THEN '대중교통'
             WHEN 3 THEN '배달앱'
             WHEN 4 THEN '배달앱'
             WHEN 5 THEN '편의점'
             WHEN 6 THEN '편의점'
             WHEN 7 THEN '장보기/마트'
             WHEN 8 THEN '장보기/마트'
             WHEN 9 THEN '온라인쇼핑'
             WHEN 10 THEN '패션'
             WHEN 11 THEN '뷰티'
             WHEN 12 THEN '영화·공연·전시'
             WHEN 13 THEN '도서'
             WHEN 14 THEN '택시/모빌리티'
             WHEN 15 THEN '음식점/외식'
             WHEN 16 THEN '병원'
             WHEN 17 THEN '약국'
             WHEN 18 THEN '생활용품'
             WHEN 19 THEN '취미'
             WHEN 20 THEN '운동시설'
             ELSE '수수료'
           END
         END AS category_name,
         CASE typed.row_type
           WHEN 'MOBILE' THEN 66750
           WHEN 'YOUTUBE' THEN 14900
           WHEN 'TVING' THEN 13500
           WHEN 'CALENDAR' THEN 3300
           WHEN 'DOIT' THEN 2990
           WHEN 'INCOME' THEN 3250000 + typed.month_num * 25000
           WHEN 'TRANSFER' THEN 40000 + MOD(typed.n * 17000 + typed.month_num * 11000, 260000)
           ELSE ROUND((
             CASE MOD(typed.n, 22)
               WHEN 0 THEN 4500 WHEN 1 THEN 3200 WHEN 2 THEN 1550
               WHEN 3 THEN 18500 WHEN 4 THEN 21000 WHEN 5 THEN 7200
               WHEN 6 THEN 5800 WHEN 7 THEN 48500 WHEN 8 THEN 53600
               WHEN 9 THEN 37900 WHEN 10 THEN 68900 WHEN 11 THEN 27800
               WHEN 12 THEN 15000 WHEN 13 THEN 19800 WHEN 14 THEN 13200
               WHEN 15 THEN 11800 WHEN 16 THEN 32600 WHEN 17 THEN 8400
               WHEN 18 THEN 14600 WHEN 19 THEN 36000 WHEN 20 THEN 55000
               ELSE 1200
             END + MOD(typed.n * 7919 + typed.month_num * 1237, 8900)
           ) * typed.spend_pct / 100, 0)
         END AS row_amount,
         CASE typed.row_type
           WHEN 'MOBILE' THEN 13 + MOD(typed.month_num, 2)
           WHEN 'YOUTUBE' THEN 7
           WHEN 'TVING' THEN 22
           WHEN 'CALENDAR' THEN 3
           WHEN 'DOIT' THEN 18
           WHEN 'INCOME' THEN 24
           ELSE CASE
             WHEN typed.n = 25 THEN 9
             WHEN typed.n = 26 AND typed.refund_count >= 2 THEN 14
             WHEN typed.n = 27 AND typed.refund_count = 3 THEN 19
             ELSE MOD(typed.n * 7 + typed.month_num * 3, typed.day_span)
           END
         END AS day_offset,
         CASE
           WHEN typed.row_type = 'INCOME' THEN TIME('09:00:00')
           WHEN typed.row_type = 'TRANSFER' THEN MAKETIME(10 + MOD(typed.n, 9), MOD(typed.n * 11, 60), 0)
           WHEN MOD(typed.n, 22) = 2 THEN MAKETIME(7 + MOD(typed.n, 3), MOD(typed.n * 13, 60), 0)
           WHEN MOD(typed.n, 22) IN (0, 1) THEN MAKETIME(14 + MOD(typed.n, 4), MOD(typed.n * 17, 60), 0)
           WHEN MOD(typed.n, 22) IN (3, 4) THEN MAKETIME(18 + MOD(typed.n, 4), MOD(typed.n * 19, 60), 0)
           ELSE MAKETIME(11 + MOD(typed.n, 10), MOD(typed.n * 23, 60), 0)
         END AS row_time
    FROM typed_rows AS typed
)
SELECT @target_user_id,
       @target_account_id,
       CONCAT('MOCK-LOCAL-U1-', decorated.month_code, '-', LPAD(decorated.n, 3, '0')),
       decorated.merchant,
       decorated.merchant,
       decorated.row_amount,
       CASE
         WHEN decorated.row_type = 'INCOME' THEN 'IN'
         WHEN decorated.row_type = 'TRANSFER' AND MOD(decorated.n, 2) = 0 THEN 'IN'
         ELSE 'OUT'
       END,
       DATE_ADD(decorated.month_start, INTERVAL decorated.day_offset DAY),
       decorated.row_time,
       CASE decorated.row_type
         WHEN 'MOBILE' THEN '월 정기 휴대폰 요금'
         WHEN 'YOUTUBE' THEN '월 정기 구독'
         WHEN 'TVING' THEN '월 정기 구독'
         WHEN 'CALENDAR' THEN '월 정기 구독'
         WHEN 'DOIT' THEN '배달앱 구독'
         WHEN 'INCOME' THEN '월 급여'
         WHEN 'TRANSFER' THEN '계좌이체'
         ELSE '로컬 리포트 목거래'
       END,
       'issue-124 deterministic seed',
       CASE
         WHEN decorated.n = 27 AND decorated.refund_count = 3 THEN 'Y'
         WHEN decorated.row_type IN ('INCOME', 'TRANSFER') THEN NULL
         ELSE 'N'
       END,
       0,
       NULL,
       NULL,
       CASE decorated.row_type
         WHEN 'INCOME' THEN 'INCOME'
         WHEN 'TRANSFER' THEN 'TRANSFER'
         ELSE 'CONSUMPTION'
       END,
       category.id,
       CASE WHEN decorated.n = 27 AND decorated.refund_count = 3 THEN 1 ELSE 0 END,
       TIMESTAMP(DATE_ADD(decorated.month_start, INTERVAL decorated.day_offset DAY), decorated.row_time),
       TIMESTAMP(DATE_ADD(decorated.month_start, INTERVAL decorated.day_offset DAY), decorated.row_time)
  FROM decorated_rows AS decorated
  LEFT JOIN tbl_category AS category
    ON category.category_name = decorated.category_name
 WHERE @seed_ready = 1;

-- ---------------------------------------------------------------------
-- 4. 환불 18건
--    당일 전액, 며칠 뒤 전액, 부분 환불, 즉시 승인 취소를 섞는다.
-- ---------------------------------------------------------------------
INSERT INTO tbl_transaction (
  user_id,
  account_id,
  codef_tr_key,
  merchant_name,
  merchant_name_normalized,
  amount,
  direction,
  tr_date,
  tr_time,
  description1,
  description2,
  cancel_yn,
  is_refund,
  original_transaction_id,
  refunded_amount,
  classification,
  category_id,
  is_excluded_from_summary,
  created_at,
  updated_at
)
WITH refund_plan AS (
  SELECT '202601' AS month_code, 1 AS refund_seq, 25 AS original_seq, 'FULL_SAME_DAY' AS refund_type, 0 AS day_offset
  UNION ALL SELECT '202601', 2, 26, 'PARTIAL', 3
  UNION ALL SELECT '202602', 1, 25, 'FULL_SAME_DAY', 0
  UNION ALL SELECT '202602', 2, 26, 'FULL_DELAYED', 4
  UNION ALL SELECT '202603', 1, 25, 'FULL_SAME_DAY', 0
  UNION ALL SELECT '202603', 2, 26, 'PARTIAL', 3
  UNION ALL SELECT '202603', 3, 27, 'EXCLUDED_CANCEL', 0
  UNION ALL SELECT '202604', 1, 25, 'FULL_SAME_DAY', 0
  UNION ALL SELECT '202604', 2, 26, 'FULL_DELAYED', 4
  UNION ALL SELECT '202605', 1, 25, 'FULL_SAME_DAY', 0
  UNION ALL SELECT '202605', 2, 26, 'PARTIAL', 3
  UNION ALL SELECT '202605', 3, 27, 'EXCLUDED_CANCEL', 0
  UNION ALL SELECT '202606', 1, 25, 'FULL_SAME_DAY', 0
  UNION ALL SELECT '202606', 2, 26, 'FULL_DELAYED', 4
  UNION ALL SELECT '202607', 1, 25, 'FULL_SAME_DAY', 0
  UNION ALL SELECT '202607', 2, 26, 'PARTIAL', 3
  UNION ALL SELECT '202607', 3, 27, 'EXCLUDED_CANCEL', 0
  UNION ALL SELECT '202608', 1, 25, 'FULL_SAME_DAY', 0
)
SELECT original.user_id,
       original.account_id,
       CONCAT('MOCK-LOCAL-U1-', plan.month_code, '-R', LPAD(plan.refund_seq, 2, '0')),
       original.merchant_name,
       original.merchant_name_normalized,
       CASE
         WHEN plan.refund_type = 'PARTIAL' THEN ROUND(original.amount * 0.40, 0)
         ELSE original.amount
       END,
       'IN',
       DATE_ADD(original.tr_date, INTERVAL plan.day_offset DAY),
       ADDTIME(original.tr_time, '00:10:00'),
       CASE plan.refund_type
         WHEN 'FULL_SAME_DAY' THEN '당일 전액 환불'
         WHEN 'FULL_DELAYED' THEN '결제 후 전액 환불'
         WHEN 'PARTIAL' THEN '부분 환불'
         ELSE '승인 직후 취소'
       END,
       'issue-124 deterministic refund',
       'Y',
       1,
       original.id,
       CASE
         WHEN plan.refund_type = 'PARTIAL' THEN ROUND(original.amount * 0.40, 0)
         ELSE original.amount
       END,
       'CONSUMPTION',
       original.category_id,
       CASE WHEN plan.refund_type = 'EXCLUDED_CANCEL' THEN 1 ELSE 0 END,
       TIMESTAMP(DATE_ADD(original.tr_date, INTERVAL plan.day_offset DAY), ADDTIME(original.tr_time, '00:10:00')),
       TIMESTAMP(DATE_ADD(original.tr_date, INTERVAL plan.day_offset DAY), ADDTIME(original.tr_time, '00:10:00'))
  FROM refund_plan AS plan
  JOIN tbl_transaction AS original
    ON original.codef_tr_key = CONCAT(
      'MOCK-LOCAL-U1-', plan.month_code, '-', LPAD(plan.original_seq, 3, '0')
    )
   AND original.user_id = @target_user_id
 WHERE @seed_ready = 1;

COMMIT;

-- =====================================================================
-- 검증 조회
-- =====================================================================

-- 1. 대상 사용자·연결 계좌 및 선행 조건 확인
SELECT @target_user_id AS target_user_id,
       @target_user_exists AS target_user_exists,
       @mock_account_key AS mock_account_key,
       @target_account_id AS target_account_id,
       @required_category_count AS required_category_count,
       @seed_ready AS seed_ready;

SELECT id, user_id, bank_code, bank_name, account_name, account_type, is_active
  FROM tbl_connected_account
 WHERE id = @target_account_id;

-- 2. 사용자별·월별 전체 거래 건수
SELECT user_id, DATE_FORMAT(tr_date, '%Y-%m') AS transaction_month, COUNT(*) AS transaction_count
  FROM tbl_transaction
 WHERE user_id = @target_user_id
   AND codef_tr_key LIKE 'MOCK-LOCAL-U1-%'
 GROUP BY user_id, DATE_FORMAT(tr_date, '%Y-%m')
 ORDER BY transaction_month;

-- 3. 월별 일반 소비 건수
SELECT DATE_FORMAT(tr_date, '%Y-%m') AS transaction_month, COUNT(*) AS consumption_count
  FROM tbl_transaction
 WHERE user_id = @target_user_id
   AND codef_tr_key LIKE 'MOCK-LOCAL-U1-%'
   AND classification = 'CONSUMPTION'
   AND is_refund = 0
   AND is_excluded_from_summary = 0
 GROUP BY DATE_FORMAT(tr_date, '%Y-%m')
 ORDER BY transaction_month;
