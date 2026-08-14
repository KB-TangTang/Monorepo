-- [업그레이드/신규 설치 공통] 전날 미션 결과 팝업을 사용자가 확인한 시각을 저장한다.
--
-- 신규 설치 순서:
--   1. db/schema.sql 실행
--   2. 필요한 seed SQL 실행
--   3. db/migration/ 파일을 날짜순으로 실행
--
-- 기존 설치 순서:
--   1. 20260814_add_fixed_expense_candidate_confirmation.sql 까지 적용된 DB에서 이 파일을 실행한다.
--   2. schema.sql은 수정하지 않는다.
--
-- 배경:
--   전날 미션의 성공/실패를 알리는 팝업이 "읽었다"를 저장할 곳이 없어 화면에 들어올 때마다 다시 뜬다.
--   판정 시각(evaluated_at)은 배치가 결과를 확정한 시점이라 사용자가 봤는지와 무관하다. 컬럼을 나눈다.
--
-- ⚠ 테이블 이름 주의: tbl_mission_info 가 아니라 **tbl_user_mission_info** 다.
--   (tbl_mission_pool = 미션 원본 목록 / tbl_user_mission_info = 사용자별 일별 배정·판정 원장)
--
-- 기존 행은 NULL로 남는다. backfill 하지 않는다 —
-- 이미 지나간 날짜의 팝업을 사용자가 봤는지 알 수 있는 근거가 없고,
-- 조회는 "오늘 기준 전날 1건"만 보므로 과거 행이 NULL이어도 화면에 영향이 없다.
ALTER TABLE tbl_user_mission_info
    ADD COLUMN result_checked_at DATETIME NULL
        COMMENT '전날 미션 결과 팝업을 사용자가 확인한 시각. NULL이면 아직 안 봤다(팝업 노출 대상)'
        AFTER evaluated_at;

-- 롤백은 수동으로 수행한다.
--   ALTER TABLE tbl_user_mission_info DROP COLUMN result_checked_at;
