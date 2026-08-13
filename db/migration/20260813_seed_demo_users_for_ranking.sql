-- 2026-08-13 · 개인 미션 랭킹 검증용 가상 사용자 시드 (이슈 #195)
--
-- 신규 설치: **실행 대상이다.** 사용자·계좌를 스스로 만들기 때문에 아무것도 없는 DB 에서도 동작한다.
--            (스키마 변경이 없는 데이터 마이그레이션이라, 안 돌려도 앱은 정상 기동한다)
--
-- 무엇을 넣나
--   로컬에 실제 로그인 가능한 계정이 2~3개뿐이라 "전체 사용자 중 순위"(#194) 를 검증할 모수가 없다.
--   가상 사용자 20명을 만들고, 거래 · 미션 판정 이력 · 연속 성공일까지 채운다.
--
-- ⚠ 거래만 넣어서는 순위가 안 생긴다
--   랭킹의 입력은 거래가 아니라 **미션 판정 이력**이다.
--     거래 → 분석 스냅샷 → 미션 배정 → 일별 판정(SUCCESS/FAIL) → streak → 점수 → 순위
--   배정 스케줄러(RelativeMissionAssignmentScheduler.java:30)는 오늘치만 만들고 판정 배치는
--   어제치만 평가한다. 과거를 소급하지 않는다. 그래서 tbl_user_mission_info 를 직접 주입한다.
--
-- 실제 로그인과 충돌하지 않는다
--   UNIQUE KEY uk_user_social (social_provider, provider_user_id) 이고 이 사용자들은
--   social_provider = 'DEMO' 다. 구글 로그인 조회에 절대 걸리지 않는다.
--
-- 되돌리기
--   tbl_user 를 참조하는 21개 테이블이 전부 ON DELETE CASCADE 다. 한 줄이면 흔적 없이 사라진다.
--     DELETE FROM tbl_user WHERE social_provider = 'DEMO';
--   ⚠ 예외는 tbl_challenge_group.admin_id 하나뿐이다(schema.sql:428 — ON DELETE 절이 없다).
--     가상 사용자를 그룹장으로 만들지 않으므로 문제되지 않는다.
--
-- 그룹 챌린지에는 영향이 없다
--   가상 사용자를 tbl_group_member 에 넣지 않는다. #168 기소 대상에 섞이지 않는다.
--
-- ⚠ 챌린지 동의(tbl_user_consent)를 **일부러 넣지 않는다**
--   배정 배치의 대상 조회(RelativeMissionAssignmentMapper.xml:6 findUnassignedChallengeConsentedUserIds)는
--   status='ACTIVE' + CHALLENGE 동의 보유 인 사용자를 전부 긁는다. social_provider 를 보지 않는다.
--   동의를 넣으면 가상 사용자도 매일 배정 대상이 되는데, 이들은 오늘 이후 거래가 없어
--   지출 0 → 목표 이하 → **매일 무조건 SUCCESS** 가 된다. 하루가 지날 때마다 20명이 나란히
--   점수를 벌어 아래에서 애써 만든 편차가 무너지고 랭킹 상위를 전부 차지한다.
--   동의를 빼면 배치가 이들을 건너뛰고, 아래 60일치 이력이 그대로 고정된다.
--   ⇒ 실제 사용자만 매일 점수가 늘고, 가상 사용자는 고정된 비교군으로 남는다. 이게 의도다.
--
-- 선행 조건
--   db/seed.sql(난이도 3종) · db/seed_category.sql · db/seed_mission_pool.sql 이 적용돼 있어야 한다.
--   셋 중 하나라도 비어 있으면 아래 @seed_ready 가 0 이 되고 **아무것도 넣지 않는다.**
--
-- 확인
--   맨 아래 확인 쿼리에서 사용자마다 점수·연속일·소비총액이 **서로 다르면** 정상이다.

USE tangtang;
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 0) 설정값
-- ---------------------------------------------------------------------
SET @demo_user_count   = 20;    -- 가상 사용자 수
SET @demo_history_days = 60;    -- 주입할 미션 이력 일수 (어제부터 과거로)

SET @difficulty_count = (SELECT COUNT(*) FROM tbl_mission_difficulty);
SET @relative_pool_count = (SELECT COUNT(*) FROM tbl_mission_pool WHERE mission_type = 'RELATIVE');
SET @category_count = (SELECT COUNT(*) FROM tbl_category);

SET @seed_ready = (
  @difficulty_count    >= 3
  AND @relative_pool_count > 0
  AND @category_count      > 0
);

-- ★ 여기서 seed_ready 가 0 이면 아래는 전부 0행이다. 오류가 안 나므로 반드시 확인한다.
SELECT @seed_ready            AS seed_ready,            -- 1 이어야 함
       @difficulty_count      AS difficulty_count,      -- 3 이상 (db/seed.sql)
       @relative_pool_count   AS relative_pool_count,   -- 1 이상 (db/seed_mission_pool.sql)
       @category_count        AS category_count;        -- 1 이상 (db/seed_category.sql)

-- ---------------------------------------------------------------------
-- 1) 기존 가상 사용자 삭제 — 재실행 멱등
--    CASCADE 로 계좌 · 거래 · 미션 이력 · streak 이 함께 사라진다.
--    실제 사용자(GOOGLE 등)는 건드리지 않는다.
-- ---------------------------------------------------------------------
SELECT COUNT(*) AS 삭제대상_가상사용자수
FROM tbl_user WHERE social_provider = 'DEMO';

