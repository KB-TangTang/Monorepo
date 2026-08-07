-- 2026-08-05 · 이슈 #12 계좌 연동
-- tbl_connected_account 에 계좌 상품명(account_name) 추가
--
-- 배경
--   CODEF·목서버는 계좌 상품명(resAccountName / accountName)을 내려주는데
--   우리 테이블에는 담을 컬럼이 없었다. 그래서 연결 계좌 관리 화면(AC_01_04)이
--   "KB국민은행 · 입출금" 같은 일반 라벨밖에 못 그렸고, 같은 은행에 입출금 계좌가
--   둘이면 마스킹된 계좌번호 뒷자리로만 구분됐다.
--   확정 디자인(21_connected_accounts)은 "KB 정기적금" · "신한 Deep Dream" 처럼
--   상품명을 그대로 보여준다.
--
-- 적용 방법 (팀원 각자 1회)
--   mysql -u tangtang -p tangtang --default-character-set=utf8mb4 \
--     -e "source D:/KB_Final_Project/app/Monorepo/db/migration/20260805_add_account_name_to_connected_account.sql"
--   PowerShell 은 < 리다이렉션을 지원하지 않으므로 위처럼 source 를 쓴다.
--
-- 신규 설치는 schema.sql 에 이미 반영돼 있어 이 파일을 실행할 필요가 없다.

ALTER TABLE tbl_connected_account
  ADD COLUMN account_name VARCHAR(100) NULL COMMENT '계좌 상품명 (CODEF resAccountName)'
  AFTER bank_name;

-- 화면 표시용 마스킹 계좌번호
--
-- 배경
--   기존에는 account_no_encrypted 하나뿐이라 화면에 보여줄 값을 담을 곳이 없었다.
--   그 컬럼은 UNIQUE KEY (user_id, account_no_encrypted) 로 중복 연결을 막는 열쇠라
--   같은 입력에 항상 같은 결과여야 한다(랜덤 IV 를 쓰는 AES-GCM 은 매번 달라져 중복 검사가 무력화된다).
--   그래서 역할을 나눈다.
--     account_no_encrypted : HMAC-SHA256 (결정적·단방향) — 중복 검사 전용
--     account_no_masked    : 110-***-****23 — 표시 전용
--   원본 계좌번호는 서버 어디에도 남지 않는다. 복호화 키를 관리할 필요도 없다.
ALTER TABLE tbl_connected_account
  ADD COLUMN account_no_masked VARCHAR(30) NULL COMMENT '표시용 마스킹 계좌번호'
  AFTER account_no_encrypted;
