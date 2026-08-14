-- =====================================================================
-- db/seed_local_demo.sql — 로컬 시연·검증용 데이터 한 방 시드 (이슈 #191 · #195)
--
--   ⚠ 로컬 전용이다. 운영·공용 DB 에서 절대 실행하지 않는다.
--
-- ---------------------------------------------------------------------
-- 이 파일 하나가 하는 일
-- ---------------------------------------------------------------------
--   ① 가상 사용자(DEMO) 20명 — 랭킹 모수. 미션 판정 이력·연속 성공일까지 채운다
--   ② 실계정(구글 로그인 사용자) 백필 — 동의·계좌·거래·미션 이력을 넣어
--      로그인 직후 온보딩 게이트를 통과한 상태로 만든다
--   ③ 사용자별 편차가 있는 7개월치 거래내역
--   ④ **오늘의 미션(PENDING)** — 없으면 홈에서 400 이 난다 (아래 5-2)
--   ⑤ 그룹 챌린지 2개 (진행중 1 · 모집중 1) + 일별 소비 집계
--   ⑥ (선택) 월간 랭킹 tbl_monthly_ranking 미리 채우기
--
--   기존 db/seed_demo_transactions.sql 의 상위 호환이다(그 파일은 거래내역만 넣는다).
--   ⚠ 이 파일을 돌린 **뒤에** seed_demo_transactions.sql 을 돌리지 말 것.
--     둘 다 DUMMY-% 키를 쓰지만 형식이 다르다 — 구버전은 {개월수}, 이 파일은 {YYYYMM}.
--     뒤에 돌리면 서로 다른 거래로 인식돼 같은 거래가 두 벌 쌓인다.
--     꼬였으면 DELETE FROM tbl_transaction WHERE codef_tr_key LIKE 'DUMMY-%'; 후 이 파일만 재실행.
--
-- ---------------------------------------------------------------------
-- 구글 로그인 계정은 어떻게 붙나  ★ 먼저 읽을 것
-- ---------------------------------------------------------------------
--   로그인 매칭 키는 (social_provider, provider_user_id) 이고 provider_user_id 는
--   구글 sub 다 (AuthService.java:62 · UserMapper.xml:16). sub 는 사람마다 달라서
--   SQL 에 박아 둘 수가 없다. 박아 두면 그 구글 계정을 가진 사람만 쓸 수 있다.
--
--   그래서 이 파일은 **실행 시점에 존재하는 실계정을 전부 찾아 백필**한다.
--   판정 기준은 social_provider <> 'DEMO' AND status = 'ACTIVE' 다.
--
--   ⇒ 사용 순서
--       1. 이 파일을 실행한다            (가상 사용자·그룹챌린지가 생긴다)
--       2. 구글 계정 ①로 로그인한다      (tbl_user 에 행이 생긴다. 아직 온보딩 화면)
--       3. 구글 계정 ②로 로그인한다      (다른 브라우저 프로필 / 시크릿 창)
--       4. **이 파일을 한 번 더 실행한다** ← 여기서 두 계정에 데이터가 붙는다
--       5. 새로고침하면 홈·챌린지·리포트가 전부 채워져 있다
--
--   몇 번을 다시 돌려도 같은 상태가 된다(멱등). 팀원은 각자 자기 구글 계정으로
--   위 절차를 그대로 밟으면 된다. 파일을 고칠 필요가 없다.
--
--   ★ 4번을 빼먹으면 홈에서 GET /api/missions/today 가 400 을 낸다.
--     미션 배정 스케줄러는 cron 시각과 앱 기동 시점에만 돈다
--     (RelativeMissionAssignmentScheduler.java:30-45). 백엔드가 떠 있는 동안 로그인해
--     새로 생긴 사용자는 오늘치 미션 행이 없다. 이 파일이 5-2 에서 그 행을 채워 주므로
--     **백엔드를 재기동할 필요가 없다.** 날이 바뀌었을 때도 다시 한 번 실행하면 된다.
--
-- ---------------------------------------------------------------------
-- 실행
-- ---------------------------------------------------------------------
--   mysql -u tangtang -p tangtang --default-character-set=utf8mb4 < db/seed_local_demo.sql
--
--   PowerShell 은 < 리다이렉션이 안 된다. source 를 쓴다(경로는 슬래시).
--   mysql -u tangtang -p tangtang --default-character-set=utf8mb4 `
--     -e "source D:/…/db/seed_local_demo.sql"
--
-- ---------------------------------------------------------------------
-- 선행 조건 — 하나라도 비면 아무것도 넣지 않는다
-- ---------------------------------------------------------------------
--   db/seed.sql            난이도 3종. tbl_user.difficulty_id 가 NOT NULL FK 라 필수
--   db/seed_category.sql   카테고리. 아래 거래 템플릿이 이름으로 참조한다
--   db/seed_mission_pool.sql  RELATIVE 미션 풀. 비면 미션 이력을 만들 수 없다
--
--   맨 위 확인 쿼리의 seed_ready 가 0 이면 여기서 멈추고 위 세 파일을 먼저 넣는다.
--   ★ seed_ready 가 0 이어도 SQL 오류는 안 난다. 조용히 0행이 들어간다. 반드시 눈으로 본다.
--
-- ---------------------------------------------------------------------
-- 되돌리기
-- ---------------------------------------------------------------------
--   DELETE FROM tbl_challenge_group  WHERE invite_code LIKE 'SEED%';
--   DELETE FROM tbl_user             WHERE social_provider = 'DEMO';     -- 21개 테이블 CASCADE
--   DELETE FROM tbl_transaction      WHERE codef_tr_key LIKE 'DUMMY-%';
--   DELETE FROM tbl_user_mission_info WHERE assignment_reason = 'LOCAL_SEED';
--   DELETE FROM tbl_connected_account WHERE account_no_encrypted LIKE 'SEED-ACCOUNT-%';
--   -- 온보딩 화면을 다시 보고 싶으면
--   UPDATE tbl_user SET tutorial_seen_at = NULL, group_tutorial_seen_at = NULL
--    WHERE social_provider <> 'DEMO';
--
--   실계정 자체는 지우지 않는다. 실 CODEF 거래(codef_tr_key 가 DUMMY- 가 아닌 것)와
--   seed_transaction_mock_local.sql 의 MOCK-LOCAL-% 거래도 건드리지 않는다.
-- =====================================================================

USE tangtang;
SET NAMES utf8mb4;

-- =====================================================================
-- 0. 설정값 — 여기만 고쳐 쓴다
-- =====================================================================

SET @demo_user_count = 20;   -- 가상 사용자 수. 랭킹 모수
SET @history_days    = 60;   -- 미션 판정 이력 일수 (어제부터 과거로)

-- 거래를 오늘로부터 며칠 뒤까지 만들지.
--   0 = 오늘까지 (기본). 리포트·홈의 "이번 달 소비"에 미래 금액이 섞이지 않는다.
--   7 = 일주일 뒤까지. 오늘 시작하는 그룹 챌린지의 2~7일 차를 미리 채워야 할 때만 쓴다.
-- ⚠ 아래 거래 템플릿이 만드는 범위는 **다음 달 말일까지**다. 그보다 크게 잡아도 안 늘어난다.
SET @future_days = 0;

-- tbl_monthly_ranking 을 채울지. **1 로 둔다** — 자세한 이유는 8절 머리말.
-- 요약: #194 는 미션 판정 이벤트에서만 돌고 total_score 만 쓴다. 시드 이력엔 안 돈다.
SET @seed_monthly_ranking = 1;

-- ---------------------------------------------------------------------
-- 선행 조건 검사
-- ---------------------------------------------------------------------
SET @difficulty_count    = (SELECT COUNT(*) FROM tbl_mission_difficulty);
SET @category_count      = (SELECT COUNT(*) FROM tbl_category);
SET @relative_pool_count = (SELECT COUNT(*) FROM tbl_mission_pool WHERE mission_type = 'RELATIVE');

SET @seed_ready = (@difficulty_count >= 3 AND @category_count > 0 AND @relative_pool_count > 0);

SELECT @seed_ready         AS seed_ready,           -- ★ 1 이 아니면 아래가 전부 0행이다
       @difficulty_count   AS difficulty_count,     -- 3 이상 (db/seed.sql)
       @category_count     AS category_count,       -- 1 이상 (db/seed_category.sql)
       @relative_pool_count AS relative_pool_count; -- 1 이상 (db/seed_mission_pool.sql)

-- 백필 대상 실계정이 몇 명인지 먼저 보여준다. 0 이면 아직 구글 로그인을 안 한 것이다.
SELECT COUNT(*) AS 백필대상_실계정수
FROM tbl_user WHERE status = 'ACTIVE' AND social_provider <> 'DEMO';


-- =====================================================================
-- 1. 초기화 — 이 파일이 만든 것만 지운다 (재실행 멱등)
--
--    순서에 의미가 있다.
--      · 그룹을 먼저 지운다 — tbl_challenge_group.admin_id 만 ON DELETE 절이 없어서
--        (schema.sql:428) 가상 사용자가 방장이면 사용자 삭제가 FK 로 막힌다.
--      · 그 다음 DEMO 사용자. 계좌·거래·미션이력·streak 이 CASCADE 로 함께 사라진다.
--      · 마지막에 남은 DUMMY 거래 = 실계정 것. 환불 행을 먼저 지운다
--        (original_transaction_id 자기참조 FK. SET NULL 이라 오류는 안 나지만
--         원거래를 잃은 환불 행이 잠깐 생기는 것을 피한다)
-- =====================================================================

DELETE FROM tbl_challenge_group   WHERE invite_code LIKE 'SEED%';
DELETE FROM tbl_user              WHERE social_provider = 'DEMO';

DELETE FROM tbl_transaction       WHERE codef_tr_key LIKE 'DUMMY-%' AND is_refund = 1;
DELETE FROM tbl_transaction       WHERE codef_tr_key LIKE 'DUMMY-%';

-- 실계정에 넣었던 미션 이력. assignment_reason 을 표식으로 쓴다 —
-- 이 컬럼은 오늘 미션 화면에서만 읽히는데(TodayMissionService.java:44) 여기서는
-- 어제 이전 날짜에만 쓰므로 화면에 노출되지 않는다. 실제 배정 이력은 이 값이 NULL 이라
-- 함께 지워지지 않는다.
DELETE FROM tbl_user_mission_info WHERE assignment_reason = 'LOCAL_SEED';

-- 연동 계좌는 남긴다. 지우면 account_id 가 매번 바뀌어 비교가 어렵다.
-- 아래 3-3 이 없을 때만 새로 만든다.


-- =====================================================================
-- 2. 가상 사용자 20명 (DEMO)
--
--    social_provider = 'DEMO' 라 UNIQUE KEY uk_user_social 상 구글 로그인 조회에
--    절대 걸리지 않는다. 실계정과 섞이지 않는다.
--
--    difficulty_id 를 1(EASY) · 2(NORMAL) · 3(HARD) 로 돌려 배정한다. 난이도별 점수가
--    10 · 20 · 35 로 달라(db/seed.sql:13-16) 총점 편차의 한 축이 된다.
--
--    ⚠ 챌린지 동의(tbl_user_consent CHALLENGE)를 **일부러 넣지 않는다.**
--      배정 배치의 대상 조회(RelativeMissionAssignmentMapper.xml:6)는 status='ACTIVE' +
--      CHALLENGE 동의 보유 사용자를 전부 긁는다. social_provider 를 보지 않는다.
--      동의를 넣으면 가상 사용자도 매일 배정 대상이 되는데, 이들은 오늘 이후 거래가 없어
--      지출 0 → 목표 이하 → **매일 무조건 SUCCESS** 다. 하루 지날 때마다 20명이 나란히
--      점수를 벌어 아래에서 만든 편차가 무너지고 랭킹 상위를 전부 차지한다.
--      동의를 빼면 배치가 이들을 건너뛰고 아래 이력이 그대로 고정된다.
--      ⇒ 실계정만 매일 점수가 늘고, 가상 사용자는 고정된 비교군으로 남는다. 이게 의도다.
-- =====================================================================

INSERT INTO tbl_user
  (social_provider, provider_user_id, email, nickname, social_name, name,
   status, difficulty_id, relative_mission_qualified_at,
   tutorial_seen_at, group_tutorial_seen_at, created_at)
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
       DATE_SUB(NOW(), INTERVAL @history_days DAY),
       DATE_SUB(NOW(), INTERVAL @history_days DAY),
       DATE_SUB(NOW(), INTERVAL @history_days DAY),
       DATE_SUB(NOW(), INTERVAL (@history_days + 30) DAY)
FROM seq
WHERE @seed_ready = 1;

-- 가상 사용자 연동 계좌 — 사용자당 1개.
-- 거래의 account_id 가 NOT NULL 이라 계좌 없이는 거래를 넣을 수 없다.
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
       DATE_SUB(NOW(), INTERVAL @history_days DAY)
FROM tbl_user u
WHERE u.social_provider = 'DEMO'
  AND @seed_ready = 1;


-- =====================================================================
-- 3. 실계정 백필 — 로그인 직후 온보딩 게이트를 통과한 상태로 만든다
--
--    로그인 응답이 게이트 3개를 계산한다 (AuthService.java:120-122).
--      needsConsent          SIGNUP 필수 동의  = TERMS · PRIVACY · FINANCIAL_DATA
--      needsFinancialConsent FINANCIAL 필수 동의 = THIRD_PARTY
--      needsAccountLink      활성 tbl_connected_account 유무
--    셋을 다 채워야 계좌연동 화면을 밟지 않고 바로 홈으로 들어간다.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 3-1. 동의 7종
--
--   is_required · terms_version 은 서버 카탈로그 값을 그대로 쓴다
--   (ConsentType.java · application.properties:38 consent.terms-version=v1.0).
--   FINANCIAL_DATA · THIRD_PARTY 만 만료가 있다(마이데이터 1년 재동의).
--   CHALLENGE 는 선택 동의지만 이게 있어야 미션 배정 배치가 실계정을 대상으로 잡는다.
-- ---------------------------------------------------------------------
INSERT INTO tbl_user_consent
  (user_id, consent_type, is_required, terms_version, status, withdrawn_at, expires_at)
SELECT u.id, c.consent_type, c.is_required, 'v1.0', 1, NULL,
       CASE WHEN c.consent_type IN ('FINANCIAL_DATA', 'THIRD_PARTY')
            THEN DATE_ADD(NOW(), INTERVAL 1 YEAR) END
FROM tbl_user u
JOIN (
  SELECT 'TERMS'          AS consent_type, 1 AS is_required UNION ALL
  SELECT 'PRIVACY',          1 UNION ALL
  SELECT 'FINANCIAL_DATA',   1 UNION ALL
  SELECT 'THIRD_PARTY',      1 UNION ALL
  SELECT 'AI_USAGE',         0 UNION ALL
  SELECT 'MARKETING',        0 UNION ALL
  SELECT 'CHALLENGE',        0
) c
WHERE u.status = 'ACTIVE'
  AND u.social_provider <> 'DEMO'
  AND @seed_ready = 1
ON DUPLICATE KEY UPDATE
  status        = 1,
  withdrawn_at  = NULL,
  terms_version = VALUES(terms_version),
  expires_at    = VALUES(expires_at);

-- ---------------------------------------------------------------------
-- 3-2. 표시명·튜토리얼·상대미션 자격
--
--   nickname 이 NULL 이면 "온보딩 미완" 신호다(UserMapper.xml:28-29). 그룹 참여자
--   목록이 nickname 을 그대로 읽으므로(GroupMemberMapper.xml:29) 비어 있으면 이름
--   없는 멤버가 보인다. 비어 있을 때만 채운다 — 직접 정한 닉네임은 덮지 않는다.
--
--   relative_mission_qualified_at 은 상대 미션 자격(28일·50건) 취득 시각이다.
--   과거로 박아 "이미 자격을 얻은 사용자" 로 만든다.
-- ---------------------------------------------------------------------
UPDATE tbl_user u
   SET u.nickname = COALESCE(u.nickname, u.social_name, CONCAT('재판관', LPAD(u.id, 2, '0'))),
       u.tutorial_seen_at = COALESCE(u.tutorial_seen_at,
                                     DATE_SUB(NOW(), INTERVAL @history_days DAY)),
       u.group_tutorial_seen_at = COALESCE(u.group_tutorial_seen_at,
                                     DATE_SUB(NOW(), INTERVAL @history_days DAY)),
       u.relative_mission_qualified_at = COALESCE(u.relative_mission_qualified_at,
                                     DATE_SUB(NOW(), INTERVAL @history_days DAY))
 WHERE u.status = 'ACTIVE'
   AND u.social_provider <> 'DEMO'
   AND @seed_ready = 1;

-- ---------------------------------------------------------------------
-- 3-3. 연동 계좌 — 활성 계좌가 하나도 없을 때만 만든다
--
--   CODEF/mock 연동 플로우를 다 밟은 사람의 실제 계좌는 건드리지 않는다.
--   account_no_encrypted 는 UNIQUE KEY uk_ca_user_account 의 일부다. 실제 연동 계좌의
--   HMAC 값과 형식이 달라 부딪히지 않는다.
-- ---------------------------------------------------------------------
INSERT INTO tbl_connected_account
  (user_id, codef_connected_id, bank_code, bank_name, account_name,
   account_no_encrypted, account_no_masked, account_type, deposit_type_code,
   balance, sync_status, last_sync_at, is_active, created_at)
SELECT u.id,
       CONCAT('SEED-CONNECTED-', LPAD(u.id, 5, '0')),
       '004', 'KB국민은행', 'KB나라사랑통장',
       CONCAT('SEED-ACCOUNT-', LPAD(u.id, 5, '0')),
       CONCAT('110-***-****', LPAD(u.id % 100, 2, '0')),
       'DEMAND_DEPOSIT', '11',
       2500000 + (u.id * 211) % 3000000,
       'NORMAL', NOW(), 1,
       DATE_SUB(NOW(), INTERVAL @history_days DAY)
FROM tbl_user u
WHERE u.status = 'ACTIVE'
  AND u.social_provider <> 'DEMO'
  AND @seed_ready = 1
  AND NOT EXISTS (
        SELECT 1 FROM tbl_connected_account ca
         WHERE ca.user_id = u.id AND ca.is_active = 1
      );


-- =====================================================================
-- 4. 거래내역 — 가상 사용자 · 실계정 공통
--
--    사용자별 편차 세 축 (이슈 #191)
--      · 변동 지출 배율  70 + ((u*5)%13)*10   → 70~190%
--      · 고정 지출 배율  85 + ((u*3)%7)*5     → 85~115%
--      · 결제일 이동    ((dom-1 + (u*11)%28) % 28) + 1  → 항상 1~28일
--
--    ⚠ 계수를 바꾸지 말 것. 아래 성질이 깨진다.
--      · 고정 지출 배율에 m 이 없다 → 매월 같은 금액 → 「동일 가맹점·동일 금액 3회」
--        고정지출 탐지 룰(db/seed.sql:18)이 유지된다. m 을 넣으면 탐지가 0건이 된다.
--      · % 28 이라 항상 1~28일 → 2월 같은 짧은 달 보정이 필요 없다.
--        gcd(11,28)=1 이라 사용자마다 다른 날로 흩어진다.
--      · 지터의 (m + 12) 는 MySQL 의 % 가 피제수 부호를 따르기 때문이다. 다음 달(m=-1)에서
--        지터가 음수가 되면 금액이 음수로 들어간다 — tbl_transaction.amount 에 CHECK 가
--        없어 오류도 안 난다.
--
--    codef_tr_key 에는 상대 개월수가 아니라 **거래 연월(YYYYMM)** 을 쓴다. 상대 개월수를
--    쓰면 8월 실행의 DUMMY-1-0-17(8/15)과 9월 실행의 DUMMY-1-1-17(8/15)이 서로 다른 키가
--    돼 같은 거래가 두 번 들어간다.
-- =====================================================================

SET @seed_upto = DATE_ADD(CURDATE(), INTERVAL @future_days DAY);

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
  -- 가상 사용자와 실계정을 함께 대상으로 잡는다. 실계정에 실제 연동 계좌가 있으면
  -- 그 계좌에 붙는다(입출금 계좌 우선).
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
  AND @seed_ready = 1
  AND (gen.cat_name IS NULL OR c.id IS NOT NULL)
  AND NOT EXISTS (
        SELECT 1 FROM tbl_transaction t
         WHERE t.codef_tr_key = CONCAT('DUMMY-', gen.user_id, '-',
                                       DATE_FORMAT(gen.tr_date, '%Y%m'), '-', gen.tno)
      );

-- 집계 제외 거래 (가승인 후 즉시취소) — is_excluded_from_summary 처리를 화면에서 보기 위한 것
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
WHERE @seed_ready = 1
  AND NOT EXISTS (
        SELECT 1 FROM tbl_transaction x
         WHERE x.codef_tr_key = CONCAT('DUMMY-', t.user_id, '-EXCLUDED'));

-- 환불 거래 (가장 최근 무신사 결제 전액 환불) — is_refund 처리 확인용
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
  SELECT t.*, ROW_NUMBER() OVER (PARTITION BY t.user_id
                                 ORDER BY t.tr_date DESC, t.id DESC) AS rn
  FROM tbl_transaction t
  WHERE t.codef_tr_key LIKE 'DUMMY-%'
    AND t.merchant_name = '무신사'
    AND t.is_refund = 0
    AND t.tr_date <= @seed_upto
) o
WHERE o.rn = 1
  AND @seed_ready = 1
  AND NOT EXISTS (
        SELECT 1 FROM tbl_transaction x
         WHERE x.codef_tr_key = CONCAT('DUMMY-', o.user_id, '-REFUND'));


-- =====================================================================
-- 5. 미션 판정 이력 — 어제부터 과거로 @history_days 일치
--
--    ★ 랭킹(#194)의 실제 입력은 거래가 아니라 이 표다.
--        거래 → 분석 스냅샷 → 미션 배정 → 일별 판정(SUCCESS/FAIL) → streak → 점수 → 순위
--      배정 스케줄러는 오늘치만 만들고 판정 배치는 어제치만 평가한다
--      (RelativeMissionAssignmentScheduler.java:47). 과거를 소급하지 않는다.
--      그래서 여기서 직접 주입한다.
--
--    성공률
--      가상 사용자  40 + ((seq*7)%55)   → 40~94%  (폭넓게 흩어 랭킹 곡선을 만든다)
--      실계정       62 + ((seq*5)%13)   → 62~74%  (중상위권. 내 순위가 가운데쯤 보인다)
--    판정  ((seq*31 + d*17) % 100) < 성공률 이면 SUCCESS. gcd(17,100)=1 이라
--          d 가 1~@history_days 도는 동안 서로 다른 값이 나온다.
--
--    난이도는 사용자의 difficulty_id 를 그대로 박는다. 실제 배정도 배정 시점 난이도를
--    tbl_user_mission_info.difficulty_id 에 저장하고 #194 가 이 값으로 점수를 낸다.
--
--    ⚠ 여기서는 어제까지만 넣는다. 오늘치는 바로 아래 5-2 가 따로 넣는다
--      (판정 이력과 진행중 미션은 result 가 다르다).
--    ⚠ 이미 행이 있는 날짜는 건너뛴다. 실제 배정 이력을 덮지 않는다.
--      UNIQUE KEY uk_umi_user_date (user_id, assign_date) 와 부딪히지 않게 한다.
-- =====================================================================

INSERT INTO tbl_user_mission_info
  (user_id, mission_id, difficulty_id, assign_date,
   target_rate, target_value, base_amount, result, assignment_reason,
   evaluated_at, created_at)
WITH RECURSIVE
days (d) AS (
  SELECT 1 UNION ALL SELECT d + 1 FROM days WHERE d < @history_days
),
target_user AS (
  SELECT u.id AS user_id,
         u.difficulty_id,
         (u.social_provider = 'DEMO') AS is_demo,
         ROW_NUMBER() OVER (ORDER BY u.id) AS seq
  FROM tbl_user u
  WHERE u.status = 'ACTIVE'
),
pool AS (
  SELECT mp.id,
         ROW_NUMBER() OVER (ORDER BY mp.id) - 1 AS rn,
         COUNT(*)    OVER ()                    AS cnt
  FROM tbl_mission_pool mp
  WHERE mp.mission_type = 'RELATIVE'
),
plan AS (
  SELECT tu.user_id, tu.difficulty_id, tu.seq, days.d,
         DATE_SUB(CURDATE(), INTERVAL days.d DAY) AS assign_date,
         30000 + ((tu.seq * 7 + days.d * 3) % 40) * 1000 AS base_amount,
         df.min_reduction_rate
           + ((tu.seq * 3 + days.d)
              % (CAST(df.max_reduction_rate - df.min_reduction_rate AS SIGNED) + 1)) AS target_rate,
         CASE WHEN ((tu.seq * 31 + days.d * 17) % 100)
                   < (CASE WHEN tu.is_demo = 1
                           THEN 40 + ((tu.seq * 7) % 55)
                           ELSE 62 + ((tu.seq * 5) % 13) END)
              THEN 'SUCCESS' ELSE 'FAIL' END AS result
  FROM target_user tu
  JOIN days
  JOIN tbl_mission_difficulty df ON df.id = tu.difficulty_id
)
SELECT plan.user_id,
       pool.id,
       plan.difficulty_id,
       plan.assign_date,
       plan.target_rate,
       ROUND(plan.base_amount * (100 - plan.target_rate) / 100, 2),
       plan.base_amount,
       plan.result,
       'LOCAL_SEED',
       TIMESTAMP(DATE_ADD(plan.assign_date, INTERVAL 1 DAY), '00:10:00'),
       TIMESTAMP(plan.assign_date, '06:00:00')
FROM plan
JOIN pool ON pool.rn = ((plan.seq * 13 + plan.d * 7) % pool.cnt)
WHERE @seed_ready = 1
  AND NOT EXISTS (
        SELECT 1 FROM tbl_user_mission_info x
         WHERE x.user_id = plan.user_id AND x.assign_date = plan.assign_date
      );


-- =====================================================================
-- 5-2. 오늘의 미션 (PENDING) — 「오늘의 미션」 화면이 400 을 내지 않게 한다
--
--    ★ 이걸 빼면 방금 구글 로그인한 계정에서 GET /api/missions/today 가 400 을 낸다.
--      (TodayMissionService.java:41 → TODAY_MISSION_NOT_FOUND)
--      배정 스케줄러는 cron 시각과 **앱 기동 시점**에만 돈다
--      (RelativeMissionAssignmentScheduler.java:30-45). 백엔드가 이미 떠 있는 상태에서
--      로그인해 새로 생긴 사용자는 백엔드를 재기동하기 전까지 오늘치 미션이 없다.
--      시연 중에 재기동을 요구할 수 없으므로 여기서 직접 넣는다.
--
--    ⚠ 실제 배정과 충돌하지 않는다. findUnassignedChallengeConsentedUserIds
--      (RelativeMissionAssignmentMapper.xml:19-24)가 오늘 행이 있는 사용자를 제외하므로
--      다음 배치는 이 사용자들을 그냥 건너뛴다.
--
--    ⚠ 미션은 반드시 **RELATIVE + category_id 있음 + 그 카테고리에 부모 있음** 이어야 한다.
--      TodayMissionMapper.xml:37-40 이 mission_pool → category → parent_category 를
--      INNER JOIN 으로 잇는다. 하나라도 어긋나면 조회 결과가 0행이 돼 똑같이 400 이 난다.
--
--    base_amount 는 실제 배정과 같은 식으로 뽑는다 —
--    최근 28일 중 그 카테고리에 지출이 있던 날들의 **일평균 순지출**
--    (RelativeMissionAssignmentMapper.xml:62-77). 카테고리는 그 기간 총지출이 가장 큰 것.
--    소비 이력이 전혀 없는 사용자는 합성 금액으로 대체해 화면이라도 뜨게 한다.
-- =====================================================================

INSERT INTO tbl_user_mission_info
  (user_id, mission_id, difficulty_id, assign_date,
   target_rate, target_value, base_amount, result, assignment_reason,
   evaluated_at, created_at)
WITH
target_user AS (
  SELECT u.id AS user_id,
         u.difficulty_id,
         ROW_NUMBER() OVER (ORDER BY u.id) AS seq
  FROM tbl_user u
  WHERE u.status = 'ACTIVE'
),
daily AS (
  SELECT t.user_id, t.category_id, t.tr_date,
         SUM(CASE WHEN t.is_refund = 1
                  THEN -COALESCE(t.refunded_amount, t.amount)
                  ELSE t.amount END) AS net_amount
  FROM tbl_transaction t
  JOIN target_user tu ON tu.user_id = t.user_id
  WHERE t.tr_date BETWEEN DATE_SUB(CURDATE(), INTERVAL 28 DAY)
                      AND DATE_SUB(CURDATE(), INTERVAL 1 DAY)
    AND t.classification = 'CONSUMPTION'
    AND t.direction = 'OUT'
    AND t.is_excluded_from_summary = 0
    AND t.category_id IS NOT NULL
  GROUP BY t.user_id, t.category_id, t.tr_date
  HAVING net_amount > 0
),
-- RELATIVE 미션이 실제로 있고 부모 카테고리까지 있는 카테고리만 후보로 둔다
usable_cat AS (
  SELECT d.user_id, d.category_id,
         ROUND(AVG(d.net_amount), 2) AS base_amount,
         ROW_NUMBER() OVER (PARTITION BY d.user_id
                            ORDER BY SUM(d.net_amount) DESC, d.category_id) AS rn
  FROM daily d
  JOIN tbl_category c ON c.id = d.category_id AND c.parent_id IS NOT NULL
  WHERE EXISTS (SELECT 1 FROM tbl_mission_pool mp
                 WHERE mp.category_id = d.category_id
                   AND mp.mission_type = 'RELATIVE')
  GROUP BY d.user_id, d.category_id
),
pool_cat AS (
  SELECT mp.id, mp.category_id,
         ROW_NUMBER() OVER (PARTITION BY mp.category_id ORDER BY mp.id) - 1 AS rn,
         COUNT(*)    OVER (PARTITION BY mp.category_id)                     AS cnt
  FROM tbl_mission_pool mp
  JOIN tbl_category c ON c.id = mp.category_id AND c.parent_id IS NOT NULL
  WHERE mp.mission_type = 'RELATIVE'
),
pool_any AS (
  SELECT mp.id,
         ROW_NUMBER() OVER (ORDER BY mp.id) - 1 AS rn,
         COUNT(*)    OVER ()                    AS cnt
  FROM tbl_mission_pool mp
  JOIN tbl_category c ON c.id = mp.category_id AND c.parent_id IS NOT NULL
  WHERE mp.mission_type = 'RELATIVE'
),
chosen AS (
  SELECT tu.user_id,
         tu.difficulty_id,
         COALESCE(pc.id, pa.id) AS mission_id,
         COALESCE(uc.base_amount, 30000 + ((tu.seq * 7) % 40) * 1000) AS base_amount,
         df.min_reduction_rate
           + ((tu.seq * 3)
              % (CAST(df.max_reduction_rate - df.min_reduction_rate AS SIGNED) + 1)) AS target_rate
  FROM target_user tu
  JOIN tbl_mission_difficulty df ON df.id = tu.difficulty_id
  LEFT JOIN usable_cat uc ON uc.user_id = tu.user_id AND uc.rn = 1
  LEFT JOIN pool_cat   pc ON pc.category_id = uc.category_id
                         AND pc.rn = ((tu.seq * 13) % pc.cnt)
  LEFT JOIN pool_any   pa ON pa.rn = ((tu.seq * 13) % pa.cnt)
)
SELECT chosen.user_id,
       chosen.mission_id,
       chosen.difficulty_id,
       CURDATE(),
       chosen.target_rate,
       ROUND(chosen.base_amount * (100 - chosen.target_rate) / 100, 2),
       chosen.base_amount,
       'PENDING',
       'LOCAL_SEED',
       NULL,
       TIMESTAMP(CURDATE(), '06:00:00')
FROM chosen
WHERE @seed_ready = 1
  AND chosen.mission_id IS NOT NULL
  AND NOT EXISTS (
        SELECT 1 FROM tbl_user_mission_info x
         WHERE x.user_id = chosen.user_id AND x.assign_date = CURDATE()
      );


-- =====================================================================
-- 6. 연속 성공일 — 위에서 넣은 이력에서 역산
--
--    #186 이 만든 증분 갱신(MissionEvaluationMapper.xml:48-68)의 의미에 맞춘다.
--      SUCCESS → streak_count + 1, longest = GREATEST(longest, streak_count + 1)
--      FAIL    → streak_count = 0
--    즉 streak_count 는 가장 최근 날짜부터 거꾸로 이어지는 연속 SUCCESS 수,
--    longest_streak_count 는 전 구간 최장 연속 SUCCESS 수다.
--    판정 배치가 이 값 위에 이어서 증분해도 결과가 맞는다.
--
--    gaps and islands — 행 번호 차이로 연속 구간을 자른다. PENDING 은 아직 판정되지
--    않은 날이므로 계산에서 뺀다. 실계정에 판정 이력이 하루 비어 있어도 "판정된 날들의
--    연속"으로 이어 센다.
-- =====================================================================

INSERT INTO tbl_streak_count
  (user_id, streak_count, longest_streak_count, last_check_date)
WITH hist AS (
  SELECT i.user_id, i.assign_date, i.result,
         ROW_NUMBER() OVER (PARTITION BY i.user_id            ORDER BY i.assign_date)
       - ROW_NUMBER() OVER (PARTITION BY i.user_id, i.result  ORDER BY i.assign_date) AS island
  FROM tbl_user_mission_info i
  JOIN tbl_user u ON u.id = i.user_id AND u.status = 'ACTIVE'
  WHERE i.result IN ('SUCCESS', 'FAIL')
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
       -- 마지막 판정일이 SUCCESS 구간에 속할 때만 현재 연속일이 살아 있다
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


-- =====================================================================
-- 7. 그룹 챌린지 2개
--
--    진행중(ACTIVE) 1개 — 상세·참여자·일별 집계 화면을 바로 볼 수 있다
--    모집중(RECRUITING) 1개 — 초대코드 미리보기·참여 플로우를 밟을 수 있다
--
--    방장은 실계정을 우선한다. 실계정이 아직 없으면 가상 사용자가 방장이 된다
--    (그래서 1번 초기화에서 그룹을 사용자보다 먼저 지운다).
--
--    ⚠ 기소·변론·투표(tbl_indictment / tbl_defense / tbl_vote)는 아직 Java 코드가 없어
--      넣지 않는다. 표만 있는 상태라 넣어도 화면에 나오지 않는다.
-- =====================================================================

SET @seed_admin_id = (
  SELECT u.id FROM tbl_user u
   WHERE u.status = 'ACTIVE'
   ORDER BY (u.social_provider = 'DEMO'), u.id   -- 실계정(0) 이 앞
   LIMIT 1
);

-- ---------------------------------------------------------------------
-- 7-1. 진행중 — 배달앱 카테고리 · 일일평가 · 오늘 기준 4일 전 시작 ~ 2일 뒤 종료
--      ck_cg_period 가 end_date <= start_date + 6일 을 강제한다. 정확히 7일이다.
--      카테고리가 없으면 category_id 가 NULL 이 되고 총소비 챌린지로 동작한다.
--
--      ★ 초대코드는 **정확히 5자리**여야 한다 (InviteCodeGenerator.java:26-27).
--        ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ" — 0·1·O·I·L 은 옮겨 적을 때
--        헷갈려서 뺐다. 프론트 참여코드 입력칸도 5칸이라, 길거나 금지 문자가 섞이면
--        **초대코드로 참여하는 플로우를 시연할 수 없다.**
--        정리 마커(invite_code LIKE 'SEED%')를 살리려고 SEED2 · SEED3 를 쓴다.
--        'SEED' 네 글자는 전부 허용 문자다.
--        실제 채번이 우연히 같은 값을 뽑을 확률은 1/2,800만이라 무시한다.
-- ---------------------------------------------------------------------
INSERT INTO tbl_challenge_group
  (admin_id, group_name, category_id, limit_amount, eval_type,
   max_members, start_date, end_date, invite_code, status, memo)
SELECT @seed_admin_id, '배달앱 끊기 7일',
       (SELECT id FROM tbl_category WHERE category_name = '배달앱'),
       15000, 'DAILY', 6,
       DATE_SUB(CURDATE(), INTERVAL 4 DAY),
       DATE_ADD(CURDATE(), INTERVAL 2 DAY),
       'SEED2', 'ACTIVE', '꼴찌가 커피 쏘기'
FROM DUAL                                  -- ★ MySQL 은 FROM 없는 SELECT 에 WHERE 를 못 붙인다
WHERE @seed_ready = 1 AND @seed_admin_id IS NOT NULL;

-- ---------------------------------------------------------------------
-- 7-2. 모집중 — 총소비(카테고리 없음) · 일일평가 · 이틀 뒤 시작
--      시작일을 미래로 둬야 #152 상태 전이 배치가 바로 ACTIVE 로 바꾸지 않는다.
--      (배치는 start_date <= 오늘 인 RECRUITING 그룹을 전이시킨다)
-- ---------------------------------------------------------------------
INSERT INTO tbl_challenge_group
  (admin_id, group_name, category_id, limit_amount, eval_type,
   max_members, start_date, end_date, invite_code, status, memo)
SELECT @seed_admin_id, '총소비 3만원 챌린지', NULL,
       30000, 'DAILY', 6,
       DATE_ADD(CURDATE(), INTERVAL 2 DAY),
       DATE_ADD(CURDATE(), INTERVAL 6 DAY),
       'SEED3', 'RECRUITING', '전원 생존하면 다 같이 치킨'
FROM DUAL
WHERE @seed_ready = 1 AND @seed_admin_id IS NOT NULL;

SET @seed_group_active = (SELECT id FROM tbl_challenge_group WHERE invite_code = 'SEED2');
SET @seed_group_open   = (SELECT id FROM tbl_challenge_group WHERE invite_code = 'SEED3');

-- ---------------------------------------------------------------------
-- 7-3. 참여자 — 실계정을 먼저 넣고 남는 자리를 가상 사용자로 채운다
--
--      lives_count 는 NOT NULL 인데 기본값이 없다. DAILY 는 기간 일수, PERIOD 는 1 이다
--      (GroupMemberMapper.xml:12-14). 둘 다 DAILY 라 각각 7일 · 5일이다.
--      정원은 6명 고정이다(ChallengeGroupService.java:52).
-- ---------------------------------------------------------------------
INSERT INTO tbl_group_member (group_id, user_id, lives_count)
WITH cand AS (
  SELECT u.id,
         ROW_NUMBER() OVER (ORDER BY (u.social_provider = 'DEMO'), u.id) AS rn
  FROM tbl_user u
  WHERE u.status = 'ACTIVE'
)
SELECT @seed_group_active, cand.id, 7
FROM cand
WHERE cand.rn <= 6
  AND @seed_ready = 1
  AND @seed_group_active IS NOT NULL;

INSERT INTO tbl_group_member (group_id, user_id, lives_count)
WITH cand AS (
  SELECT u.id,
         ROW_NUMBER() OVER (ORDER BY (u.social_provider = 'DEMO'), u.id) AS rn
  FROM tbl_user u
  WHERE u.status = 'ACTIVE'
)
SELECT @seed_group_open, cand.id, 5
FROM cand
WHERE cand.rn <= 4          -- 모집중이므로 자리를 남겨 둔다. 초대코드 SEED3 로 더 들어올 수 있다
  AND @seed_ready = 1
  AND @seed_group_open IS NOT NULL;

-- ---------------------------------------------------------------------
-- 7-4. 일별 소비 집계 — 진행중 그룹만. 시작일부터 오늘까지
--
--      실제로는 1시간마다 도는 배치가 채우는 표다(#168, 아직 미구현). 위에서 넣은
--      거래에서 그대로 계산해 두면 배치가 생겼을 때와 같은 값이 된다.
--      그룹의 category_id 가 NULL 이면 총소비, 아니면 그 카테고리만 센다.
-- ---------------------------------------------------------------------
INSERT INTO tbl_group_challenge_daily_result
  (group_id, user_id, challenge_date, daily_amount, target_count)
WITH RECURSIVE d (dt) AS (
  SELECT DATE_SUB(CURDATE(), INTERVAL 4 DAY)
  UNION ALL
  SELECT DATE_ADD(dt, INTERVAL 1 DAY) FROM d WHERE dt < CURDATE()
)
SELECT m.group_id, m.user_id, d.dt,
       COALESCE(SUM(t.amount), 0),
       COUNT(t.id)
FROM tbl_group_member m
JOIN tbl_challenge_group g ON g.id = m.group_id
JOIN d ON 1 = 1              -- ON 을 생략하면 뒤 LEFT JOIN 의 ON 이 (d LEFT JOIN t) 에 묶여
                             -- m·g 를 못 보는 파싱이 될 수 있다. 명시해서 결합 순서를 고정한다
LEFT JOIN tbl_transaction t
       ON t.user_id = m.user_id
      AND t.tr_date = d.dt
      AND t.classification = 'CONSUMPTION'
      AND t.direction = 'OUT'
      AND t.is_refund = 0
      AND t.is_excluded_from_summary = 0
      AND (g.category_id IS NULL OR t.category_id = g.category_id)
WHERE m.group_id = @seed_group_active
  AND @seed_ready = 1
GROUP BY m.group_id, m.user_id, d.dt
ON DUPLICATE KEY UPDATE
  daily_amount = VALUES(daily_amount),
  target_count = VALUES(target_count);


-- =====================================================================
-- 8. 월간 랭킹 (선택 — @seed_monthly_ranking = 1 일 때만)
--
--    #194(월간 점수 계산·누적)가 머지됐지만 그것만으로는 랭킹 화면이 비어 있다.
--      total_score  = SUCCESS 미션의 difficulty score 합 + (전날도 SUCCESS면 하루당 +5)
--                     ← MissionScoreMapper.xml `calculateMonthlyScore` 와 같은 식
--      ranking      = 그 달 total_score 내림차순 RANK
--      ratio        = ranking / 그 달 인원 × 100  (상위 %)
--      success_ratio= SUCCESS / 판정 완료 배정일수 × 100
--
--    ⚠ **@seed_monthly_ranking 을 0 으로 두지 말 것.** #194 는 배치가 아니라
--      미션 판정 이벤트에서만 도는 데다(MissionScoreService.recalculate),
--      total_score 하나만 쓰고 ranking·ratio 는 0 으로 박는다
--      (MissionScoreMapper.xml `upsertMonthlyScore`). 시드 이력엔 영영 안 돈다.
--      0 으로 두면 랭킹 화면이 전부 0등·0% 로 뜬다.
--    ⚠ 반대로 #194 가 나중에 돌아도 이 값을 망가뜨리지 않는다.
--      그쪽 ON DUPLICATE KEY UPDATE 는 total_score 만 건드린다.
--    ⚠ monthly_longest_streak 는 NULL 로 둔다. 월 단위로 다시 잘라 세야 하는 값이다.
-- =====================================================================

INSERT INTO tbl_monthly_ranking
  (user_id, `year_month`, total_score, ranking, ratio, monthly_longest_streak, success_ratio)
WITH agg AS (
  SELECT i.user_id,
         DATE_FORMAT(i.assign_date, '%Y-%m') AS ym,
         SUM(CASE WHEN i.result = 'SUCCESS'
                  THEN df.score
                       + CASE WHEN EXISTS (
                           SELECT 1 FROM tbl_user_mission_info prev
                            WHERE prev.user_id     = i.user_id
                              AND prev.assign_date = DATE_SUB(i.assign_date, INTERVAL 1 DAY)
                              AND prev.result      = 'SUCCESS'
                         ) THEN 5 ELSE 0 END
                  ELSE 0 END) AS total_score,
         SUM(i.result = 'SUCCESS') AS success_days,
         COUNT(*)                  AS total_days
  FROM tbl_user_mission_info i
  JOIN tbl_mission_difficulty df ON df.id = i.difficulty_id
  WHERE i.result IN ('SUCCESS', 'FAIL')
  GROUP BY i.user_id, ym
),
ranked AS (
  SELECT agg.user_id, agg.ym, agg.total_score, agg.success_days, agg.total_days,
         RANK()   OVER (PARTITION BY agg.ym ORDER BY agg.total_score DESC) AS rnk,
         COUNT(*) OVER (PARTITION BY agg.ym)                               AS cnt
  FROM agg
)
SELECT ranked.user_id, ranked.ym, ranked.total_score, ranked.rnk,
       ROUND(100 * ranked.rnk / ranked.cnt, 2),
       NULL,
       ROUND(100 * ranked.success_days / NULLIF(ranked.total_days, 0), 2)
FROM ranked
WHERE @seed_ready = 1
  AND @seed_monthly_ranking = 1
ON DUPLICATE KEY UPDATE
  total_score   = VALUES(total_score),
  ranking       = VALUES(ranking),
  ratio         = VALUES(ratio),
  success_ratio = VALUES(success_ratio);


-- =====================================================================
-- 9. 확인 — 여기가 전부 기대대로면 성공이다
-- =====================================================================

-- ① 사용자별 종합. 점수·연속일·소비총액이 **사용자마다 달라야** 정상이다.
--    구분이 '실계정' 인 행이 로그인해서 볼 계정이다.
--    누적점수_60일 = 배정 60일치 전체 합이라 확인용일 뿐이다.
--    **앱 화면이 읽는 점수는 이게 아니라 이번 달 tbl_monthly_ranking.total_score 다**
--    (GET /api/missions/score). 그 값은 아래 확인 쿼리 ⑥ 의 '총점' 열에서 본다.
--    계산식은 둘이 같다 — difficulty score 합 + 전날도 SUCCESS면 하루당 +5.
SELECT u.id                                       AS user_id,
       CASE WHEN u.social_provider = 'DEMO' THEN '가상' ELSE '실계정' END AS 구분,
       COALESCE(u.nickname, u.social_name, u.email) AS 표시명,
       df.difficulty_name                         AS 난이도,
       COUNT(i.id)                                AS 미션수,
       SUM(i.result = 'SUCCESS')                  AS 성공,
       ROUND(100 * SUM(i.result = 'SUCCESS') / NULLIF(COUNT(i.id), 0), 1) AS 성공률,
       SUM(CASE WHEN i.result = 'SUCCESS'
                THEN idf.score
                     + CASE WHEN EXISTS (
                         SELECT 1 FROM tbl_user_mission_info prev
                          WHERE prev.user_id     = i.user_id
                            AND prev.assign_date = DATE_SUB(i.assign_date, INTERVAL 1 DAY)
                            AND prev.result      = 'SUCCESS'
                       ) THEN 5 ELSE 0 END
                ELSE 0 END)                        AS 누적점수_60일,
       sc.streak_count                            AS 현재연속,
       sc.longest_streak_count                    AS 최장연속,
       (SELECT COALESCE(SUM(t.amount), 0) FROM tbl_transaction t
         WHERE t.user_id = u.id AND t.classification = 'CONSUMPTION'
           AND t.is_refund = 0 AND t.is_excluded_from_summary = 0)        AS 소비총액
FROM tbl_user u
JOIN tbl_mission_difficulty df       ON df.id  = u.difficulty_id
LEFT JOIN tbl_user_mission_info i    ON i.user_id = u.id
                                    AND i.result IN ('SUCCESS', 'FAIL')  -- 오늘치(PENDING)는 성공률에서 뺀다
LEFT JOIN tbl_mission_difficulty idf ON idf.id = i.difficulty_id
LEFT JOIN tbl_streak_count sc        ON sc.user_id = u.id
WHERE u.status = 'ACTIVE'
GROUP BY u.id, 구분, 표시명, df.difficulty_name, sc.streak_count, sc.longest_streak_count
ORDER BY 누적점수_60일 DESC;

-- ② 실계정 온보딩 게이트 — 세 칸이 모두 '통과' 여야 로그인 즉시 홈으로 들어간다
SELECT u.id AS user_id,
       COALESCE(u.nickname, u.social_name, u.email) AS 표시명,
       CASE WHEN (SELECT COUNT(*) FROM tbl_user_consent c
                   WHERE c.user_id = u.id AND c.status = 1 AND c.withdrawn_at IS NULL
                     AND c.consent_type IN ('TERMS','PRIVACY','FINANCIAL_DATA')
                     AND (c.expires_at IS NULL OR c.expires_at > NOW())) = 3
            THEN '통과' ELSE '막힘' END AS 가입동의,
       CASE WHEN EXISTS (SELECT 1 FROM tbl_user_consent c
                          WHERE c.user_id = u.id AND c.consent_type = 'THIRD_PARTY'
                            AND c.status = 1 AND c.withdrawn_at IS NULL
                            AND (c.expires_at IS NULL OR c.expires_at > NOW()))
            THEN '통과' ELSE '막힘' END AS 금융동의,
       CASE WHEN EXISTS (SELECT 1 FROM tbl_connected_account ca
                          WHERE ca.user_id = u.id AND ca.is_active = 1)
            THEN '통과' ELSE '막힘' END AS 계좌연동,
       CASE WHEN EXISTS (SELECT 1 FROM tbl_user_consent c
                          WHERE c.user_id = u.id AND c.consent_type = 'CHALLENGE'
                            AND c.status = 1 AND c.withdrawn_at IS NULL)
            THEN '있음' ELSE '없음' END AS 챌린지동의
FROM tbl_user u
WHERE u.status = 'ACTIVE' AND u.social_provider <> 'DEMO'
ORDER BY u.id;

-- ③ 고정지출 탐지 대상 — 사용자마다 아래 5종이 **반드시 다 나와야** 정상이다
--    (넷플릭스 · SKT통신요금 · 유튜브뮤직 · 피트니스클럽 · 월세이체)
--    반복횟수는 6 또는 7 이다 — 과거 6개월 + 당월. 당월 결제일이 아직 안 지났으면 6.
--    하나라도 빠지면 고정 지출 배율에 m 이 섞여 매월 금액이 달라진 것이다.
--    스타벅스·배달의민족 같은 변동 가맹점이 3회로 함께 잡히는 것은 정상이다 —
--    지터가 11주기라 8개월 중 같은 값이 겹칠 수 있다. 탐지 시연에는 오히려 도움이 된다.
SELECT user_id, merchant_name AS 가맹점, amount AS 금액, COUNT(*) AS 반복횟수
FROM tbl_transaction
WHERE codef_tr_key LIKE 'DUMMY-%'
  AND classification = 'CONSUMPTION'
  AND is_refund = 0
GROUP BY user_id, merchant_name, amount
HAVING COUNT(*) >= 3
ORDER BY user_id, 반복횟수 DESC, 금액 DESC;

-- ④ 그룹 챌린지 — 참여자 수와 일별 집계가 붙었는지
SELECT g.id AS group_id, g.group_name AS 챌린지, g.status AS 상태,
       COALESCE(c.category_name, '총소비') AS 대상,
       g.limit_amount AS 한도, g.start_date AS 시작, g.end_date AS 종료,
       g.invite_code AS 초대코드,
       (SELECT COUNT(*) FROM tbl_group_member m WHERE m.group_id = g.id) AS 참여자수,
       (SELECT COUNT(*) FROM tbl_group_challenge_daily_result r
         WHERE r.group_id = g.id)                                        AS 일별집계행수
FROM tbl_challenge_group g
LEFT JOIN tbl_category c ON c.id = g.category_id
WHERE g.invite_code LIKE 'SEED%'
ORDER BY g.id;

-- ⑤ 진행중 그룹의 날짜별 소비 — 한도를 넘는 사람과 아닌 사람이 섞여야 기소 시연이 된다
SELECT r.challenge_date AS 날짜,
       COALESCE(u.nickname, u.social_name, u.email) AS 표시명,
       r.daily_amount AS 소비액, g.limit_amount AS 한도,
       CASE WHEN r.daily_amount > g.limit_amount THEN '초과' ELSE '' END AS 판정
FROM tbl_group_challenge_daily_result r
JOIN tbl_challenge_group g ON g.id = r.group_id
JOIN tbl_user u            ON u.id = r.user_id
WHERE g.invite_code = 'SEED2'
ORDER BY r.challenge_date, r.daily_amount DESC;

-- ⑥ 월간 랭킹 — 실계정이 가운데쯤 있으면 좋다
SELECT r.`year_month` AS 연월, r.ranking AS 순위,
       CASE WHEN u.social_provider = 'DEMO' THEN '가상' ELSE '실계정' END AS 구분,
       COALESCE(u.nickname, u.social_name, u.email) AS 표시명,
       r.total_score AS 총점, r.ratio AS 상위퍼센트, r.success_ratio AS 성공률
FROM tbl_monthly_ranking r
JOIN tbl_user u ON u.id = r.user_id
WHERE r.`year_month` >= DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 1 MONTH), '%Y-%m')
ORDER BY r.`year_month` DESC, r.ranking;

-- ⑦ 오늘의 미션 — ACTIVE 사용자가 **한 명도 빠짐없이** 한 줄씩 나와야 한다.
--    빠진 사용자는 GET /api/missions/today 에서 400(TODAY_MISSION_NOT_FOUND)을 받는다.
--    이 쿼리는 TodayMissionMapper.xml 의 INNER JOIN 사슬을 그대로 흉내낸 것이라,
--    여기 안 나오면 화면에서도 안 나온다.
SELECT u.id AS user_id,
       CASE WHEN u.social_provider = 'DEMO' THEN '가상' ELSE '실계정' END AS 구분,
       COALESCE(u.nickname, u.social_name, u.email) AS 표시명,
       i.assign_date AS 배정일,
       p.category_name AS 대분류, ch.category_name AS 소분류,
       mp.mission_title AS 미션,
       i.base_amount AS 기준금액, i.target_rate AS 절감률, i.target_value AS 목표금액,
       i.result AS 상태, i.assignment_reason AS 출처
FROM tbl_user u
LEFT JOIN tbl_user_mission_info i ON i.user_id = u.id AND i.assign_date = CURDATE()
LEFT JOIN tbl_mission_pool mp     ON mp.id = i.mission_id
LEFT JOIN tbl_category ch         ON ch.id = mp.category_id
LEFT JOIN tbl_category p          ON p.id  = ch.parent_id
WHERE u.status = 'ACTIVE'
ORDER BY u.id;
