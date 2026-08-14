-- 신규 설치 및 기존 설치 모두 실행
-- 개인 미션 확정 판정의 사용자 확인 시각을 저장한다.

ALTER TABLE tbl_user_mission_info
  ADD COLUMN result_checked_at DATETIME NULL
  COMMENT '사용자가 확정 판정 결과를 확인한 시각'
  AFTER evaluated_at;
