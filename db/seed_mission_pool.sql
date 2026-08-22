-- =====================================================================
-- db/seed_mission_pool.sql — 데일리 미션 풀 초기 데이터
--
-- 선행 조건
--   ① db/seed_category.sql 적용 완료 (대분류 12 · 소분류 46)
--   ② db/migration/20260809_ai_verdict_and_mission_pool_check.sql 적용 완료
--
-- 실행: mysql -u tangtang -p tangtang < db/seed_mission_pool.sql
--
-- 미션 대상 소분류 15개 (2026-08-11 팀 확정)
--   소분류 46개 전부가 미션 대상은 아니다. **사용자가 하루 단위로 줄일 수 있는 것만** 넣는다.
--   월세·관리비·통신비·보험료·이자 같은 고정성 지출은 "오늘 줄이세요" 가 성립하지 않는다.
--   (초판은 44개를 전부 넣어서 과소비 1위가 월세로 잡히는 문제가 있었다 — 2026-08-11 정정)
--
--     식비       음식점/외식 · 배달앱 · 카페/간식 · 편의점        (4)
--     쇼핑       온라인쇼핑 · 패션 · 뷰티                        (3)
--     교통       택시/모빌리티 · 주유/충전 · 주차/통행료          (3)   ※ 대중교통 제외
--     생활       장보기/마트                                     (1)
--     문화/여가  영화·공연·전시 · 도서 · 취미 · 레저              (4)
--
--   미션 풀에 행이 없는 소분류는 과소비 순위에 올라도 배정에서 건너뛴다(MC_02_01).
--   즉 이 목록이 곧 "미션이 나올 수 있는 카테고리" 다.
--
-- 상대형(RELATIVE) — 15 × 2문구 = 30행
--   목표 금액은 배정 시점에 검사 성향 구간에서 랜덤으로 뽑은 절감률로 계산한다(A-1b).
--   문구에 금액을 넣지 않는다. 개인화 목표가 화면에서 합쳐진다.
--
-- 절대형(ABSOLUTE) — 15 × 2문구 = 30행
--   2026-08-11 확정 — **상대형과 같은 소분류 단위**로 맞춘다.
--   (2026-08-09 의 "절대형은 대분류 단위" 결정을 뒤집는다. 무지출 미션은 기준액 B 를 쓰지 않고
--    "그 소분류 지출이 0인가" 만 보므로 데이터가 없어도 소분류로 판정된다. 오히려 대분류
--    "식비 전체 0원" 은 하루 미션으로 과하다.)
--   두 경로로 배정된다 — ① 콜드스타트(순위를 낼 데이터가 없을 때 15개 중 무작위 1개)
--                        ② 월 1회 불시 검문(전체 단속)
--   limit_price 가 곧 목표다. 0 = 무지출, >0 = 상한형. 문구의 금액과 반드시 일치시킨다.
--
-- 총 60행. 재실행해도 NOT EXISTS 로 중복이 쌓이지 않는다.
-- =====================================================================

