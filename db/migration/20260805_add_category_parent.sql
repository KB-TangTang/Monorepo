-- =====================================================================
-- 소비 카테고리 계층화
--
-- 적용 대상: schema.sql 이 이미 실행된 TangTang DB
-- 적용 후: tbl_category.parent_id 로 대분류·소분류를 자기참조 구조로 관리
-- =====================================================================

USE tangtang;

ALTER TABLE tbl_category
  ADD COLUMN parent_id BIGINT NULL COMMENT '상위 카테고리. NULL이면 대분류' AFTER category_name,
  ADD KEY idx_category_parent (parent_id),
  ADD CONSTRAINT fk_category_parent
    FOREIGN KEY (parent_id) REFERENCES tbl_category(id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE;
