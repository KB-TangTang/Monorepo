-- =====================================================================
--  db/schema.sql — 탕탕 전체 DDL (실행 기준 원본)
--
--  원본 : doc/개발산출물/ERD/탕탕_구현용_v2_0803.sql (2026-08-03, MySQL 8.0.46 검증본)
--  동기화: ERD 엑셀(탕탕-V3.xlsx) ≡ 이 파일. 어긋나면 이 파일이 맞다.
--
--  실행
--    로컬 : mysql -u tangtang -p tangtang < db/schema.sql
--    도커 : docker-compose 가 db/ 를 initdb.d 로 마운트해 최초 기동 시 자동 실행
--           (파일명 알파벳순: 00_init_local_db.sql → schema.sql → seed.sql)
--
--  ⚠️ 팀에 공유된 뒤에는 이 파일을 직접 고치지 말고 migration/ 에 변경분을 추가한다.
-- =====================================================================

USE tangtang;

-- =====================================================================
--  탕탕 (TangTang) — 구현용 전체 스키마
--  기준 ERD : doc/개발산출물/ERD/탕탕-V3.xlsx (2026-07-30 갱신본, 33 테이블)
--  생성일   : 2026-07-30 (2026-08-03 갱신: 거래 3분류, verdict_method, ERD 동기화)
--  대상     : MySQL 8.0 / InnoDB / utf8mb4
--
--  ERD 엑셀에는 표현되지 않는 아래 항목을 보강했다.
--    · PRIMARY KEY AUTO_INCREMENT
--    · FOREIGN KEY 제약 + ON DELETE/UPDATE 동작
--    · UNIQUE KEY (중복 방지·upsert 키)
--    · 조회 패턴 기반 보조 인덱스
--    · CHECK 제약 (상태값 도메인)
--    · 생성컬럼(net_amount)
--  ERD와 어긋났던 부분은 [FIX·ERD반영 0803] 주석으로 표시했다.
--  → 2026-08-03 탕탕-V3.xlsx 를 아래 내용대로 교정 완료. 현재 ERD ≡ 본 SQL (차이 없음).
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================================
-- 1. 기준정보 (참조 대상이므로 최우선 생성)
-- =====================================================================

