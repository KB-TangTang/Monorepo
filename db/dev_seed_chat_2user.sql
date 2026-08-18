-- 그룹 채팅 2인 테스트용 데이터 (이슈 #174)
--
-- 로컬 DB 와 배포 DB 어디서든 같은 스크립트를 쓴다.
--
-- ⚠ 실행 전 조건: 두 계정이 각각 그 환경에서 **한 번씩 로그인**해 tbl_user 에 행이 있어야 한다.
--   (로컬 테스트면 로컬 DB 에, 배포 테스트면 배포 DB 에 각각 필요하다)
--
-- 만들어지는 것: 오늘이 3일차이고 D-4 인 진행 중(ACTIVE) 그룹 챌린지 + 참여자 2명.
--   기간을 이렇게 잡은 이유는 DB 제약(ck_cg_period: 종료일 <= 시작일 + 6일)을 지키면서
--   헤더의 "N일차 · D-N" 표기를 둘 다 보이게 하려는 것이다.
--
-- 채팅방 자체는 미리 만들지 않아도 된다. 앱이 처음 입장할 때 참여자 캐시를 DB 에서 읽어
-- Redis 를 데운다(ChatRoomAccessService.memberIdsOf 폴백).

-- ── 여기 두 줄만 채운다 ────────────────────────────────
SET @email_a = 'CHANGE_ME_계정1@example.com';   -- 방장이 될 계정
SET @email_b = 'CHANGE_ME_계정2@example.com';
-- ──────────────────────────────────────────────────────

SET @group_name = '채팅 2인 테스트';
SET @invite_code = 'CHATT';   -- 5자리. 이미 있으면 uk_cg_invite 로 실패하니 다른 값으로 바꾼다

-- 1) 두 계정이 실제로 있는지 먼저 확인한다. 두 줄이 나와야 한다.
SELECT id, email, nickname, status FROM tbl_user WHERE email IN (@email_a, @email_b);

-- 2) 그룹 생성 (방장 = 계정1)
INSERT INTO tbl_challenge_group
    (admin_id, group_name, category_id, limit_amount, eval_type,
     max_members, start_date, end_date, invite_code, status, memo)
SELECT u.id, @group_name, NULL, 30000, 'DAILY',
       6, DATE_SUB(CURDATE(), INTERVAL 2 DAY), DATE_ADD(CURDATE(), INTERVAL 4 DAY),
       @invite_code, 'ACTIVE', '그룹 채팅 테스트용'
FROM tbl_user u
WHERE u.email = @email_a;

SET @group_id = LAST_INSERT_ID();

-- 3) 참여자 2명. lives_count 는 일일평가라 기간 일수(7)를 준다
INSERT INTO tbl_group_member (group_id, user_id, lives_count)
SELECT @group_id, u.id, 7
FROM tbl_user u
WHERE u.email IN (@email_a, @email_b);

-- 4) 확인 — 그룹 1건 + 참여자 2건이 나와야 한다
SELECT @group_id AS group_id;

SELECT g.id, g.group_name, g.status, g.start_date, g.end_date, g.invite_code,
       DATEDIFF(CURDATE(), g.start_date) + 1 AS day_index,
       DATEDIFF(g.end_date, CURDATE())        AS days_left
FROM tbl_challenge_group g
WHERE g.id = @group_id;

SELECT m.user_id, u.email, u.nickname
FROM tbl_group_member m
JOIN tbl_user u ON u.id = m.user_id
WHERE m.group_id = @group_id;

-- 접속 주소: /group-challenges/{group_id}/chat


-- ══════════════════════════════════════════════════════
-- 정리 (테스트가 끝난 뒤)
-- ══════════════════════════════════════════════════════
-- 그룹을 지우면 tbl_group_member 는 ON DELETE CASCADE 로 함께 지워진다.
--
--   DELETE FROM tbl_challenge_group WHERE invite_code = 'CHATT';
--
-- Redis 의 대화 내용은 종료일 + 2일이 지나면 스스로 사라진다. 즉시 지우려면(로컬 기준):
--
--   docker exec tangtang-redis redis-cli --scan --pattern 'chat:*:{group_id}' | \
--     xargs -r docker exec -i tangtang-redis redis-cli DEL
