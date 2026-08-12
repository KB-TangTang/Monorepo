-- 2026-08-12 · 프로필 이미지 (스펙: docs/superpowers/specs/2026-08-12-profile-image-design.md)
-- tbl_user 에 profile_image_key 추가
--
-- 배경
--   닉네임 설정 화면(AU_03_01)과 마이페이지(MY_01_03)에서 프로필 사진을 올릴 수 있게 한다.
--   NULL 이면 미설정이고 화면은 지금처럼 이니셜 아바타를 그린다.
--
-- ⚠ URL 이 아니라 **키**를 저장한다. 값은 'profile/{userId}/{uuid}.jpg' 형식이다.
--   URL 로 저장하면 로컬 → S3 로 옮길 때 기존 행을 전부 변환해야 한다.
--   키로 두면 저장소 구현의 urlOf() 만 바뀌고 DB 는 손대지 않는다.
--
-- 적용 방법 (팀원 각자 1회)
--   mysql -u tangtang -p tangtang --default-character-set=utf8mb4 \
--     -e "source D:/KB_Final_Project/app/Monorepo/db/migration/20260812_add_user_profile_image.sql"
--   PowerShell 은 < 리다이렉션을 지원하지 않으므로 위처럼 source 를 쓴다.
--
-- 신규 설치: **실행 대상이다.** 이 변경은 schema.sql 에 반영돼 있지 않다
--   (db/AGENTS.md 규칙 · DECISIONS.md 2026-08-09 (4) — 공유된 schema.sql 은 고치지 않는다).

USE tangtang;

ALTER TABLE tbl_user
  ADD COLUMN profile_image_key VARCHAR(255) NULL
    COMMENT '프로필 이미지 저장 키. NULL 이면 미설정(이니셜 아바타). URL 이 아니라 키다'
    AFTER social_name;

-- 확인
--   DESC tbl_user;   기대: social_name 바로 뒤에 profile_image_key
--
-- ⚠ 이 컬럼은 UserMapper.xml 의 공용 <sql id="userColumns"> 에 들어간다.
--   컬럼 없는 DB 에 새 코드가 뜨면 Unknown column 으로 **로그인부터 500** 이 된다.
--   반드시 이 마이그레이션을 먼저 적용한 뒤 코드를 배포한다.
