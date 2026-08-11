import { useAuthStore } from '@/stores/auth';
import {
    completeGroupTutorial,
    completeMainTutorial,
    resetGroupTutorial as resetGroupTutorialApi,
    resetMainTutorial,
} from '@/api/tutorial';

/**
 * 튜토리얼(개인=대법원 MC_01_05 · 그룹=지방법원 GC_01_01)을 봤는지 판정하고 기록한다.
 *
 * 판정 근거는 서버가 준 사용자 정보의 `tutorialSeenAt` · `groupTutorialSeenAt` 이다.
 * localStorage 는 쓰지 않는다 — 기기를 바꾸면 튜토리얼이 다시 뜨기 때문에
 * 명시적으로 기각된 방식이다. (DECISIONS.md 2026-08-11 RV-108)
 */

/*
 * 스토어는 모듈 최상단이 아니라 각 함수 안에서 얻는다.
 * 최상단에서 useAuthStore() 를 부르면 main.js 가 Pinia 를 앱에 붙이기 전에 실행돼
 * "no active Pinia" 로 앱 전체가 죽는다.
 */
function authStore() {
    return useAuthStore();
}

/*
 * hasSeen* 은 **동기 함수로 유지한다.**
 * 호출부가 `if (!hasSeenPersonalTutorial())` 처럼 onMounted 안에서 바로 쓰고 있어
 * async 로 바꾸면 세 화면이 모두 깨진다. 게다가 사용자 정보는 앱 부팅 시
 * POST /api/auth/refresh 로 이미 스토어에 들어와 있으므로 추가 요청도 필요 없다.
 *
 * 판정에 `!= null` 을 쓰는 이유: 서버가 아직 안 본 사용자에게 null 을 주지만,
 * 이 필드가 없는 옛 응답·로그아웃 직후에는 undefined 가 된다.
 * `!== null` 로 비교하면 undefined 를 "봤음"으로 잘못 읽어 튜토리얼이 영영 안 뜬다.
 */

/* ── 개인(대법원) ───────────────────── */
export function hasSeenPersonalTutorial() {
    return authStore().user?.tutorialSeenAt != null;
}

export async function markPersonalTutorialSeen() {
    await markSeen(completeMainTutorial);
}

export async function resetPersonalTutorial() {
    await resetSeen(resetMainTutorial);
}

/* ── 그룹(지방법원) ──────────────────── */
export function hasSeenGroupTutorial() {
    return authStore().user?.groupTutorialSeenAt != null;
}

export async function markGroupTutorialSeen() {
    await markSeen(completeGroupTutorial);
}

export async function resetGroupTutorial() {
    await resetSeen(resetGroupTutorialApi);
}

/*
 * 실패를 다루는 방식이 mark* 와 reset* 이 정반대인 이유:
 *
 * - mark* 는 사용자가 튜토리얼을 끝까지 본 직후 배경에서 저장하는 동작이다.
 *   여기서 오류를 던지면 방금 튜토리얼을 다 본 사람에게 아무 의미 없는 오류가 뜬다.
 *   저장이 실패해도 최악의 결과는 "다음에 튜토리얼이 한 번 더 뜬다" 뿐이라
 *   삼키고 console.warn 만 남긴다.
 * - reset* 은 사용자가 방금 「튜토리얼 다시 보기」를 누른 결과다.
 *   조용히 실패하면 화면만 이동하고 튜토리얼은 안 뜨는 상태가 돼 고장으로 보인다.
 *   그래서 예외를 그대로 던져 호출부가 알리게 한다.
 */
async function markSeen(call) {
    try {
        /* 응답이 갱신된 사용자 정보다. 이걸 스토어에 반영해야 다음 hasSeen* 판정이 맞는다. */
        authStore().mergeUser(await call());
    } catch (err) {
        console.warn('[tutorialGuide] 튜토리얼 완료 저장에 실패했습니다.', err);
    }
}

async function resetSeen(call) {
    authStore().mergeUser(await call());
}
