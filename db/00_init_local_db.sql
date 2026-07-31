-- 로컬 개발용 DB · 계정 초기화 스크립트
-- MySQL 관리자(root) 계정으로 1회 실행한다.
--   mysql -u root -p < db/00_init_local_db.sql
-- 또는 MySQL Workbench 에서 열어 실행.
--
-- 로컬 개발 전용 계정이므로 비밀번호는 팀 공통으로 고정한다.
-- (운영 환경에서는 절대 이 계정·비밀번호를 사용하지 않는다)

CREATE DATABASE IF NOT EXISTS tangtang
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

CREATE USER IF NOT EXISTS 'tangtang'@'localhost' IDENTIFIED BY 'tangtang1234';
GRANT ALL PRIVILEGES ON tangtang.* TO 'tangtang'@'localhost';
FLUSH PRIVILEGES;

-- 확인
SELECT user, host FROM mysql.user WHERE user = 'tangtang';
SHOW DATABASES LIKE 'tangtang';
