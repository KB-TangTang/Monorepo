-- CPR-V3.xlsx vs 현재 DB(schema.sql + db/migration/*.sql) 비교 결과
-- 작성일: 2026-08-19
--
-- 결론:
--   바로 적용할 추가/수정 DDL은 없음.
--
-- 이유:
--   CPR-V3.xlsx는 현재 repo DB 마이그레이션보다 오래된 기준으로 보인다.
--   현재 DB에는 CPR에 없는 최신 테이블 9개와 최신 컬럼 48개가 이미 존재한다.
--   아래 항목들은 CPR 기준으로 보면 차이지만, 대부분 과거 컬럼명/오타/이미 반영된 설계 변경이므로
--   운영 DB에 그대로 적용하면 최신 기능(금융 동기화, 카드, LLM 분류, 월간 AI 분석 등)이 퇴행할 수 있다.
--
-- 비교 기준:
--   - ERD: C:/Users/student/Downloads/CPR-V3.xlsx
--   - DB : db/schema.sql + db/migration/*.sql 파일명 오름차순 반영 기준

-- ============================================================================
-- 1. CPR에는 있지만 현재 DB에 그대로 추가하면 안 되는 컬럼 후보
-- ============================================================================

-- tbl_savings_product_cache.update_date
-- 현재 DB는 updated_at을 사용한다. update_date는 과거/오타성 컬럼명으로 판단된다.
-- 적용 안 함:
-- ALTER TABLE tbl_savings_product_cache
--   ADD COLUMN update_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- tbl_investment_holding.account_id2
-- account_id가 이미 FK로 존재한다. account_id2는 ERD 오타/중복 컬럼으로 판단된다.
-- 적용 안 함:
-- ALTER TABLE tbl_investment_holding
--   ADD COLUMN account_id2 BIGINT NOT NULL;

-- tbl_defense.requested_adjustment_amount
-- 현재 DB는 20260814_group_challenge_verdict_deduction.sql에서
-- requested_adjustment_amount를 actual_burden_amount로 의미 변경했고,
-- deduction_amount를 별도 컬럼으로 추가했다.
-- 적용 안 함:
-- ALTER TABLE tbl_defense
--   ADD COLUMN requested_adjustment_amount DECIMAL(15,2) NULL;

-- tbl_notification_setting.update_date
-- 현재 DB는 updated_at을 사용한다.
-- 적용 안 함:
-- ALTER TABLE tbl_notification_setting
--   ADD COLUMN update_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- tbl_connected_account.account_masked
-- 현재 DB는 account_no_masked를 사용한다.
-- 적용 안 함:
-- ALTER TABLE tbl_connected_account
--   ADD COLUMN account_masked VARCHAR(30) NOT NULL;

-- tbl_streak_count.Field / Field2 / Field3 / Field4 / Field5
-- 현재 DB는 실제 의미가 있는 물리명(streak_count, longest_streak_count, last_check_date,
-- created_at, updated_at)을 사용한다. CPR의 Field*는 미정리 물리명이다.
-- 적용 안 함:
-- ALTER TABLE tbl_streak_count
--   ADD COLUMN Field BIGINT NULL,
--   ADD COLUMN Field2 BIGINT NULL,
--   ADD COLUMN Field3 DATETIME NOT NULL,
--   ADD COLUMN Field5 DATETIME NOT NULL,
--   ADD COLUMN Field4 DATETIME NOT NULL;

-- tbl_indictment.update_at
-- 현재 DB는 updated_at을 사용한다.
-- 적용 안 함:
-- ALTER TABLE tbl_indictment
--   ADD COLUMN update_at DATETIME NOT NULL;

-- tbl_youth_policy_cache.update_date
-- 현재 DB는 updated_at을 사용한다.
-- 적용 안 함:
-- ALTER TABLE tbl_youth_policy_cache
--   ADD COLUMN update_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- ============================================================================
-- 2. CPR과 현재 DB의 타입/NULL 차이 중 퇴행 위험이 있어 보류한 항목
-- ============================================================================

-- tbl_vote.comment: 현재 VARCHAR(40), CPR VARCHAR(255)
-- 현재 schema.sql 주석에 GC_06_05 최대 40자 정책이 명시되어 있어 유지.
-- 정책을 255자로 바꾸기로 확정한 경우에만 아래 SQL을 적용한다.
-- ALTER TABLE tbl_vote
--   MODIFY COLUMN comment VARCHAR(255) NULL COMMENT '익명 한줄 코멘트';

-- tbl_group_member.final_outcome: 현재 VARCHAR(20), CPR VARCHAR(10)
-- ELIMINATED가 10자로 딱 맞지만, 상태 컬럼은 전반적으로 VARCHAR(20)을 쓰는 현재 설계를 유지.
-- 적용 안 함:
-- ALTER TABLE tbl_group_member
--   MODIFY COLUMN final_outcome VARCHAR(10) NULL;

-- tbl_notification_setting.notification_type: 현재 NULL 허용, CPR NOT NULL
-- 현재 DB 주석은 "현재는 NULL" 확장 설계를 설명하므로 유지.
-- 적용 안 함:
-- ALTER TABLE tbl_notification_setting
--   MODIFY COLUMN notification_type VARCHAR(30) NOT NULL;

-- tbl_connected_account.last_sync_at/account_name: 현재 NULL 허용, CPR NOT NULL
-- 연결 직후/동기화 실패/마이그레이션 데이터에서 NULL이 정상적으로 가능하므로 유지.
-- 적용 안 함:
-- ALTER TABLE tbl_connected_account
--   MODIFY COLUMN last_sync_at DATETIME NOT NULL,
--   MODIFY COLUMN account_name VARCHAR(100) NOT NULL;

-- tbl_transaction.account_id: 현재 NULL 허용, CPR NOT NULL
-- 20260812_add_financial_sync_tables.sql에서 카드/대출 출처 거래를 위해 NULL 허용으로 변경됨.
-- 최신 금융 동기화 설계에 필요하므로 유지.
-- 적용 안 함:
-- ALTER TABLE tbl_transaction
--   MODIFY COLUMN account_id BIGINT NOT NULL;

-- tbl_asset_snapshot.ai_comment/compare_comment: 현재 ai_comment JSON NULL, compare_comment TEXT NULL
-- 20260813_add_monthly_report_ai_analysis_to_asset_snapshot.sql에서 최신 월간 AI 분석 설계로 변경됨.
-- 적용 안 함:
-- ALTER TABLE tbl_asset_snapshot
--   MODIFY COLUMN ai_comment TEXT NOT NULL,
--   MODIFY COLUMN compare_comment TEXT NOT NULL;

-- tbl_challenge_monthly_report.net_amount: 현재 생성 컬럼, CPR 일반 DECIMAL
-- 현재 DB는 saved_amount - over_amount 생성 컬럼으로 계산 일관성을 보장하므로 유지.
-- 적용 안 함:
-- ALTER TABLE tbl_challenge_monthly_report
--   MODIFY COLUMN net_amount DECIMAL(15,2) NULL;

-- tbl_user.difficulty_id DEFAULT 'EASY'
-- difficulty_id는 BIGINT FK라 문자열 기본값 'EASY'는 적용 불가.
-- 적용 안 함:
-- ALTER TABLE tbl_user
--   MODIFY COLUMN difficulty_id BIGINT NOT NULL DEFAULT 'EASY';

-- ============================================================================
-- 3. CPR에 없지만 현재 DB에 존재하는 최신 구조
-- ============================================================================

-- CPR-V3에는 아래 최신 구조가 없다. 삭제 대상이 아니라 CPR 쪽을 갱신해야 하는 항목이다.
--   - tbl_card
--   - tbl_card_bill
--   - tbl_financial_sync_history
--   - tbl_merchant_keyword_rule
--   - tbl_llm_categorization_job
--   - tbl_llm_categorization_job_item
--   - tbl_fixed_expense_payment_reminder
--   - tbl_mission_certificate_title
--   - tbl_user_mission_analysis
--
-- 또한 CPR에는 아래 최신 컬럼들이 없다. 삭제하지 않는다.
--   - tbl_transaction.source_type/card_id/correlation_id/linked_transaction_id/original_approval_no/
--     merchant_category_code/merchant_category_name/raw_json/category_source
--   - tbl_asset_snapshot.ai_analysis_*
--   - tbl_challenge_monthly_report.monthly_longest_streak/best_weekday/earned_score/group_record_json
--   - tbl_group_challenge_daily_result.verdict_deduction_amount/effective_amount
--   - tbl_user.social_name/tutorial_seen_at/group_tutorial_seen_at/profile_image_key/
--     relative_mission_qualified_at
--
-- 따라서 이 파일에는 실행 DDL을 두지 않는다.
