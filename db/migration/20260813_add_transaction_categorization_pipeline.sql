-- 20260813_add_transaction_categorization_pipeline.sql
-- 이슈 #147 — 거래 카테고리화 파이프라인 (규칙 1~4단계 + LLM 5단계 작업 큐)
--
-- 신규 설치: 실행 대상이다 (예외 목록에 없음. db/AGENTS.md 참고).
--
-- 배경
--   1~2단계(사용자/공용 가맹점 매핑)는 기존 tbl_user_category_map · tbl_merchant_category_map을
--   그대로 재사용한다(신규 테이블 아님). 3단계(MCC/업종명)도 tbl_merchant_category_map을
--   merchant_category_name 정규화 값으로 조회하는 방식이라 신규 테이블이 필요 없다.
--   4단계(키워드)만 부분매칭·우선순위가 필요해 tbl_merchant_keyword_rule을 신규로 둔다.
--   5단계(LLM)는 작업 배치 단위를 표현할 테이블이 아예 없어 tbl_llm_categorization_job(배치 헤더)과
--   tbl_llm_categorization_job_item(배치에 속한 거래)을 신규로 둔다.

USE tangtang;

CREATE TABLE tbl_merchant_keyword_rule (
  id                 BIGINT       NOT NULL AUTO_INCREMENT,
  keyword             VARCHAR(100) NOT NULL COMMENT '원본 키워드 (화면 표시·관리용)',
  keyword_normalized  VARCHAR(100) NOT NULL COMMENT '정규화된 키워드 (매칭 키). 공백/특수문자 제거 + 소문자',
  category_id         BIGINT       NOT NULL,
  created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_mkr_keyword_normalized (keyword_normalized),
  KEY idx_mkr_category (category_id),
  CONSTRAINT fk_mkr_category FOREIGN KEY (category_id) REFERENCES tbl_category(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='가맹점명 키워드 → 카테고리 규칙(부분매칭). keyword_normalized 가 길수록(더 구체적일수록) 우선한다';

CREATE TABLE tbl_llm_categorization_job (
  id                BIGINT        NOT NULL AUTO_INCREMENT,
  user_id           BIGINT        NOT NULL,
  status            VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/COMPLETED/FAILED',
  transaction_count INT           NOT NULL,
  started_at        DATETIME      NULL,
  finished_at       DATETIME      NULL,
  created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_lcj_user_status (user_id, status),
  CONSTRAINT fk_lcj_user FOREIGN KEY (user_id) REFERENCES tbl_user(id) ON DELETE CASCADE,
  CONSTRAINT ck_lcj_status CHECK (status IN ('PENDING','PROCESSING','COMPLETED','FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='LLM 카테고리 분류 작업 배치. 사용자별 transaction_date 오름차순 최대 20건 단위';

CREATE TABLE tbl_llm_categorization_job_item (
  id             BIGINT   NOT NULL AUTO_INCREMENT,
  job_id         BIGINT   NOT NULL,
  transaction_id BIGINT   NOT NULL,
  created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_lcji_transaction (transaction_id),
  KEY idx_lcji_job (job_id),
  CONSTRAINT fk_lcji_job FOREIGN KEY (job_id) REFERENCES tbl_llm_categorization_job(id) ON DELETE CASCADE,
  CONSTRAINT fk_lcji_transaction FOREIGN KEY (transaction_id) REFERENCES tbl_transaction(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='LLM 작업에 포함된 거래. transaction_id 전역 UNIQUE — 같은 거래가 재동기화로 여러 작업에 중복 등록되지 않는다';

-- =====================================================================
-- 적용 확인 (선택)
-- =====================================================================
-- SHOW CREATE TABLE tbl_merchant_keyword_rule\G
-- SHOW CREATE TABLE tbl_llm_categorization_job\G
-- SHOW CREATE TABLE tbl_llm_categorization_job_item\G
