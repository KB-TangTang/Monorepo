-- 20260812_add_financial_sync_tables.sql
-- 이슈 #147 — 금융 데이터 동기화 API
--
-- 신규 설치: 실행 대상이다 (예외 목록에 없음. db/AGENTS.md 참고).
--
-- 이 파일이 하는 일:
--   1) tbl_card / tbl_card_bill / tbl_financial_sync_history 신규 생성
--   2) tbl_transaction 에 소스 구분·카드 연결·거래 연결·원본 보존용 컬럼 추가
--   3) tbl_transaction.account_id 를 NOT NULL -> NULL 허용으로 변경
--      (대출·카드 출처 거래는 tbl_connected_account 에 걸리지 않는다)

USE tangtang;

-- ---------------------------------------------------------------------
-- 카드
-- ---------------------------------------------------------------------
CREATE TABLE tbl_card (
  id                BIGINT        NOT NULL AUTO_INCREMENT,
  user_id           BIGINT        NOT NULL,
  institution_code  VARCHAR(10)   NULL,
  institution_name  VARCHAR(50)   NULL,
  card_no_masked    VARCHAR(30)   NOT NULL,
  product_name      VARCHAR(100)  NULL,
  card_product_code VARCHAR(50)   NULL,
  card_type_code    VARCHAR(10)   NOT NULL COMMENT '01=신용카드, 02=체크카드 (목서버 시드 관례 — 공식 enum 없음)',
  card_status_code  VARCHAR(10)   NULL,
  currency          CHAR(3)       NOT NULL DEFAULT 'KRW',
  issued_at         DATE          NULL,
  last_sync_at      DATETIME      NULL,
  created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_card_user_no (user_id, card_no_masked),
  KEY idx_card_user (user_id),
  CONSTRAINT fk_card_user FOREIGN KEY (user_id) REFERENCES tbl_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='연동된 카드';

CREATE TABLE tbl_card_bill (
  id               BIGINT        NOT NULL AUTO_INCREMENT,
  card_id          BIGINT        NOT NULL,
  billing_month    CHAR(7)       NOT NULL COMMENT 'YYYY-MM',
  due_date         DATE          NULL,
  bill_status_code VARCHAR(10)   NULL,
  bill_status_name VARCHAR(50)   NULL,
  total_amount     DECIMAL(15,2) NOT NULL DEFAULT 0,
  paid_amount      DECIMAL(15,2) NOT NULL DEFAULT 0,
  raw_json         JSON          NULL,
  created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_card_bill_month (card_id, billing_month),
  CONSTRAINT fk_card_bill_card FOREIGN KEY (card_id) REFERENCES tbl_card(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='카드 청구서 (카드 사용 거래와 별개 개념)';

-- ---------------------------------------------------------------------
-- 동기화 이력
-- ---------------------------------------------------------------------
CREATE TABLE tbl_financial_sync_history (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  user_id         BIGINT        NOT NULL,
  status          VARCHAR(20)   NOT NULL COMMENT 'COMPLETED/FAILED',
  synced_sources  JSON          NULL COMMENT '성공한 소스 배열, 예: ["BANK","DEPOSIT"]',
  failed_source   VARCHAR(20)   NULL COMMENT 'BANK/DEPOSIT/SECURITIES/LOAN/PAY_MONEY/CARD',
  fail_reason     VARCHAR(500)  NULL,
  started_at      DATETIME      NOT NULL,
  finished_at     DATETIME      NULL,
  created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_fsh_user_created (user_id, created_at),
  CONSTRAINT fk_fsh_user FOREIGN KEY (user_id) REFERENCES tbl_user(id) ON DELETE CASCADE,
  CONSTRAINT ck_fsh_status CHECK (status IN ('COMPLETED','FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='금융 동기화 이력';

-- ---------------------------------------------------------------------
-- tbl_transaction 확장
-- ---------------------------------------------------------------------
ALTER TABLE tbl_transaction
  MODIFY COLUMN account_id BIGINT NULL COMMENT '대출·카드 출처 거래는 NULL (connected_account 에 안 걸림)';

ALTER TABLE tbl_transaction
  ADD COLUMN source_type VARCHAR(20) NOT NULL DEFAULT 'BANK'
    COMMENT 'BANK/DEPOSIT/SECURITIES/LOAN/PAYMONEY/CARD_CREDIT/CARD_CHECK' AFTER account_id,
  ADD COLUMN card_id BIGINT NULL AFTER loan_id,
  ADD COLUMN correlation_id VARCHAR(100) NULL COMMENT '체크카드 승인 ↔ 은행출금 연결키 (raw_json 유래)',
  ADD COLUMN linked_transaction_id BIGINT NULL COMMENT '연결된 상대 거래 (자기참조)',
  ADD COLUMN original_approval_no VARCHAR(100) NULL COMMENT '카드 취소·환불 원거래 승인번호 (raw_json 유래)',
  ADD COLUMN merchant_category_code VARCHAR(30) NULL COMMENT '후속 카테고리화용 (이번 범위 아님, 보존만)',
  ADD COLUMN merchant_category_name VARCHAR(100) NULL,
  ADD COLUMN raw_json JSON NULL COMMENT '목서버 원본 응답 보존';

ALTER TABLE tbl_transaction
  ADD CONSTRAINT fk_tx_card FOREIGN KEY (card_id) REFERENCES tbl_card(id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_tx_linked FOREIGN KEY (linked_transaction_id) REFERENCES tbl_transaction(id) ON DELETE SET NULL;

ALTER TABLE tbl_transaction
  ADD KEY idx_tx_correlation (user_id, correlation_id),
  ADD KEY idx_tx_source (user_id, source_type);