USE tangtang;
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 상대형(RELATIVE) — 소분류 15개 × 2문구 = 30행
-- ---------------------------------------------------------------------
INSERT INTO tbl_mission_pool (mission_title, mission_content, mission_type, limit_price, category_id)
SELECT v.title, v.content, 'RELATIVE', NULL, child.id
FROM (
  SELECT '외식 현장 급습' AS title, '오늘 외식비를 목표 금액 안에서 해결한다.' AS content, '식비' AS parent_cat, '음식점/외식' AS child_cat UNION ALL
  SELECT '바깥밥 한 끼 줄이기', '오늘 외식 결제를 한 건으로 제한한다.', '식비', '음식점/외식' UNION ALL
  SELECT '배달 현장 급습', '오늘 배달 지출을 목표 금액 안으로 묶는다.', '식비', '배달앱' UNION ALL
  SELECT '배달 앱 봉인', '오늘 배달 앱을 열지 않는다. 탕이가 주문 내역을 본다.', '식비', '배달앱' UNION ALL
  SELECT '카페 영수증 심문', '오늘 카페·간식 지출을 목표 금액 안에서 끝낸다.', '식비', '카페/간식' UNION ALL
  SELECT '커피 한 잔 줄이기', '오늘 카페 결제를 한 건으로 제한한다.', '식비', '카페/간식' UNION ALL
  SELECT '편의점 순찰', '오늘 편의점 지출을 목표 금액 안으로 묶는다.', '식비', '편의점' UNION ALL
  SELECT '충동 집기 방지', '편의점에서 계획에 없던 물건을 담지 않는다.', '식비', '편의점' UNION ALL
  SELECT '장바구니 압수', '오늘 온라인 쇼핑 지출을 목표 금액 안으로 묶는다.', '쇼핑', '온라인쇼핑' UNION ALL
  SELECT '결제 하루 미루기', '담아두되 오늘은 결제하지 않는다.', '쇼핑', '온라인쇼핑' UNION ALL
  SELECT '옷장 현장 검증', '오늘 패션 지출을 목표 금액 안에서 해결한다.', '쇼핑', '패션' UNION ALL
  SELECT '세일 유혹 방어', '할인 알림이 와도 오늘은 사지 않는다.', '쇼핑', '패션' UNION ALL
  SELECT '화장대 재고 확인', '오늘 뷰티 지출을 목표 금액 안으로 묶는다.', '쇼핑', '뷰티' UNION ALL
  SELECT '사기 전 재고 확인', '집에 남은 게 있는지 먼저 본다.', '쇼핑', '뷰티' UNION ALL
  SELECT '택시비 심문', '오늘 택시·모빌리티 지출을 목표 금액 안으로 묶는다.', '교통', '택시/모빌리티' UNION ALL
  SELECT '택시 대신 대중교통', '오늘은 택시를 부르지 않는다.', '교통', '택시/모빌리티' UNION ALL
  SELECT '주유비 점검', '오늘 주유·충전 지출을 목표 금액 안에서 해결한다.', '교통', '주유/충전' UNION ALL
  SELECT '공회전 줄이기', '불필요한 운행을 오늘 하루 접는다.', '교통', '주유/충전' UNION ALL
  SELECT '주차비 감시', '오늘 주차·통행료를 목표 금액 안으로 묶는다.', '교통', '주차/통행료' UNION ALL
  SELECT '주차장 대신 대중교통', '유료 주차를 오늘은 쓰지 않는다.', '교통', '주차/통행료' UNION ALL
  SELECT '장바구니 검문', '오늘 장보기 지출을 목표 금액 안으로 묶는다.', '생활', '장보기/마트' UNION ALL
  SELECT '목록만 사기', '적어간 것만 담는다.', '생활', '장보기/마트' UNION ALL
  SELECT '관람비 심리', '오늘 영화·공연 지출을 목표 금액 안에서 해결한다.', '문화/여가', '영화·공연·전시' UNION ALL
  SELECT '할인 먼저 찾기', '제값 결제 전에 할인을 확인한다.', '문화/여가', '영화·공연·전시' UNION ALL
  SELECT '도서 구입 보류', '오늘 도서 지출을 목표 금액 안으로 묶는다.', '문화/여가', '도서' UNION ALL
  SELECT '도서관 먼저', '사기 전에 빌릴 수 있는지 본다.', '문화/여가', '도서' UNION ALL
  SELECT '취미비 절감', '오늘 취미 지출을 목표 금액 안에서 해결한다.', '문화/여가', '취미' UNION ALL
  SELECT '장비 추가 보류', '새 장비 결제를 오늘은 미룬다.', '문화/여가', '취미' UNION ALL
  SELECT '레저비 점검', '오늘 레저 지출을 목표 금액 안으로 묶는다.', '문화/여가', '레저' UNION ALL
  SELECT '무료 활동으로 대체', '돈 쓰지 않는 방식으로 논다.', '문화/여가', '레저'
) v
JOIN tbl_category parent ON parent.category_name = v.parent_cat AND parent.parent_id IS NULL
JOIN tbl_category child  ON child.category_name  = v.child_cat  AND child.parent_id = parent.id
WHERE NOT EXISTS (SELECT 1 FROM tbl_mission_pool m WHERE m.mission_title = v.title);

