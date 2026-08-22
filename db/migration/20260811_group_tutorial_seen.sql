-- 2026-08-11 · 그룹 챌린지 튜토리얼 완료 플래그 (이슈 #128)
-- tbl_user 에 group_tutorial_seen_at 추가
--
-- 배경
--   같은 날 RV-108 을 종결하며 튜토리얼 완료 플래그를 tbl_user.tutorial_seen_at 에 두기로 했는데,
--   그때는 메인 챌린지(MC_01_05)만 대상이었고 그룹 챌린지(GC_01_01)는 "담당자 확인 대상"으로
--   열려 있었다. 실제 화면은 튜토리얼이 둘(개인=대법원 · 그룹=지방법원)이고 마이페이지의
--   「튜토리얼 다시 보기」도 둘을 따로 되돌린다. 그래서 컬럼을 하나 더 둔다.
--
--   tutorial_seen_at       : 메인(개인·대법원) 챌린지 튜토리얼 — 이번 마이그레이션으로 용도 확정
--   group_tutorial_seen_at : 그룹(지방법원) 챌린지 튜토리얼 — 신설
--
--   DATETIME 인 이유는 앞선 결정과 같다. "봤는지"뿐 아니라 "언제 봤는지"도 남아
--   나중에 재노출 정책을 붙일 수 있다. NULL = 아직 안 봄.
--
-- ⚠ 실행 순서 주의
--   이 파일은 20260811_add_user_social_name_and_tutorial_seen.sql **뒤에** 실행해야 한다.
--   AFTER tutorial_seen_at 을 쓰므로 그 컬럼이 먼저 있어야 한다.
--   파일명이 날짜순 정렬에서 add_* 뒤에 오도록 'group' 으로 시작하게 지었다
--   (20260811_add_… < 20260811_group_…).
--
-- 적용 방법 (팀원 각자 1회)
--   mysql -u tangtang -p tangtang --default-character-set=utf8mb4 \
--     -e "source D:/KB_Final_Project/app/Monorepo/db/migration/20260811_group_tutorial_seen.sql"
--   PowerShell 은 < 리다이렉션을 지원하지 않으므로 위처럼 source 를 쓴다.
--
-- 신규 설치: **실행 대상이다.** 이 변경은 schema.sql 에 반영돼 있지 않다.
--   (db/AGENTS.md 규칙 · DECISIONS.md 2026-08-09 (4) — 공유된 schema.sql 은 고치지 않는다)

USE tangtang;

ALTER TABLE tbl_user
  ADD COLUMN group_tutorial_seen_at DATETIME NULL
    COMMENT '그룹 챌린지 튜토리얼(GC_01_01) 완료 시각. NULL 이면 미완료'
    AFTER tutorial_seen_at;

-- 확인
--   DESC tbl_user;
--   기대: tutorial_seen_at 바로 뒤에 group_tutorial_seen_at
--
-- ⚠ 기존 사용자 이관은 하지 않는다.
--   지금까지 완료 여부는 브라우저 localStorage 에만 있었고(이슈 #128 의 문제 그 자체),
--   서버가 읽을 방법이 없다. 이관 대상 데이터가 존재하지 않으므로 UPDATE 가 없다.
--   기존 사용자는 튜토리얼을 한 번 더 보게 된다 — 의도된 결과다.
