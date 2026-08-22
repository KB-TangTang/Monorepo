-- 신규 설치 시 실행: 예. schema.sql → seed.sql 뒤 migration 순서에 따라 실행한다.
--
-- tbl_loan 에 기관코드가 없어(bank_name 만 존재) 자산현황 화면이 대출 계좌만 금융기관 로고를
-- 못 그리고 이니셜 배지로 대체하던 문제를 해소한다. 목서버 financial_institution 에 LOAN 업권
-- (캐피탈 5·저축은행 3)이 추가돼 이제 LoanSyncDto.institutionCode 가 CP_*/SB_* 형식으로 오므로,
-- 그 값을 저장해 InstitutionCatalog.LOANS 코드와 그대로 맞춘다.
ALTER TABLE tbl_loan
    ADD COLUMN bank_code VARCHAR(10) NULL COMMENT '대출 기관코드(캐피탈 CP_*·저축은행 SB_*). 목서버 institutionCode 를 그대로 저장'
    AFTER bank_name;
