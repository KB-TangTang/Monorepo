-- [신규 설치·기존 설치 공통] 이슈 #129 맞춤 미션 개시 안내 상태 저장
--
-- UNTRACKED    : 데이터 부족 상태를 아직 관찰하지 않음
-- INSUFFICIENT : 데이터 부족 상태를 한 번 이상 관찰함
-- PENDING      : 이후 데이터가 충족되어 개시 안내를 보여줘야 함
-- SEEN         : 사용자가 개시 안내를 확인함
--
-- 단일 상태 컬럼으로 전이를 관리해 wasDataInsufficient와 hasSeenDataUnlock이
-- 서로 모순되는 조합으로 저장되지 않게 한다. 기존 사용자는 UNTRACKED라서
-- 이미 데이터가 충분하다는 이유만으로 뒤늦게 안내가 뜨지 않는다.

ALTER TABLE tbl_user
  ADD COLUMN personal_mission_unlock_status VARCHAR(20) NOT NULL DEFAULT 'UNTRACKED'
    COMMENT '맞춤 미션 개시 안내 상태: UNTRACKED/INSUFFICIENT/PENDING/SEEN'
    AFTER group_tutorial_seen_at,
  ADD CONSTRAINT ck_user_personal_mission_unlock_status
    CHECK (personal_mission_unlock_status IN ('UNTRACKED','INSUFFICIENT','PENDING','SEEN'));

-- 되돌리기:
-- ALTER TABLE tbl_user DROP CHECK ck_user_personal_mission_unlock_status;
-- ALTER TABLE tbl_user DROP COLUMN personal_mission_unlock_status;

