-- 2026-08-11 · 메인 챌린지 카테고리 로테이션 (DECISIONS.md 2026-08-11 (3) 참고)
-- tbl_user_mission_analysis 신설 — 상위 3개 카테고리 분석 스냅샷
--
-- 신규 설치: **실행 대상이다.** 이 변경은 schema.sql 에 반영돼 있지 않다.
--
-- 배경
--   미션 대상 카테고리를 매일 재산정하면 최근 28일 누적 순위가 하루로는 거의 안 바뀌어
--   **매일 같은 카테고리(1위)만 나온다.** 그렇다고 화면의 "3곳 중 2번째" 를 그리려면
--   3개 목록이 며칠간 고정돼야 하는데, 매일 SQL 을 다시 돌리면 1일 차와 2일 차의 상위 3개가
--   달라질 수 있다. 그래서 산정 결과를 스냅샷으로 남긴다.
--
-- 주기 정의 — 날짜가 아니라 **소진** 기준이다
--   "3일 주기" 가 아니라 **"상위 3개를 다 쓸 때까지"** 가 한 주기다.
--   assigned_date 가 전 행에 채워지면 그 주기가 끝나고, 다음 자정 배치가 재산정한다.
--
--   이렇게 한 이유: 절대형(월 1회 불시 검문)이 하루를 가로채는 날이 있다. 날짜로 주기를 고정하면
--   그때마다 종료일을 다시 계산해야 하는데, 소진 기준이면 그날은 아무 rank 도 소진되지 않아
--   **자동으로 다음 날 이어진다.** 상위 카테고리가 2개뿐이면 2일 만에 소진돼 재산정되므로
--   "1위 → 2위 → 1위" 같은 억지 규칙도 필요 없다.
--
-- 자정 배치 흐름
--   ① 전일 미션 판정
--   ② 오늘이 절대형 배정일인가?  → YES: 절대형 배정 후 종료(스냅샷 미변경)
--   ③ assigned_date IS NULL 인 rank 가 있나?
--        YES → 최소 rank 를 오늘 미션으로 배정하고 assigned_date = 오늘
--        NO  → 최근 28일 재산정 → 새 주기 INSERT → rank 1 배정
--   ④ 상위 카테고리가 0개면 절대형(콜드스타트)
--
-- 적용 방법 (팀원 각자 1회)
--   mysql -u tangtang -p tangtang --default-character-set=utf8mb4 \
--     -e "source D:/KB_Final_Project/app/Monorepo/db/migration/20260811_add_mission_analysis_snapshot.sql"

USE tangtang;

CREATE TABLE tbl_user_mission_analysis (
  id                BIGINT        NOT NULL AUTO_INCREMENT,
  user_id           BIGINT        NOT NULL,
  cycle_start_date  DATE          NOT NULL COMMENT '이 주기를 산정한 날',
  category_id       BIGINT        NOT NULL COMMENT '미션 풀에 행이 있는 소분류만 들어온다',
  category_rank     TINYINT       NOT NULL COMMENT '1~3. 배정 순서',
  spending_amount   DECIMAL(15,2) NOT NULL COMMENT '산정 시점 최근 28일 합계',
  spending_ratio    DECIMAL(5,2)  NOT NULL COMMENT '전체 소비 대비 비중(%). 화면 "배달앱 24%" 표기용',
  transaction_count INT           NOT NULL COMMENT '산정 시점 거래 건수',
  assigned_date     DATE          NULL     COMMENT '이 순위가 배정된 날. NULL 이면 아직 안 썼다',
                                           -- ⚠ 기준액 B·이용일은 여기 저장하지 않는다.
                                           --   배정 당일 A-1 이 계산해야 2·3일 차에 낡은 값으로
                                           --   목표 금액이 나오지 않는다.
  created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  -- 한 주기에 같은 순위가 둘일 수 없다
  UNIQUE KEY uk_uma_user_cycle_rank (user_id, cycle_start_date, category_rank),
  -- 자정 배치의 주 경로: "아직 안 쓴 rank 가 있나" 조회
  KEY idx_uma_user_pending (user_id, assigned_date, category_rank),
  CONSTRAINT fk_uma_user     FOREIGN KEY (user_id)     REFERENCES tbl_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_uma_category FOREIGN KEY (category_id) REFERENCES tbl_category(id),
  CONSTRAINT ck_uma_rank     CHECK (category_rank BETWEEN 1 AND 3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='메인 챌린지 카테고리 분석 스냅샷 (주기당 사용자별 최대 3행)';

-- 확인
--   SHOW CREATE TABLE tbl_user_mission_analysis\G
--   기대: uk_uma_user_cycle_rank · idx_uma_user_pending · ck_uma_rank · FK 2개