DELETE FROM tbl_user WHERE social_provider = 'DEMO';

-- ---------------------------------------------------------------------
-- 2) 가상 사용자 20명
--    difficulty_id 를 1(EASY) · 2(NORMAL) · 3(HARD) 로 돌려 배정한다.
--    난이도별 점수가 10 · 20 · 35 로 달라(db/seed.sql:14-16) 총점 편차의 한 축이 된다.
--    relative_mission_qualified_at 을 과거로 박아 "이미 자격을 얻은 사용자" 로 만든다.
-- ---------------------------------------------------------------------
INSERT INTO tbl_user
  (social_provider, provider_user_id, email, nickname, social_name, name,
   status, difficulty_id, relative_mission_qualified_at, tutorial_seen_at, created_at)
WITH RECURSIVE seq (u) AS (
  SELECT 1 UNION ALL SELECT u + 1 FROM seq WHERE u < @demo_user_count
)
SELECT 'DEMO',
       CONCAT('demo-', LPAD(seq.u, 3, '0')),
       CONCAT('demo', LPAD(seq.u, 3, '0'), '@tangtang.local'),
       CONCAT(
         ELT((seq.u % 10) + 1, '알뜰한','굳건한','냉철한','성실한','재빠른',
                               '우직한','담대한','치밀한','느긋한','꼼꼼한'),
         ELT((seq.u %  7) + 1, '지갑','판사','배심원','절약러','통장','계산기','저축왕'),
         LPAD(seq.u, 2, '0')
       ),
       CONCAT('데모', LPAD(seq.u, 2, '0')),
       CONCAT('데모', LPAD(seq.u, 2, '0')),
       'ACTIVE',
       (seq.u % 3) + 1,
       DATE_SUB(NOW(), INTERVAL @demo_history_days DAY),
       DATE_SUB(NOW(), INTERVAL @demo_history_days DAY),
       DATE_SUB(NOW(), INTERVAL @demo_history_days + 30 DAY)
FROM seq
WHERE @seed_ready = 1;

-- ---------------------------------------------------------------------
-- 3) 연동 계좌 — 사용자당 1개
--    거래의 account_id 가 NOT NULL 이라 계좌 없이는 거래를 넣을 수 없다.
--    account_no_encrypted 는 UNIQUE KEY uk_ca_user_account (user_id, account_no_encrypted)
--    의 일부다. 실제 연동 계좌의 HMAC 값과 형식이 달라 부딪히지 않는다.
-- ---------------------------------------------------------------------
INSERT INTO tbl_connected_account
  (user_id, codef_connected_id, bank_code, bank_name, account_name,
   account_no_encrypted, account_no_masked, account_type, deposit_type_code,
   balance, sync_status, last_sync_at, is_active, created_at)
