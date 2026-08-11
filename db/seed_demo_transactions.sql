-- =====================================================================
-- db/seed_demo_transactions.sql — 시연용 거래내역 시드 (이슈 #115)
--
-- 무엇을 채우나
--   tbl_transaction 에 최근 7개월치 거래를 넣는다. 소비 리포트 전용이 아니라
--   거래내역·미션 배정·고정지출 탐지·환불 처리까지 함께 시연할 수 있게 구성했다.
--
--     · 최근 7개월(당월 포함)      → 소비 리포트 6개월 추이 (SP_01_01·02)
--     · 소분류 단위 카테고리 분포   → 메인 챌린지 과소비 순위·미션 배정 (MC_02_01~04)
--     · 매월 동일 가맹점·동일 금액  → 고정지출 자동 탐지 (FS_01_01, 룰 v1 = 3회 반복)
--     · 환불 거래 1건(원거래 연결)  → 그룹 챌린지 환불 반영 (GC_05_01)
--     · CONSUMPTION/TRANSFER/INCOME → 거래 3분류 (AN_01_04)
--     · is_excluded_from_summary=1  → 집계 제외 처리
--
-- 선행 조건 (없으면 아무것도 안 들어간다)
--   ① db/seed_category.sql 적용 — 카테고리를 이름으로 JOIN 한다
--   ② 앱에서 계좌 연동을 1회 완료 — tbl_transaction.account_id 가 NOT NULL FK 다
--      계좌는 시드로 만들지 않는다. 가짜 계좌를 넣으면 즉시조회에서 전부
--      NEED_RECONNECT 로 뒤집혀 계좌 화면이 깨진다(PROGRESS.md 2026-08-07).
--      목서버 계좌가 1개뿐이어도 상관없다 — 사용자당 계좌 1개만 골라 쓴다.
--
-- 대상
--   status='ACTIVE' 이고 활성 계좌가 있는 **모든 사용자**. 팀원 각자 로컬에서 그대로 돌리면
--   자기 계정에 데이터가 붙는다. 계좌가 없는 사용자는 건너뛰고 맨 끝에 안내를 출력한다.
--
-- 재실행
--   codef_tr_key 를 DUMMY-{user_id}-{개월}-{템플릿번호} 로 결정적으로 만들고
--   NOT EXISTS 로 막는다. 몇 번을 돌려도 중복이 쌓이지 않는다.
--   날짜는 실행 시점(CURDATE()) 기준 상대 계산이라 다음 달에 돌려도 최신 데이터가 된다.
--
-- 되돌리기
--   DELETE FROM tbl_transaction WHERE codef_tr_key LIKE 'DUMMY-%';
--
-- 실행
--   mysql -u tangtang -p tangtang --default-character-set=utf8mb4 \
--     -e "source D:/…/db/seed_demo_transactions.sql"
-- =====================================================================

USE tangtang;
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 본 거래 — 템플릿 34종 × 7개월
--
-- is_fixed=1 은 고정지출 탐지 대상이라 매월 금액을 똑같이 유지한다.
-- is_fixed=0 은 (개월, 템플릿번호) 로 정해지는 값만큼 흔들어 현실감을 준다.
--   난수를 쓰지 않는 이유: 재실행해도 같은 값이어야 NOT EXISTS 중복 방지가 성립한다.
-- ---------------------------------------------------------------------
INSERT INTO tbl_transaction
  (user_id, account_id, codef_tr_key, merchant_name, merchant_name_normalized,
   amount, direction, tr_date, tr_time, classification, category_id,
   is_refund, is_excluded_from_summary)
WITH RECURSIVE
months (m) AS (
  SELECT 0 UNION ALL SELECT m + 1 FROM months WHERE m < 6      -- 0=당월 … 6=6개월 전
),
tpl (tno, merchant, base_amount, cat_name, dom, cls, is_fixed) AS (
  -- 고정지출 (매월 동일 가맹점·동일 금액) — 3회 이상 반복돼야 탐지된다
  SELECT  1, '넷플릭스',       17000, 'OTT',            15, 'CONSUMPTION', 1 UNION ALL
  SELECT  2, 'SKT통신요금',    55000, '통신비',          25, 'CONSUMPTION', 1 UNION ALL
  SELECT  3, '유튜브뮤직',     11900, '음원',             7, 'CONSUMPTION', 1 UNION ALL
  SELECT  4, '피트니스클럽',   79000, '운동시설',          5, 'CONSUMPTION', 1 UNION ALL
  SELECT  5, '월세이체',      500000, '월세',            25, 'CONSUMPTION', 1 UNION ALL
  -- 배달앱 — 과소비 1순위가 되도록 건수·금액을 크게 잡았다
  SELECT  6, '배달의민족',      24000, '배달앱',           3, 'CONSUMPTION', 0 UNION ALL
  SELECT  7, '배달의민족',      31000, '배달앱',           9, 'CONSUMPTION', 0 UNION ALL
  SELECT  8, '쿠팡이츠',       19000, '배달앱',          14, 'CONSUMPTION', 0 UNION ALL
  SELECT  9, '배달의민족',      27000, '배달앱',          20, 'CONSUMPTION', 0 UNION ALL
  SELECT 10, '요기요',         22000, '배달앱',          27, 'CONSUMPTION', 0 UNION ALL
  -- 음식점/외식 — 2순위
  SELECT 11, '김밥천국',        9500, '음식점/외식',       2, 'CONSUMPTION', 0 UNION ALL
  SELECT 12, '고기집',         46000, '음식점/외식',      11, 'CONSUMPTION', 0 UNION ALL
  SELECT 13, '백반집',         11000, '음식점/외식',      18, 'CONSUMPTION', 0 UNION ALL
  SELECT 14, '파스타집',       28000, '음식점/외식',      24, 'CONSUMPTION', 0 UNION ALL
  -- 카페/간식 — 3순위
  SELECT 15, '스타벅스',        5800, '카페/간식',         4, 'CONSUMPTION', 0 UNION ALL
  SELECT 16, '스타벅스',        6300, '카페/간식',         8, 'CONSUMPTION', 0 UNION ALL
  SELECT 17, '투썸플레이스',    7200, '카페/간식',        13, 'CONSUMPTION', 0 UNION ALL
  SELECT 18, '메가커피',        2500, '카페/간식',        17, 'CONSUMPTION', 0 UNION ALL
  SELECT 19, '스타벅스',        5800, '카페/간식',        22, 'CONSUMPTION', 0 UNION ALL
  -- 편의점
  SELECT 20, 'GS25',           8400, '편의점',            6, 'CONSUMPTION', 0 UNION ALL
  SELECT 21, 'CU',             5200, '편의점',           16, 'CONSUMPTION', 0 UNION ALL
  SELECT 22, '세븐일레븐',      6800, '편의점',           26, 'CONSUMPTION', 0 UNION ALL
  -- 쇼핑
  SELECT 23, '쿠팡',           38000, '온라인쇼핑',       10, 'CONSUMPTION', 0 UNION ALL
  SELECT 24, '무신사',         64000, '패션',            19, 'CONSUMPTION', 0 UNION ALL
  SELECT 25, '올리브영',       32000, '뷰티',            23, 'CONSUMPTION', 0 UNION ALL
  -- 교통
  SELECT 26, '티머니충전',      40000, '대중교통',          1, 'CONSUMPTION', 0 UNION ALL
  SELECT 27, '카카오T',        12800, '택시/모빌리티',     12, 'CONSUMPTION', 0 UNION ALL
  SELECT 28, '카카오T',        18400, '택시/모빌리티',     28, 'CONSUMPTION', 0 UNION ALL
  -- 생활·건강·문화
  SELECT 29, '이마트',         87000, '장보기/마트',      21, 'CONSUMPTION', 0 UNION ALL
  SELECT 30, '다이소',         14000, '생활용품',         15, 'CONSUMPTION', 0 UNION ALL
  SELECT 31, '내과의원',       15000, '병원',             9, 'CONSUMPTION', 0 UNION ALL
  SELECT 32, 'CGV',           15000, '영화·공연·전시',    6, 'CONSUMPTION', 0 UNION ALL
  -- 수입·이체 (카테고리 없음 — 소비가 아니라 3분류 시연용)
  SELECT 33, '급여입금',     2800000, NULL,              25, 'INCOME',      1 UNION ALL
  SELECT 34, '내계좌이체',    300000, NULL,              26, 'TRANSFER',    1
),
tgt (user_id, account_id) AS (
  -- 사용자당 계좌 1개만 고른다. 입출금(DEMAND_DEPOSIT) 우선, 없으면 아무거나.
  SELECT u.id,
         (SELECT ca.id FROM tbl_connected_account ca
           WHERE ca.user_id = u.id AND ca.is_active = 1
           ORDER BY (ca.account_type = 'DEMAND_DEPOSIT') DESC, ca.id
           LIMIT 1)
  FROM tbl_user u
  WHERE u.status = 'ACTIVE'
),
gen AS (
  -- 말일이 짧은 달(2월 등)에 31일을 넣지 않도록 LEAST 로 잘라낸다
  SELECT tgt.user_id, tgt.account_id, months.m,
         tpl.tno, tpl.merchant, tpl.base_amount, tpl.cat_name, tpl.cls, tpl.is_fixed,
         DATE_ADD(
           DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL months.m MONTH), '%Y-%m-01'),
           INTERVAL LEAST(tpl.dom,
                          DAY(LAST_DAY(DATE_SUB(CURDATE(), INTERVAL months.m MONTH)))) - 1 DAY
         ) AS tr_date
  FROM tgt
  JOIN months
  JOIN tpl
  WHERE tgt.account_id IS NOT NULL
)
SELECT
  gen.user_id,
  gen.account_id,
  CONCAT('DUMMY-', gen.user_id, '-', gen.m, '-', gen.tno),
  gen.merchant,
  gen.merchant,
  -- ⚠ 계수를 바꾸지 말 것. (m*17 + tno*7) % 7 로 두면 tno*7 % 7 = 0 이라 템플릿 번호가
  --   금액에 아무 영향을 못 준다. 같은 base 를 쓰는 템플릿(스타벅스 15·19)이 매월 같은 금액이
  --   돼 "동일 가맹점·동일 금액 3회" 를 만족, 고정지출로 오탐지된다(실제로 발생해 고쳤다).
  gen.base_amount + CASE WHEN gen.is_fixed = 1 THEN 0
                         ELSE ((gen.m * 17 + gen.tno * 5) % 11) * 300 END,
  CASE WHEN gen.cls = 'INCOME' THEN 'IN' ELSE 'OUT' END,
  gen.tr_date,
  SEC_TO_TIME(((gen.tno * 1373 + gen.m * 631) % 43200) + 32400),   -- 09:00~21:00 사이
  gen.cls,
  c.id,
  0,
  0