-- ---------------------------------------------------------------------
-- 절대형(ABSOLUTE) — 소분류 15개 × 2문구(무지출 + 상한형) = 30행
-- 콜드스타트 · 월 1회 불시 검문 공용. limit_price 와 문구의 금액을 일치시킨다.
-- ---------------------------------------------------------------------
INSERT INTO tbl_mission_pool (mission_title, mission_content, mission_type, limit_price, category_id)
SELECT v.title, v.content, 'ABSOLUTE', v.limit_price, child.id
FROM (
  SELECT '무지출 명령 · 외식' AS title, '오늘 외식 결제를 0원으로 만든다.' AS content, 0 AS limit_price, '식비' AS parent_cat, '음식점/외식' AS child_cat UNION ALL
  SELECT '외식 상한 1만원', '오늘 외식비를 1만원 안에서 해결한다.', 10000, '식비', '음식점/외식' UNION ALL
  SELECT '무지출 명령 · 배달', '오늘 배달 결제를 0원으로 만든다.', 0, '식비', '배달앱' UNION ALL
  SELECT '배달 상한 1만 5천원', '오늘 배달 지출을 1만 5천원 안으로 묶는다.', 15000, '식비', '배달앱' UNION ALL
  SELECT '무지출 명령 · 카페', '오늘 카페·간식 결제를 0원으로 만든다.', 0, '식비', '카페/간식' UNION ALL
  SELECT '카페 상한 5천원', '오늘 카페·간식을 5천원 안에서 끝낸다.', 5000, '식비', '카페/간식' UNION ALL
  SELECT '무지출 명령 · 편의점', '오늘 편의점 결제를 0원으로 만든다.', 0, '식비', '편의점' UNION ALL
  SELECT '편의점 상한 5천원', '오늘 편의점 지출을 5천원 안으로 묶는다.', 5000, '식비', '편의점' UNION ALL
  SELECT '무지출 명령 · 온라인쇼핑', '오늘 온라인 쇼핑 결제를 0원으로 만든다.', 0, '쇼핑', '온라인쇼핑' UNION ALL
  SELECT '온라인쇼핑 상한 2만원', '오늘 온라인 쇼핑을 2만원 안으로 묶는다.', 20000, '쇼핑', '온라인쇼핑' UNION ALL
  SELECT '무지출 명령 · 패션', '오늘 패션 결제를 0원으로 만든다.', 0, '쇼핑', '패션' UNION ALL
  SELECT '패션 상한 3만원', '오늘 패션 지출을 3만원 안에서 해결한다.', 30000, '쇼핑', '패션' UNION ALL
  SELECT '무지출 명령 · 뷰티', '오늘 뷰티 결제를 0원으로 만든다.', 0, '쇼핑', '뷰티' UNION ALL
  SELECT '뷰티 상한 2만원', '오늘 뷰티 지출을 2만원 안으로 묶는다.', 20000, '쇼핑', '뷰티' UNION ALL
  SELECT '무지출 명령 · 택시', '오늘 택시·모빌리티 결제를 0원으로 만든다.', 0, '교통', '택시/모빌리티' UNION ALL
  SELECT '택시 상한 1만원', '오늘 택시비를 1만원 안으로 묶는다.', 10000, '교통', '택시/모빌리티' UNION ALL
  SELECT '무지출 명령 · 주유', '오늘 주유·충전 결제를 0원으로 만든다.', 0, '교통', '주유/충전' UNION ALL
  SELECT '주유 상한 3만원', '오늘 주유·충전을 3만원 안에서 해결한다.', 30000, '교통', '주유/충전' UNION ALL
  SELECT '무지출 명령 · 주차', '오늘 주차·통행료 결제를 0원으로 만든다.', 0, '교통', '주차/통행료' UNION ALL
  SELECT '주차 상한 5천원', '오늘 주차·통행료를 5천원 안으로 묶는다.', 5000, '교통', '주차/통행료' UNION ALL
  SELECT '무지출 명령 · 장보기', '오늘 장보기 결제를 0원으로 만든다.', 0, '생활', '장보기/마트' UNION ALL
  SELECT '장보기 상한 3만원', '오늘 장보기를 3만원 안으로 묶는다.', 30000, '생활', '장보기/마트' UNION ALL
  SELECT '무지출 명령 · 관람', '오늘 영화·공연·전시 결제를 0원으로 만든다.', 0, '문화/여가', '영화·공연·전시' UNION ALL
  SELECT '관람 상한 1만 5천원', '오늘 관람비를 1만 5천원 안에서 해결한다.', 15000, '문화/여가', '영화·공연·전시' UNION ALL
  SELECT '무지출 명령 · 도서', '오늘 도서 결제를 0원으로 만든다.', 0, '문화/여가', '도서' UNION ALL
  SELECT '도서 상한 1만 5천원', '오늘 도서 지출을 1만 5천원 안으로 묶는다.', 15000, '문화/여가', '도서' UNION ALL
  SELECT '무지출 명령 · 취미', '오늘 취미 결제를 0원으로 만든다.', 0, '문화/여가', '취미' UNION ALL
  SELECT '취미 상한 2만원', '오늘 취미 지출을 2만원 안에서 해결한다.', 20000, '문화/여가', '취미' UNION ALL
  SELECT '무지출 명령 · 레저', '오늘 레저 결제를 0원으로 만든다.', 0, '문화/여가', '레저' UNION ALL
  SELECT '레저 상한 3만원', '오늘 레저 지출을 3만원 안으로 묶는다.', 30000, '문화/여가', '레저'
) v
JOIN tbl_category parent ON parent.category_name = v.parent_cat AND parent.parent_id IS NULL
JOIN tbl_category child  ON child.category_name  = v.child_cat  AND child.parent_id = parent.id
WHERE NOT EXISTS (SELECT 1 FROM tbl_mission_pool m WHERE m.mission_title = v.title);

