-- [업그레이드 설치 전용] 월간 리포트 AI 분석 결과를 tbl_asset_snapshot에 저장한다.
--
-- 신규 설치 순서:
--   1. db/schema.sql 실행
--   2. 필요한 seed SQL 실행
--   3. db/migration/ 파일을 날짜순으로 실행
--
-- 기존 설치 순서:
--   1. 20260812_* 마이그레이션까지 적용된 DB에서 이 파일을 실행한다.
--   2. schema.sql은 수정하지 않는다. 이 파일은 기존 설치와 신규 설치 모두에 필요하다.
--
-- 변경 내용:
--   - ai_comment: AI 소비 피드백 문자열 배열(JSON)로 저장
--   - compare_comment: 절약 비유 문장(TEXT, 절감액이 없으면 NULL)으로 저장
--   - 생성 상태·모델·실패 정보는 중복 생성 방지와 재시도 판단에만 사용
--
-- 기존 ai_comment의 일반 텍스트는 JSON 문자열 배열로 감싸 보존한다.
-- 이미 JSON인 값은 그대로 유지한다.
UPDATE tbl_asset_snapshot
SET ai_comment = JSON_ARRAY(ai_comment)
WHERE ai_comment IS NOT NULL
  AND JSON_VALID(ai_comment) = 0;

ALTER TABLE tbl_asset_snapshot
    MODIFY COLUMN ai_comment JSON NULL
        COMMENT '월간 AI 소비 피드백 문자열 배열(JSON, 1~3개)',
    MODIFY COLUMN compare_comment TEXT NULL
        COMMENT '월간 절약 비유 문장. 절감액이 없으면 NULL',
    ADD COLUMN ai_analysis_status VARCHAR(20) NOT NULL DEFAULT 'NOT_REQUESTED'
        COMMENT 'NOT_REQUESTED, IN_PROGRESS, COMPLETED, FAILED' AFTER compare_comment,
    ADD COLUMN ai_analysis_provider VARCHAR(30) NULL
        COMMENT '분석 결과를 생성한 외부 제공자' AFTER ai_analysis_status,
    ADD COLUMN ai_analysis_model VARCHAR(100) NULL
        COMMENT '분석 결과를 생성한 모델명' AFTER ai_analysis_provider,
    ADD COLUMN ai_analysis_prompt_version VARCHAR(40) NULL
        COMMENT '분석 프롬프트 버전' AFTER ai_analysis_model,
    ADD COLUMN ai_analysis_input_hash CHAR(64) NULL
        COMMENT '비식별 월간 집계 입력의 SHA-256 해시' AFTER ai_analysis_prompt_version,
    ADD COLUMN ai_analysis_attempt_count INT UNSIGNED NOT NULL DEFAULT 0
        COMMENT '생성 시도 횟수' AFTER ai_analysis_input_hash,
    ADD COLUMN ai_analysis_requested_at DATETIME NULL
        COMMENT '생성 요청 시각' AFTER ai_analysis_attempt_count,
    ADD COLUMN ai_analysis_completed_at DATETIME NULL
        COMMENT '생성 성공 시각' AFTER ai_analysis_requested_at,
    ADD COLUMN ai_analysis_failed_at DATETIME NULL
        COMMENT '생성 실패 시각' AFTER ai_analysis_completed_at,
    ADD COLUMN ai_analysis_failure_code VARCHAR(64) NULL
        COMMENT '사용자 정보나 원문을 포함하지 않는 실패 코드' AFTER ai_analysis_failed_at;

-- 롤백은 수동으로 수행한다.
-- 1) ai_comment / compare_comment의 NULL을 기존 NOT NULL 정책에 맞는 값으로 채운다.
-- 2) 추가한 ai_analysis_* 컬럼을 제거한다.
-- 3) ai_comment를 TEXT NOT NULL, compare_comment를 TEXT NOT NULL로 되돌린다.
-- JSON 배열을 일반 텍스트로 되돌릴지 여부는 기존 자산 스냅샷 소비처와 협의한 뒤 결정한다.
