-- =====================================================================
-- k6 부하테스트용 대량 데이터 생성 — 로컬 전용 (이슈 #188)
--
-- ⚠ 로컬 DB 에서만 실행한다. EC2·공용 DB 에 절대 넣지 말 것.
--    시연 화면에 「부하유저42」 같은 계정이 섞여 나온다.
--
-- 생성물은 전부 provider_user_id LIKE 'LOADTEST-%' 로 식별된다.
-- 되돌리려면 cleanup_loadtest_local.sql 을 실행한다.
--
-- 실행 (PowerShell):
--   mysql -u tangtang -p tangtang --default-character-set=utf8mb4 `
--     -e "source D:/KB_Final_Project/app/Monorepo/perf/seed_loadtest_local.sql"
-- =====================================================================

-- ── 규모 ── 기본값 = 사용자 50명 × 거래 1,000건 = 50,000건
SET @user_count  = 50;
SET @tx_per_user = 1000;

-- 재귀 CTE 기본 상한이 1000 이라 tx_per_user 를 늘리면 여기서 먼저 막힌다
SET SESSION cte_max_recursion_depth = 1000000;

-- ---------------------------------------------------------------------
-- 1. 사용자
--    difficulty_id 는 NOT NULL FK 다. 가입 시 EASY 를 넣는 규칙을 그대로 따른다.
--
--    ⚠ created_at 을 과거로 박는 것이 핵심이다. 기본값(오늘)으로 두면 가입 첫 달 정책
--      (DECISIONS.md 2026-08-12)에 걸려 리포트가 REPORT_NOT_AVAILABLE 로 막히고,
--      조회 가능한 월이 하나도 없어 부하테스트가 400 만 받는다.
--      13개월 전으로 잡아 지난 열두 달이 모두 「완료된 월」이 되게 한다.
-- ---------------------------------------------------------------------
INSERT INTO tbl_user (social_provider, provider_user_id, email, nickname, name, status,
                      difficulty_id, created_at)
WITH RECURSIVE seq(n) AS (
    SELECT 0 UNION ALL SELECT n + 1 FROM seq WHERE n < @user_count - 1
)
SELECT 'GOOGLE',
       CONCAT('LOADTEST-', LPAD(n, 4, '0')),
       CONCAT('loadtest', n, '@example.invalid'),
       CONCAT('부하유저', n),
       CONCAT('부하유저', n),
       'ACTIVE',
       (SELECT MIN(id) FROM tbl_mission_difficulty),
       DATE_SUB(NOW(), INTERVAL 13 MONTH)
FROM seq;

-- ---------------------------------------------------------------------
-- 2. 연동 계좌 — 사용자당 1개. 거래의 account_id FK 를 채우기 위한 것이다.
--    account_no_encrypted 는 (user_id, account_no_encrypted) UNIQUE 를 타므로 유일해야 한다.
-- ---------------------------------------------------------------------
INSERT INTO tbl_connected_account
    (user_id, bank_code, bank_name, account_name, account_no_encrypted, account_no_masked,
     account_type, balance, sync_status, is_active)
SELECT u.id, '004', '국민은행', '부하테스트 입출금',
       CONCAT('LOADTEST-ENC-', u.id),
       CONCAT('110-***-', LPAD(u.id % 10000, 4, '0')),
       '입출금', 1000000, 'NORMAL', 1
FROM tbl_user u
WHERE u.provider_user_id LIKE 'LOADTEST-%';

-- ---------------------------------------------------------------------
-- 3. 거래내역
--
--    월간 리포트 쿼리(MonthlyReportMapper.reportConsumptionCondition)가 요구하는 조건에 맞춘다:
--      classification = 'CONSUMPTION' AND is_excluded_from_summary = 0
--    이 조건을 벗어나면 리포트가 빈 결과를 돌려줘 부하테스트가 무의미해진다.
--
--    tr_date 는 최근 365일에 고르게 흩는다 — 월별 GROUP BY 와 6개월 추이가 모두 데이터를 만나야 한다.
--    금액·가맹점·카테고리는 난수가 아니라 seq 기반 결정식이다. 몇 번을 다시 만들어도 같은 데이터가 나와야
--    측정값을 비교할 수 있다.
-- ---------------------------------------------------------------------

-- 카테고리에 0-based 연속번호를 붙인다. 모듈러로 순환시키기 위한 것.
-- (MySQL 은 한 쿼리에서 같은 TEMPORARY 테이블을 두 번 참조하지 못해 개수는 변수에 따로 담는다)
DROP TEMPORARY TABLE IF EXISTS tmp_loadtest_cat;
CREATE TEMPORARY TABLE tmp_loadtest_cat AS
SELECT id, ROW_NUMBER() OVER (ORDER BY id) - 1 AS rn FROM tbl_category;
SET @cat_count = (SELECT COUNT(*) FROM tbl_category);

DROP TEMPORARY TABLE IF EXISTS tmp_loadtest_acct;
CREATE TEMPORARY TABLE tmp_loadtest_acct AS
SELECT ca.id AS account_id, ca.user_id
FROM tbl_connected_account ca
JOIN tbl_user u ON u.id = ca.user_id
WHERE u.provider_user_id LIKE 'LOADTEST-%';

INSERT INTO tbl_transaction
    (user_id, account_id, codef_tr_key, merchant_name, merchant_name_normalized,
     amount, direction, tr_date, tr_time, classification, category_id,
     is_excluded_from_summary, is_refund)
WITH RECURSIVE seq(n) AS (
    SELECT 0 UNION ALL SELECT n + 1 FROM seq WHERE n < @tx_per_user - 1
)
SELECT a.user_id,
       a.account_id,
       CONCAT('LOADTEST-', a.user_id, '-', s.n),
       m.name,
       m.name,
       1000 + (s.n * 137) % 99000,
       'OUT',
       DATE_SUB(CURDATE(), INTERVAL (s.n * 7 + a.user_id) % 365 DAY),
       SEC_TO_TIME((s.n * 61) % 86400),
       'CONSUMPTION',
       c.id,
       0,
       0
FROM tmp_loadtest_acct a
CROSS JOIN seq s
JOIN tmp_loadtest_cat c ON c.rn = s.n % @cat_count
JOIN (
    SELECT 0 AS i, '스타벅스'   AS name UNION ALL SELECT 1, 'GS25'      UNION ALL
    SELECT 2, '배달의민족'      UNION ALL SELECT 3, '쿠팡'             UNION ALL
    SELECT 4, '올리브영'        UNION ALL SELECT 5, 'CU'               UNION ALL
    SELECT 6, '넷플릭스'        UNION ALL SELECT 7, '카카오T'
) m ON m.i = s.n % 8;

DROP TEMPORARY TABLE IF EXISTS tmp_loadtest_cat;
DROP TEMPORARY TABLE IF EXISTS tmp_loadtest_acct;

-- ---------------------------------------------------------------------
-- 4. 결과 확인
-- ---------------------------------------------------------------------
SELECT (SELECT COUNT(*) FROM tbl_user WHERE provider_user_id LIKE 'LOADTEST-%') AS 생성_사용자,
       (SELECT MIN(id)  FROM tbl_user WHERE provider_user_id LIKE 'LOADTEST-%') AS 시작_user_id,
       (SELECT MAX(id)  FROM tbl_user WHERE provider_user_id LIKE 'LOADTEST-%') AS 끝_user_id,
       (SELECT COUNT(*) FROM tbl_transaction WHERE codef_tr_key LIKE 'LOADTEST-%') AS 생성_거래;
