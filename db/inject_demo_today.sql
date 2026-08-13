-- =====================================================================
-- db/inject_demo_today.sql — 시연용 거래를 지정한 날짜까지 앞당겨 채운다 (이슈 #191)
--
-- 왜 필요한가
--   그룹 챌린지는 시작일부터 7일간의 소비를 매일 평가한다. 그런데 시드는 미래 날짜를
--   넣지 않으므로(WHERE tr_date <= CURDATE()), 오늘 시작한 챌린지는 **오늘 하루만**
--   데이터가 있고 2~7일 차는 전부 소비 0원이 된다. 기소·투표·순위를 시연할 수 없다.
--
--   이 파일은 seed_demo_transactions_ver2.sql 의 본 INSERT 를 @seed_upto 만 바꿔 다시 돌린다.
--   codef_tr_key 가 날짜가 아니라 (사용자, 연월, 템플릿) 로 정해지므로 이미 들어간 행은
--   NOT EXISTS 가 걸러내고, **@seed_upto 까지의 미래 행만 추가**된다.
--
-- 선행 조건
--   db/migration/20260813_reseed_demo_transactions_per_user.sql 이 적용돼 있어야 한다.
--   (ver2 계산식으로 만들어진 거래가 이미 있어야 이어붙는 값이 일관된다)
--
-- 사용법
--   ① 아래 @seed_upto 를 원하는 날짜로 바꾼다
--   ② 실행한다
--        mysql -u tangtang -p tangtang --default-character-set=utf8mb4 \
--          -e "source D:/…/db/inject_demo_today.sql"
--   ③ 맨 아래 확인 쿼리로 날짜별 소비액이 사용자마다 다른지 본다
--
--   챌린지 하루씩 진행시키며 시연하려면 날짜를 하루씩 늘려 반복 실행하면 된다.
--
-- 되돌리기
--   미래 행만 지우고 싶을 때
--     DELETE FROM tbl_transaction WHERE codef_tr_key LIKE 'DUMMY-%' AND tr_date > CURDATE();
--   전체를 되돌리려면 migration 파일을 다시 실행한다(삭제 후 재생성이라 멱등하다).
--
-- 한계
--   ver2 가 만들어 두는 범위는 **다음 달 말일까지**다. 그보다 먼 날짜를 넣어도 행이 늘지 않는다.
-- =====================================================================

USE tangtang;
SET NAMES utf8mb4;

-- ★ 여기만 고쳐 쓴다. 이 날짜까지 채운다.
SET @seed_upto = DATE_ADD(CURDATE(), INTERVAL 7 DAY);

-- ---------------------------------------------------------------------
-- 본 거래 주입 — seed_demo_transactions_ver2.sql 과 동일한 계산식
-- 계수의 의미와 "바꾸지 말 것" 경고는 ver2 파일 주석에 적혀 있다.
-- ---------------------------------------------------------------------
INSERT INTO tbl_transaction
  (user_id, account_id, codef_tr_key, merchant_name, merchant_name_normalized,
   amount, direction, tr_date, tr_time, classification, category_id,
   is_refund, is_excluded_from_summary)
WITH RECURSIVE
months (m) AS (
  SELECT -1 UNION ALL SELECT m + 1 FROM months WHERE m < 6
),
tpl (tno, merchant, base_amount, cat_name, dom, cls, is_fixed) AS (
  SELECT  1, '넷플릭스',       17000, 'OTT',            15, 'CONSUMPTION', 1 UNION ALL
  SELECT  2, 'SKT통신요금',    55000, '통신비',          25, 'CONSUMPTION', 1 UNION ALL
  SELECT  3, '유튜브뮤직',     11900, '음원',             7, 'CONSUMPTION', 1 UNION ALL
  SELECT  4, '피트니스클럽',   79000, '운동시설',          5, 'CONSUMPTION', 1 UNION ALL
  SELECT  5, '월세이체',      500000, '월세',            25, 'CONSUMPTION', 1 UNION ALL
  SELECT  6, '배달의민족',      24000, '배달앱',           3, 'CONSUMPTION', 0 UNION ALL
  SELECT  7, '배달의민족',      31000, '배달앱',           9, 'CONSUMPTION', 0 UNION ALL
  SELECT  8, '쿠팡이츠',       19000, '배달앱',          14, 'CONSUMPTION', 0 UNION ALL
  SELECT  9, '배달의민족',      27000, '배달앱',          20, 'CONSUMPTION', 0 UNION ALL
  SELECT 10, '요기요',         22000, '배달앱',          27, 'CONSUMPTION', 0 UNION ALL
  SELECT 11, '김밥천국',        9500, '음식점/외식',       2, 'CONSUMPTION', 0 UNION ALL
  SELECT 12, '고기집',         46000, '음식점/외식',      11, 'CONSUMPTION', 0 UNION ALL
  SELECT 13, '백반집',         11000, '음식점/외식',      18, 'CONSUMPTION', 0 UNION ALL
  SELECT 14, '파스타집',       28000, '음식점/외식',      24, 'CONSUMPTION', 0 UNION ALL
  SELECT 15, '스타벅스',        5800, '카페/간식',         4, 'CONSUMPTION', 0 UNION ALL
  SELECT 16, '스타벅스',        6300, '카페/간식',         8, 'CONSUMPTION', 0 UNION ALL
  SELECT 17, '투썸플레이스',    7200, '카페/간식',        13, 'CONSUMPTION', 0 UNION ALL
  SELECT 18, '메가커피',        2500, '카페/간식',        17, 'CONSUMPTION', 0 UNION ALL
  SELECT 19, '스타벅스',        5800, '카페/간식',        22, 'CONSUMPTION', 0 UNION ALL
  SELECT 20, 'GS25',           8400, '편의점',            6, 'CONSUMPTION', 0 UNION ALL
  SELECT 21, 'CU',             5200, '편의점',           16, 'CONSUMPTION', 0 UNION ALL
  SELECT 22, '세븐일레븐',      6800, '편의점',           26, 'CONSUMPTION', 0 UNION ALL
  SELECT 23, '쿠팡',           38000, '온라인쇼핑',       10, 'CONSUMPTION', 0 UNION ALL
  SELECT 24, '무신사',         64000, '패션',            19, 'CONSUMPTION', 0 UNION ALL
  SELECT 25, '올리브영',       32000, '뷰티',            23, 'CONSUMPTION', 0 UNION ALL
  SELECT 26, '티머니충전',      40000, '대중교통',          1, 'CONSUMPTION', 0 UNION ALL
  SELECT 27, '카카오T',        12800, '택시/모빌리티',     12, 'CONSUMPTION', 0 UNION ALL
  SELECT 28, '카카오T',        18400, '택시/모빌리티',     28, 'CONSUMPTION', 0 UNION ALL
  SELECT 29, '이마트',         87000, '장보기/마트',      21, 'CONSUMPTION', 0 UNION ALL
  SELECT 30, '다이소',         14000, '생활용품',         15, 'CONSUMPTION', 0 UNION ALL
  SELECT 31, '내과의원',       15000, '병원',             9, 'CONSUMPTION', 0 UNION ALL
  SELECT 32, 'CGV',           15000, '영화·공연·전시',    6, 'CONSUMPTION', 0 UNION ALL
  SELECT 33, '급여입금',     2800000, NULL,              25, 'INCOME',      1 UNION ALL
  SELECT 34, '내계좌이체',    300000, NULL,              26, 'TRANSFER',    1
),
tgt (user_id, account_id) AS (
  SELECT u.id,
         (SELECT ca.id FROM tbl_connected_account ca
           WHERE ca.user_id = u.id AND ca.is_active = 1
           ORDER BY (ca.account_type = 'DEMAND_DEPOSIT') DESC, ca.id
           LIMIT 1)
  FROM tbl_user u
  WHERE u.status = 'ACTIVE'
),
gen AS (
  SELECT tgt.user_id, tgt.account_id, months.m,
         tpl.tno, tpl.merchant, tpl.base_amount, tpl.cat_name, tpl.cls, tpl.is_fixed,
         DATE_ADD(
           DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL months.m MONTH), '%Y-%m-01'),
           INTERVAL ((tpl.dom - 1 + ((tgt.user_id * 11) % 28)) % 28) DAY
         ) AS tr_date
  FROM tgt
  JOIN months
  JOIN tpl
  WHERE tgt.account_id IS NOT NULL
)
SELECT
  gen.user_id,
  gen.account_id,
  CONCAT('DUMMY-', gen.user_id, '-', DATE_FORMAT(gen.tr_date, '%Y%m'), '-', gen.tno),
  gen.merchant,
  gen.merchant,
  CASE WHEN gen.is_fixed = 1
       THEN ROUND(gen.base_amount * (85 + ((gen.user_id * 3) % 7) * 5) / 100, -2)
       ELSE ROUND(gen.base_amount * (70 + ((gen.user_id * 5) % 13) * 10) / 100, -2)
            + ((((gen.m + 12) * 17) + (gen.tno * 5) + (gen.user_id * 3)) % 11) * 300
  END,
  CASE WHEN gen.cls = 'INCOME' THEN 'IN' ELSE 'OUT' END,
  gen.tr_date,
  SEC_TO_TIME(((gen.tno * 1373 + gen.m * 631 + gen.user_id * 971) % 43200) + 32400),
  gen.cls,
  c.id,
  0,
  0
