-- =====================================================================
-- db/seed_demo_transactions_ver2.sql — 시연용 거래내역 시드 v2 (이슈 #191)
--
-- v1(seed_demo_transactions.sql) 과 무엇이 다른가
--   v1 은 금액을 (개월, 템플릿번호) 로만, 날짜를 템플릿 고정 결제일로만 정해서
--   **모든 사용자에게 완전히 동일한 거래**가 들어갔다(6명 전원 219행·8,204,200원).
--   그룹 챌린지는 "누가 한도를 넘었나" 로 기소·투표·순위를 만드는데, 전원이 같으면
--   기소 0명 아니면 전원밖에 안 나와 #168~#173 을 시연할 수 없었다.
--   v2 는 금액·결제일·거래시각 세 축 모두에 user_id 를 넣어 사용자별로 갈라놓는다.
--
-- ⚠ 먼저 지워야 한다
--   codef_tr_key 접두사가 v1 과 같은 DUMMY- 라서, v1 을 돌린 적이 있으면 맨 아래
--   NOT EXISTS 에 상당수가 막혀 **거래가 뒤섞인다**.
--   팀 전파는 db/migration/20260813_reseed_demo_transactions_per_user.sql 이 담당한다.
--   그 마이그레이션이 삭제 + 이 파일의 INSERT 를 함께 수행한다.
--   이 파일을 단독으로 쓰는 경우는 다음 둘뿐이다.
--     · 마이그레이션 적용 시점에 계좌 연동이 안 돼 0행이 들어간 사용자가 뒤늦게 연동한 경우
--     · 새로 합류한 팀원이 계좌 연동을 마친 경우
--
-- ⚠ 이 파일을 고치면
--   이미 마이그레이션을 돌린 팀원에게는 반영되지 않는다. 새 마이그레이션 파일을
--   db/migration/ 에 추가해야 전파된다. (마이그레이션은 적용 시점 스냅샷이라 소급 수정하지 않는다)
--
-- 무엇을 채우나 — v1 과 동일
--   tbl_transaction 에 최근 8개월치(다음 달 포함) 거래를 넣는다.
--     · 최근 7개월(당월 포함)      → 소비 리포트 6개월 추이
--     · 다음 달                    → 월 경계를 넘는 7일 챌린지 시연용(아래 「다음 달」 참고)
--     · 소분류 단위 카테고리 분포   → 과소비 순위·미션 배정
--     · 매월 동일 가맹점·동일 금액  → 고정지출 자동 탐지 (룰 v1 = 3회 반복)
--     · 환불 거래 1건(원거래 연결)  → 그룹 챌린지 환불 반영
--     · CONSUMPTION/TRANSFER/INCOME → 거래 3분류
--     · is_excluded_from_summary=1  → 집계 제외 처리
--
-- 선행 조건 (없으면 아무것도 안 들어간다)
--   ① db/seed_category.sql 적용 — 카테고리를 이름으로 JOIN 한다
--   ② 앱에서 계좌 연동을 1회 완료 — tbl_transaction.account_id 가 NOT NULL FK 다
--      계좌는 시드로 만들지 않는다. 가짜 계좌를 넣으면 즉시조회에서 전부
--      NEED_RECONNECT 로 뒤집혀 계좌 화면이 깨진다(PROGRESS.md 2026-08-07).
--
-- 대상
--   status='ACTIVE' 이고 활성 계좌가 있는 **모든 사용자**. 계좌가 없는 사용자는 건너뛰고
--   맨 끝에 안내를 출력한다.
--
-- 재실행
--   codef_tr_key 를 DUMMY-{user_id}-{거래연월}-{템플릿번호} 로 결정적으로 만들고
--   NOT EXISTS 로 막는다. 몇 번을 돌려도 중복이 쌓이지 않는다.
--
--   ⚠ v1 은 연월 대신 **상대 개월수(0=당월 … 6=6개월 전)** 를 키에 넣었다. 그러면 다음 달에
--     다시 실행할 때 같은 달 거래가 다른 키로 한 번 더 들어간다.
--     (8월 실행 → DUMMY-1-0-17 = 8/15  /  9월 실행 → DUMMY-1-1-17 = 8/15 중복)
--     v2 는 다음 달치를 미리 만들기 때문에 이 문제가 반드시 터진다. 그래서 거래가 실제로
--     찍히는 **연월(YYYYMM)** 을 키로 쓴다. 언제 실행해도 같은 달은 같은 키가 된다.
--
-- 되돌리기
--   DELETE FROM tbl_transaction WHERE codef_tr_key LIKE 'DUMMY-%' AND is_refund = 1;
--   DELETE FROM tbl_transaction WHERE codef_tr_key LIKE 'DUMMY-%';
--   (환불을 먼저 지운다 — original_transaction_id 자기참조 FK)
--
-- 실행
--   mysql -u tangtang -p tangtang --default-character-set=utf8mb4 \
--     -e "source D:/…/db/seed_demo_transactions_ver2.sql"
-- =====================================================================

USE tangtang;
SET NAMES utf8mb4;

-- 어느 날짜까지 채울지. 기본은 오늘.
-- 챌린지 7일치를 미리 채워 시연하려면 db/inject_demo_today.sql 을 쓴다.
SET @seed_upto = CURDATE();

-- ---------------------------------------------------------------------
-- 본 거래 — 템플릿 34종 × 8개월(다음 달 ~ 6개월 전)
--
-- ■ 사용자별로 갈라놓는 3개의 계수 (u = user_id)
--
--   결제일 이동  day_shift = (u * 11) % 28          → 0~27일
--                shifted   = ((dom - 1 + day_shift) % 28) + 1   → 항상 1~28일
--     gcd(11,28)=1 이라 u 가 1~28 이면 전부 다른 날로 흩어진다.
--     % 28 로 되감아 29~31일을 만들지 않으므로 짧은 달(2월) 보정이 필요 없다.
--
--   변동지출 배율 var_pct = 70 + ((u * 5) % 13) * 10  → 70~190%
--     gcd(5,13)=1. "적게 쓰는 사람 / 많이 쓰는 사람" 이 갈린다.
--
--   고정지출 배율 fix_pct = 85 + ((u * 3) %  7) *  5  → 85~115%
--     gcd(3,7)=1. **m(개월)이 들어가지 않는다** — 한 사용자 안에서는 매월 같은 금액이어야
--     "동일 가맹점·동일 금액 3회 이상" 룰이 살아있다. 사용자 간에는 달라진다.
--
-- ■ 지터 계수를 바꾸지 말 것 (v1 에서 실제로 터졌던 사고)
--   ((((m+12)*17) + tno*5 + u*3) % 11) * 300 은 0~3,000원을 결정적으로 더한다.
--   계수와 모듈러가 서로소여야 한다 — gcd(17,11)=gcd(5,11)=gcd(3,11)=1.
--   예컨대 (m*17 + tno*7) % 7 로 두면 tno*7 % 7 = 0 이라 템플릿 번호가 금액에 아무
--   영향을 못 준다. 그러면 같은 base 를 쓰는 템플릿(스타벅스 15·19, 둘 다 5800)이 매월
--   같은 금액이 돼 "동일 가맹점·동일 금액 3회" 를 만족, 고정지출로 오탐지된다.
--
--   난수를 쓰지 않는 이유: 재실행해도 같은 값이어야 NOT EXISTS 중복 방지가 성립한다.
--
-- ■ m 에 +12 를 더하는 이유 — MySQL 의 % 는 피제수 부호를 따른다
--   다음 달은 m = -1 이다. 그대로 두면 (-17 + tno*5 + u*3) 이 음수가 되고,
--   MySQL 에서 -9 % 11 = -9 다(Python 은 2). 지터가 -2,700원이 돼 작은 금액은
--   **음수 amount** 로 들어간다. tbl_transaction.amount 에 CHECK 가 없어서
--   오류도 안 나고 조용히 들어간다. m+12(=11~18)로 항상 양수를 만든다.
--   상수를 더하는 것이라 사용자·템플릿 간 상대 차이는 그대로다.
--
-- ■ 다음 달(m = -1)을 넣는 이유
--   그룹 챌린지는 오늘 이후 7일을 평가한다. v1 은 당월까지만 만들고 미래를 잘라내서
--   챌린지 기간과 겹치는 날이 **오늘 하루뿐**이었다. 8/28 시작처럼 월을 넘는 기간을
--   위해 다음 달까지 미리 만들어 두고, @seed_upto 로 어디까지 넣을지만 조절한다.
-- ---------------------------------------------------------------------
INSERT INTO tbl_transaction
  (user_id, account_id, codef_tr_key, merchant_name, merchant_name_normalized,
   amount, direction, tr_date, tr_time, classification, category_id,
   is_refund, is_excluded_from_summary)
WITH RECURSIVE
months (m) AS (
  SELECT -1 UNION ALL SELECT m + 1 FROM months WHERE m < 6   -- -1=다음 달, 0=당월 … 6=6개월 전
),
tpl (tno, merchant, base_amount, cat_name, dom, cls, is_fixed) AS (
  -- 고정지출 (한 사용자 안에서 매월 동일 가맹점·동일 금액) — 3회 이상 반복돼야 탐지된다
  SELECT  1, '넷플릭스',       17000, 'OTT',            15, 'CONSUMPTION', 1 UNION ALL
  SELECT  2, 'SKT통신요금',    55000, '통신비',          25, 'CONSUMPTION', 1 UNION ALL
  SELECT  3, '유튜브뮤직',     11900, '음원',             7, 'CONSUMPTION', 1 UNION ALL
  SELECT  4, '피트니스클럽',   79000, '운동시설',          5, 'CONSUMPTION', 1 UNION ALL
  SELECT  5, '월세이체',      500000, '월세',            25, 'CONSUMPTION', 1 UNION ALL
  -- 배달앱 — 과소비 상위가 되도록 건수·금액을 크게 잡았다
  SELECT  6, '배달의민족',      24000, '배달앱',           3, 'CONSUMPTION', 0 UNION ALL
  SELECT  7, '배달의민족',      31000, '배달앱',           9, 'CONSUMPTION', 0 UNION ALL
  SELECT  8, '쿠팡이츠',       19000, '배달앱',          14, 'CONSUMPTION', 0 UNION ALL
  SELECT  9, '배달의민족',      27000, '배달앱',          20, 'CONSUMPTION', 0 UNION ALL
  SELECT 10, '요기요',         22000, '배달앱',          27, 'CONSUMPTION', 0 UNION ALL
  -- 음식점/외식
  SELECT 11, '김밥천국',        9500, '음식점/외식',       2, 'CONSUMPTION', 0 UNION ALL
  SELECT 12, '고기집',         46000, '음식점/외식',      11, 'CONSUMPTION', 0 UNION ALL
  SELECT 13, '백반집',         11000, '음식점/외식',      18, 'CONSUMPTION', 0 UNION ALL
  SELECT 14, '파스타집',       28000, '음식점/외식',      24, 'CONSUMPTION', 0 UNION ALL
  -- 카페/간식
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
  SELECT tgt.user_id, tgt.account_id, months.m,
         tpl.tno, tpl.merchant, tpl.base_amount, tpl.cat_name, tpl.cls, tpl.is_fixed,
         -- 사용자별로 (u*11)%28 일 만큼 민 결제일. % 28 이라 항상 1~28 → 말일 보정 불필요
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
       -- 고정지출: 사용자별 배율만. m 이 없어 매월 같은 금액 → 탐지 룰 유지
       THEN ROUND(gen.base_amount * (85 + ((gen.user_id * 3) % 7) * 5) / 100, -2)
       -- 변동지출: 사용자별 배율 + (개월·템플릿·사용자) 지터
       ELSE ROUND(gen.base_amount * (70 + ((gen.user_id * 5) % 13) * 10) / 100, -2)
            + ((((gen.m + 12) * 17) + (gen.tno * 5) + (gen.user_id * 3)) % 11) * 300
  END,
  CASE WHEN gen.cls = 'INCOME' THEN 'IN' ELSE 'OUT' END,
  gen.tr_date,
  -- 09:00~21:00 사이. u*971 로 사용자별 시각도 갈라놓는다 (gcd(971,43200)=1)
  SEC_TO_TIME(((gen.tno * 1373 + gen.m * 631 + gen.user_id * 971) % 43200) + 32400),
  gen.cls,
  c.id,
  0,
  0
FROM gen
LEFT JOIN tbl_category c ON c.category_name = gen.cat_name
WHERE gen.tr_date <= @seed_upto                      -- 기본은 오늘까지. 미래는 넣지 않는다
  AND (gen.cat_name IS NULL OR c.id IS NOT NULL)     -- 카테고리 시드 누락 시 조용히 넣지 않는다
  AND NOT EXISTS (
        SELECT 1 FROM tbl_transaction t
         WHERE t.codef_tr_key = CONCAT('DUMMY-', gen.user_id, '-',
                                       DATE_FORMAT(gen.tr_date, '%Y%m'), '-', gen.tno)
      );

-- ---------------------------------------------------------------------
-- 집계 제외 거래 — 가승인 후 즉시취소 같은 케이스
-- 리포트·미션 판정에서 빠지는지 확인하는 용도다.
-- 위에서 들어간 행을 기준으로 잡아 특정 템플릿에 의존하지 않는다.
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
-- 환불 거래 — 가장 최근 '무신사' 결제를 전액 환불한다
-- amount 는 항상 양수이고 is_refund=1 로 구분한다는 스키마 규칙을 따른다.
-- 미래 행이 섞이지 않도록 @seed_upto 이하에서만 고른다.
-- ---------------------------------------------------------------------
INSERT INTO tbl_transaction
  (user_id, account_id, codef_tr_key, merchant_name, merchant_name_normalized,
   amount, direction, tr_date, tr_time, classification, category_id,
   is_refund, original_transaction_id, refunded_amount, is_excluded_from_summary)
SELECT o.user_id, o.account_id,
       CONCAT('DUMMY-', o.user_id, '-REFUND'),
       o.merchant_name, o.merchant_name_normalized,
       o.amount, 'IN', LEAST(DATE_ADD(o.tr_date, INTERVAL 2 DAY), @seed_upto), '11:05:00',
       'CONSUMPTION', o.category_id,
       1, o.id, o.amount, 0
FROM (
  SELECT t.*, ROW_NUMBER() OVER (PARTITION BY t.user_id ORDER BY t.tr_date DESC, t.id DESC) AS rn
  FROM tbl_transaction t
  WHERE t.codef_tr_key LIKE 'DUMMY-%'
    AND t.merchant_name = '무신사'
    AND t.is_refund = 0
    AND t.tr_date <= @seed_upto
) o
WHERE o.rn = 1
  AND NOT EXISTS (
    SELECT 1 FROM tbl_transaction x
     WHERE x.codef_tr_key = CONCAT('DUMMY-', o.user_id, '-REFUND'));

-- =====================================================================
-- 결과 확인
-- =====================================================================

-- ① 사용자별 시드 현황 — 소비총액이 사용자마다 달라야 정상이다.
--    (v1 은 여기가 전원 8,204,200원으로 같았다)
SELECT u.id AS user_id,
       COALESCE(u.nickname, u.social_name, u.email)                        AS 표시명,
       COUNT(t.id)                                                         AS 시드거래수,
       SUM(CASE WHEN t.classification = 'CONSUMPTION' AND t.is_refund = 0
                 AND t.is_excluded_from_summary = 0
                THEN t.amount ELSE 0 END)                                   AS 소비총액,
       MIN(t.tr_date)                                                       AS 시작일,
       MAX(t.tr_date)                                                       AS 종료일
FROM tbl_user u
LEFT JOIN tbl_transaction t ON t.user_id = u.id AND t.codef_tr_key LIKE 'DUMMY-%'
WHERE u.status = 'ACTIVE'
GROUP BY u.id, 표시명
ORDER BY u.id;

-- ② 계좌가 없어 건너뛴 사용자 — 앱에서 계좌 연동 후 이 파일을 다시 실행한다
SELECT u.id AS user_id,
       COALESCE(u.nickname, u.social_name, u.email) AS 표시명,
       '앱에서 계좌 연동 후 seed_demo_transactions_ver2.sql 을 다시 실행하세요' AS 안내
FROM tbl_user u
WHERE u.status = 'ACTIVE'
  AND NOT EXISTS (SELECT 1 FROM tbl_connected_account ca
                   WHERE ca.user_id = u.id AND ca.is_active = 1);

-- ③ 오늘 하루 소비액 — 그룹 챌린지 기소 판정이 쓰는 값. 사용자마다 달라야 정상이다.
--    한도(예: 30,000원)를 넘는 사용자와 아닌 사용자가 섞여야 기소 시연이 된다.
SELECT t.user_id,
       COUNT(*)       AS 건수,
       SUM(t.amount)  AS 오늘소비액
FROM tbl_transaction t
WHERE t.codef_tr_key LIKE 'DUMMY-%'
  AND t.classification = 'CONSUMPTION'
  AND t.is_refund = 0
  AND t.is_excluded_from_summary = 0
  AND t.tr_date = CURDATE()
GROUP BY t.user_id
ORDER BY t.user_id;

-- ④ 최근 28일 과소비 순위 — 미션 배정이 쓰는 순서 (사용자별 상위 3개)
--    앱의 MissionCategoryAnalysisMapper.findTopCategorySpending 과 같은 조건을 쓴다.
--    ⚠ 미션 풀에 RELATIVE 미션이 있는 카테고리만 후보다. 이 필터가 없으면 월세가 1위로
--      나오는데, 월세는 미션 대상이 아니라 실제 화면 순위와 달라진다.
--    사용자마다 1위가 다를 수 있다 — 그게 v2 의 목적이다.
SELECT ranked.user_id, ranked.소분류, ranked.대분류, ranked.합계
FROM (
  SELECT t.user_id,
         c.category_name AS 소분류,
         p.category_name AS 대분류,
         SUM(t.amount)   AS 합계,
         ROW_NUMBER() OVER (PARTITION BY t.user_id ORDER BY SUM(t.amount) DESC) AS rn
  FROM tbl_transaction t
  JOIN tbl_category c ON c.id = t.category_id
  LEFT JOIN tbl_category p ON p.id = c.parent_id
  WHERE t.codef_tr_key LIKE 'DUMMY-%'
    AND t.classification = 'CONSUMPTION'
    AND t.is_excluded_from_summary = 0
    AND t.is_refund = 0
    AND t.tr_date >= DATE_SUB(CURDATE(), INTERVAL 28 DAY)
    AND t.tr_date <= CURDATE()
    AND EXISTS (SELECT 1 FROM tbl_mission_pool mp
                 WHERE mp.category_id = c.id AND mp.mission_type = 'RELATIVE')
  GROUP BY t.user_id, c.category_name, p.category_name
) ranked
WHERE ranked.rn <= 3
ORDER BY ranked.user_id, ranked.합계 DESC;

-- ⑤ 고정지출 탐지 대상 (동일 가맹점·동일 금액 3회 이상 = 룰 v1 조건)
--    ⚠ user_id 로 묶어야 한다. 안 묶으면 사용자별 금액이 섞여 실제 탐지 결과와 달라진다.
--    기대: 사용자마다 정확히 5종
--          (넷플릭스 · SKT통신요금 · 유튜브뮤직 · 피트니스클럽 · 월세이체)
--    이 외의 가맹점이 나오면 지터 계수가 깨진 것이다 — 위 「지터 계수」 주석을 볼 것.
SELECT user_id, merchant_name AS 가맹점, amount AS 금액, COUNT(*) AS 반복횟수
FROM tbl_transaction
WHERE codef_tr_key LIKE 'DUMMY-%'
  AND classification = 'CONSUMPTION'
  AND is_refund = 0
GROUP BY user_id, merchant_name, amount
HAVING COUNT(*) >= 3
ORDER BY user_id, 반복횟수 DESC, 금액 DESC;
