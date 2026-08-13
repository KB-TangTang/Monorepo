-- 2026-08-13 · 시연용 거래내역을 사용자별로 분리해 재생성 (이슈 #191)
--
-- 신규 설치: **실행 대상이다.** 다만 이 시점에는 사용자·연동계좌가 없어 0행이 들어간다.
--            앱에서 계좌 연동을 마친 뒤 db/seed_demo_transactions_ver2.sql 을 한 번 더 실행한다.
--            (스키마 변경이 없는 데이터 마이그레이션이라, 안 돌려도 앱은 정상 기동한다.
--             그룹 챌린지·미션 시연 데이터가 필요할 때만 의미가 있다.)
--
-- 무엇을 바꾸나
--   기존 시드(db/seed_demo_transactions.sql)는 금액을 (개월, 템플릿번호) 로만, 날짜를
--   템플릿에 박힌 결제일로만 정해서 **모든 사용자에게 완전히 동일한 거래**를 넣었다.
--   (6명 전원 219행 · 7개월 소비 8,204,200원 · 오늘 거래는 전원 투썸플레이스 9,600원 하나)
--   그룹 챌린지는 "누가 한도를 넘었나" 로 기소·투표·순위를 만드는데 전원이 같으면
--   기소 0명 아니면 전원밖에 안 나온다. #168~#173 을 시연할 수가 없다.
--
--   이 마이그레이션은 기존 DUMMY-% 거래를 지우고, 사용자별 배율·결제일 이동·시각 분산을
--   넣은 v2 계산식으로 다시 만든다. 계수의 의미와 주의사항은
--   db/seed_demo_transactions_ver2.sql 헤더에 자세히 적혀 있다.
--
-- 왜 삭제가 선행인가
--   v2 도 codef_tr_key 접두사가 DUMMY- 로 같다. 지우지 않고 넣으면 맨 아래 NOT EXISTS 에
--   막혀 옛 거래와 새 거래가 뒤섞인다(오류도 안 나서 더 위험하다). 그래서 먼저 비운다.
--   삭제 → 재삽입이라 이 파일은 **몇 번을 다시 돌려도 같은 상태**가 된다.
--   시연 리허설 중 "깨끗하게 되돌리기" 용도로 그대로 다시 써도 된다.
--
-- ⚠ 지워지는 것은 codef_tr_key LIKE 'DUMMY-%' 인 시연용 거래뿐이다.
--   실제 CODEF 수집 거래, seed_transaction_mock_local.sql 의 MOCK-LOCAL-% 거래는 건드리지 않는다.
--
-- 확인
--   맨 아래 확인 쿼리에서
--     · 사용자별 소비총액이 **서로 다르고**
--     · 고정지출 탐지 대상이 **사용자마다 5종**
--   이면 정상이다.

USE tangtang;
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1) 기존 시연용 거래 삭제
--    환불 거래를 먼저 지운다 — original_transaction_id 자기참조 FK.
--    (ON DELETE SET NULL 이라 순서를 어겨도 오류는 안 나지만, 원거래를 잃은 환불 행이
--     잠깐 생기는 것을 피한다)
-- ---------------------------------------------------------------------
SELECT COUNT(*) AS 삭제대상_거래수
FROM tbl_transaction WHERE codef_tr_key LIKE 'DUMMY-%';

START TRANSACTION;

DELETE FROM tbl_transaction WHERE codef_tr_key LIKE 'DUMMY-%' AND is_refund = 1;
DELETE FROM tbl_transaction WHERE codef_tr_key LIKE 'DUMMY-%';

COMMIT;

-- ---------------------------------------------------------------------
-- 2) v2 계산식으로 재생성 — db/seed_demo_transactions_ver2.sql 본문과 동일
--
--    ※ SOURCE 로 그 파일을 부르지 않고 복사해 둔 이유
--      중첩 SOURCE 는 mysql 클라이언트의 현재 디렉터리 기준으로 경로를 푼다. 팀 문서의
--      Windows 실행 방식이 절대경로라 기계마다 깨진다. 마이그레이션은 원래 적용 시점
--      스냅샷이므로 복사가 관례에도 맞다.
--    ※ 앞으로 ver2 파일을 고쳐도 이 파일은 고치지 않는다. 이미 적용된 사람에게 전파하려면
--      db/migration/ 에 새 파일을 추가해야 한다.
-- ---------------------------------------------------------------------
SET @seed_upto = CURDATE();

