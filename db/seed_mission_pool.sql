-- =====================================================================
-- db/seed_mission_pool.sql — 데일리 미션 풀 초기 데이터
--
-- 선행 조건
--   ① db/seed_category.sql 적용 완료 (대분류 12 · 소분류 46)
--   ② db/migration/20260809_ai_verdict_and_mission_pool_check.sql 적용 완료
--
-- 실행: mysql -u tangtang -p tangtang < db/seed_mission_pool.sql
--
-- 카테고리 단위 (2026-08-09 확정)
--   · 상대형(RELATIVE) = **소분류** 단위. 거래에 붙는 카테고리와 같은 레벨이라
--     "식비 › 배달앱" 처럼 대·소분류를 함께 보여줄 수 있다.
--     기타(자동 분류 불가·사용자 직접 지정) 소분류 2개는 미션 대상에서 제외했다 →  44개 × 2 = 88행
--   · 절대형(ABSOLUTE) = **대분류** 단위. 콜드스타트 사용자는 소분류로 나눌 데이터 자체가 없다.
--     limit_price 가 곧 목표다(0=무지출, >0=상한형) →  12개 × 2 = 24행
--
-- 미션 풀이 비어 있으면 그 카테고리는 배정에서 건너뛴다.
-- 문구 규칙: 상대형은 금액을 넣지 않는다(개인화 목표가 화면에서 합쳐진다).
--            절대형은 상한 금액을 그대로 적고 limit_price 와 일치시킨다.
-- =====================================================================

