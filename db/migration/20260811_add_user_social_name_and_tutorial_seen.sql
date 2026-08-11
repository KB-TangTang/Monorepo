-- 2026-08-11 · 팀 결정 반영 (DECISIONS.md 2026-08-11 참고)
-- tbl_user 에 social_name · tutorial_seen_at 추가 + 기존 nickname 값 이관
--
-- 배경 1 — social_name (닉네임 온보딩 신설)
--   지금까지는 가입 시 구글 이름을 tbl_user.nickname 에 그대로 넣었다(AuthService.createUser).
--   닉네임 설정 화면을 계좌연동 완료 직후에 강제로 넣기로 하면서
--   "닉네임을 설정했는지"를 판별할 방법이 필요해졌는데, 구글 이름이 미리 들어가 있으면
--   설정한 값과 구분되지 않는다. 그래서 역할을 나눈다.
--     nickname    : 사용자가 직접 정한 표시명. 미설정이면 NULL (= 온보딩 미완료)
--     social_name : OAuth 제공자가 준 이름. 입력창 prefill · 온보딩 미완료 구간의 표시명
--     name        : 실명(본인확인용). 간편인증 화면에서 채운다 — 별개 컬럼이다
--   표시명 규칙은 nickname ?? social_name 이다.
--   닉네임 중복은 허용하므로 UNIQUE 를 걸지 않는다.
--
-- 배경 2 — tutorial_seen_at (RV-108 종결)
--   MC_01_05 튜토리얼 완료 플래그를 저장할 곳이 ERD 에 없어 미결이던 건이다.
--   tbl_user_consent.consent_type 도메인에 끼우면 동의 철회 API 가 튜토리얼 플래그까지
--   건드릴 수 있는 구조가 되므로 tbl_user 에 둔다.
--   DATETIME 으로 두면 "봤는지"뿐 아니라 "언제 봤는지"도 남는다.
--
-- 적용 방법 (팀원 각자 1회)
--   mysql -u tangtang -p tangtang --default-character-set=utf8mb4 \
--     -e "source D:/KB_Final_Project/app/Monorepo/db/migration/20260811_add_user_social_name_and_tutorial_seen.sql"
--   PowerShell 은 < 리다이렉션을 지원하지 않으므로 위처럼 source 를 쓴다.
--
-- ⚠ schema.sql 은 고치지 않았다.
--   팀에 이미 공유된 스키마라 변경분은 migration/ 으로만 관리한다
--   (db/AGENTS.md 규칙 · DECISIONS.md 2026-08-09 (4)).
--   따라서 신규 설치도 schema.sql 을 넣은 뒤 migration/ 을 날짜순으로 전부 적용해야 한다.

ALTER TABLE tbl_user
  ADD COLUMN social_name VARCHAR(50) NULL
    COMMENT 'OAuth 제공자가 준 이름(구글 name). 닉네임 미설정 시 표시명 fallback'
    AFTER nickname;

ALTER TABLE tbl_user
  ADD COLUMN tutorial_seen_at DATETIME NULL
    COMMENT '메인 챌린지 튜토리얼(MC_01_05) 완료 시각. NULL 이면 미완료'
    AFTER difficulty_id;

-- 기존 행 이관 — 반드시 위 ALTER 뒤에 실행한다.
--
--   현재 tbl_user.nickname 에 들어 있는 값은 전부 가입 시 복사된 구글 이름이다.
--   닉네임 설정 API 가 아직 없어 사용자가 직접 정한 값은 존재하지 않는다(2026-08-11 기준).
--   그래서 통째로 social_name 으로 옮기고 nickname 을 비운다.
--   이 단계를 빼면 기존 사용자가 전부 "닉네임 설정 완료" 로 판정돼 온보딩 화면을 영영 못 본다.
--
--   ⚠ 닉네임 설정 API 가 배포된 뒤에는 이 UPDATE 를 다시 실행하면 안 된다.
--      사용자가 정한 닉네임을 구글 이름으로 되돌린다.
--      WHERE social_name IS NULL 조건이 재실행을 막지만, 조건을 지우고 돌리지 말 것.
UPDATE tbl_user
   SET social_name = nickname,
       nickname    = NULL
 WHERE social_name IS NULL;