INSERT INTO tbl_transaction
  (user_id, account_id, codef_tr_key, merchant_name, merchant_name_normalized,
   amount, direction, tr_date, tr_time, classification, category_id,
   is_refund, is_excluded_from_summary)
WITH RECURSIVE
months (m) AS (
  SELECT -1 UNION ALL SELECT m + 1 FROM months WHERE m < 6   -- -1=다음 달, 0=당월 … 6=6개월 전
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
  -- 결제일을 사용자별로 (user_id * 11) % 28 일 만큼 민다. % 28 이라 항상 1~28일 →
  -- 짧은 달(2월) 보정이 필요 없다. gcd(11,28)=1 이라 사용자마다 다른 날로 흩어진다.
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
  -- 키에 상대 개월수가 아니라 거래 연월을 쓴다. 다음 달에 다시 실행해도 같은 달은 같은 키다.
  CONCAT('DUMMY-', gen.user_id, '-', DATE_FORMAT(gen.tr_date, '%Y%m'), '-', gen.tno),
  gen.merchant,
  gen.merchant,
  CASE WHEN gen.is_fixed = 1
       -- 고정지출: 사용자별 배율(85~115%)만. m 이 없어 매월 같은 금액 → 탐지 룰 유지
       THEN ROUND(gen.base_amount * (85 + ((gen.user_id * 3) % 7) * 5) / 100, -2)
       -- 변동지출: 사용자별 배율(70~190%) + 지터. 계수와 모듈러는 서로소여야 한다
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

-- 집계 제외 거래 (가승인 후 즉시취소)
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

-- 환불 거래 (가장 최근 무신사 결제 전액 환불)
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

-- ---------------------------------------------------------------------
-- 3) 확인
-- ---------------------------------------------------------------------

-- ① 사용자별 시드 현황 — 소비총액이 사용자마다 달라야 정상이다
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

-- ② 계좌가 없어 건너뛴 사용자
SELECT u.id AS user_id,
       COALESCE(u.nickname, u.social_name, u.email) AS 표시명,
       '앱에서 계좌 연동 후 db/seed_demo_transactions_ver2.sql 을 실행하세요' AS 안내
FROM tbl_user u
WHERE u.status = 'ACTIVE'
  AND NOT EXISTS (SELECT 1 FROM tbl_connected_account ca
                   WHERE ca.user_id = u.id AND ca.is_active = 1);

-- ③ 고정지출 탐지 대상 — 사용자마다 정확히 5종이어야 정상이다
--    (넷플릭스 · SKT통신요금 · 유튜브뮤직 · 피트니스클럽 · 월세이체)
--    다른 가맹점이 섞이면 지터 계수가 깨진 것이다.
SELECT user_id, merchant_name AS 가맹점, amount AS 금액, COUNT(*) AS 반복횟수
FROM tbl_transaction
WHERE codef_tr_key LIKE 'DUMMY-%'
  AND classification = 'CONSUMPTION'
  AND is_refund = 0
GROUP BY user_id, merchant_name, amount
HAVING COUNT(*) >= 3
ORDER BY user_id, 반복횟수 DESC, 금액 DESC;

-- ---------------------------------------------------------------------
-- (선택) 미션 분석 스냅샷 초기화
--
--   tbl_user_mission_analysis 는 최근 28일 소비를 산정해 저장해 둔 스냅샷이다.
--   이 마이그레이션 이전에 만들어진 스냅샷은 **옛 거래 기준 순위**를 그대로 들고 있고,
--   해당 주기의 미션 3개가 모두 배정될 때까지 갱신되지 않는다.
--   그룹 챌린지 기소는 일일 소비액을 직접 보므로 영향이 없지만, 개인 미션 화면에서
--   사용자별로 다른 카테고리가 나오길 원하면 아래를 직접 실행한다.
--
--   ⚠ 배정 이력이 지워진다. 참조하는 FK 는 없어 삭제 자체는 안전하다.
--
--   DELETE FROM tbl_user_mission_analysis
--    WHERE user_id IN (SELECT DISTINCT user_id FROM tbl_transaction
--                       WHERE codef_tr_key LIKE 'DUMMY-%');
-- ---------------------------------------------------------------------
