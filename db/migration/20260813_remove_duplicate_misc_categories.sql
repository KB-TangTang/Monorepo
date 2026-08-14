-- 20260813_remove_duplicate_misc_categories.sql
-- 이슈 #147 — 거래 카테고리화(후속 작업) 선행 작업: 카테고리 출처 추적 기반
--
-- 신규 설치: 실행 대상이다 (제거 대상 두 소분류가 애초에 없으면 프로시저가 아무 것도 하지
-- 않고 끝난다 — db/seed_category.sql 이 이미 이 두 행 없이 갱신됐기 때문. 항상 실행해도 안전하다).
--
-- 배경
--   tbl_category '기타' 하위의 두 소분류가 category_source(이 폴더의
--   20260813_add_transaction_category_source.sql 참고) 도입으로 중복이 됐다.
--     - '자동 분류 불가 거래' — category_id IS NULL 자체로 이미 이 상태를 표현한다.
--     - '사용자 직접 지정'   — category_source='USER' 가 이 역할을 대체한다.
--   이미 팀에 공유된 시드라 schema.sql 이 아니라 마이그레이션으로 걷어낸다.
--
-- 안전장치
--   삭제 전에 두 category_id 를 참조하는 행이 있는지 category_id FK 를 가진 모든 테이블에서
--   확인한다. 하나라도 있으면 삭제하지 않고 예외를 던진다 — 특히 tbl_merchant_category_map·
--   tbl_user_category_map·tbl_user_category_pref·tbl_budget 은 FK 가 ON DELETE CASCADE 라
--   가드 없이 지우면 매핑·설정 행이 조용히 함께 사라진다.
--   (2026-08-13 기준 실 데이터 없음을 확인했으나, 방어적으로 게이트를 둔다)

USE tangtang;

DELIMITER $$

DROP PROCEDURE IF EXISTS _tmp_remove_duplicate_misc_categories $$
CREATE PROCEDURE _tmp_remove_duplicate_misc_categories()
proc_body: BEGIN
    DECLARE parent_etc_id BIGINT DEFAULT NULL;
    DECLARE cat_auto_id BIGINT DEFAULT NULL;
    DECLARE cat_user_id BIGINT DEFAULT NULL;
    DECLARE ref_count INT DEFAULT 0;

    SELECT id INTO parent_etc_id
      FROM tbl_category
     WHERE category_name = '기타' AND parent_id IS NULL
     LIMIT 1;

    IF parent_etc_id IS NULL THEN
        LEAVE proc_body;
    END IF;

    SELECT id INTO cat_auto_id
      FROM tbl_category
     WHERE category_name = '자동 분류 불가 거래' AND parent_id = parent_etc_id
     LIMIT 1;

    SELECT id INTO cat_user_id
      FROM tbl_category
     WHERE category_name = '사용자 직접 지정' AND parent_id = parent_etc_id
     LIMIT 1;

    IF cat_auto_id IS NULL AND cat_user_id IS NULL THEN
        -- 이미 삭제됐거나(재실행) 신규 설치라 애초에 시드되지 않음
        LEAVE proc_body;
    END IF;

    SELECT
        (SELECT COUNT(*) FROM tbl_transaction            WHERE category_id IN (cat_auto_id, cat_user_id))
      + (SELECT COUNT(*) FROM tbl_fixed_expense_candidate WHERE category_id IN (cat_auto_id, cat_user_id))
      + (SELECT COUNT(*) FROM tbl_mission_pool            WHERE category_id IN (cat_auto_id, cat_user_id))
      + (SELECT COUNT(*) FROM tbl_merchant_category_map   WHERE category_id IN (cat_auto_id, cat_user_id))
      + (SELECT COUNT(*) FROM tbl_user_category_map       WHERE category_id IN (cat_auto_id, cat_user_id))
      + (SELECT COUNT(*) FROM tbl_user_category_pref      WHERE category_id IN (cat_auto_id, cat_user_id))
      + (SELECT COUNT(*) FROM tbl_challenge_group         WHERE category_id IN (cat_auto_id, cat_user_id))
      + (SELECT COUNT(*) FROM tbl_budget                  WHERE category_id IN (cat_auto_id, cat_user_id))
      INTO ref_count;

    IF ref_count > 0 THEN
        SIGNAL SQLSTATE '45000'
          SET MESSAGE_TEXT = '자동 분류 불가 거래/사용자 직접 지정 카테고리를 참조하는 행이 있어 삭제를 중단했다';
    END IF;

    DELETE FROM tbl_category WHERE id IN (cat_auto_id, cat_user_id);
END proc_body $$

DELIMITER ;

CALL _tmp_remove_duplicate_misc_categories();
DROP PROCEDURE _tmp_remove_duplicate_misc_categories;

-- =====================================================================
-- 적용 확인 (선택)
-- =====================================================================
-- SELECT * FROM tbl_category
--  WHERE category_name IN ('자동 분류 불가 거래', '사용자 직접 지정');
-- -- 0행이 기대 결과
