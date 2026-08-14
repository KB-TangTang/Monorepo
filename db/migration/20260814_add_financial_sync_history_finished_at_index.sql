-- 20260814_add_financial_sync_history_finished_at_index.sql
-- 이슈 #199 — 배치 스케줄러 대상 조회 쿼리 성능 (최종 리뷰 후속, PR 코멘트 P2)
--
-- 신규 설치: 실행 대상이다 (예외 목록에 없음. db/AGENTS.md 참고).
--
-- ConnectedAccountMapper.findUserIdsDueForSync 의 서브쿼리가
-- SELECT user_id, MAX(finished_at) FROM tbl_financial_sync_history GROUP BY user_id
-- 를 WHERE 절 없이 매 틱(기본 30분)마다 돌린다. 기존 idx_fsh_user_created(user_id, created_at)
-- 는 finished_at 기준 그룹핑에 쓰이지 않아 테이블이 커지면 풀스캔·임시테이블이 된다.
-- (user_id, finished_at) 복합 인덱스를 추가하면 MySQL 이 루스 인덱스 스캔으로
-- GROUP BY user_id / MAX(finished_at) 을 처리할 수 있다.

USE tangtang;

ALTER TABLE tbl_financial_sync_history
  ADD KEY idx_fsh_user_finished (user_id, finished_at);
