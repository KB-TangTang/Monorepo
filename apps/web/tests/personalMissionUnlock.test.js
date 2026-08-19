import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { shouldShowPersonalMissionUnlock } from '../src/services/personalMissionFlow.js';

/*
 * ✅ 구현됐다 — PR #311(이슈 #129)이 시트를 개인 미션 홈에 붙였다. 아래 머리말은 그 전의 기록이다.
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

test('소비 데이터가 부족했던 동의 사용자에게 데이터 충족 후 맞춤 미션 개시를 보여준다', () => {
    assert.equal(
        shouldShowPersonalMissionUnlock({
            hasAgreed: true,
            hasEnoughData: true,
            wasDataInsufficient: true,
            hasSeenDataUnlock: false,
        }),
        true,
    );
});

test('신규 충분 데이터 사용자나 이미 확인한 사용자에게는 맞춤 미션 개시를 다시 보여주지 않는다', () => {
    assert.equal(
        shouldShowPersonalMissionUnlock({
            hasAgreed: true,
            hasEnoughData: true,
            wasDataInsufficient: false,
            hasSeenDataUnlock: false,
        }),
        false,
    );
    assert.equal(
        shouldShowPersonalMissionUnlock({
            hasAgreed: true,
            hasEnoughData: true,
            wasDataInsufficient: true,
            hasSeenDataUnlock: true,
        }),
        false,
    );
});

/*
 * ── #311 후속 (이슈 #313 · #314) ──────────────────────────────────
 *
 * 렌더링 하네스가 없어 소스를 검사한다. 두 결함 모두 "코드가 그 자리에 있는가" 로 잡히는 종류다.
 */

function homeSource() {
    return readFileSync(
        new URL('../src/views/challenge/personal/PersonalMissionHomeView.vue', import.meta.url),
        'utf8',
    );
}

/*
 * #313 — 데이터 부족 화면이 고정 픽스처를 실사용자에게 보여줬다.
 *
 * hasEnoughData 가 목 프로필(항상 true)을 보던 시절엔 이 화면이 도달 불가능한 분기였는데,
 * #311 이 서버의 relativeEligible 을 보게 되면서 열렸다. 그 순간부터 MOCK_DATA_REQUIREMENTS 의
 * 「19일째」·「12 / 50」이 자기 데이터인 줄 알고 읽히게 됐다.
 */
test('개인 미션 홈은 데이터 요건 목데이터를 화면에 그리지 않는다', () => {
    const src = homeSource();

    assert.doesNotMatch(
        src,
        /store\.dataRequirements/,
        'dataRequirements 는 MOCK_DATA_REQUIREMENTS 고정값이다 - 서버가 주지 않는 진행도를 지어내지 않는다',
    );
    assert.doesNotMatch(src, /calculateDataProgress/, '진행 막대도 같은 목데이터로 계산된다');
});

test('맞춤 사건이 열리는 조건은 값 없이 조건만 보여준다', () => {
    const src = homeSource();

    assert.match(src, /unlockConditions/, '조건 목록이 있어야 한다');
    assert.match(src, /전체 소비 50건 확보/, '조건 문구 자체는 남긴다');
});

/*
 * #314 — 안내 API 하나가 페이지 전체를 지웠다.
 *
 * 진입 경로의 바깥 try 는 실패 시 consentState='ERROR' 로 StateError 를 띄운다.
 * 동의 경로의 catch 는 「동의를 저장하지 못했어요」를 띄운다 - 동의는 이미 저장된 뒤인데도.
 * 그래서 동기화는 자기 자리에서 실패를 삼켜야 한다.
 */
test('맞춤 미션 안내 동기화는 실패를 삼켜 페이지를 지우지 않는다', () => {
    const src = homeSource();

    const helper = src.match(/async function syncMissionUnlockQuietly\(\)\s*\{[\s\S]*?\n\}/);
    assert.ok(helper, 'syncMissionUnlockQuietly 헬퍼가 없다');
    assert.match(helper[0], /try\s*\{/, '헬퍼가 자기 try/catch 를 가져야 한다');
    assert.match(helper[0], /catch/, '실패를 삼키는 catch 가 있어야 한다');
    assert.match(
        helper[0],
        /hasPendingMissionUnlock\.value = false/,
        '실패하면 안내를 띄우지 않는 쪽으로 확정해야 한다',
    );
});

test('안내 동기화를 store 에서 직접 부르는 곳이 남아 있지 않다', () => {
    const src = homeSource();

    const directCalls = src.match(/await store\.syncMissionUnlock\(\)/g) ?? [];
    assert.equal(
        directCalls.length,
        1,
        '헬퍼 안의 한 번만 남아야 한다 - 바깥에서 직접 부르면 그 자리의 catch 가 다시 페이지를 지운다',
    );
});