-- ---------------------------------------------------------------------
-- 정리 — 미션 대상에서 제외된 소분류의 구 미션을 지운다
--
-- 초판(2026-08-09)이 소분류 44개를 전부 넣어서 월세·통신비·보험료 같은
-- 고정성 지출에도 미션이 달려 있었다. 이미 시드를 넣은 팀원 DB 에서 그것들을 걷어낸다.
-- 배정 이력(tbl_user_mission_info)이 참조 중인 행은 FK 때문에 지워지지 않으므로 건너뛴다.
-- ---------------------------------------------------------------------
DELETE mp FROM tbl_mission_pool mp
WHERE NOT EXISTS (SELECT 1 FROM tbl_user_mission_info umi WHERE umi.mission_id = mp.id)
  AND (mp.category_id IS NULL
       OR mp.category_id NOT IN (
            SELECT id FROM (
              SELECT c.id FROM tbl_category c
              WHERE c.category_name IN (
                '음식점/외식','배달앱','카페/간식','편의점',
                '온라인쇼핑','패션','뷰티',
                '택시/모빌리티','주유/충전','주차/통행료',
                '장보기/마트',
                '영화·공연·전시','도서','취미','레저')
                AND c.parent_id IS NOT NULL
            ) t
          ));

-- ---------------------------------------------------------------------
-- 결과 확인 — RELATIVE 30 / ABSOLUTE 30 이어야 한다
-- ---------------------------------------------------------------------
SELECT mission_type AS 미션유형, COUNT(*) AS 문구수, COUNT(DISTINCT category_id) AS 소분류수
FROM tbl_mission_pool
GROUP BY mission_type;

-- 미션 대상 소분류 목록 (15개여야 한다)
SELECT p.category_name AS 대분류, c.category_name AS 소분류,
       SUM(mp.mission_type = 'RELATIVE') AS 상대형,
       SUM(mp.mission_type = 'ABSOLUTE') AS 절대형
FROM tbl_mission_pool mp
JOIN tbl_category c ON c.id = mp.category_id
LEFT JOIN tbl_category p ON p.id = c.parent_id
GROUP BY p.category_name, c.category_name
ORDER BY p.category_name, c.category_name;
