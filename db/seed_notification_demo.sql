-- 시연용 알림 데이터. 팀 공용 seed.sql 과 분리한다 (DECISIONS.md 2026-08-06 (3)).
-- 실행: mysql -u tangtang -p tangtang --default-character-set=utf8mb4 -e "source db/seed_notification_demo.sql"
--       DataGrip·Workbench 에서 파일을 열어 전체 실행해도 된다.
--
-- 대상 사용자는 **가장 최근에 가입한 ACTIVE 사용자**로 자동 지정된다.
-- 특정 사용자에게 넣으려면 아래 두 INSERT 의 WHERE 절을 바꾼다:
--   WHERE status = 'ACTIVE' ORDER BY id DESC LIMIT 1   →   WHERE email = 'someone@example.com'
--
-- ⚠ 세션 변수(@uid)를 쓰지 않는다. DataGrip 등 일부 클라이언트는 문장마다 세션을 새로 잡아
--    SET 한 값이 다음 문장까지 넘어가지 않는다. 그러면 user_id 가 NULL 이 되어
--    fk_noti_user 제약에 걸리고, 원인을 찾기 어렵다 (2026-08-06 실제 발생).
--    사용자 조회를 INSERT 안에 넣어 한 문장으로 끝낸다.
--
-- ⚠ id 를 하드코딩하지 않는다. 구글 로그인으로 가입하면 id 가 1 인 경우가 오히려 드물다.
--
-- ⚠ deep_link_url 은 반드시 apps/web/src/router 에 **실제로 있는 경로**여야 한다.
--    라우터에 catch-all 이 없어서 없는 경로를 넣으면 시연 중 빈 화면이 된다.
--    개인 미션은 /challenges/personal 이 아니라 /mission/personal 이다
--    (router/personalMissionChallengeRoutes.js).

INSERT INTO tbl_notification (user_id, type, title, content, deep_link_url, is_read, created_at)
SELECT target.id, item.type, item.title, item.content, item.deep_link_url, item.is_read, item.created_at
FROM (SELECT id FROM tbl_user WHERE status = 'ACTIVE' ORDER BY id DESC LIMIT 1) AS target
CROSS JOIN (
    SELECT 'GROUP_JUDGMENT'     AS type, '판결이 확정됐어요'   AS title,
           '배달 재판 · 내 사건 유죄 4:2 · 목숨 1 차감'         AS content,
           '/group-challenges'                                AS deep_link_url,
           0 AS is_read, NOW() - INTERVAL 10 MINUTE           AS created_at
    UNION ALL SELECT 'GROUP_TRIAL_OPENED', '재판이 열렸어요',
           '카페비 방어단 · 투표 마감 오늘 22:00',
           '/group-challenges',     0, NOW() - INTERVAL 1 HOUR
    UNION ALL SELECT 'MISSION_DEADLINE',   '오늘 미션 마감 임박',
           '무지출 미션 · 자정까지 · 성공 시 +20점',
           '/mission/personal',     0, NOW() - INTERVAL 3 HOUR
    UNION ALL SELECT 'MONTHLY_REPORT',     '판결문이 도착했어요',
           '순자산 평결액 +214,000원',
           '/asset/fixed-expenses', 1, NOW() - INTERVAL 1 DAY
    UNION ALL SELECT 'PAYMENT_DUE',        '결제 예정 알림',
           '넷플릭스 13,500원 · 3일 후 결제',
           '/asset/fixed-expenses', 1, NOW() - INTERVAL 1 DAY
) AS item;

-- 결과 확인. 0행이면 ACTIVE 사용자가 없다는 뜻이므로 먼저 로그인해 계정을 만든다.
SELECT u.id AS 대상_id, u.nickname AS 닉네임, u.email AS 이메일, COUNT(n.id) AS 알림수
FROM tbl_user u
LEFT JOIN tbl_notification n ON n.user_id = u.id
WHERE u.status = 'ACTIVE'
GROUP BY u.id, u.nickname, u.email
ORDER BY u.id DESC;