FROM gen
LEFT JOIN tbl_category c ON c.category_name = gen.cat_name
WHERE gen.tr_date <= CURDATE()                       -- 미래 날짜는 넣지 않는다
  AND (gen.cat_name IS NULL OR c.id IS NOT NULL)     -- 카테고리 시드 누락 시 조용히 넣지 않는다
  AND NOT EXISTS (
        SELECT 1 FROM tbl_transaction t
         WHERE t.codef_tr_key = CONCAT('DUMMY-', gen.user_id, '-', gen.m, '-', gen.tno)
      );

-- ---------------------------------------------------------------------
-- 집계 제외 거래 — 가승인 후 즉시취소 같은 케이스 (AN_01_04)
-- 리포트·미션 판정에서 빠지는지 확인하는 용도다.
-- 특정 템플릿에 의존하지 않는다 — 월초에 실행해도 붙도록 위에서 들어간 행을 기준으로 잡는다.
-- ---------------------------------------------------------------------
INSERT INTO tbl_transaction
  (user_id, account_id, codef_tr_key, merchant_name, merchant_name_normalized,
   amount, direction, tr_date, tr_time, classification, category_id,
   is_refund, is_excluded_from_summary, cancel_yn)
SELECT t.user_id, t.account_id,
       CONCAT('DUMMY-', t.user_id, '-EXCLUDED'),
       '가승인테스트', '가승인테스트',
       33000, 'OUT', DATE_SUB(CURDATE(), INTERVAL 5 DAY), '13:20:00',
       'CONSUMPTION', (SELECT id FROM tbl_category WHERE category_name = '온라인쇼핑'),
       0, 1, 'Y'
