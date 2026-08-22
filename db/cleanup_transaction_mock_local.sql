-- =====================================================================
-- db/cleanup_transaction_mock_local.sql
-- seed_transaction_mock_local.sql이 만든 거래만 안전하게 제거한다.
-- 사용자·계좌·카테고리·가맹점 카테고리 매핑은 삭제하지 않는다.
-- =====================================================================

USE tangtang;
SET NAMES utf8mb4;

SET @target_user_id = 1;

-- 정리 전 삭제 대상 건수
SELECT COUNT(*) AS cleanup_target_count
  FROM tbl_transaction
 WHERE user_id = @target_user_id
   AND codef_tr_key LIKE 'MOCK-LOCAL-U1-%';

START TRANSACTION;

-- 자기참조 FK를 고려해 환불 거래를 먼저 삭제한다.
DELETE FROM tbl_transaction
 WHERE user_id = @target_user_id
   AND codef_tr_key LIKE 'MOCK-LOCAL-U1-%'
   AND is_refund = 1;

SET @deleted_refund_count = ROW_COUNT();

-- 남은 일반 소비·수입·이체 목거래를 삭제한다.
DELETE FROM tbl_transaction
 WHERE user_id = @target_user_id
   AND codef_tr_key LIKE 'MOCK-LOCAL-U1-%';

SET @deleted_original_count = ROW_COUNT();

COMMIT;

-- 정리 결과와 잔여 건수
SELECT @deleted_refund_count AS deleted_refund_count,
       @deleted_original_count AS deleted_original_count,
       @deleted_refund_count + @deleted_original_count AS deleted_total_count;

SELECT COUNT(*) AS remaining_mock_transaction_count
  FROM tbl_transaction
 WHERE user_id = @target_user_id
   AND codef_tr_key LIKE 'MOCK-LOCAL-U1-%';
