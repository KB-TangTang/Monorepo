-- 2026-08-12 · 상대형 저액 목표의 무지출 전환 사유 저장 (이슈 #139)
-- 신규 설치: **실행 대상이다.** schema.sql 에는 반영하지 않는다.

USE tangtang;

ALTER TABLE tbl_user_mission_info
  ADD COLUMN assignment_reason VARCHAR(30) NULL
  COMMENT '배정 사유. LOW_SPENDING_NO_SPEND=낮은 상대형 목표를 같은 카테고리 무지출로 전환'
  AFTER base_amount;

ALTER TABLE tbl_user_mission_info
  MODIFY COLUMN target_value DECIMAL(15,2) NULL
  COMMENT '실제 목표 상한. 상대형=B x (1-절감률), 무지출 절대형=0';
