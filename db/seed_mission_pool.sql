-- =====================================================================
-- db/seed_mission_pool.sql — 개인 미션 1차 풀 초기 데이터
--
-- 선행 조건: db/seed_category.sql 실행 완료
-- 실행: mysql -u tangtang -p tangtang --default-character-set=utf8mb4
--       -e "source C:/TangTang/db/seed_mission_pool.sql"
--
-- 사용자가 당일 통제하기 쉬운 소비 카테고리 15개를 대상으로
-- 절대형(무지출) 15개와 상대형(개인 목표 금액) 15개를 추가한다.
-- =====================================================================

USE tangtang;
SET NAMES utf8mb4;

INSERT INTO tbl_mission_pool (
  mission_title,
  mission_content,
  mission_type,
  category_id,
  limit_price
)
SELECT
  mission_seed.mission_title,
  mission_seed.mission_content,
  mission_seed.mission_type,
  child_category.id,
  mission_seed.limit_price
FROM (
  SELECT '식비' AS parent_name, '음식점/외식' AS category_name,
         '오늘 외식비 0원 도전' AS mission_title,
         '오늘은 외식 결제 없이 보내보세요.' AS mission_content,
         'ABSOLUTE' AS mission_type, 0 AS limit_price
  UNION ALL SELECT '식비', '음식점/외식',
         '오늘 외식비 {targetValue}원 안에서',
         '음식점 결제 합계를 목표 이하로 유지해보세요.',
         'RELATIVE', NULL

  UNION ALL SELECT '식비', '배달앱',
         '오늘 배달앱 0원 도전',
         '오늘은 배달 주문을 쉬어보세요.',
         'ABSOLUTE', 0
  UNION ALL SELECT '식비', '배달앱',
         '오늘 배달비 {targetValue}원 안에서',
         '배달앱 결제 합계를 목표 이하로 유지해보세요.',
         'RELATIVE', NULL

  UNION ALL SELECT '식비', '카페/간식',
         '오늘 카페·간식비 0원 도전',
         '오늘은 음료와 간식 구매를 쉬어보세요.',
         'ABSOLUTE', 0
  UNION ALL SELECT '식비', '카페/간식',
         '오늘 카페·간식비 {targetValue}원 안에서',
         '카페와 간식 결제 합계를 목표 이하로 유지해보세요.',
         'RELATIVE', NULL

  UNION ALL SELECT '식비', '편의점',
         '오늘 편의점 0원 도전',
         '오늘은 편의점 결제를 쉬어보세요.',
         'ABSOLUTE', 0
  UNION ALL SELECT '식비', '편의점',
         '오늘 편의점비 {targetValue}원 안에서',
         '편의점 결제 합계를 목표 이하로 유지해보세요.',
         'RELATIVE', NULL

  UNION ALL SELECT '쇼핑', '온라인쇼핑',
         '오늘 온라인쇼핑 0원 도전',
         '오늘은 온라인 결제를 쉬어보세요.',
         'ABSOLUTE', 0
  UNION ALL SELECT '쇼핑', '온라인쇼핑',
         '오늘 온라인쇼핑비 {targetValue}원 안에서',
         '온라인쇼핑 결제 합계를 목표 이하로 유지해보세요.',
         'RELATIVE', NULL

  UNION ALL SELECT '쇼핑', '패션',
         '오늘 패션비 0원 도전',
         '오늘은 의류와 잡화 구매를 쉬어보세요.',
         'ABSOLUTE', 0
  UNION ALL SELECT '쇼핑', '패션',
         '오늘 패션비 {targetValue}원 안에서',
         '패션 결제 합계를 목표 이하로 유지해보세요.',
         'RELATIVE', NULL

  UNION ALL SELECT '쇼핑', '뷰티',
         '오늘 뷰티비 0원 도전',
         '오늘은 화장품과 미용용품 구매를 쉬어보세요.',
         'ABSOLUTE', 0
  UNION ALL SELECT '쇼핑', '뷰티',
         '오늘 뷰티비 {targetValue}원 안에서',
         '뷰티 결제 합계를 목표 이하로 유지해보세요.',
         'RELATIVE', NULL

  UNION ALL SELECT '교통', '택시/모빌리티',
         '오늘 택시비 0원 도전',
         '오늘은 택시와 모빌리티 이용을 쉬어보세요.',
         'ABSOLUTE', 0
  UNION ALL SELECT '교통', '택시/모빌리티',
         '오늘 택시비 {targetValue}원 안에서',
         '택시와 모빌리티 결제 합계를 목표 이하로 유지해보세요.',
         'RELATIVE', NULL

  UNION ALL SELECT '교통', '주유/충전',
         '오늘 주유·충전비 0원 도전',
         '오늘은 주유와 차량 충전을 쉬어보세요.',
         'ABSOLUTE', 0
  UNION ALL SELECT '교통', '주유/충전',
         '오늘 주유·충전비 {targetValue}원 안에서',
         '주유와 충전 결제 합계를 목표 이하로 유지해보세요.',
         'RELATIVE', NULL

  UNION ALL SELECT '교통', '주차/통행료',
         '오늘 주차·통행료 0원 도전',
         '오늘은 주차비와 통행료를 아껴보세요.',
         'ABSOLUTE', 0
  UNION ALL SELECT '교통', '주차/통행료',
         '오늘 주차·통행료 {targetValue}원 안에서',
         '주차와 통행료 합계를 목표 이하로 유지해보세요.',
         'RELATIVE', NULL

  UNION ALL SELECT '생활', '장보기/마트',
         '오늘 장보기 0원 도전',
         '오늘은 마트와 장보기 결제를 쉬어보세요.',
         'ABSOLUTE', 0
  UNION ALL SELECT '생활', '장보기/마트',
         '오늘 장보기비 {targetValue}원 안에서',
         '마트와 장보기 결제 합계를 목표 이하로 유지해보세요.',
         'RELATIVE', NULL

  UNION ALL SELECT '문화/여가', '영화·공연·전시',
         '오늘 문화비 0원 도전',
         '오늘은 영화와 공연, 전시 결제를 쉬어보세요.',
         'ABSOLUTE', 0
  UNION ALL SELECT '문화/여가', '영화·공연·전시',
         '오늘 문화비 {targetValue}원 안에서',
         '영화와 공연, 전시 결제 합계를 목표 이하로 유지해보세요.',
         'RELATIVE', NULL

  UNION ALL SELECT '문화/여가', '도서',
         '오늘 도서비 0원 도전',
         '오늘은 새 책 구매를 쉬어보세요.',
         'ABSOLUTE', 0
  UNION ALL SELECT '문화/여가', '도서',
         '오늘 도서비 {targetValue}원 안에서',
         '도서 결제 합계를 목표 이하로 유지해보세요.',
         'RELATIVE', NULL

  UNION ALL SELECT '문화/여가', '취미',
         '오늘 취미비 0원 도전',
         '오늘은 취미용품 구매를 쉬어보세요.',
         'ABSOLUTE', 0
  UNION ALL SELECT '문화/여가', '취미',
         '오늘 취미비 {targetValue}원 안에서',
         '취미 관련 결제 합계를 목표 이하로 유지해보세요.',
         'RELATIVE', NULL

  UNION ALL SELECT '문화/여가', '레저',
         '오늘 레저비 0원 도전',
         '오늘은 유료 레저 활동 결제를 쉬어보세요.',
         'ABSOLUTE', 0
  UNION ALL SELECT '문화/여가', '레저',
         '오늘 레저비 {targetValue}원 안에서',
         '레저 결제 합계를 목표 이하로 유지해보세요.',
         'RELATIVE', NULL
) AS mission_seed
JOIN tbl_category AS parent_category
  ON parent_category.category_name = mission_seed.parent_name
 AND parent_category.parent_id IS NULL
