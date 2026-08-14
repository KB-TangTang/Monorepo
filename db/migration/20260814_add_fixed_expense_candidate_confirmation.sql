-- [업그레이드/신규 설치 공통] 고정지출 후보의 사용자 확정 시각을 분리해 저장한다.
--
-- 신규 설치 순서:
--   1. db/schema.sql 실행
--   2. 필요한 seed SQL 실행
--   3. db/migration/ 파일을 날짜순으로 실행
--
-- 기존 설치 순서:
--   1. 20260813_* 마이그레이션까지 적용된 DB에서 이 파일을 실행한다.
--   2. schema.sql은 수정하지 않는다. 이 파일은 기존 설치와 신규 설치 모두에 필요하다.
--
-- 변경 내용:
--   - confirmed_at: 사용자가 고정지출로 확정한 시각. NULL이면 미확정 탐지 후보다.
--   - 후보 조회 인덱스: user_id, 상태, 제외 여부, 확정 여부 조건을 함께 지원한다.
--
-- 기존 데이터에는 신뢰할 수 있는 사용자 확정 이력이 없으므로 backfill하지 않는다.
-- 기존 행은 confirmed_at = NULL로 남아 탐지 후보로 취급한다.
ALTER TABLE tbl_fixed_expense_candidate
    ADD COLUMN confirmed_at DATETIME NULL
        COMMENT '사용자가 고정지출로 확정한 시각. NULL이면 미확정 탐지 후보' AFTER is_excluded,
    ADD KEY idx_fec_user_candidate_lookup (user_id, status, is_excluded, confirmed_at);

-- 롤백은 수동으로 수행한다.
-- 1) idx_fec_user_candidate_lookup 인덱스를 제거한다.
-- 2) confirmed_at 컬럼을 제거한다.
