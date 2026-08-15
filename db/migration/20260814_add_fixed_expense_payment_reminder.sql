-- [업그레이드/신규 설치 공통] 고정지출 결제 예정 알림의 발송 이력을 저장한다.
--
-- 같은 고정지출 후보·다음 결제 예정일·알림 유형에는 한 번만 발송한다.
-- 다음 예정일이 바뀌면 새 결제 주기로 취급해 새 발송 이력을 만들 수 있다.
CREATE TABLE tbl_fixed_expense_payment_reminder (
    id                         BIGINT      NOT NULL AUTO_INCREMENT,
    fixed_expense_candidate_id BIGINT      NOT NULL,
    expected_payment_date      DATE        NOT NULL,
    notification_type          VARCHAR(30) NOT NULL,
    created_at                 DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_fepr_candidate_due_type
        (fixed_expense_candidate_id, expected_payment_date, notification_type),
    CONSTRAINT fk_fepr_candidate
        FOREIGN KEY (fixed_expense_candidate_id)
        REFERENCES tbl_fixed_expense_candidate(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='고정지출 결제 예정 알림 발송 이력';

-- 롤백은 수동으로 수행한다.
-- DROP TABLE tbl_fixed_expense_payment_reminder;
