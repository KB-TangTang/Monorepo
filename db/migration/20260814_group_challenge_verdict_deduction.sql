-- [업그레이드/신규 설치 공통] 그룹챌린지 집계액과 판결 감액을 서로 다른 컬럼으로 분리한다.
--
-- 신규 설치 순서:
--   1. db/schema.sql 실행
--   2. 필요한 seed SQL 실행
--   3. db/migration/ 파일을 날짜순으로 실행
--
-- 기존 설치 순서:
--   1. 20260814_add_mission_result_checked_at.sql 까지 적용된 DB에서 이 파일을 실행한다.
--   2. schema.sql은 수정하지 않는다.
--
-- ── 배경 ────────────────────────────────────────────────────────────────
-- tbl_group_challenge_daily_result.daily_amount 를 5분 주기 집계 배치(#168)와
-- 재판 결과(무죄 감액)가 **같은 컬럼에** 쓰고 있었다. 배치가 UPSERT 로 덮어쓰면
-- 판결로 깎인 금액이 사라진다. 소유자를 컬럼 단위로 갈라 서로 건드리지 않게 한다.
--
--   daily_amount              → 배치 전용 (집계 원본)
--   verdict_deduction_amount  → 판결 전용 (무죄 시 인정된 감액)
--   effective_amount          → 자동 계산 GREATEST(daily_amount - verdict_deduction_amount, 0)
--
-- ⚠ 배치 구현 시 UPSERT 의 UPDATE 절에 verdict_deduction_amount 를 **절대 넣지 말 것.**
--   넣는 순간 감액이 5분마다 0으로 초기화되고, 원인이 화면에 드러나지 않는다.
--
-- 변론(tbl_defense)은 사용자가 입력하는 값의 의미가 바뀐다.
--   requested_adjustment_amount(정산 조정 요청액) → actual_burden_amount(실제 부담 금액)
--   감액분은 deduction_amount 로 따로 저장한다.
--
-- ⚠ 기존 행이 있다면 값의 의미가 달라진다. 지금은 두 테이블 모두 seed 가 없고
--   시연 데이터만 있을 수 있어 backfill 하지 않는다. 운영 데이터가 생긴 뒤에 적용하지 말 것.
--
-- ⚠ 이 시점에 두 컬럼을 참조하는 Java·매퍼 XML 은 0건이다(변론·일별집계 #168·#170 미구현).
--   구현이 들어간 뒤에 적용하면 컬럼명 불일치로 조회가 깨진다. 순서를 지킬 것.

-- ── 1. 일별 집계: 배치 전용임을 주석에 못박는다 ──────────────────────────
-- (기존 주석 "변론 후 정산액 반영된 값이 최종" 이 이번 변경으로 사실이 아니게 됐다)
ALTER TABLE tbl_group_challenge_daily_result
    MODIFY COLUMN daily_amount DECIMAL(15,2) NOT NULL
        COMMENT '집계 배치 전용. 5분마다 UPSERT로 덮어쓴다. 판결 감액을 여기에 반영하지 말 것';

-- ── 2. 판결 감액 컬럼 ───────────────────────────────────────────────────
ALTER TABLE tbl_group_challenge_daily_result
    ADD COLUMN verdict_deduction_amount DECIMAL(15,2) NOT NULL DEFAULT 0
        COMMENT '판결 전용. 무죄로 인정된 감액 누계. 배치는 이 컬럼을 쓰지 않는다'
        AFTER daily_amount,
    ADD CONSTRAINT ck_gcdr_deduction CHECK (verdict_deduction_amount >= 0);

-- ── 3. 실효 금액 (생성 컬럼) ────────────────────────────────────────────
-- 별도 ALTER 로 나눈 이유: 같은 ALTER 안에서 방금 추가한 컬럼을 참조하는 생성 컬럼은
-- MySQL 이 정의 검증 순서 때문에 거부할 수 있다. 나누면 확실하다.
--
-- STORED 인 이유: 랭킹 정렬·집계에 쓰이므로 인덱스를 걸 수 있어야 한다(VIRTUAL 은 제약이 많다).
--
-- GREATEST(..., 0) 으로 **0 에서 바닥을 친다.** 이 값은 일일 결산 랭킹의 정렬 키이고
-- 적게 쓴 사람이 위로 간다. 음수가 나오면 감액을 크게 받은 사람이 **한 푼도 안 쓴 사람보다
-- 위로 올라간다** — 표시가 이상한 정도가 아니라 순위가 뒤집힌다.
--
-- 음수는 실제로 생긴다. 두 컬럼의 갱신 주체가 다르기 때문이다.
--   daily_amount             : 5분마다 배치가 다시 계산해 덮어쓴다
--   verdict_deduction_amount : 판결 시점에 한 번 박히고 그대로 남는다
-- 거래가 환불되거나 카테고리가 재분류돼 daily_amount 가 줄면 그 순간 역전된다.
-- 환불은 이미 다루는 시나리오다(GC_05_01·02).
--
-- 원본 두 컬럼이 그대로 남아 있어 바닥을 치는 것은 파생값뿐이다.
-- 원래 차이가 필요하면 언제든 daily_amount - verdict_deduction_amount 로 다시 구한다.
--
-- ⚠ 대신 "감액이 집계액을 넘었다"는 사실이 이 컬럼에서는 안 보이게 된다. 데이터 이상 신호이므로
--   배치(#168)에서 verdict_deduction_amount > daily_amount 인 행을 로그로 남길 것.
--   조용히 0 으로 덮이면 원인을 찾을 수 없다.
ALTER TABLE tbl_group_challenge_daily_result
    ADD COLUMN effective_amount DECIMAL(15,2)
        AS (GREATEST(daily_amount - verdict_deduction_amount, 0)) STORED
        COMMENT '실효 소비액. 일일 결산 랭킹이 쓰는 값. GREATEST(daily_amount - verdict_deduction_amount, 0)'
        AFTER verdict_deduction_amount;

-- ── 4. 변론: 컬럼 의미 변경 + 감액 컬럼 추가 ────────────────────────────
-- ⚠ CHECK 제약이 컬럼을 참조하면 MySQL 8 은 이름 변경을 거부한다
--   (ERROR 3959: Check constraint 'ck_def_adjust' uses column 'requested_adjustment_amount',
--    hence column cannot be dropped or renamed).
--   그래서 **제약을 먼저 지우고 → 이름을 바꾸고 → 제약을 다시 만든다.** 순서를 바꾸면 실패한다.
ALTER TABLE tbl_defense
    DROP CHECK ck_def_adjust;

ALTER TABLE tbl_defense
    CHANGE COLUMN requested_adjustment_amount actual_burden_amount DECIMAL(15,2) NULL
        COMMENT '사용자가 변론에서 신고한 실제 부담 금액. 0 이상 — 원 거래액 초과 여부는 애플리케이션이 검증',
    ADD COLUMN deduction_amount DECIMAL(15,2) NULL
        COMMENT '인정된 감액(원 거래액 - 실제 부담 금액). 무죄와 함께 확정, 유죄면 반영하지 않는다'
        AFTER actual_burden_amount,
    ADD CONSTRAINT ck_def_burden CHECK (actual_burden_amount IS NULL OR actual_burden_amount >= 0),
    ADD CONSTRAINT ck_def_deduction CHECK (deduction_amount IS NULL OR deduction_amount >= 0);

-- 롤백은 수동으로 수행한다. 위 순서를 거꾸로 밟는다.
--   1) ALTER TABLE tbl_defense DROP CHECK ck_def_deduction, DROP CHECK ck_def_burden;
--   2) ALTER TABLE tbl_defense DROP COLUMN deduction_amount,
--        CHANGE COLUMN actual_burden_amount requested_adjustment_amount DECIMAL(15,2) NULL;
--   3) ALTER TABLE tbl_defense ADD CONSTRAINT ck_def_adjust
--        CHECK (requested_adjustment_amount IS NULL OR requested_adjustment_amount >= 0);
--   4) ALTER TABLE tbl_group_challenge_daily_result DROP COLUMN effective_amount;
--   5) ALTER TABLE tbl_group_challenge_daily_result DROP CHECK ck_gcdr_deduction,
--        DROP COLUMN verdict_deduction_amount;
