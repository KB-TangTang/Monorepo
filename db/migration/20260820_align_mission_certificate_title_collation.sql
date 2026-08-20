-- [신규 설치·기존 설치 공통] 개인 미션 월 키 collation 통일
--
-- MySQL 서버 기본 collation이 utf8mb4_0900_ai_ci인 로컬 환경에서는 schema.sql로 생성한
-- tbl_monthly_ranking.year_month와 기존 AI 타이틀 마이그레이션이 명시한
-- tbl_mission_certificate_title.year_month의 collation이 달라 후보 조회 JOIN이 실패했다.
-- 두 월 키를 프로젝트 표준인 utf8mb4_general_ci로 통일한다.

ALTER TABLE tbl_monthly_ranking
    MODIFY COLUMN `year_month` CHAR(7)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci
        NOT NULL COMMENT 'YYYY-MM';

ALTER TABLE tbl_mission_certificate_title
    MODIFY COLUMN `year_month` CHAR(7)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci
        NOT NULL COMMENT '인증서 대상 월(YYYY-MM)';

-- 롤백은 설치 환경의 기존 collation에 따라 달라 자동 SQL을 제공하지 않는다.
