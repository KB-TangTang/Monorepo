-- 2026-08-09 · 0808 멘토링 피드백 반영
-- ① tbl_mission_pool  : 컬럼 삭제 없음. CHECK 1개 추가 + 코멘트 정리
--                       (초안의 "절대형 폐지 → 컬럼 DROP" 은 철회했다.
--                        절대형은 콜드스타트 사용자 전용으로 유지한다)
-- ② tbl_indictment    : 투표 동률 처리를 스크래치 복권 → 판사 탕이 AI 판결로 교체
--
-- 근거 문서
--   .claude/context/DECISIONS.md  2026-08-09 (4건)
--   .claude/context/팀공유_20260809_멘토링피드백_반영.md
--   doc/개발산출물/ERD/탕탕_구현용_v2_0803.sql  (동일 내용 반영 완료)
--   doc/개발산출물/ERD/탕탕-V3.xlsx             (동일 내용 반영 완료)
--
-- 적용 방법 (팀원 각자 1회)
--   mysql -u tangtang -p tangtang --default-character-set=utf8mb4 \
--     -e "source D:/KB_Final_Project/app/Monorepo/db/migration/20260809_ai_verdict_and_mission_pool_check.sql"
--   PowerShell 은 < 리다이렉션을 지원하지 않으므로 위처럼 source 를 쓴다.
--
-- ⚠ 실행 순서 주의
--   ②-1(CHECK 삭제) → ②-2(데이터 이관) → ②-4(CHECK 재생성) 순서를 지켜야 한다.
--   CHECK 를 남긴 채 UPDATE 하면 옛 CHECK 에 걸리고,
--   새 CHECK 를 먼저 걸면 남아 있는 SCRATCH_LOTTERY 행에 걸린다. 둘 다 실측으로 확인했다.
--   ①-1(데이터 정합화) 도 ①-2(CHECK 추가) 보다 먼저 와야 한다.
--
-- ⚠ EC2 운영 DB 에도 같은 순서로 적용한다.
--
-- 신규 설치: **실행 대상이다.** 이 변경은 schema.sql 에 반영돼 있지 않다.
--   (2026-08-11 정정 — 원래 "schema.sql 에 반영한 뒤에는 실행할 필요가 없다"고 적혀 있었으나
--    schema.sql 은 팀 공유 후 갱신하지 않는 것이 규칙이라 반영된 적이 없다. 그대로 두면
--    신규 설치가 ai_verdict_reason 없이 만들어진다. db/AGENTS.md 「신규 설치 순서」 참고)


-- =====================================================================
-- ① tbl_mission_pool — 변경 없음 (2026-08-09 2차 정정)
-- =====================================================================
--
--   당일 오전 초안에서는 "절대형 미션 폐지"로 mission_type · limit_price 를 DROP 하려 했으나,
--   **절대형은 폐지가 아니라 콜드스타트 전용으로 유지**하기로 정정했다.
--   가입 직후 사용자는 최근 28일 거래가 없어 기준액 B 를 낼 수 없고,
--   그러면 상대형 목표(T = B x (1 - 절감률)) 자체를 계산할 방법이 없다.
--   → schema.sql 의 tbl_mission_pool 은 **그대로 두면 된다. 이 파일은 손대지 않는다.**
--
--   다만 상대형 행에 limit_price 가 섞이지 않도록 CHECK 를 하나 추가한다.
--   (목표가 limit_price 와 target_value 두 곳에서 나오면 판정이 어긋난다)

-- ①-1. 기존 데이터 정합화 — 상대형 행의 limit_price 를 NULL 로, 절대형 행의 NULL 을 0 으로
UPDATE tbl_mission_pool SET limit_price = NULL WHERE mission_type = 'RELATIVE';
UPDATE tbl_mission_pool SET limit_price = 0    WHERE mission_type = 'ABSOLUTE' AND limit_price IS NULL;

-- ①-2. 절대형만 상한을 갖는다
ALTER TABLE tbl_mission_pool
  ADD CONSTRAINT ck_mp_limit
  CHECK ((mission_type = 'ABSOLUTE' AND limit_price IS NOT NULL AND limit_price >= 0)
         OR (mission_type = 'RELATIVE' AND limit_price IS NULL));

-- ①-3. 컬럼 코멘트를 새 의미에 맞춘다
ALTER TABLE tbl_mission_pool
  MODIFY COLUMN mission_type VARCHAR(10) NOT NULL
  COMMENT 'ABSOLUTE(절대형=무지출·상한형, 콜드스타트 전용)/RELATIVE(상대형)';

ALTER TABLE tbl_mission_pool
  MODIFY COLUMN limit_price INT NULL
  COMMENT '절대형 미션의 하루 지출 상한. 0=무지출, >0=상한형(예: 카페 5,000원 이하). 상대형은 NULL';