FROM (SELECT user_id, MIN(account_id) AS account_id
        FROM tbl_transaction
       WHERE codef_tr_key LIKE 'DUMMY-%'
       GROUP BY user_id) t
WHERE NOT EXISTS (
  SELECT 1 FROM tbl_transaction x
   WHERE x.codef_tr_key = CONCAT('DUMMY-', t.user_id, '-EXCLUDED'));

-- ---------------------------------------------------------------------
-- 환불 거래 — 가장 최근 '무신사' 결제를 전액 환불한다 (GC_05_01)
-- amount 는 항상 양수이고 is_refund=1 로 구분한다는 스키마 규칙을 따른다.
-- 월초 실행 시 당월 무신사 행이 아직 없을 수 있어 "가장 최근" 건을 고른다.
-- ---------------------------------------------------------------------
INSERT INTO tbl_transaction
  (user_id, account_id, codef_tr_key, merchant_name, merchant_name_normalized,
   amount, direction, tr_date, tr_time, classification, category_id,
   is_refund, original_transaction_id, refunded_amount, is_excluded_from_summary)
SELECT o.user_id, o.account_id,
       CONCAT('DUMMY-', o.user_id, '-REFUND'),
       o.merchant_name, o.merchant_name_normalized,
       o.amount, 'IN', LEAST(DATE_ADD(o.tr_date, INTERVAL 2 DAY), CURDATE()), '11:05:00',
       'CONSUMPTION', o.category_id,
       1, o.id, o.amount, 0
