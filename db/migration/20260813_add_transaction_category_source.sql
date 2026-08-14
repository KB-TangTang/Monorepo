-- 20260813_add_transaction_category_source.sql
-- 이슈 #147 — 거래 카테고리화(후속 작업) 선행 작업: 카테고리 출처 추적 기반
--
-- 신규 설치: 실행 대상이다 (예외 목록에 없음. db/AGENTS.md 참고).
--
-- 배경
--   tbl_transaction.category_id 는 "무엇으로 분류됐는지"만 담을 뿐, 그 값이 규칙(MCC/키워드)·LLM·
--   사용자 직접 지정 중 어디서 왔는지 구분할 방법이 없었다. 후속 작업(거래 자동 카테고리화)의
--   "사용자가 직접 지정한 카테고리는 자동 분류가 절대 덮어쓰지 않는다" 규칙을 지키려면 출처를
--   먼저 저장할 수 있어야 한다. 이 파일은 그 저장소만 만든다 — 실제 분류 로직은 이번 범위가 아니다.
--
--   category_id 는 현재 어떤 서비스 코드도 값을 쓰지 않으므로(Transaction.java 필드로만 존재),
--   기존 데이터를 건드릴 걱정 없이 컬럼을 추가할 수 있다.

USE tangtang;

ALTER TABLE tbl_transaction
  ADD COLUMN category_source VARCHAR(20) NULL
    COMMENT '카테고리 판정 출처. RULE_MCC/RULE_KEYWORD(규칙)/LLM/USER(사용자 직접 지정). category_id 가 NULL 이면 반드시 NULL'
    AFTER category_id;

-- 허용값 제한
ALTER TABLE tbl_transaction
  ADD CONSTRAINT ck_tx_category_source
  CHECK (category_source IS NULL OR category_source IN ('RULE_MCC', 'RULE_KEYWORD', 'LLM', 'USER'));

-- category_id 가 NULL(아직 분류 안 됨)이면 category_source 도 반드시 NULL —
-- "미분류" 상태와 "무언가로 분류된" 상태를 명확히 구분하기 위함
ALTER TABLE tbl_transaction
  ADD CONSTRAINT ck_tx_category_source_requires_id
  CHECK (category_id IS NOT NULL OR category_source IS NULL);

-- =====================================================================
-- 적용 확인 (선택)
-- =====================================================================
-- SHOW CREATE TABLE tbl_transaction\G
--
-- 기대 결과
--   category_source 컬럼 있음, ck_tx_category_source·ck_tx_category_source_requires_id 있음