SELECT u.id,
       CONCAT('DEMO-CONNECTED-', LPAD(u.id, 5, '0')),
       '004', 'KB국민은행', 'KB나라사랑통장',
       CONCAT('DEMO-ACCOUNT-', LPAD(u.id, 5, '0')),
       CONCAT('110-***-****', LPAD(u.id % 100, 2, '0')),
       'DEMAND_DEPOSIT', '11',
       1000000 + (u.id * 137) % 4000000,
       'NORMAL', NOW(), 1,
       DATE_SUB(NOW(), INTERVAL @demo_history_days DAY)
FROM tbl_user u
WHERE u.social_provider = 'DEMO'
  AND @seed_ready = 1;

-- ---------------------------------------------------------------------
-- 4) 거래 — #191 ver2 계산식 그대로 (db/seed_demo_transactions_ver2.sql)
--
--    이 마이그레이션은 자기완결형이다. 20260813_reseed_demo_transactions_per_user.sql 이
--    먼저 돌든 나중에 돌든 결과가 같도록 여기서 직접 넣는다.
--    (reseed 는 실행 시점에 존재하는 사용자만 대상으로 한다. 이 파일이 뒤에 오면
--     가상 사용자는 거래 없이 남게 되므로 스스로 채워야 한다)
--
--    사용자별 편차 세 축
--      · 변동 지출 배율  70 + ((u*5)%13)*10   → 70~190%
--      · 고정 지출 배율  85 + ((u*3)%7)*5     → 85~115%   ※ m 이 없어 매월 같은 금액 (탐지 룰 유지)
--      · 결제일 이동    ((dom-1 + (u*11)%28) % 28) + 1     → 항상 1~28일
--    ⚠ 지터의 (m + 12) 는 MySQL 의 % 가 피제수 부호를 따르기 때문이다.
--      다음 달(m = -1)에서 지터가 음수가 되면 금액이 음수로 들어간다
--      (tbl_transaction.amount 에 CHECK 가 없어 오류도 안 난다).
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
    AND u.social_provider = 'DEMO'          -- ← 실제 사용자는 reseed 마이그레이션이 맡는다
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
  AND @seed_ready = 1
  AND (gen.cat_name IS NULL OR c.id IS NOT NULL)
  AND NOT EXISTS (
        SELECT 1 FROM tbl_transaction t
         WHERE t.codef_tr_key = CONCAT('DUMMY-', gen.user_id, '-',
                                       DATE_FORMAT(gen.tr_date, '%Y%m'), '-', gen.tno)
      );