-- ---------------------------------------------------------------------
-- 소비 카테고리
-- ---------------------------------------------------------------------
CREATE TABLE tbl_category (
  id            BIGINT      NOT NULL AUTO_INCREMENT,
  category_name VARCHAR(50) NOT NULL,
  created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_category_name (category_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='표준 소비 카테고리';

-- ---------------------------------------------------------------------
-- 미션 난이도 (tbl_user.difficulty_id 가 참조하므로 tbl_user 보다 먼저)
-- ---------------------------------------------------------------------
CREATE TABLE tbl_mission_difficulty (
  id                 BIGINT       NOT NULL AUTO_INCREMENT,
  difficulty_name    VARCHAR(10)  NOT NULL COMMENT 'EASY/NORMAL/HARD',   -- [FIX·ERD반영 0803] 구 ERD 'VACHAR(10)' 오타
  min_reduction_rate DECIMAL(5,2) NOT NULL COMMENT '절감률 하한(%)',
  max_reduction_rate DECIMAL(5,2) NOT NULL COMMENT '절감률 상한(%)',
  score              INT          NOT NULL COMMENT '성공 시 부여 점수',
  created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_difficulty_name (difficulty_name),
  CONSTRAINT ck_difficulty_name CHECK (difficulty_name IN ('EASY','NORMAL','HARD'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='미션 난이도 정책';

-- ---------------------------------------------------------------------
-- 고정지출 탐지 룰 (DB 관리 — 코드 하드코딩 금지)
-- ---------------------------------------------------------------------
CREATE TABLE tbl_fixed_expense_rule (
  id                   BIGINT       NOT NULL AUTO_INCREMENT,
  rule_name            VARCHAR(50)  NULL,
  min_repeat_count     INT          NOT NULL DEFAULT 3     COMMENT '최소 반복 횟수(2026-07-13 확정값 3)',
  amount_tolerance_pct DECIMAL(5,2) NOT NULL DEFAULT 10.00 COMMENT '금액 오차 허용 %(5~10)',
  cycle_tolerance_days INT          NOT NULL DEFAULT 7     COMMENT '결제 주기 오차 허용일',
  is_active            TINYINT(1)   NOT NULL DEFAULT 1,
  created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,  -- [FIX·ERD반영 0803] 구 ERD 'DATETIMe' 오타
  PRIMARY KEY (id),
  KEY idx_fer_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='고정지출 탐지 룰';

-- ---------------------------------------------------------------------
-- 미션 풀
-- ---------------------------------------------------------------------
CREATE TABLE tbl_mission_pool (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  mission_title   VARCHAR(100) NOT NULL,
  mission_content VARCHAR(255) NOT NULL,
  mission_type    VARCHAR(10)  NOT NULL COMMENT 'ABSOLUTE(절대형=무지출)/RELATIVE(상대형)',
  category_id     BIGINT       NULL,
  limit_price     INT          NULL     COMMENT '절대형은 전 행 0 고정(2026-07-30). v2 상한형 대비 컬럼 유지',
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_mp_type_cat (mission_type, category_id),
  CONSTRAINT fk_mp_category FOREIGN KEY (category_id) REFERENCES tbl_category(id) ON DELETE SET NULL,
  CONSTRAINT ck_mp_type CHECK (mission_type IN ('ABSOLUTE','RELATIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='데일리 미션 풀';

-- =====================================================================
-- 2. 사용자·인증·동의
-- =====================================================================

CREATE TABLE tbl_user (
  id               BIGINT       NOT NULL AUTO_INCREMENT,
  social_provider  VARCHAR(20)  NOT NULL COMMENT '소셜 로그인 제공자 (GOOGLE 등)',
  provider_user_id VARCHAR(100) NOT NULL COMMENT '제공자 측 고유 ID',
  email            VARCHAR(100) NULL,
  nickname         VARCHAR(50)  NULL     COMMENT '서비스 내 표시명',
  name             VARCHAR(50)  NULL     COMMENT '실명(본인확인용)',
  phone            VARCHAR(20)  NULL,
  birth_date       DATE         NULL     COMMENT '연령대 코호트 비교(v2)용',
  gender           VARCHAR(10)  NULL,
  status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/WITHDRAWN/BLOCKED — 물리 삭제 없음',
  withdrawn_at     DATETIME     NULL,
  difficulty_id    BIGINT       NOT NULL COMMENT '현재 선택 난이도. 가입 시 EASY(id=1) 부여',
                                          -- [FIX·ERD반영 0803] 구 ERD Default 'EASY' 는 BIGINT 컬럼에 넣을 수 없어 제거.
                                          --       애플리케이션이 가입 시 EASY의 id를 세팅한다.
  created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_social (social_provider, provider_user_id),
  KEY idx_user_status (status),
  CONSTRAINT fk_user_difficulty FOREIGN KEY (difficulty_id) REFERENCES tbl_mission_difficulty(id),
  CONSTRAINT ck_user_status CHECK (status IN ('ACTIVE','WITHDRAWN','BLOCKED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='사용자';

CREATE TABLE tbl_refresh_token (
  id         BIGINT     NOT NULL AUTO_INCREMENT,
  user_id    BIGINT     NOT NULL,
  token_hash CHAR(64)   NOT NULL COMMENT 'SHA-256 해시. 원문 토큰은 서버 미저장',
  expires_at DATETIME   NOT NULL,
  is_revoked TINYINT(1) NOT NULL DEFAULT 0 COMMENT '탈취 의심/재사용 감지 시 조기 폐기 (AU_02_01)',
  revoked_at DATETIME   NULL,
  created_at DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_rt_hash (token_hash),
  KEY idx_rt_user_valid (user_id, is_revoked, expires_at),
  CONSTRAINT fk_rt_user FOREIGN KEY (user_id) REFERENCES tbl_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='리프레시 토큰';

CREATE TABLE tbl_user_consent (
  id            BIGINT      NOT NULL AUTO_INCREMENT,
  user_id       BIGINT      NOT NULL,
  consent_type  VARCHAR(30) NOT NULL COMMENT 'TERMS/PRIVACY/THIRD_PARTY/FINANCIAL_DATA/MARKETING/AI_USAGE/CHALLENGE',
  is_required   TINYINT(1)  NOT NULL COMMENT '필수/선택 동의 구분',
  terms_version VARCHAR(20) NOT NULL COMMENT '동의 시점의 약관 버전',
  withdrawn_at  DATETIME    NULL     COMMENT '철회 시각. 철회는 이 행을 UPDATE (DS_01_02)',
  status        TINYINT(1)  NOT NULL COMMENT '철회 상태 관리',
  expires_at    DATETIME    NULL     COMMENT 'FINANCIAL_DATA 동의는 1년. NULL=만료 개념 없음',
  created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_consent_user_type (user_id, consent_type),
  KEY idx_consent_expire (expires_at),
  CONSTRAINT fk_consent_user FOREIGN KEY (user_id) REFERENCES tbl_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='약관·금융데이터 동의';

-- =====================================================================
-- 3. 계좌·거래 (CODEF 연동)
-- =====================================================================

CREATE TABLE tbl_connected_account (
  id                   BIGINT        NOT NULL AUTO_INCREMENT,
  user_id              BIGINT        NOT NULL,
  codef_connected_id   VARCHAR(100)  NULL COMMENT 'CODEF 커넥티드아이디',
  bank_code            VARCHAR(10)   NULL COMMENT '금융기관 코드(은행/증권사)',
  bank_name            VARCHAR(50)   NULL,
  account_name         VARCHAR(100)  NULL COMMENT '계좌 상품명 (CODEF resAccountName)',
                                          -- [ADD 0805·#12] 상품명 컬럼이 없어 관리 화면이 "입출금" 같은
                                          -- 일반 라벨밖에 못 그렸다. migration/20260805_add_account_name_to_connected_account.sql
  account_no_encrypted VARCHAR(200)  NULL COMMENT '계좌번호 HMAC-SHA256 (결정적·단방향). 중복 검사 전용',
  account_no_masked    VARCHAR(30)   NULL COMMENT '표시용 마스킹 계좌번호 (110-***-****23)',
                                          -- [ADD 0805·#12] 원본 계좌번호는 서버에 남기지 않는다.
                                          -- UNIQUE KEY 가 이 암호값을 쓰므로 결정적이어야 한다(랜덤 IV 금지).
  account_type         VARCHAR(20)   NOT NULL COMMENT '입출금/예적금/펀드/페이머니/SECURITIES',
  deposit_type_code    VARCHAR(5)    NULL COMMENT '은행 전용 CODEF resAccountDeposit 코드',
  provider             VARCHAR(20)   NULL COMMENT '페이머니 전용: KAKAOPAY/NAVERPAY',
  balance              DECIMAL(15,2) NULL DEFAULT 0,
  sync_status          VARCHAR(20)   NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/SYNCING/NEED_SYNC/FAILED/NEED_RECONNECT',
  last_sync_at         DATETIME      NULL COMMENT '마지막 동기화 성공 시각 (AC_02_04)',
                                          -- [FIX·ERD반영 0803] 최초 연결 직후엔 성공 이력이 없어 NULL 허용 (구 ERD는 NOT NULL)
  sync_fail_reason     VARCHAR(200)  NULL,
  is_active            TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '연결 해제 여부 (AC_01_04)',
  expires_at           DATETIME      NULL COMMENT '재연동(재동의) 만료 예정일. 마이데이터는 1년마다 재동의',
                                          -- [FIX·ERD반영 0803] 구 ERD에서 이 설명이 created_at 코멘트 칸에 잘못 들어가 있었음
  created_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ca_user_account (user_id, account_no_encrypted),
  KEY idx_ca_user_active (user_id, is_active),
  KEY idx_ca_sync (sync_status, expires_at),
  CONSTRAINT fk_ca_user FOREIGN KEY (user_id) REFERENCES tbl_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='연동 계좌';

CREATE TABLE tbl_loan (
  id                BIGINT        NOT NULL AUTO_INCREMENT,
  user_id           BIGINT        NOT NULL,
  loan_no_encrypted VARCHAR(200)  NOT NULL COMMENT 'CODEF 대출 식별값(암호화). 재동기화 upsert 키',
  bank_name         VARCHAR(50)   NOT NULL,
  loan_type         VARCHAR(100)  NOT NULL,
  loan_amount       DECIMAL(15,2) NOT NULL,
  balance           DECIMAL(15,2) NOT NULL COMMENT '완제 시 0으로 갱신(행 삭제 안 함)',
  interest_rate     DECIMAL(5,2)  NOT NULL,
  start_date        DATE          NOT NULL,
  maturity_date     DATE          NOT NULL,
  monthly_payment   DECIMAL(15,2) NOT NULL COMMENT '월 상환액(CODEF 제공 시)',
  next_payment_date DATE          NULL COMMENT '다음 상환 예정일(D-day 표시용)',
  created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_loan_user_no (user_id, loan_no_encrypted),
  KEY idx_loan_user (user_id),
  CONSTRAINT fk_loan_user FOREIGN KEY (user_id) REFERENCES tbl_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='대출';

CREATE TABLE tbl_investment_holding (
  id                     BIGINT         NOT NULL AUTO_INCREMENT,  -- [FIX·ERD반영 0803] 구 ERD 'BIGSERIAL'은 PostgreSQL 타입
                                          -- [0803 결정] 테이블명 investment_holding → tbl_investment_holding (tbl_ 접두어 통일)
  user_id                BIGINT         NOT NULL,
  account_id             BIGINT         NOT NULL,
                                          -- [FIX·ERD반영 0803] 구 ERD에 account_id 가 2행(account_id / account_id2) 중복 정의 → 1개로 통합
  symbol                 VARCHAR(30)    NOT NULL,
  name                   VARCHAR(100)   NOT NULL,
  market_country         CHAR(2)        NOT NULL,
  currency               CHAR(3)        NOT NULL,
  quantity               DECIMAL(20,8)  NOT NULL,
  average_purchase_price DECIMAL(20,4)  NOT NULL,
  last_price             DECIMAL(20,4)  NOT NULL,
  purchase_amount        DECIMAL(20,4)  NOT NULL,
  market_value           DECIMAL(20,4)  NOT NULL,
  profit_loss_amount     DECIMAL(20,4)  NOT NULL,
  profit_loss_rate       DECIMAL(20,4)  NOT NULL,
  created_at             DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at             DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ih_account_symbol (account_id, symbol),
  KEY idx_ih_user (user_id),
  CONSTRAINT fk_ih_user    FOREIGN KEY (user_id)    REFERENCES tbl_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_ih_account FOREIGN KEY (account_id) REFERENCES tbl_connected_account(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='증권 보유상품 (※ 유일하게 tbl_ 접두사가 없는 테이블 — 명명 통일 검토 필요)';

-- ---------------------------------------------------------------------
-- 고정지출 후보 (tbl_transaction 이 참조하므로 먼저 생성)
-- ---------------------------------------------------------------------
CREATE TABLE tbl_fixed_expense_candidate (
  id                       BIGINT        NOT NULL AUTO_INCREMENT,
  user_id                  BIGINT        NOT NULL,
  merchant_name_normalized VARCHAR(200)  NOT NULL,
  avg_amount               DECIMAL(15,2) NOT NULL,
  cycle_days               INT           NOT NULL,
  detected_count           INT           NOT NULL,
  category_id              BIGINT        NULL,
  is_excluded              TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '사용자가 "고정지출 아님"으로 직접 제외',
  status                   VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/BUFFER/VERIFIED_CANCELLED',
  next_expected_date       DATE          NULL COMMENT '검증 루프: 다음 결제 예정일(±7일)',
  verified_at              DATETIME      NULL COMMENT '해지 검증 완료 시각 (FS_02_03)',
  relapse_detected_at      DATETIME      NULL COMMENT '요요 재발 감지 시각 (FS_02_04)',
  created_at               DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at               DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_fec_user_merchant (user_id, merchant_name_normalized),
  KEY idx_fec_user_status (user_id, status, is_excluded),
  KEY idx_fec_next_expected (next_expected_date),
  CONSTRAINT fk_fec_user     FOREIGN KEY (user_id)     REFERENCES tbl_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_fec_category FOREIGN KEY (category_id) REFERENCES tbl_category(id) ON DELETE SET NULL,
  CONSTRAINT ck_fec_status CHECK (status IN ('ACTIVE','BUFFER','VERIFIED_CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='고정지출 후보';

CREATE TABLE tbl_transaction (
  id                       BIGINT        NOT NULL AUTO_INCREMENT,
  user_id                  BIGINT        NOT NULL,
  account_id               BIGINT        NOT NULL,
  codef_tr_key             VARCHAR(100)  NOT NULL COMMENT 'CODEF 거래 고유키(중복 수집 방지). 더미는 DUMMY-{user_id}-{seq}',
  merchant_name            VARCHAR(200)  NULL,
  merchant_name_normalized VARCHAR(200)  NULL COMMENT '정규화명 (고정지출 매칭용)',
  amount                   DECIMAL(15,2) NOT NULL COMMENT '항상 양수. 환불도 양수 + is_refund=1',
  direction                VARCHAR(10)   NULL COMMENT 'IN/OUT',
  tr_date                  DATE          NOT NULL,
  tr_time                  TIME          NULL,
  description1             VARCHAR(200)  NULL,
  description2             VARCHAR(200)  NULL,
  description3             VARCHAR(200)  NULL,
  description4             VARCHAR(200)  NULL,
  cancel_yn                CHAR(1)       NULL COMMENT '카드 승인내역 API resCancelYN',
  is_refund                TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '환불 거래 여부 (GC_05_01)',
  original_transaction_id  BIGINT        NULL COMMENT '환불 대상 원거래',
  refunded_amount          DECIMAL(15,2) NULL COMMENT '이 환불 행이 환불한 금액(부분환불 가능)',
  classification           VARCHAR(20)   NOT NULL COMMENT 'CONSUMPTION/TRANSFER/INCOME (3분류). [2026-08-03] 불가피지출 OBLIGATION은 v1 범위 제외',
  category_id              BIGINT        NULL,
  is_excluded_from_summary TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '가승인-즉시취소 등 집계 제외',
  loan_id                  BIGINT        NULL,
  candidate_id             BIGINT        NULL,
  created_at               DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at               DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tx_codef_key (codef_tr_key),
  -- 일별 카테고리 합계(S) 산출의 주 경로. 미션 판정·리포트 배치가 매일 탄다.
  KEY idx_tx_user_date_cat (user_id, tr_date, category_id, is_excluded_from_summary),
  KEY idx_tx_user_merchant (user_id, merchant_name_normalized, tr_date),
  KEY idx_tx_account_date (account_id, tr_date),
  KEY idx_tx_candidate (candidate_id),
  KEY idx_tx_loan (loan_id),
  KEY idx_tx_original (original_transaction_id),
  CONSTRAINT fk_tx_user      FOREIGN KEY (user_id)     REFERENCES tbl_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_tx_account   FOREIGN KEY (account_id)  REFERENCES tbl_connected_account(id) ON DELETE CASCADE,
  CONSTRAINT fk_tx_category  FOREIGN KEY (category_id) REFERENCES tbl_category(id) ON DELETE SET NULL,
  CONSTRAINT fk_tx_loan      FOREIGN KEY (loan_id)     REFERENCES tbl_loan(id) ON DELETE SET NULL,
  CONSTRAINT fk_tx_candidate FOREIGN KEY (candidate_id) REFERENCES tbl_fixed_expense_candidate(id) ON DELETE SET NULL,
  CONSTRAINT fk_tx_original  FOREIGN KEY (original_transaction_id) REFERENCES tbl_transaction(id) ON DELETE SET NULL,
  CONSTRAINT ck_tx_class CHECK (classification IN ('CONSUMPTION','TRANSFER','INCOME'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='거래내역';

-- ---------------------------------------------------------------------
-- 카테고리 매핑
-- ---------------------------------------------------------------------
CREATE TABLE tbl_merchant_category_map (
  id                       BIGINT       NOT NULL AUTO_INCREMENT,
  merchant_name_normalized VARCHAR(200) NOT NULL,
  category_id              BIGINT       NOT NULL,
  source                   VARCHAR(20)  NOT NULL COMMENT 'MCC/KEYWORD/LLM',
  created_at               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_mcm_merchant (merchant_name_normalized),
  CONSTRAINT fk_mcm_category FOREIGN KEY (category_id) REFERENCES tbl_category(id) ON DELETE CASCADE,
  CONSTRAINT ck_mcm_source CHECK (source IN ('MCC','KEYWORD','LLM'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='전역 가맹점→카테고리 매핑';

CREATE TABLE tbl_user_category_map (
  id                       BIGINT       NOT NULL AUTO_INCREMENT,
  user_id                  BIGINT       NOT NULL,
  merchant_name_normalized VARCHAR(200) NOT NULL,
  category_id              BIGINT       NOT NULL,
  created_at               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ucm_user_merchant (user_id, merchant_name_normalized),
  CONSTRAINT fk_ucm_user     FOREIGN KEY (user_id)     REFERENCES tbl_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_ucm_category FOREIGN KEY (category_id) REFERENCES tbl_category(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='사용자 개인 가맹점→카테고리 매핑(전역보다 우선)';

CREATE TABLE tbl_user_category_pref (
  id          BIGINT     NOT NULL AUTO_INCREMENT,
  user_id     BIGINT     NOT NULL,
  category_id BIGINT     NOT NULL,
  is_hidden   TINYINT(1) NOT NULL DEFAULT 0 COMMENT '표준 카테고리 숨김 여부',
  created_at  DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ucp_user_category (user_id, category_id),
  CONSTRAINT fk_ucp_user     FOREIGN KEY (user_id)     REFERENCES tbl_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_ucp_category FOREIGN KEY (category_id) REFERENCES tbl_category(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='사용자 카테고리 표시 설정';

-- =====================================================================
-- 4. 데일리 미션 (개인 챌린지)
-- =====================================================================

CREATE TABLE tbl_user_mission_info (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  user_id       BIGINT        NOT NULL,
  mission_id    BIGINT        NOT NULL,
  difficulty_id BIGINT        NULL,
  assign_date   DATE          NOT NULL,
  target_rate   DECIMAL(5,2)  NULL COMMENT '적용 절감률(%)',
  target_value  DECIMAL(15,2) NULL COMMENT '목표 상한 T. 상대형=B×(1−절감률), 절대형=0',
  base_amount   DECIMAL(15,2) NULL COMMENT '배정 시점 기준액 B(최근 28일 이용일 하루 합산 지출 평균). 절감액 산정·검산 근거. NULL 허용은 의도적 — B=0(미사용)과 계산 안 됨을 구분',
  result        VARCHAR(10)   NOT NULL DEFAULT 'PENDING' COMMENT 'SUCCESS/FAIL/PENDING',
  evaluated_at  DATETIME      NULL COMMENT '배치가 SUCCESS/FAIL로 확정 판정한 시각',
  created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  -- 미션은 하루 1개. 중복 배정 방지 겸 월 확정 배치의 주 조회 경로.
  UNIQUE KEY uk_umi_user_date (user_id, assign_date),
  KEY idx_umi_user_result (user_id, assign_date, result),
  KEY idx_umi_difficulty (difficulty_id),
  KEY idx_umi_mission (mission_id),
  CONSTRAINT fk_umi_user       FOREIGN KEY (user_id)       REFERENCES tbl_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_umi_mission    FOREIGN KEY (mission_id)    REFERENCES tbl_mission_pool(id),
  CONSTRAINT fk_umi_difficulty FOREIGN KEY (difficulty_id) REFERENCES tbl_mission_difficulty(id),
  CONSTRAINT ck_umi_result CHECK (result IN ('SUCCESS','FAIL','PENDING'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='일별 미션 배정·판정 원장';

CREATE TABLE tbl_streak_count (
  user_id              BIGINT   NOT NULL,
                                -- [FIX·ERD반영 0803] 구 ERD Logical='id' / Physical='user_id' 불일치 → user_id 로 통일
  streak_count         BIGINT   NULL DEFAULT 0 COMMENT '현재 연속 성공 일수',
  longest_streak_count BIGINT   NULL DEFAULT 0 COMMENT '미션 첫 시작부터 last_check_date까지 최장 연속 일수',
  last_check_date      DATETIME NOT NULL,
  created_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                -- [FIX·ERD반영 0803] 구 ERD Physical명이 Field/Field2/Field3/Field4/Field5 로 미기입 → Logical명 사용
  PRIMARY KEY (user_id),
  CONSTRAINT fk_sc_user FOREIGN KEY (user_id) REFERENCES tbl_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='연속 성공 스트릭';

-- =====================================================================
-- 5. 그룹 챌린지 · 재판
-- =====================================================================

CREATE TABLE tbl_challenge_group (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  admin_id     BIGINT       NOT NULL COMMENT '방장',
  group_name   VARCHAR(100) NOT NULL,
  category_id  BIGINT       NULL COMMENT 'NULL=총소비 챌린지 / NOT NULL=카테고리별 챌린지',
  limit_amount INT          NOT NULL,
  eval_type    VARCHAR(10)  NOT NULL COMMENT 'DAILY(일일평가)/PERIOD(기간평가)',
  max_members  INT          NOT NULL DEFAULT 6 COMMENT '정원(2~6명)',
  start_date   DATE         NOT NULL,
  end_date     DATE         NOT NULL COMMENT '시작일로부터 최대 7일',
  invite_code  VARCHAR(20)  NOT NULL,
  status       VARCHAR(20)  NOT NULL DEFAULT 'RECRUITING' COMMENT 'RECRUITING/ACTIVE/JUDGING/CLOSED',
  memo         VARCHAR(300) NULL COMMENT '리워드·벌칙 자유 메모(표시 전용, 시스템 미집행)',
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cg_invite (invite_code),
  -- 월 귀속은 end_date 기준(2026-07-30 확정). 리포트 조회가 이 인덱스를 탄다.
  KEY idx_cg_end_status (end_date, status),
  KEY idx_cg_status_start (status, start_date),
  KEY idx_cg_admin (admin_id),
  KEY idx_cg_category (category_id),
  CONSTRAINT fk_cg_admin    FOREIGN KEY (admin_id)    REFERENCES tbl_user(id),
  CONSTRAINT fk_cg_category FOREIGN KEY (category_id) REFERENCES tbl_category(id) ON DELETE SET NULL,
  CONSTRAINT ck_cg_eval   CHECK (eval_type IN ('DAILY','PERIOD')),
  CONSTRAINT ck_cg_status CHECK (status IN ('RECRUITING','ACTIVE','JUDGING','CLOSED')),
  -- [0803 결정] 문서상 '시작일로부터 최대 7일'을 DB 제약으로 강제(경계 포함: start+6일)
  CONSTRAINT ck_cg_period CHECK (end_date >= start_date AND end_date <= start_date + INTERVAL 6 DAY),
  CONSTRAINT ck_cg_members CHECK (max_members BETWEEN 2 AND 6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='커스텀 그룹 챌린지';

CREATE TABLE tbl_group_member (
  group_id            BIGINT        NOT NULL,
  user_id             BIGINT        NOT NULL,
  lives_count         INT           NOT NULL COMMENT '일일평가=챌린지 기간(일수), 기간평가=1 고정',
  final_outcome       VARCHAR(20)   NULL COMMENT 'SURVIVED/ELIMINATED. 종료 다음날 확정 배치가 final_rank와 동시 기록(write-once). NULL=미확정',
                                          -- [FIX·ERD반영 0803] 구 ERD VARCHAR(10) → 'ELIMINATED'가 정확히 10자로 여유 0.
                                          --                          ERD 전체가 상태 컬럼을 VARCHAR(20)으로 통일하고 있어 20으로 맞춤.
  final_rank          INT           NULL COMMENT '종료 후 배치에서 확정. 탈락자는 전원 공동 최하위',
  final_charge_amount DECIMAL(15,2) NULL COMMENT '최종 실제 부담금(write-once, final_rank와 동일 시점)',
  joined_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (group_id, user_id),
  -- PK가 (group_id, user_id)라 user_id 단독 조회는 인덱스를 못 탄다.
  -- CR_01_06 그룹 전적 집계의 필수 인덱스(커버링).
  KEY idx_gm_user (user_id, final_outcome),
  CONSTRAINT fk_gm_group FOREIGN KEY (group_id) REFERENCES tbl_challenge_group(id) ON DELETE CASCADE,
  CONSTRAINT fk_gm_user  FOREIGN KEY (user_id)  REFERENCES tbl_user(id) ON DELETE CASCADE,
  CONSTRAINT ck_gm_outcome CHECK (final_outcome IN ('SURVIVED','ELIMINATED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='그룹 참여자';

CREATE TABLE tbl_group_challenge_daily_result (
  id             BIGINT        NOT NULL AUTO_INCREMENT,
  group_id       BIGINT        NOT NULL,
  user_id        BIGINT        NOT NULL,
  challenge_date DATE          NOT NULL,
  daily_amount   DECIMAL(15,2) NOT NULL COMMENT '1시간마다 갱신. 변론 후 정산액 반영된 값이 최종',
  target_count   INT           NOT NULL COMMENT '미션 해당 타겟의 소비 횟수',
  created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_gcdr_group_user_date (group_id, user_id, challenge_date),
  KEY idx_gcdr_user_date (user_id, challenge_date),
  CONSTRAINT fk_gcdr_group FOREIGN KEY (group_id) REFERENCES tbl_challenge_group(id) ON DELETE CASCADE,
  CONSTRAINT fk_gcdr_user  FOREIGN KEY (user_id)  REFERENCES tbl_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='그룹 일별 소비 집계';

CREATE TABLE tbl_indictment (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  result_id  BIGINT       NOT NULL COMMENT '기소 대상 일별 결과',
  group_id   BIGINT       NOT NULL,
  user_id    BIGINT       NOT NULL COMMENT '피기소자',
  status     VARCHAR(20)  NOT NULL DEFAULT 'DEFENSE_WAIT'
             COMMENT 'DEFENSE_WAIT/VOTING/GUILTY/INNOCENT. 판결 확정 후 재심·재투표 없음(단방향)',
             -- [0803] 마감 시각 전용 컬럼은 두지 않는다. 배치·스케줄러가 아래 규칙으로 계산한다.
             --   변론 마감 = created_at + 6시간 (GC_06_01) → 이 시점에 DEFENSE_WAIT → VOTING
             --   투표 마감 = 변론 마감 + 24시간 = created_at + 30시간 (GC_06_02) → 개표
             --   ※ 마감 연장·일시정지 정책이 생기면 deadline 컬럼 도입 필요(팀 확인 대상)
  message    VARCHAR(300) NULL,
  result     TINYINT(1)   NULL COMMENT '판결 요약 플래그. 1=GUILTY / 0=INNOCENT / NULL=미확정. status와 동일 정보이며 집계 쿼리 편의용(핵심로직 D-3·무투표마감·혐의인정 3곳이 이 규칙으로 기록)',
  verdict_method VARCHAR(20) NULL
             COMMENT '판결 근거. VOTE(다수결)/NO_VOTE(0표 무죄추정)/SCRATCH_LOTTERY(동률)/CONFESSION(혐의 인정, GC_06_06). 확정 시 기록',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                -- [FIX·ERD반영 0803] 구 ERD Physical명 'update_at' 오타 → updated_at
  PRIMARY KEY (id),
  -- CR_01_06 피기소·유죄·무죄 카운트용 커버링 인덱스
  KEY idx_ind_user_status (user_id, status),
  KEY idx_ind_group (group_id, status),
  KEY idx_ind_result (result_id),
  CONSTRAINT fk_ind_result FOREIGN KEY (result_id) REFERENCES tbl_group_challenge_daily_result(id) ON DELETE CASCADE,
  CONSTRAINT fk_ind_group  FOREIGN KEY (group_id)  REFERENCES tbl_challenge_group(id) ON DELETE CASCADE,
  CONSTRAINT fk_ind_user   FOREIGN KEY (user_id)   REFERENCES tbl_user(id) ON DELETE CASCADE,
  CONSTRAINT ck_ind_status CHECK (status IN ('DEFENSE_WAIT','VOTING','GUILTY','INNOCENT')),
  -- [0803 결정] result 와 status 가 어긋나지 않도록 DB에서 강제
  CONSTRAINT ck_ind_result CHECK ((status='GUILTY' AND result=1) OR (status='INNOCENT' AND result=0)
                                  OR (status IN ('DEFENSE_WAIT','VOTING') AND result IS NULL)),
  CONSTRAINT ck_ind_verdict_method CHECK (verdict_method IS NULL OR verdict_method IN ('VOTE','NO_VOTE','SCRATCH_LOTTERY','CONFESSION'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='기소';
-- [2026-08-03] 혐의 인정(GC_06_06): status를 DEFENSE_WAIT → GUILTY로 바로 전이하고
--   verdict_method='CONFESSION'을 기록한다. 투표(tbl_vote)·변론(tbl_defense) 행은 생성하지 않으며,
--   조정 요청(tbl_defense.requested_adjustment_amount)도 없으므로 랭킹엔 원 거래 금액이 그대로 반영된다.
--   기존 랭킹·목숨 차감 쿼리는 status='GUILTY' 기준이라 수정 없이 그대로 동작한다.

CREATE TABLE tbl_defense (
  id                          BIGINT        NOT NULL AUTO_INCREMENT,
  indictment_id               BIGINT        NOT NULL,
  content                     TEXT          NULL,
  requested_adjustment_amount DECIMAL(15,2) NULL
    COMMENT '정산 조정 요청 금액. 무죄와 함께 자동 승인, 유죄면 자동 기각. 0 이상 — 원 거래액 초과 여부는 애플리케이션이 검증(원 거래액이 이 테이블에 없음)',
  created_at                  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at                  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_def_indictment (indictment_id),  -- 기소 1건당 변론 1건
  CONSTRAINT fk_def_indictment FOREIGN KEY (indictment_id) REFERENCES tbl_indictment(id) ON DELETE CASCADE,
  -- [0803 결정] 음수 조정 요청 차단. 상한(원 거래액)은 원장 조회가 필요해 애플리케이션 검증
  CONSTRAINT ck_def_adjust CHECK (requested_adjustment_amount IS NULL OR requested_adjustment_amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='변론';

CREATE TABLE tbl_defense_images (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  defense_id BIGINT       NOT NULL,
  image_url  VARCHAR(100) NOT NULL,
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP,
                                -- 삽입 전용 테이블이라 최초 INSERT 시 NULL(수정된 적 없음). 수정 시에만 자동 기록
  PRIMARY KEY (id),
  KEY idx_di_defense (defense_id),
  CONSTRAINT fk_di_defense FOREIGN KEY (defense_id) REFERENCES tbl_defense(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='변론 증빙 이미지';

CREATE TABLE tbl_vote (
  group_id      BIGINT       NOT NULL,
  user_id       BIGINT       NOT NULL COMMENT '투표자',
  indictment_id BIGINT       NOT NULL,
  verdict       VARCHAR(10)  NOT NULL COMMENT 'GUILTY/INNOCENT',
  comment       VARCHAR(40)  NULL COMMENT '익명 한줄 코멘트. GC_06_05 최대 40자',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (group_id, user_id, indictment_id),
  KEY idx_vote_indictment (indictment_id, verdict),  -- 개표 집계용 커버링
  KEY idx_vote_user (user_id),
  CONSTRAINT fk_vote_group      FOREIGN KEY (group_id)      REFERENCES tbl_challenge_group(id) ON DELETE CASCADE,
  CONSTRAINT fk_vote_user       FOREIGN KEY (user_id)       REFERENCES tbl_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_vote_indictment FOREIGN KEY (indictment_id) REFERENCES tbl_indictment(id) ON DELETE CASCADE,
  CONSTRAINT ck_vote_verdict CHECK (verdict IN ('GUILTY','INNOCENT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='재판 투표';

-- =====================================================================
-- 6. 리포트 · 랭킹
-- =====================================================================

CREATE TABLE tbl_monthly_ranking (
  user_id                BIGINT        NOT NULL,
  `year_month`           CHAR(7)       NOT NULL COMMENT 'YYYY-MM',
  total_score            INT           NOT NULL COMMENT '그 달 획득 점수',
  ranking                INT           NOT NULL COMMENT '월간 순위',
  ratio                  DECIMAL(15,2) NOT NULL COMMENT '상위 퍼센트',
  monthly_longest_streak INT           NULL COMMENT '그 달 최장 연속 성공일',
  success_ratio          DECIMAL(15,2) NULL
    COMMENT '월간 미션 성공률(%). 분모는 판정 완료된 미션 배정 일수 — tbl_challenge_monthly_report.total_days와 동일 정의(2026-07-30 확정)',
  created_at             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    COMMENT '마지막 배치 갱신 시각. 당월엔 매일 갱신, 지난달은 마지막 갱신값이 곧 확정 랭킹',
  PRIMARY KEY (user_id, `year_month`),
  KEY idx_mr_month_rank (`year_month`, ranking),  -- 월별 랭킹 보드 조회
  CONSTRAINT fk_mr_user FOREIGN KEY (user_id) REFERENCES tbl_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='월간 랭킹';

CREATE TABLE tbl_challenge_monthly_report (
  id                      BIGINT        NOT NULL AUTO_INCREMENT,
  user_id                 BIGINT        NOT NULL,
  `year_month`            CHAR(7)       NOT NULL COMMENT 'YYYY-MM. tbl_monthly_ranking과 동일 키',
  total_days              SMALLINT      NOT NULL COMMENT '판정 완료된 미션 배정 일수(PENDING·base_amount NULL 제외). 실패일 = total_days - success_days',
  success_days            SMALLINT      NOT NULL COMMENT '해당 월 미션 성공 일수',
  saved_amount            DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT '성공일 Σ(B−S) · 화면 "덜 쓴 돈"',
  over_amount             DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT '실패일 Σmax(S−B,0) · 화면 "더 쓴 돈". 실패일 절약분 불인정',
  net_amount              DECIMAL(15,2) AS (saved_amount - over_amount) STORED
                                        COMMENT '순 효과 · 히어로 숫자 겸 보관함 정렬키',
                                        -- [FIX·ERD반영 0803] ERD에는 생성컬럼 표기 추가. MySQL 5.7 미만이면 일반 컬럼으로 바꾸고 배치가 채운다.
  category_results_json   JSON          NULL COMMENT 'CR_01_03 카테고리 × 성공/실패 breakdown',
  weekly_results_json     JSON          NULL COMMENT '주차별 도전·성공 일수(비율 아님). 달력 주(월요일 시작), 월 경계 제외',
  difficulty_results_json JSON          NULL COMMENT 'CR_01_05 난이도별 도전·성공',
  best_saving_date        DATE          NULL COMMENT '그 달 최고 절약일 트로피 카드',
  best_saving_amount      DECIMAL(15,2) NULL,
  calculation_version     VARCHAR(20)   NOT NULL COMMENT '산정 로직 버전. 예: v2026-07-30',
  created_at              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '월 확정 배치 실행 시각 = 확정 시각',
  updated_at              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                                        COMMENT '로직 개정으로 과거 월 재산출 시에만 변경',
  PRIMARY KEY (id),
  UNIQUE KEY uk_cmr_user_month (user_id, `year_month`),
  KEY idx_cmr_user_month_desc (user_id, `year_month` DESC),  -- 보관함 목록(FIX_F) 정렬
  CONSTRAINT fk_cmr_user FOREIGN KEY (user_id) REFERENCES tbl_user(id) ON DELETE CASCADE,
  CONSTRAINT ck_cmr_days CHECK (success_days <= total_days)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='챌린지 리포트 월 확정 스냅샷. 익월 1일 배치 1회 INSERT, write-once. 당월 행은 생성하지 않음';

CREATE TABLE tbl_asset_snapshot (
  id                    BIGINT        NOT NULL AUTO_INCREMENT,
  user_id               BIGINT        NOT NULL,
  `year_month`          CHAR(7)       NOT NULL COMMENT 'YYYY-MM',
  net_worth             DECIMAL(15,2) NULL COMMENT '월말 순자산 = total_asset - total_debt (AS_02_03)',
  total_asset           DECIMAL(15,2) NULL,
  total_debt            DECIMAL(15,2) NULL COMMENT '월말 부채 총액 (tbl_loan SUM 대신 배치 확정 시 저장)',
  category_summary_json JSON          NULL COMMENT '카테고리별 소비 합계 {"FOOD_01":350000,...}',
  ai_comment            TEXT          NOT NULL,
  compare_comment       TEXT          NOT NULL,
  created_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_as_user_month (user_id, `year_month`),
  CONSTRAINT fk_as_user FOREIGN KEY (user_id) REFERENCES tbl_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='월간 자산 스냅샷';

CREATE TABLE tbl_budget (
  id             BIGINT        NOT NULL AUTO_INCREMENT,
  user_id        BIGINT        NOT NULL,
  `year_month`   CHAR(7)       NOT NULL COMMENT 'YYYY-MM',
  category_id    BIGINT        NULL COMMENT 'NULL=월 총예산(BU_01_01) / NOT NULL=카테고리별(BU_01_02)',
  category_key   BIGINT        NOT NULL COMMENT 'category_id가 NULL이면 -1, 아니면 category_id와 동일. UNIQUE 성립용',
  budget_amount  DECIMAL(15,2) NOT NULL,
  created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  -- category_id가 NULL이면 MySQL UNIQUE가 중복을 허용하므로 category_key로 대체한다(ERD 주석의 의도).
  UNIQUE KEY uk_budget_user_month_cat (user_id, `year_month`, category_key),
  CONSTRAINT fk_budget_user     FOREIGN KEY (user_id)     REFERENCES tbl_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_budget_category FOREIGN KEY (category_id) REFERENCES tbl_category(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='예산 (2026-07-26 기능 범위 제외, 스키마만 유지)';

-- =====================================================================
-- 7. 알림
-- =====================================================================

CREATE TABLE tbl_notification (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  user_id       BIGINT       NOT NULL,
  type          VARCHAR(30)  NOT NULL COMMENT 'DIET_REPORT/CHALLENGE_RESULT/GROUP_JUDGMENT 등',
  title         VARCHAR(100) NOT NULL,
  content       VARCHAR(300) NOT NULL,
  deep_link_url VARCHAR(300) NULL COMMENT '알림 클릭 시 이동할 딥링크 (NT_01_03)',
  is_read       TINYINT(1)   NOT NULL DEFAULT 0,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_noti_user_read (user_id, is_read, created_at DESC),
  CONSTRAINT fk_noti_user FOREIGN KEY (user_id) REFERENCES tbl_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='알림 내역';

CREATE TABLE tbl_notification_setting (
  id                BIGINT      NOT NULL AUTO_INCREMENT,
  user_id           BIGINT      NOT NULL,
  notification_type VARCHAR(30) NULL COMMENT '알림 종류별 개별 설정은 추후 확장. 현재는 NULL',
                                -- [FIX·ERD반영 0803] 구 ERD는 NOT NULL인데 코멘트가 "현재는 NULL로 설정"이라 모순 → NULL 허용
  is_enabled        TINYINT(1)  NOT NULL DEFAULT 1,
  created_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                -- [ERD반영 0803] 구 컬럼명 update_date → 전 테이블 updated_at 통일
  PRIMARY KEY (id),
  KEY idx_ns_user (user_id),
  CONSTRAINT fk_ns_user FOREIGN KEY (user_id) REFERENCES tbl_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='알림 설정';

CREATE TABLE tbl_notification_dlq (
  dlq_id              BIGINT       NOT NULL AUTO_INCREMENT,
  original_event_type VARCHAR(50)  NULL,
  payload_json        JSON         NULL,
  error_message       VARCHAR(500) NULL,
  retry_count         INT          NOT NULL DEFAULT 0,
  reg_date            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (dlq_id),
  KEY idx_dlq_retry (retry_count, reg_date)  -- 스케줄 재시도 배치 조회
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='알림 발송 실패 DLQ (브로커 없는 구조의 대체 수단)';

-- =====================================================================
-- 8. 외부 API 캐시
-- =====================================================================

CREATE TABLE tbl_savings_product_cache (
  product_id                 BIGINT        NOT NULL AUTO_INCREMENT,
  external_product_code      VARCHAR(50)   NOT NULL COMMENT '금감원 API 상품고유코드. 주 1회 배치 upsert 키',
  bank_name                  VARCHAR(50)   NOT NULL,
  product_name               VARCHAR(100)  NOT NULL,
  product_type               VARCHAR(10)   NOT NULL COMMENT '예금/적금',
  term_months                INT           NULL,
  base_rate                  DECIMAL(5,2)  NULL,
  preferential_rate          DECIMAL(5,2)  NULL,
  preferential_condition_raw VARCHAR(1000) NULL,
  is_active                  TINYINT(1)    NOT NULL DEFAULT 1,
  fetched_at                 DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at                 DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,  -- [ERD반영 0803] 구 update_date
  PRIMARY KEY (product_id),
  UNIQUE KEY uk_spc_external (external_product_code),
  KEY idx_spc_active_rate (is_active, base_rate DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='저축상품 캐시';

CREATE TABLE tbl_youth_policy_cache (
  policy_id          BIGINT       NOT NULL AUTO_INCREMENT,
  external_policy_id VARCHAR(50)  NOT NULL COMMENT '온통청년 API 정책고유ID. 일 1회 배치 upsert 키',
  policy_name        VARCHAR(200) NOT NULL,
  category           VARCHAR(50)  NULL,
  min_age            INT          NULL,
  max_age            INT          NULL,
  income_condition   VARCHAR(200) NULL,
  zip_cd             TEXT         NULL COMMENT '신청 가능 시군구 코드 CSV. FIND_IN_SET으로 필터',
  is_nationwide      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '1이면 지역 매칭 스킵',
  apply_start_date   DATE         NULL,
  apply_end_date     DATE         NULL,
  apply_url          VARCHAR(300) NULL,
  is_active          TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '조회 시 apply_end_date>=CURDATE() 이중 필터 권장',
  description        VARCHAR(500) NULL,
  updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,  -- [ERD반영 0803] 구 update_date
  PRIMARY KEY (policy_id),
  UNIQUE KEY uk_ypc_external (external_policy_id),
  KEY idx_ypc_active_age (is_active, min_age, max_age),
  KEY idx_ypc_nationwide (is_nationwide, apply_end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='청년정책 캐시';

SET FOREIGN_KEY_CHECKS = 1;
