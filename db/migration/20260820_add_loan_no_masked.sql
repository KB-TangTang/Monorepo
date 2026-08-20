-- 신규 설치 시 실행: 예. schema.sql → seed.sql 뒤 migration 순서에 따라 실행한다.
-- 목서버가 주는 마스킹 대출번호를 보존해 연결 계좌 관리·완료 화면에 표시한다.
ALTER TABLE tbl_loan
    ADD COLUMN loan_no_masked VARCHAR(100) NULL COMMENT '목서버 제공 마스킹 대출번호';
