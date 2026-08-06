-- 시연용 알림 데이터. 팀 공용 seed.sql 과 분리한다 (DECISIONS.md 2026-08-06 (3)).
-- 실행: mysql -u tangtang -p tangtang --default-character-set=utf8mb4 -e "source db/seed_notification_demo.sql"
--
-- 대상 사용자는 **가장 최근에 가입한 사용자**로 자동 지정된다.
-- 다른 사용자를 쓰려면 같은 세션에서 @uid 를 먼저 정해 두면 그 값이 유지된다:
--   mysql ... -e "SET @uid = 7; source db/seed_notification_demo.sql"
--
-- ⚠ id 를 하드코딩하지 않는다. 구글 로그인으로 가입하면 id 가 1 이 아닌 경우가 대부분이고,
--    없는 id 를 넣으면 fk_noti_user 제약에 걸려 한 건도 안 들어간다(2026-08-06 실제 발생).
--
-- ⚠ deep_link_url 은 반드시 apps/web/src/router 에 **실제로 있는 경로**여야 한다.
--    라우터에 catch-all 이 없어서 없는 경로를 넣으면 시연 중 빈 화면이 된다.
--    개인 미션은 /challenges/personal 이 아니라 /mission/personal 이다
--    (router/personalMissionChallengeRoutes.js).

SET @uid = IFNULL(@uid, (SELECT id FROM tbl_user WHERE status = 'ACTIVE' ORDER BY id DESC LIMIT 1));

-- 대상이 없으면 여기서 멈춘다. 그냥 두면 FK 오류만 나와 원인을 알기 어렵다.
SELECT IF(@uid IS NULL,
          '중단: tbl_user 에 ACTIVE 사용자가 없습니다. 먼저 로그인해 계정을 만드세요.',
          CONCAT('대상 사용자 id = ', @uid)) AS 안내;

SELECT nickname AS 대상_닉네임, email AS 대상_이메일 FROM tbl_user WHERE id = @uid;

INSERT INTO tbl_notification (user_id, type, title, content, deep_link_url, is_read, created_at) VALUES
  (@uid, 'GROUP_JUDGMENT',     '판결이 확정됐어요',    '배달 재판 · 내 사건 유죄 4:2 · 목숨 1 차감',  '/group-challenges',        0, NOW() - INTERVAL 10 MINUTE),
  (@uid, 'GROUP_TRIAL_OPENED', '재판이 열렸어요',      '카페비 방어단 · 투표 마감 오늘 22:00',        '/group-challenges',        0, NOW() - INTERVAL 1 HOUR),
  (@uid, 'MISSION_DEADLINE',   '오늘 미션 마감 임박',  '무지출 미션 · 자정까지 · 성공 시 +20점',      '/mission/personal',        0, NOW() - INTERVAL 3 HOUR),
  (@uid, 'MONTHLY_REPORT',     '판결문이 도착했어요',  '순자산 평결액 +214,000원',                    '/asset/fixed-expenses',    1, NOW() - INTERVAL 1 DAY),
  (@uid, 'PAYMENT_DUE',        '결제 예정 알림',       '넷플릭스 13,500원 · 3일 후 결제',             '/asset/fixed-expenses',    1, NOW() - INTERVAL 1 DAY);
