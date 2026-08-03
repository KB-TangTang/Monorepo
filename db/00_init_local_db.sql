-- 로컬 개발용 DB · 계정 초기화 스크립트
-- MySQL 관리자(root) 계정으로 1회 실행한다.
--   mysql -u root -p < db/00_init_local_db.sql
-- 또는 MySQL Workbench 에서 열어 실행.
--
-- ⚠️ 실행 전, 아래 CHANGE_ME_DB_PASSWORD 를 본인이 정한 비밀번호로 바꾼다.
--    같은 값을 다음 두 곳에도 넣어야 한다. 셋이 다르면 접속이 실패한다.
--      - .env                                             (MYSQL_PASSWORD)
--      - apps/api/src/main/resources/application-local.properties (jdbc.password)
--    이 파일은 git 추적 대상이므로, 바꾼 비밀번호를 그대로 커밋하지 않는다.
--    (커밋 전에 CHANGE_ME_DB_PASSWORD 로 되돌리거나 `git checkout db/00_init_local_db.sql`)
--
-- 운영 환경에서는 절대 이 계정을 사용하지 않는다.

CREATE DATABASE IF NOT EXISTS tangtang
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

CREATE USER IF NOT EXISTS 'tangtang'@'localhost' IDENTIFIED BY 'CHANGE_ME_DB_PASSWORD';
GRANT ALL PRIVILEGES ON tangtang.* TO 'tangtang'@'localhost';
FLUSH PRIVILEGES;

-- 확인
SELECT user, host FROM mysql.user WHERE user = 'tangtang';
SHOW DATABASES LIKE 'tangtang';
