-- 2026-08-13 · 상대형 미션 최초 자격 획득 시각 추가 (이슈 #165)
--
-- 신규 설치: **실행 대상이다.** 이 변경은 schema.sql 에 반영돼 있지 않다.
--
-- 최초 자격 조건(최근 28일 이력 + 소비 거래 50건)을 통과한 시각을 저장한다.
-- NULL이면 아직 최초 자격을 획득하지 않았고, 값이 있으면 이후 거래가 50건 미만이어도
-- 현재 최근 28일 소비를 기준으로 상대형 미션 카테고리를 다시 분석한다.
-- 챌린지 참여 동의를 철회해도 획득한 자격은 유지한다.

USE tangtang;

ALTER TABLE tbl_user
  ADD COLUMN relative_mission_qualified_at DATETIME NULL
    COMMENT '최초 28일·50건 조건을 충족한 상대형 미션 자격 획득 시각'
    AFTER difficulty_id;

-- 기존 분석 스냅샷이 있는 사용자는 이미 최초 자격을 통과한 사용자이므로 소급 적용한다.
UPDATE tbl_user user
JOIN (
    SELECT user_id, MIN(created_at) AS qualified_at
    FROM tbl_user_mission_analysis
    GROUP BY user_id
) analysis ON analysis.user_id = user.id
SET user.relative_mission_qualified_at = analysis.qualified_at
WHERE user.relative_mission_qualified_at IS NULL;

-- 확인
--   DESC tbl_user;
--   기대: relative_mission_qualified_at DATETIME NULL