JOIN tbl_category AS child_category
  ON child_category.category_name = mission_seed.category_name
 AND child_category.parent_id = parent_category.id
WHERE NOT EXISTS (
  SELECT 1
    FROM tbl_mission_pool AS existing_mission
   WHERE existing_mission.mission_type = mission_seed.mission_type
     AND existing_mission.category_id = child_category.id
     AND existing_mission.mission_title = mission_seed.mission_title
);

-- 검증: ABSOLUTE 15개, RELATIVE 15개, 전체 30개가 조회되어야 한다.
SELECT
  mission_type,
  COUNT(*) AS mission_count
  FROM tbl_mission_pool
 WHERE mission_title LIKE '오늘%'
 GROUP BY mission_type
 ORDER BY mission_type;

SELECT
  parent_category.category_name AS parent_category_name,
  child_category.category_name AS child_category_name,
  mission_pool.mission_type,
  mission_pool.mission_title,
  mission_pool.limit_price
  FROM tbl_mission_pool AS mission_pool
  JOIN tbl_category AS child_category
    ON child_category.id = mission_pool.category_id
  JOIN tbl_category AS parent_category
    ON parent_category.id = child_category.parent_id
 WHERE mission_pool.mission_title LIKE '오늘%'
 ORDER BY parent_category.id, child_category.id, mission_pool.mission_type;
