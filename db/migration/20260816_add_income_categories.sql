-- =====================================================================
-- db/migration/20260816_add_income_categories.sql
-- 수입 거래 카테고리 편집 지원 — '수입' 대분류 1개 + 소분류 7개 추가.
--
-- 신규 설치 시 실행 대상: 예 (db/seed_category.sql 다음, 아무 때나 적용 가능한 순수 추가 데이터)
--
-- 배경
--   db/seed_category.sql 은 "표준 소비 카테고리"만 넣어서 tbl_category 에 수입 카테고리가
--   한 행도 없었다. 그래서 장부 화면에서 수입 거래의 카테고리를 바꾸면 이름으로 id 를 찾지 못해
--   항상 실패했다(프론트가 들고 있던 수입 카테고리는 fixtures 목업뿐이었다).
--
--   uk_category_name 이 테이블 전체에서 유일해야 하므로, 소분류명은 기존 지출 '기타'와
--   겹치지 않게 '수입 기타'로 둔다(프론트 fixtures/category.js 의 income-etc 항목도 이 이름으로
--   맞춰 고친다).
--
-- 되돌리기
--   DELETE FROM tbl_category
--    WHERE parent_id = (SELECT id FROM (SELECT id FROM tbl_category
--                                        WHERE category_name = '수입' AND parent_id IS NULL) AS t);
--   DELETE FROM tbl_category WHERE category_name = '수입' AND parent_id IS NULL;
-- =====================================================================

USE tangtang;
SET NAMES utf8mb4;

-- 대분류
INSERT INTO tbl_category (category_name, parent_id) VALUES ('수입', NULL)
ON DUPLICATE KEY UPDATE parent_id = NULL;

-- 소분류
INSERT INTO tbl_category (category_name, parent_id)
SELECT category_seed.category_name, parent_category.id
  FROM (
    SELECT '급여' AS category_name
    UNION ALL SELECT '상여금'
    UNION ALL SELECT '용돈'
    UNION ALL SELECT '부수입'
    UNION ALL SELECT '이자/배당'
    UNION ALL SELECT '환급/캐시백'
    UNION ALL SELECT '수입 기타'
  ) AS category_seed
  JOIN tbl_category AS parent_category
    ON parent_category.category_name = '수입'
   AND parent_category.parent_id IS NULL
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id);

-- =====================================================================
-- 적용 확인 (선택)
-- =====================================================================
-- SELECT child_category.category_name
--   FROM tbl_category AS child_category
--   JOIN tbl_category AS parent_category
--     ON parent_category.id = child_category.parent_id
--  WHERE parent_category.category_name = '수입';
-- -- 7행이 기대 결과
