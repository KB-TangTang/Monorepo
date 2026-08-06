-- 시연용 알림 데이터. 팀 공용 seed.sql 과 분리한다 (DECISIONS.md 2026-08-06 (3)).
-- 실행: mysql -u tangtang -p tangtang --default-character-set=utf8mb4 -e "source db/seed_notification_demo.sql"
-- 대상 사용자 id 는 환경마다 다르므로 @uid 를 바꿔서 실행한다.

SET @uid = 1;

INSERT INTO tbl_notification (user_id, type, title, content, deep_link_url, is_read, created_at) VALUES
  (@uid, 'GROUP_JUDGMENT',     '판결이 확정됐어요',    '배달 재판 · 내 사건 유죄 4:2 · 목숨 1 차감',  '/group-challenges',        0, NOW() - INTERVAL 10 MINUTE),
  (@uid, 'GROUP_TRIAL_OPENED', '재판이 열렸어요',      '카페비 방어단 · 투표 마감 오늘 22:00',        '/group-challenges',        0, NOW() - INTERVAL 1 HOUR),
  (@uid, 'MISSION_DEADLINE',   '오늘 미션 마감 임박',  '무지출 미션 · 자정까지 · 성공 시 +20점',      '/challenges/personal',     0, NOW() - INTERVAL 3 HOUR),
  (@uid, 'MONTHLY_REPORT',     '판결문이 도착했어요',  '순자산 평결액 +214,000원',                    '/asset/fixed-expenses',    1, NOW() - INTERVAL 1 DAY),
  (@uid, 'PAYMENT_DUE',        '결제 예정 알림',       '넷플릭스 13,500원 · 3일 후 결제',             '/asset/fixed-expenses',    1, NOW() - INTERVAL 1 DAY);