FROM (
  SELECT t.*, ROW_NUMBER() OVER (PARTITION BY t.user_id ORDER BY t.tr_date DESC, t.id DESC) AS rn
  FROM tbl_transaction t
  WHERE t.codef_tr_key LIKE 'DUMMY-%'
    AND t.merchant_name = '무신사'
    AND t.is_refund = 0
) o
WHERE o.rn = 1
  AND NOT EXISTS (
    SELECT 1 FROM tbl_transaction x
     WHERE x.codef_tr_key = CONCAT('DUMMY-', o.user_id, '-REFUND'));

-- =====================================================================
-- 결과 확인
-- =====================================================================

-- 사용자별 시드 현황
SELECT u.id AS user_id,
       COALESCE(u.nickname, u.social_name, u.email) AS 표시명,
       COUNT(t.id)                                   AS 시드거래수,
       MIN(t.tr_date)                                AS 시작일,
       MAX(t.tr_date)                                AS 종료일
FROM tbl_user u
LEFT JOIN tbl_transaction t ON t.user_id = u.id AND t.codef_tr_key LIKE 'DUMMY-%'
WHERE u.status = 'ACTIVE'
GROUP BY u.id, 표시명;

-- 계좌가 없어 건너뛴 사용자 — 앱에서 계좌 연동을 먼저 해야 한다
SELECT u.id AS user_id,
       COALESCE(u.nickname, u.social_name, u.email) AS 표시명,
       '앱에서 계좌 연동 후 이 시드를 다시 실행하세요' AS 안내
FROM tbl_user u
WHERE u.status = 'ACTIVE'
  AND NOT EXISTS (SELECT 1 FROM tbl_connected_account ca
                   WHERE ca.user_id = u.id AND ca.is_active = 1);

-- 최근 28일 과소비 순위 (미션 배정이 이 순서를 쓴다). 배달앱이 1위여야 정상이다.
SELECT c.category_name AS 소분류,
       p.category_name AS 대분류,
       SUM(t.amount)   AS 합계
FROM tbl_transaction t
JOIN tbl_category c ON c.id = t.category_id
LEFT JOIN tbl_category p ON p.id = c.parent_id
WHERE t.codef_tr_key LIKE 'DUMMY-%'
  AND t.classification = 'CONSUMPTION'
  AND t.is_excluded_from_summary = 0
  AND t.is_refund = 0
  AND t.tr_date >= DATE_SUB(CURDATE(), INTERVAL 28 DAY)
GROUP BY c.category_name, p.category_name
ORDER BY 합계 DESC
LIMIT 5;

-- 고정지출 탐지 대상 (동일 가맹점·동일 금액 3회 이상 = 룰 v1 조건)
SELECT merchant_name AS 가맹점, amount AS 금액, COUNT(*) AS 반복횟수
FROM tbl_transaction
WHERE codef_tr_key LIKE 'DUMMY-%' AND classification = 'CONSUMPTION'
GROUP BY merchant_name, amount
HAVING COUNT(*) >= 3
ORDER BY 반복횟수 DESC, 금액 DESC;
