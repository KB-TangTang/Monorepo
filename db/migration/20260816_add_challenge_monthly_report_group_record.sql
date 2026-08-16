-- 신규 설치 시 실행: 예
-- 이슈 #256: 개인 챌린지 월 확정 스냅샷에 종료월 기준의 확정 그룹 전적을 함께 보관한다.
-- 그룹 챌린지의 final_* 값은 challenge 모듈이 소유하며, 이 컬럼에는 집계 결과만 저장한다.

ALTER TABLE tbl_challenge_monthly_report
    ADD COLUMN group_record_json JSON NULL
        COMMENT '종료월에 CLOSED 및 final_* 완결 조건을 만족한 그룹 챌린지 전적 요약. 전적이 없으면 NULL'
        AFTER difficulty_results_json;