ALTER TABLE tbl_mission_pool
  MODIFY COLUMN category_id BIGINT NULL
  COMMENT 'NULL=카테고리 무관 미션 / NOT NULL=해당 카테고리 전용. AI 과소비 카테고리 1~3순위와 매칭해 배정(MC_02_01)';

-- ①-4. 목표 상한 컬럼 코멘트
ALTER TABLE tbl_user_mission_info
  MODIFY COLUMN target_value DECIMAL(15,2) NULL
  COMMENT '목표 상한 T. 상대형=B x (1-절감률) 후 하한 클램프 / 절대형=미션 풀의 limit_price(0=무지출)';


-- =====================================================================
-- ② tbl_indictment — 투표 동률은 판사 탕이가 AI 로 판결한다
-- =====================================================================
--
-- 배경
--   재판인데 결과가 운(스크래치 복권)으로 갈리면 판결에 근거가 없어 컨셉이 무너진다는
--   0808 멘토링 지적을 반영했다. 동률이면 판사 탕이가 기소 사유·변론 원문·투표 분포·
--   해당 일자 소비 요약을 근거로 유무죄와 사유 한 문장을 낸다.
--
--   판결문 화면(GC_07_04)과 채팅 봇 메시지에 사유 원문을 그대로 띄워야 해서
--   저장할 컬럼이 필요하다. message(기소 사유)와 섞으면 둘을 구분할 수 없다.
--
-- ⚠ 순서 주의 (실측으로 확인했다)
--   CHECK 를 먼저 없애지 않으면 ②-2 의 UPDATE 가 실패한다 —
--   옛 CHECK 가 AI_JUDGMENT 를 허용하지 않기 때문이다.
--   반대로 새 CHECK 를 먼저 걸어도 실패한다 — 남아 있는 SCRATCH_LOTTERY 행에 걸린다.
--   반드시 [CHECK 삭제 → 데이터 이관 → CHECK 재생성] 순서로 간다.

-- ②-1. 옛 CHECK 삭제 (AI_JUDGMENT 를 막고 있다)
ALTER TABLE tbl_indictment
  DROP CHECK ck_ind_verdict_method;

-- ②-2. 기존 SCRATCH_LOTTERY 데이터를 AI_JUDGMENT 로 이관
--      (현재 개발 DB 에는 재판 데이터가 없어 0행이 정상. 운영 DB 대비 안전장치다)
UPDATE tbl_indictment
SET verdict_method = 'AI_JUDGMENT'
WHERE verdict_method = 'SCRATCH_LOTTERY';

-- ②-3. 판결 사유 컬럼 신설
ALTER TABLE tbl_indictment
  ADD COLUMN ai_verdict_reason VARCHAR(300) NULL
  COMMENT '판사 탕이의 AI 판결 사유 원문. verdict_method=AI_JUDGMENT 일 때만 기록, 그 외 NULL. 판결 상세(GC_07_04)에 그대로 노출'
  AFTER verdict_method;

-- ②-4. 허용값 교체한 CHECK 재생성 (SCRATCH_LOTTERY 제거 → AI_JUDGMENT)
ALTER TABLE tbl_indictment
  ADD CONSTRAINT ck_ind_verdict_method
  CHECK (verdict_method IS NULL OR verdict_method IN ('VOTE','NO_VOTE','AI_JUDGMENT','CONFESSION'));

-- ②-5. 사유는 AI 판결일 때만 존재할 수 있다
--      다른 판결 경로(VOTE·NO_VOTE·CONFESSION)에 사유가 섞이면
--      판결문 화면이 잘못된 문장을 띄우므로 DB 가 막는다.
ALTER TABLE tbl_indictment
  ADD CONSTRAINT ck_ind_ai_reason
  CHECK (ai_verdict_reason IS NULL OR verdict_method = 'AI_JUDGMENT');

-- ②-6. verdict_method 코멘트 갱신
ALTER TABLE tbl_indictment
  MODIFY COLUMN verdict_method VARCHAR(20) NULL
  COMMENT '판결 근거. VOTE(다수결)/NO_VOTE(0표 무죄추정)/AI_JUDGMENT(동률 시 판사 탕이 AI 판결)/CONFESSION(혐의 인정, GC_06_06). 확정 시 기록';


-- =====================================================================
-- 적용 확인 (선택 — 아래를 실행하면 결과를 눈으로 볼 수 있다)
-- =====================================================================
-- SHOW CREATE TABLE tbl_mission_pool\G
-- SHOW CREATE TABLE tbl_indictment\G
--
-- 기대 결과
--   tbl_mission_pool  : mission_type·limit_price 유지, ck_mp_type·ck_mp_limit 있음, idx_mp_type_cat 있음
--   tbl_indictment    : ai_verdict_reason 있음, ck_ind_verdict_method 에 AI_JUDGMENT, ck_ind_ai_reason 있음
