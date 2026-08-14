-- =====================================================================
-- 부하테스트 데이터 되돌리기 — 로컬 전용 (이슈 #188)
--
-- seed_loadtest_local.sql 이 만든 것만 지운다.
-- 판별 기준은 tbl_user.provider_user_id LIKE 'LOADTEST-%' 하나다.
--
-- tbl_connected_account · tbl_transaction 은 user_id FK 가 ON DELETE CASCADE 라
-- 사용자만 지우면 함께 사라진다. 그래도 지운 개수를 눈으로 보려고 순서대로 센다.
--
-- 실행 (PowerShell):
--   mysql -u tangtang -p tangtang --default-character-set=utf8mb4 `
--     -e "source D:/KB_Final_Project/app/Monorepo/perf/cleanup_loadtest_local.sql"
-- =====================================================================

SELECT (SELECT COUNT(*) FROM tbl_user WHERE provider_user_id LIKE 'LOADTEST-%') AS 지울_사용자,
       (SELECT COUNT(*) FROM tbl_transaction WHERE codef_tr_key LIKE 'LOADTEST-%') AS 지울_거래;

-- CASCADE 로 함께 지워지지만, 혹시 다른 경로로 만들어진 잔여 행이 있으면 여기서 걸린다.
DELETE FROM tbl_transaction WHERE codef_tr_key LIKE 'LOADTEST-%';

DELETE FROM tbl_user WHERE provider_user_id LIKE 'LOADTEST-%';

SELECT (SELECT COUNT(*) FROM tbl_user WHERE provider_user_id LIKE 'LOADTEST-%') AS 남은_사용자,
       (SELECT COUNT(*) FROM tbl_transaction WHERE codef_tr_key LIKE 'LOADTEST-%') AS 남은_거래;