-- ---------------------------------------------------------------------
-- 5) 미션 판정 이력 — 어제부터 과거로 60일치
--
--    ★ 이것이 랭킹의 실제 입력이다(#194).
--
--    사용자별 성공률  40 + ((u*7)%55)  → 40~94%
--    판정             ((u*31 + d*17) % 100) < 성공률 이면 SUCCESS, 아니면 FAIL
--                     gcd(17,100)=1 이라 d 가 1~60 도는 동안 서로 다른 값이 나온다.
--
--    난이도는 사용자의 difficulty_id 를 그대로 쓴다. 실제 배정도 배정 시점 난이도를
--    tbl_user_mission_info.difficulty_id 에 박아 두므로(#194 가 이 값으로 점수를 낸다)
--    사용자별로 고정인 편이 점수 검산이 쉽다.
--
--    ⚠ 오늘(CURDATE())은 비워 둔다. 오늘치는 배정 스케줄러·챌린지 동의 흐름이 만든다.
--      UNIQUE KEY uk_umi_user_date (user_id, assign_date) 와 부딪히지 않게 한다.
-- ---------------------------------------------------------------------
INSERT INTO tbl_user_mission_info
  (user_id, mission_id, difficulty_id, assign_date,
   target_rate, target_value, base_amount, result, evaluated_at, created_at)
WITH RECURSIVE
days (d) AS (
  SELECT 1 UNION ALL SELECT d + 1 FROM days WHERE d < @demo_history_days
),
demo AS (
  SELECT u.id AS user_id, u.difficulty_id,
         ROW_NUMBER() OVER (ORDER BY u.id) AS seq
  FROM tbl_user u
  WHERE u.social_provider = 'DEMO' AND u.status = 'ACTIVE'
),
pool AS (
  SELECT mp.id,
         ROW_NUMBER() OVER (ORDER BY mp.id) - 1 AS rn,
         COUNT(*)    OVER ()                    AS cnt
  FROM tbl_mission_pool mp
  WHERE mp.mission_type = 'RELATIVE'
),
plan AS (
  SELECT demo.user_id, demo.difficulty_id, demo.seq, days.d,
         DATE_SUB(CURDATE(), INTERVAL days.d DAY) AS assign_date,
         30000 + ((demo.seq * 7 + days.d * 3) % 40) * 1000 AS base_amount,
         df.min_reduction_rate
           + ((demo.seq * 3 + days.d)
              % (CAST(df.max_reduction_rate - df.min_reduction_rate AS SIGNED) + 1)) AS target_rate,
         CASE WHEN ((demo.seq * 31 + days.d * 17) % 100) < (40 + ((demo.seq * 7) % 55))
              THEN 'SUCCESS' ELSE 'FAIL' END AS result
  FROM demo
  JOIN days
  JOIN tbl_mission_difficulty df ON df.id = demo.difficulty_id
)
SELECT plan.user_id,
       pool.id,
       plan.difficulty_id,
       plan.assign_date,
       plan.target_rate,
       ROUND(plan.base_amount * (100 - plan.target_rate) / 100, 2),
       plan.base_amount,
       plan.result,
       TIMESTAMP(DATE_ADD(plan.assign_date, INTERVAL 1 DAY), '00:10:00'),
       TIMESTAMP(plan.assign_date, '06:00:00')
FROM plan
JOIN pool ON pool.rn = ((plan.seq * 13 + plan.d * 7) % pool.cnt)
WHERE @seed_ready = 1;

-- ---------------------------------------------------------------------
-- 6) 연속 성공일 — 위에서 넣은 이력에서 역산
--
--    #186 이 만든 증분 갱신(MissionEvaluationMapper.xml)의 의미에 맞춘다.
--      SUCCESS → streak_count + 1, longest = GREATEST(longest, streak_count + 1)
--      FAIL    → streak_count = 0
--    즉 streak_count 는 **가장 최근 날짜부터 거꾸로 이어지는 연속 SUCCESS 수**,
--    longest_streak_count 는 **전 구간 최장 연속 SUCCESS 수**다.
--
--    날짜가 하루도 빠짐없이 이어지므로 행 번호 차이로 연속 구간을 자를 수 있다
--    (gaps and islands). 판정 배치가 이 값 위에 이어서 증분해도 결과가 맞는다.
-- ---------------------------------------------------------------------
INSERT INTO tbl_streak_count
  (user_id, streak_count, longest_streak_count, last_check_date)
WITH hist AS (
  SELECT i.user_id, i.assign_date, i.result,
         ROW_NUMBER() OVER (PARTITION BY i.user_id            ORDER BY i.assign_date)
       - ROW_NUMBER() OVER (PARTITION BY i.user_id, i.result  ORDER BY i.assign_date) AS island
  FROM tbl_user_mission_info i
  JOIN tbl_user u ON u.id = i.user_id AND u.social_provider = 'DEMO'
),
islands AS (
  SELECT user_id, island, COUNT(*) AS len, MAX(assign_date) AS last_date
  FROM hist WHERE result = 'SUCCESS'
  GROUP BY user_id, island
),
latest AS (
  SELECT user_id, MAX(assign_date) AS d FROM hist GROUP BY user_id
)
SELECT ld.user_id,
       -- 마지막 날이 SUCCESS 구간에 속할 때만 현재 연속일이 살아 있다
       COALESCE((SELECT s.len FROM islands s
                  WHERE s.user_id = ld.user_id AND s.last_date = ld.d), 0),
       COALESCE((SELECT MAX(s.len) FROM islands s WHERE s.user_id = ld.user_id), 0),
       TIMESTAMP(ld.d, '23:59:00')
FROM latest ld
WHERE @seed_ready = 1
ON DUPLICATE KEY UPDATE
  streak_count         = VALUES(streak_count),
  longest_streak_count = VALUES(longest_streak_count),
  last_check_date      = VALUES(last_check_date);

-- ---------------------------------------------------------------------
-- 7) 확인
-- ---------------------------------------------------------------------

