import test from 'node:test';
import assert from 'node:assert/strict';

/*
 * ⏸ 개발 중 — 「맞춤 미션 개시」 안내는 아직 구현되지 않았다. (이슈 #129)
 *
 * 무슨 일이 있었나
 *   v4 리디자인(커밋 cafafe1, #97)에서 개인챌린지 홈이 상태머신
 *   (consent / no-account / insufficient / verdict / active)으로 전면 교체되면서
 *   `shouldShowPersonalMissionUnlock` 이 사라졌다. 이 테스트만 남아 import 에서 죽었고,
 *   `PersonalMissionUnlockSheet.vue`(「NEW · 맞춤 미션 개시」 시트)도 어디서도 쓰이지 않는
 *   고아 컴포넌트가 됐다.
 *
 * 왜 지우지 않고 skip 인가
 *   화면과 판단 규칙이 어떤 모습이어야 하는지가 이 파일에 남아 있다. 지우면 되살릴 때
 *   조건 4개(hasAgreed · hasEnoughData · wasDataInsufficient · hasSeenDataUnlock)를
 *   다시 추론해야 한다. 반대로 실패하는 채로 두면 `npm test` 가 영구적으로 빨간불이라
 *   **새로 생긴 진짜 실패를 아무도 알아채지 못한다.**
 *
 * 왜 급하지 않은가
 *   미션 개시 조건이 「최근 28일 + 거래 50건」인데 계좌 연동 시 과거 내역을 함께 받아오므로
 *   대부분 연동 직후 바로 충족된다. `insufficient` 를 거치는 사용자 자체가 드물고,
 *   「부족했다가 채워지는 순간」은 더 드물다.
 *
 * 되살릴 때 필요한 것 (구현이 1줄로 안 끝나는 이유)
 *   `wasDataInsufficient`(이전에 부족했는지) · `hasSeenDataUnlock`(안내를 봤는지)을
 *   **어딘가에 저장**해야 한다. localStorage 는 쓰지 않기로 했으므로(이슈 #128, 기기를 바꾸면
 *   다시 뜬다) `tbl_user` 컬럼이 하나 더 필요하다. 즉 스키마 변경이 따라온다.
 */

test(
    '소비 데이터가 부족했던 동의 사용자에게 데이터 충족 후 맞춤 미션 개시를 보여준다',
    { skip: '개발 중 — 이슈 #129' },
    () => {
        assert.fail('구현되면 shouldShowPersonalMissionUnlock 을 import 해 되살린다');
    },
);

test(
    '신규 충분 데이터 사용자나 이미 확인한 사용자에게는 맞춤 미션 개시를 다시 보여주지 않는다',
    { skip: '개발 중 — 이슈 #129' },
    () => {
        assert.fail('구현되면 shouldShowPersonalMissionUnlock 을 import 해 되살린다');
    },
);
