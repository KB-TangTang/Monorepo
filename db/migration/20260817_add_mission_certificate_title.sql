-- [신규 설치·기존 설치 공통] 개인 미션 명예 인증서 AI 타이틀 저장 테이블
--
-- 신규 설치 순서:
--   1. db/schema.sql 실행
--   2. 필요한 seed SQL 실행
--   3. db/migration/ 파일을 날짜순으로 실행
--
-- 인증서 화면 조회 시 외부 AI를 재호출하지 않도록 사용자·월별 제목 후보와 생성 상태를 보관한다.

CREATE TABLE tbl_mission_certificate_title (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    user_id               BIGINT       NOT NULL,
    `year_month`          CHAR(7)      NOT NULL COMMENT '인증서 대상 월(YYYY-MM)',
    title_1               VARCHAR(50)  NULL COMMENT 'AI 명예 타이틀 후보 1',
    title_2               VARCHAR(50)  NULL COMMENT 'AI 명예 타이틀 후보 2',
    title_3               VARCHAR(50)  NULL COMMENT 'AI 명예 타이틀 후보 3',
    status                VARCHAR(20)  NOT NULL DEFAULT 'NOT_REQUESTED'
        COMMENT 'NOT_REQUESTED, IN_PROGRESS, COMPLETED, FAILED',
    provider              VARCHAR(30)  NULL COMMENT 'OPENAI 또는 FALLBACK',
    model                 VARCHAR(100) NULL COMMENT '생성에 사용한 모델명',
    prompt_version        VARCHAR(40)  NULL COMMENT '타이틀 프롬프트 버전',
    input_hash            CHAR(64)     NULL COMMENT '비식별 성과 집계 입력 SHA-256',
    attempt_count         INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'AI 생성 시도 횟수',
    requested_at          DATETIME     NULL COMMENT 'AI 생성 요청 시각',
    completed_at          DATETIME     NULL COMMENT 'AI 생성 완료 시각',
    failed_at             DATETIME     NULL COMMENT 'AI 생성 실패 시각',
    failure_code          VARCHAR(64)  NULL COMMENT '외부 API 실패 코드',
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mission_certificate_title_user_month (user_id, `year_month`),
    KEY idx_mission_certificate_title_status (status, failed_at),
    CONSTRAINT fk_mission_certificate_title_user
        FOREIGN KEY (user_id) REFERENCES tbl_user(id) ON DELETE CASCADE,
    CONSTRAINT ck_mission_certificate_title_status
        CHECK (status IN ('NOT_REQUESTED', 'IN_PROGRESS', 'COMPLETED', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='개인 미션 월간 명예 인증서 AI 타이틀';

-- 롤백: DROP TABLE tbl_mission_certificate_title;
