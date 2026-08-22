-- =====================================================================
-- db/seed_category.sql — 표준 소비 카테고리 초기 데이터
--
-- 선행 조건: db/migration/20260805_add_category_parent.sql 적용 완료
-- 실행: mysql -u tangtang -p tangtang < db/seed_category.sql
--
-- 대분류 12개와 소분류 44개를 추가한다. 재실행 시에도 부모 관계를 동일하게 맞춘다.
-- ('기타' 하위 '자동 분류 불가 거래'·'사용자 직접 지정'은 category_id IS NULL / category_source='USER'
--  로 이미 표현되는 상태라 제외했다 — 2026-08-13, 이슈 #147)
-- =====================================================================

USE tangtang;
SET NAMES utf8mb4;

-- 대분류
INSERT INTO tbl_category (category_name, parent_id) VALUES
  ('식비', NULL),
  ('쇼핑', NULL),
  ('교통', NULL),
  ('주거/공과금', NULL),
  ('생활', NULL),
  ('구독/디지털', NULL),
  ('문화/여가', NULL),
  ('여행', NULL),
  ('건강', NULL),
  ('교육/자기계발', NULL),
  ('금융/보험', NULL),
  ('기타', NULL)
ON DUPLICATE KEY UPDATE
  parent_id = NULL;

-- 소분류
INSERT INTO tbl_category (category_name, parent_id)
SELECT category_seed.category_name, parent_category.id
  FROM (
    SELECT '식비' AS parent_name, '음식점/외식' AS category_name
    UNION ALL SELECT '식비', '배달앱'
    UNION ALL SELECT '식비', '카페/간식'
    UNION ALL SELECT '식비', '편의점'
    UNION ALL SELECT '쇼핑', '온라인쇼핑'
    UNION ALL SELECT '쇼핑', '패션'
    UNION ALL SELECT '쇼핑', '뷰티'
    UNION ALL SELECT '교통', '대중교통'
    UNION ALL SELECT '교통', '택시/모빌리티'
    UNION ALL SELECT '교통', '주유/충전'
    UNION ALL SELECT '교통', '주차/통행료'
    UNION ALL SELECT '주거/공과금', '월세'
    UNION ALL SELECT '주거/공과금', '관리비'
    UNION ALL SELECT '주거/공과금', '전기·가스·수도'
    UNION ALL SELECT '생활', '장보기/마트'
    UNION ALL SELECT '생활', '생활용품'
    UNION ALL SELECT '생활', '세탁비'
    UNION ALL SELECT '생활', '경조사/선물'
    UNION ALL SELECT '생활', '자녀'
    UNION ALL SELECT '생활', '반려동물'
    UNION ALL SELECT '구독/디지털', '통신비'
    UNION ALL SELECT '구독/디지털', 'OTT'
    UNION ALL SELECT '구독/디지털', '음원'
    UNION ALL SELECT '구독/디지털', '앱·게임'
    UNION ALL SELECT '구독/디지털', '소프트웨어·클라우드'
    UNION ALL SELECT '문화/여가', '영화·공연·전시'
    UNION ALL SELECT '문화/여가', '도서'
    UNION ALL SELECT '문화/여가', '취미'
    UNION ALL SELECT '문화/여가', '레저'
    UNION ALL SELECT '여행', '교통권'
    UNION ALL SELECT '여행', '숙박'
    UNION ALL SELECT '여행', '관광·여행상품'
    UNION ALL SELECT '건강', '병원'
    UNION ALL SELECT '건강', '약국'
    UNION ALL SELECT '건강', '운동시설'
    UNION ALL SELECT '건강', '건강관리'
    UNION ALL SELECT '교육/자기계발', '학원'
    UNION ALL SELECT '교육/자기계발', '온라인 강의'
    UNION ALL SELECT '교육/자기계발', '시험·자격증'
    UNION ALL SELECT '교육/자기계발', '학습 교재'
    UNION ALL SELECT '금융/보험', '보험료'
    UNION ALL SELECT '금융/보험', '이자'
    UNION ALL SELECT '금융/보험', '수수료'
    UNION ALL SELECT '금융/보험', '금융상품'
    -- '자동 분류 불가 거래'·'사용자 직접 지정'은 여기 없다.
    -- category_id IS NULL 과 category_source='USER' 로 이미 표현되는 상태라
    -- tbl_category 에 별도 행을 두면 중복이다 (db/migration/20260813_remove_duplicate_misc_categories.sql 참고)
  ) AS category_seed
  JOIN tbl_category AS parent_category
    ON parent_category.category_name = category_seed.parent_name
   AND parent_category.parent_id IS NULL
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id);

-- 검증: 대분류 12개, 소분류 44개, 전체 56개가 조회되어야 한다.
SELECT
  SUM(parent_id IS NULL) AS parent_category_count,
  SUM(parent_id IS NOT NULL) AS child_category_count,
  COUNT(*) AS total_category_count
  FROM tbl_category;

SELECT
  parent_category.category_name AS parent_category_name,
  child_category.category_name AS child_category_name
  FROM tbl_category AS child_category
  JOIN tbl_category AS parent_category
    ON parent_category.id = child_category.parent_id
 ORDER BY parent_category.id, child_category.id;
