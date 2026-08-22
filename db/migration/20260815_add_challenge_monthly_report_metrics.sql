-- 신규 설치 시 실행: 예
-- 이슈 #244: 개인 챌린지 월 확정 성과에 화면 조회용 확정 지표를 보관한다.

ALTER TABLE tbl_challenge_monthly_report
    ADD COLUMN monthly_longest_streak SMALLINT NOT NULL DEFAULT 0
        COMMENT '대상 월 내부에서만 계산한 최장 연속 SUCCESS 일수' AFTER success_days,
    ADD COLUMN best_weekday VARCHAR(3) NULL
        COMMENT '성공률·도전일수·월요일~일요일 순으로 결정한 최고 성공 요일' AFTER monthly_longest_streak,
    ADD COLUMN earned_score INT NOT NULL DEFAULT 0
        COMMENT '성공 미션 난이도 점수와 전일 SUCCESS 보너스를 합산한 확정 점수' AFTER best_weekday;