-- ① 가상 사용자 종합 — 점수·연속일·소비총액이 사용자마다 달라야 정상이다
--    점수 계산식은 #194 이슈와 같다: SUCCESS 미션의 배정 당시 난이도 점수 합
SELECT u.id                                        AS user_id,
       u.nickname                                  AS 표시명,
       df.difficulty_name                          AS 난이도,
       COUNT(i.id)                                 AS 미션수,
       SUM(i.result = 'SUCCESS')                   AS 성공,
       ROUND(100 * SUM(i.result = 'SUCCESS') / NULLIF(COUNT(i.id), 0), 1) AS 성공률,
       SUM(CASE WHEN i.result = 'SUCCESS' THEN idf.score ELSE 0 END)      AS 예상총점,
       sc.streak_count                             AS 현재연속,
       sc.longest_streak_count                     AS 최장연속,
       (SELECT COALESCE(SUM(t.amount), 0) FROM tbl_transaction t
         WHERE t.user_id = u.id AND t.classification = 'CONSUMPTION'
           AND t.is_refund = 0 AND t.is_excluded_from_summary = 0)        AS 소비총액
FROM tbl_user u
JOIN tbl_mission_difficulty df ON df.id = u.difficulty_id
LEFT JOIN tbl_user_mission_info i    ON i.user_id = u.id
LEFT JOIN tbl_mission_difficulty idf ON idf.id = i.difficulty_id
LEFT JOIN tbl_streak_count sc        ON sc.user_id = u.id
WHERE u.social_provider = 'DEMO'
GROUP BY u.id, u.nickname, df.difficulty_name, sc.streak_count, sc.longest_streak_count
ORDER BY 예상총점 DESC;

-- ② 월별 랭킹 미리보기 — #194 배치가 만들 결과와 같아야 한다
--    실제 사용자도 함께 나온다(미션 이력이 있는 사람만).
--    ⚠ 이력이 어제부터 60일치라 **월초에 실행하면 이번 달 행이 몇 개 안 된다.**
--      그래서 지난달까지 두 달을 함께 본다. 이번 달이 비어 보여도 오류가 아니다.
SELECT DATE_FORMAT(i.assign_date, '%Y-%m')  AS 연월,
       RANK() OVER (PARTITION BY DATE_FORMAT(i.assign_date, '%Y-%m')
                    ORDER BY SUM(df.score) DESC) AS 순위,
       u.id                                 AS user_id,
       u.social_provider                    AS 구분,
       COALESCE(u.nickname, u.social_name)  AS 표시명,
       COUNT(*)                             AS 성공일,
       SUM(df.score)                        AS 총점
FROM tbl_user_mission_info i
JOIN tbl_user u                ON u.id  = i.user_id
JOIN tbl_mission_difficulty df ON df.id = i.difficulty_id
WHERE i.result = 'SUCCESS'
  AND i.assign_date >= DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 1 MONTH), '%Y-%m-01')
GROUP BY 연월, u.id, u.social_provider, 표시명
ORDER BY 연월 DESC, 총점 DESC;

-- ③ 총 인원 — 랭킹 모수 확인
SELECT social_provider AS 구분, COUNT(*) AS 인원
FROM tbl_user WHERE status = 'ACTIVE'
GROUP BY social_provider;