FROM gen
LEFT JOIN tbl_category c ON c.category_name = gen.cat_name
WHERE gen.tr_date <= @seed_upto
  AND (gen.cat_name IS NULL OR c.id IS NOT NULL)
  AND NOT EXISTS (
        SELECT 1 FROM tbl_transaction t
         WHERE t.codef_tr_key = CONCAT('DUMMY-', gen.user_id, '-',
                                       DATE_FORMAT(gen.tr_date, '%Y%m'), '-', gen.tno)
      );

-- =====================================================================
-- 결과 확인 — 오늘부터 @seed_upto 까지 날짜별 소비액
-- 사용자마다 값이 다르고, 한도(예: 30,000원)를 넘는 날과 아닌 날이 섞여야 정상이다.
-- =====================================================================
SELECT t.tr_date AS 날짜,
       t.user_id,
       COUNT(*)      AS 건수,
       SUM(t.amount) AS 일일소비액
FROM tbl_transaction t
WHERE t.codef_tr_key LIKE 'DUMMY-%'
  AND t.classification = 'CONSUMPTION'
  AND t.is_refund = 0
  AND t.is_excluded_from_summary = 0
  AND t.tr_date BETWEEN CURDATE() AND @seed_upto
GROUP BY t.tr_date, t.user_id
ORDER BY t.tr_date, t.user_id;