USE tangtang;
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 상대형(RELATIVE) — 소분류 44개 × 2문구
-- ---------------------------------------------------------------------
INSERT INTO tbl_mission_pool (mission_title, mission_content, mission_type, limit_price, category_id)
SELECT v.title, v.content, 'RELATIVE', NULL, child.id
FROM (
  SELECT '외식 현장 급습' AS title, '오늘 외식비를 목표 금액 안에서 해결한다.' AS content, '식비' AS parent_cat, '음식점/외식' AS child_cat UNION ALL
  SELECT '바깥밥 한 끼 줄이기' AS title, '오늘 외식 결제를 한 건으로 제한한다.' AS content, '식비' AS parent_cat, '음식점/외식' AS child_cat UNION ALL
  SELECT '배달 현장 급습' AS title, '오늘 배달 지출을 목표 금액 안으로 묶는다.' AS content, '식비' AS parent_cat, '배달앱' AS child_cat UNION ALL
  SELECT '배달 앱 봉인' AS title, '오늘 배달 앱을 열지 않는다. 탕이가 주문 내역을 본다.' AS content, '식비' AS parent_cat, '배달앱' AS child_cat UNION ALL
  SELECT '카페 영수증 심문' AS title, '오늘 카페·간식 지출을 목표 금액 안에서 끝낸다.' AS content, '식비' AS parent_cat, '카페/간식' AS child_cat UNION ALL
  SELECT '커피 한 잔 줄이기' AS title, '오늘 카페 결제를 한 건으로 제한한다.' AS content, '식비' AS parent_cat, '카페/간식' AS child_cat UNION ALL
  SELECT '편의점 순찰' AS title, '오늘 편의점 지출을 목표 금액 안으로 묶는다.' AS content, '식비' AS parent_cat, '편의점' AS child_cat UNION ALL
  SELECT '충동 집기 방지' AS title, '편의점에서 계획에 없던 물건을 담지 않는다.' AS content, '식비' AS parent_cat, '편의점' AS child_cat UNION ALL
  SELECT '장바구니 압수' AS title, '오늘 온라인 쇼핑 지출을 목표 금액 안으로 묶는다.' AS content, '쇼핑' AS parent_cat, '온라인쇼핑' AS child_cat UNION ALL
  SELECT '결제 하루 미루기' AS title, '담아두되 오늘은 결제하지 않는다.' AS content, '쇼핑' AS parent_cat, '온라인쇼핑' AS child_cat UNION ALL
  SELECT '옷장 현장 검증' AS title, '오늘 패션 지출을 목표 금액 안에서 해결한다.' AS content, '쇼핑' AS parent_cat, '패션' AS child_cat UNION ALL
  SELECT '세일 유혹 방어' AS title, '할인 알림이 와도 오늘은 사지 않는다.' AS content, '쇼핑' AS parent_cat, '패션' AS child_cat UNION ALL
  SELECT '화장대 재고 확인' AS title, '오늘 뷰티 지출을 목표 금액 안으로 묶는다.' AS content, '쇼핑' AS parent_cat, '뷰티' AS child_cat UNION ALL
  SELECT '사기 전 재고 확인' AS title, '집에 남은 게 있는지 먼저 본다.' AS content, '쇼핑' AS parent_cat, '뷰티' AS child_cat UNION ALL
  SELECT '교통비 절감 명령' AS title, '오늘 대중교통 지출을 목표 금액 안으로 줄인다.' AS content, '교통' AS parent_cat, '대중교통' AS child_cat UNION ALL
  SELECT '두 정거장 걷기' AS title, '가까운 거리는 걷는다.' AS content, '교통' AS parent_cat, '대중교통' AS child_cat UNION ALL
  SELECT '택시비 심문' AS title, '오늘 택시·모빌리티 지출을 목표 금액 안으로 묶는다.' AS content, '교통' AS parent_cat, '택시/모빌리티' AS child_cat UNION ALL
  SELECT '택시 대신 대중교통' AS title, '오늘은 택시를 부르지 않는다.' AS content, '교통' AS parent_cat, '택시/모빌리티' AS child_cat UNION ALL
  SELECT '주유비 점검' AS title, '오늘 주유·충전 지출을 목표 금액 안에서 해결한다.' AS content, '교통' AS parent_cat, '주유/충전' AS child_cat UNION ALL
  SELECT '공회전 줄이기' AS title, '불필요한 운행을 오늘 하루 접는다.' AS content, '교통' AS parent_cat, '주유/충전' AS child_cat UNION ALL
  SELECT '주차비 감시' AS title, '오늘 주차·통행료를 목표 금액 안으로 묶는다.' AS content, '교통' AS parent_cat, '주차/통행료' AS child_cat UNION ALL
  SELECT '주차장 대신 대중교통' AS title, '유료 주차를 오늘은 쓰지 않는다.' AS content, '교통' AS parent_cat, '주차/통행료' AS child_cat UNION ALL
  SELECT '주거비 방어선' AS title, '오늘 월세 관련 추가 지출을 목표 금액 안으로 묶는다.' AS content, '주거/공과금' AS parent_cat, '월세' AS child_cat UNION ALL
  SELECT '추가 납부 보류' AS title, '예정에 없던 주거 납부를 오늘은 하지 않는다.' AS content, '주거/공과금' AS parent_cat, '월세' AS child_cat UNION ALL
  SELECT '관리비 점검' AS title, '오늘 관리비 관련 지출을 목표 금액 안에서 해결한다.' AS content, '주거/공과금' AS parent_cat, '관리비' AS child_cat UNION ALL
  SELECT '고지서 확인' AS title, '청구 내역을 확인하고 추가 지출을 만들지 않는다.' AS content, '주거/공과금' AS parent_cat, '관리비' AS child_cat UNION ALL
  SELECT '공과금 현장 검증' AS title, '오늘 전기·가스·수도 관련 지출을 목표 금액 안으로 묶는다.' AS content, '주거/공과금' AS parent_cat, '전기·가스·수도' AS child_cat UNION ALL
  SELECT '대기전력 차단' AS title, '오늘 하루 쓰지 않는 플러그를 뽑는다.' AS content, '주거/공과금' AS parent_cat, '전기·가스·수도' AS child_cat UNION ALL
  SELECT '장바구니 검문' AS title, '오늘 장보기 지출을 목표 금액 안으로 묶는다.' AS content, '생활' AS parent_cat, '장보기/마트' AS child_cat UNION ALL
  SELECT '목록만 사기' AS title, '적어간 것만 담는다.' AS content, '생활' AS parent_cat, '장보기/마트' AS child_cat UNION ALL
  SELECT '생활용품 재고 확인' AS title, '오늘 생활용품 지출을 목표 금액 안에서 해결한다.' AS content, '생활' AS parent_cat, '생활용품' AS child_cat UNION ALL
  SELECT '사기 전 확인' AS title, '집에 있는지 먼저 본다.' AS content, '생활' AS parent_cat, '생활용품' AS child_cat UNION ALL
  SELECT '세탁비 절감' AS title, '오늘 세탁 지출을 목표 금액 안으로 묶는다.' AS content, '생활' AS parent_cat, '세탁비' AS child_cat UNION ALL
  SELECT '모아서 한 번에' AS title, '오늘은 세탁 결제를 하지 않는다.' AS content, '생활' AS parent_cat, '세탁비' AS child_cat UNION ALL
  SELECT '선물비 심문' AS title, '오늘 경조사·선물 지출을 목표 금액 안에서 해결한다.' AS content, '생활' AS parent_cat, '경조사/선물' AS child_cat UNION ALL
  SELECT '예산 정하고 사기' AS title, '금액을 먼저 정하고 고른다.' AS content, '생활' AS parent_cat, '경조사/선물' AS child_cat UNION ALL
  SELECT '자녀 지출 점검' AS title, '오늘 자녀 관련 지출을 목표 금액 안으로 묶는다.' AS content, '생활' AS parent_cat, '자녀' AS child_cat UNION ALL
  SELECT '충동 구매 방지' AS title, '예정에 없던 구매를 오늘은 미룬다.' AS content, '생활' AS parent_cat, '자녀' AS child_cat UNION ALL
  SELECT '반려동물 지출 점검' AS title, '오늘 반려동물 지출을 목표 금액 안에서 해결한다.' AS content, '생활' AS parent_cat, '반려동물' AS child_cat UNION ALL
  SELECT '간식 재고 확인' AS title, '남은 게 있는지 먼저 본다.' AS content, '생활' AS parent_cat, '반려동물' AS child_cat UNION ALL
  SELECT '통신비 감시' AS title, '오늘 통신 관련 추가 지출을 목표 금액 안으로 묶는다.' AS content, '구독/디지털' AS parent_cat, '통신비' AS child_cat UNION ALL
  SELECT '부가서비스 점검' AS title, '오늘 부가 결제를 하지 않는다.' AS content, '구독/디지털' AS parent_cat, '통신비' AS child_cat UNION ALL
  SELECT 'OTT 구독 현장 검증' AS title, '오늘 OTT 지출을 목표 금액 안으로 묶는다.' AS content, '구독/디지털' AS parent_cat, 'OTT' AS child_cat UNION ALL
  SELECT '새 구독 금지' AS title, '오늘 새 OTT를 결제하지 않는다. 무료체험도 결제로 본다.' AS content, '구독/디지털' AS parent_cat, 'OTT' AS child_cat UNION ALL
  SELECT '음원 구독 점검' AS title, '오늘 음원 지출을 목표 금액 안에서 해결한다.' AS content, '구독/디지털' AS parent_cat, '음원' AS child_cat UNION ALL
  SELECT '중복 구독 확인' AS title, '겹치는 구독이 없는지 본다.' AS content, '구독/디지털' AS parent_cat, '음원' AS child_cat UNION ALL
  SELECT '인앱 결제 정지' AS title, '오늘 앱·게임 결제를 목표 금액 안으로 묶는다.' AS content, '구독/디지털' AS parent_cat, '앱·게임' AS child_cat UNION ALL
  SELECT '아이템 결제 금지' AS title, '오늘 인앱 결제를 하지 않는다.' AS content, '구독/디지털' AS parent_cat, '앱·게임' AS child_cat UNION ALL
  SELECT '구독료 현장 검증' AS title, '오늘 소프트웨어·클라우드 지출을 목표 금액 안으로 묶는다.' AS content, '구독/디지털' AS parent_cat, '소프트웨어·클라우드' AS child_cat UNION ALL
  SELECT '안 쓰는 요금제 확인' AS title, '사용하지 않는 구독을 점검한다.' AS content, '구독/디지털' AS parent_cat, '소프트웨어·클라우드' AS child_cat UNION ALL
  SELECT '관람비 심리' AS title, '오늘 영화·공연 지출을 목표 금액 안에서 해결한다.' AS content, '문화/여가' AS parent_cat, '영화·공연·전시' AS child_cat UNION ALL
  SELECT '할인 먼저 찾기' AS title, '제값 결제 전에 할인을 확인한다.' AS content, '문화/여가' AS parent_cat, '영화·공연·전시' AS child_cat UNION ALL
  SELECT '도서 구입 보류' AS title, '오늘 도서 지출을 목표 금액 안으로 묶는다.' AS content, '문화/여가' AS parent_cat, '도서' AS child_cat UNION ALL
  SELECT '도서관 먼저' AS title, '사기 전에 빌릴 수 있는지 본다.' AS content, '문화/여가' AS parent_cat, '도서' AS child_cat UNION ALL
  SELECT '취미비 절감' AS title, '오늘 취미 지출을 목표 금액 안에서 해결한다.' AS content, '문화/여가' AS parent_cat, '취미' AS child_cat UNION ALL
  SELECT '장비 추가 보류' AS title, '새 장비 결제를 오늘은 미룬다.' AS content, '문화/여가' AS parent_cat, '취미' AS child_cat UNION ALL
  SELECT '레저비 점검' AS title, '오늘 레저 지출을 목표 금액 안으로 묶는다.' AS content, '문화/여가' AS parent_cat, '레저' AS child_cat UNION ALL
  SELECT '무료 활동으로 대체' AS title, '돈 쓰지 않는 방식으로 논다.' AS content, '문화/여가' AS parent_cat, '레저' AS child_cat UNION ALL
  SELECT '여행 교통권 심문' AS title, '오늘 여행 교통권 지출을 목표 금액 안으로 묶는다.' AS content, '여행' AS parent_cat, '교통권' AS child_cat UNION ALL
  SELECT '예약 하루 미루기' AS title, '오늘은 예약을 확정하지 않는다.' AS content, '여행' AS parent_cat, '교통권' AS child_cat UNION ALL
  SELECT '숙소 결제 재검토' AS title, '오늘 숙박 지출을 목표 금액 안에서 해결한다.' AS content, '여행' AS parent_cat, '숙박' AS child_cat UNION ALL
  SELECT '비교 먼저' AS title, '결제 전에 다른 숙소와 비교한다.' AS content, '여행' AS parent_cat, '숙박' AS child_cat UNION ALL
  SELECT '여행상품 검문' AS title, '오늘 여행상품 지출을 목표 금액 안으로 묶는다.' AS content, '여행' AS parent_cat, '관광·여행상품' AS child_cat UNION ALL
  SELECT '충동 예약 방지' AS title, '오늘은 새 상품을 결제하지 않는다.' AS content, '여행' AS parent_cat, '관광·여행상품' AS child_cat UNION ALL
  SELECT '병원비 점검' AS title, '오늘 병원 지출을 목표 금액 안에서 해결한다.' AS content, '건강' AS parent_cat, '병원' AS child_cat UNION ALL
  SELECT '예약 확인' AS title, '불필요한 추가 진료를 만들지 않는다.' AS content, '건강' AS parent_cat, '병원' AS child_cat UNION ALL
  SELECT '약국 재고 확인' AS title, '오늘 약국 지출을 목표 금액 안으로 묶는다.' AS content, '건강' AS parent_cat, '약국' AS child_cat UNION ALL
  SELECT '집에 있는 약 먼저' AS title, '사기 전에 남은 것을 본다.' AS content, '건강' AS parent_cat, '약국' AS child_cat UNION ALL
  SELECT '운동시설 결제 심문' AS title, '오늘 운동시설 지출을 목표 금액 안에서 해결한다.' AS content, '건강' AS parent_cat, '운동시설' AS child_cat UNION ALL
  SELECT '충동 등록 방지' AS title, '오늘 새 등록·결제를 하지 않는다.' AS content, '건강' AS parent_cat, '운동시설' AS child_cat UNION ALL
  SELECT '건강관리비 점검' AS title, '오늘 건강관리 지출을 목표 금액 안으로 묶는다.' AS content, '건강' AS parent_cat, '건강관리' AS child_cat UNION ALL
  SELECT '영양제 재고 확인' AS title, '남은 게 있는지 먼저 본다.' AS content, '건강' AS parent_cat, '건강관리' AS child_cat UNION ALL
  SELECT '학원비 심문' AS title, '오늘 학원 지출을 목표 금액 안에서 해결한다.' AS content, '교육/자기계발' AS parent_cat, '학원' AS child_cat UNION ALL
  SELECT '추가 등록 보류' AS title, '오늘 새 등록을 하지 않는다.' AS content, '교육/자기계발' AS parent_cat, '학원' AS child_cat UNION ALL
  SELECT '강의 결제 보류' AS title, '오늘 온라인 강의 지출을 목표 금액 안으로 묶는다.' AS content, '교육/자기계발' AS parent_cat, '온라인 강의' AS child_cat UNION ALL
  SELECT '듣던 것부터' AS title, '새 강의를 결제하지 않는다.' AS content, '교육/자기계발' AS parent_cat, '온라인 강의' AS child_cat UNION ALL
  SELECT '응시료 점검' AS title, '오늘 시험·자격증 지출을 목표 금액 안에서 해결한다.' AS content, '교육/자기계발' AS parent_cat, '시험·자격증' AS child_cat UNION ALL
  SELECT '일정 확인 후 결제' AS title, '오늘은 접수를 미룬다.' AS content, '교육/자기계발' AS parent_cat, '시험·자격증' AS child_cat UNION ALL
  SELECT '교재 구입 보류' AS title, '오늘 교재 지출을 목표 금액 안으로 묶는다.' AS content, '교육/자기계발' AS parent_cat, '학습 교재' AS child_cat UNION ALL
  SELECT '있는 책부터' AS title, '사기 전에 가진 교재를 본다.' AS content, '교육/자기계발' AS parent_cat, '학습 교재' AS child_cat UNION ALL
  SELECT '보험료 점검' AS title, '오늘 보험 관련 추가 지출을 목표 금액 안으로 묶는다.' AS content, '금융/보험' AS parent_cat, '보험료' AS child_cat UNION ALL
  SELECT '중복 보장 확인' AS title, '겹치는 보장이 없는지 본다.' AS content, '금융/보험' AS parent_cat, '보험료' AS child_cat UNION ALL
  SELECT '이자 감시' AS title, '오늘 이자 관련 지출을 목표 금액 안에서 해결한다.' AS content, '금융/보험' AS parent_cat, '이자' AS child_cat UNION ALL
  SELECT '추가 대출 보류' AS title, '오늘은 새 대출을 만들지 않는다.' AS content, '금융/보험' AS parent_cat, '이자' AS child_cat UNION ALL
  SELECT '수수료 차단' AS title, '오늘 수수료 지출을 목표 금액 안으로 묶는다.' AS content, '금융/보험' AS parent_cat, '수수료' AS child_cat UNION ALL
  SELECT '무료 경로 찾기' AS title, '수수료 붙는 거래를 오늘은 피한다.' AS content, '금융/보험' AS parent_cat, '수수료' AS child_cat UNION ALL
  SELECT '금융상품 결제 점검' AS title, '오늘 금융상품 지출을 목표 금액 안에서 해결한다.' AS content, '금융/보험' AS parent_cat, '금융상품' AS child_cat UNION ALL
  SELECT '조건 확인 후 가입' AS title, '오늘은 신규 가입을 미룬다.' AS content, '금융/보험' AS parent_cat, '금융상품' AS child_cat
) v
JOIN tbl_category parent ON parent.category_name = v.parent_cat AND parent.parent_id IS NULL
JOIN tbl_category child  ON child.category_name  = v.child_cat  AND child.parent_id = parent.id
WHERE NOT EXISTS (SELECT 1 FROM tbl_mission_pool m WHERE m.mission_title = v.title);


-- ---------------------------------------------------------------------
-- 절대형(ABSOLUTE) — 콜드스타트 전용, 대분류 12개 × 2문구
-- ---------------------------------------------------------------------
INSERT INTO tbl_mission_pool (mission_title, mission_content, mission_type, limit_price, category_id)
SELECT v.title, v.content, 'ABSOLUTE', v.limit_price, c.id
FROM (
  SELECT '무지출 명령 · 식비' AS title, '오늘 식비 결제를 0원으로 만든다.' AS content, 0 AS limit_price, '식비' AS cat UNION ALL
  SELECT '식비 상한 1만원', '오늘 식비를 1만원 안에서 해결한다.', 10000, '식비' UNION ALL
  SELECT '무지출 명령 · 쇼핑' AS title, '오늘 쇼핑 결제를 0원으로 만든다.' AS content, 0 AS limit_price, '쇼핑' AS cat UNION ALL
  SELECT '쇼핑 상한 2만원', '오늘 쇼핑 지출을 2만원 안으로 묶는다.', 20000, '쇼핑' UNION ALL
  SELECT '무지출 명령 · 교통' AS title, '오늘 교통 추가 결제를 0원으로 만든다.' AS content, 0 AS limit_price, '교통' AS cat UNION ALL
  SELECT '교통 상한 5천원', '오늘 교통비를 5천원 안에서 해결한다.', 5000, '교통' UNION ALL
  SELECT '무지출 명령 · 주거' AS title, '오늘 주거·공과금 추가 결제를 0원으로 만든다.' AS content, 0 AS limit_price, '주거/공과금' AS cat UNION ALL
  SELECT '주거 상한 1만원', '오늘 주거 관련 추가 지출을 1만원 안으로 묶는다.', 10000, '주거/공과금' UNION ALL
  SELECT '무지출 명령 · 생활' AS title, '오늘 생활 결제를 0원으로 만든다.' AS content, 0 AS limit_price, '생활' AS cat UNION ALL
  SELECT '생활 상한 5천원', '오늘 생활 지출을 5천원 안에서 해결한다.', 5000, '생활' UNION ALL
  SELECT '무지출 명령 · 구독' AS title, '오늘 구독·앱 결제를 0원으로 만든다.' AS content, 0 AS limit_price, '구독/디지털' AS cat UNION ALL
  SELECT '구독 상한 5천원', '오늘 디지털 결제를 5천원 안으로 묶는다.', 5000, '구독/디지털' UNION ALL
  SELECT '무지출 명령 · 여가' AS title, '오늘 문화·여가 결제를 0원으로 만든다.' AS content, 0 AS limit_price, '문화/여가' AS cat UNION ALL
  SELECT '여가 상한 1만원', '오늘 여가 지출을 1만원 안에서 해결한다.', 10000, '문화/여가' UNION ALL
  SELECT '무지출 명령 · 여행' AS title, '오늘 여행 관련 결제를 0원으로 만든다.' AS content, 0 AS limit_price, '여행' AS cat UNION ALL
  SELECT '여행 상한 3만원', '오늘 여행 지출을 3만원 안으로 묶는다.', 30000, '여행' UNION ALL
  SELECT '무지출 명령 · 건강' AS title, '오늘 건강 관련 추가 결제를 0원으로 만든다.' AS content, 0 AS limit_price, '건강' AS cat UNION ALL
  SELECT '건강 상한 1만원', '오늘 건강 지출을 1만원 안에서 해결한다.', 10000, '건강' UNION ALL
  SELECT '무지출 명령 · 자기계발' AS title, '오늘 교육·자기계발 결제를 0원으로 만든다.' AS content, 0 AS limit_price, '교육/자기계발' AS cat UNION ALL
  SELECT '자기계발 상한 2만원', '오늘 교육 지출을 2만원 안으로 묶는다.', 20000, '교육/자기계발' UNION ALL
  SELECT '무지출 명령 · 금융' AS title, '오늘 금융·보험 추가 결제를 0원으로 만든다.' AS content, 0 AS limit_price, '금융/보험' AS cat UNION ALL
  SELECT '금융 상한 5천원', '오늘 금융 관련 지출을 5천원 안으로 묶는다.', 5000, '금융/보험' UNION ALL
  SELECT '무지출 명령 · 기타' AS title, '오늘 기타로 잡히는 결제를 0원으로 만든다.' AS content, 0 AS limit_price, '기타' AS cat UNION ALL
  SELECT '기타 상한 5천원', '오늘 기타 지출을 5천원 안으로 묶는다.', 5000, '기타'
) v
JOIN tbl_category c ON c.category_name = v.cat AND c.parent_id IS NULL
WHERE NOT EXISTS (SELECT 1 FROM tbl_mission_pool m WHERE m.mission_title = v.title);


-- =====================================================================
-- 커버리지 확인 — 결과가 0행이면 정상
-- =====================================================================
-- 상대형: 기타를 뺀 소분류 44개가 모두 채워졌는지
-- SELECT ch.category_name FROM tbl_category ch
-- JOIN tbl_category p ON p.id = ch.parent_id
-- LEFT JOIN tbl_mission_pool m ON m.category_id = ch.id AND m.mission_type='RELATIVE'
-- WHERE p.category_name <> '기타' AND m.id IS NULL;
--
-- 절대형: 대분류 12개가 모두 채워졌는지
-- SELECT c.category_name FROM tbl_category c
-- LEFT JOIN tbl_mission_pool m ON m.category_id = c.id AND m.mission_type='ABSOLUTE'
-- WHERE c.parent_id IS NULL AND m.id IS NULL;
