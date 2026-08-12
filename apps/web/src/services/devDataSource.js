/*
 * 그룹 챌린지 화면의 데이터 출처 토글 (개발 전용).
 *
 * 그룹 챌린지는 화면이 먼저 목데이터로 만들어졌고 API 는 뒤따라 붙는 중이다.
 * 그래서 한 화면 안에 「이미 서버에서 오는 것」과 「아직 목데이터인 것」이 섞여 있다.
 * 어느 쪽을 보고 있는지 눈으로 구분할 수 없으면 연동 진척을 확인할 수가 없어서
 * 출처를 명시적으로 바꾸는 스위치를 둔다.
 *
 * - `mock`: 전부 목데이터. 팀 시연·디자인 확인용 (기본값)
 * - `live`: 실제 API 만. 아직 서버에 없는 화면은 그대로 실패한다 — 그게 이 모드의 목적이다.
 *
 * **프로덕션 빌드에서는 토글이 존재하지 않고 항상 `live` 다.**
 * `import.meta.env.DEV` 가 false 면 저장값을 읽지도 쓰지도 않는다.
 */
import { ref, computed } from 'vue';

const STORAGE_KEY = 'tt.dev.dataSource';
const IS_DEV = import.meta.env.DEV;

function readStored() {
    if (!IS_DEV) return 'live';
    return localStorage.getItem(STORAGE_KEY) === 'live' ? 'live' : 'mock';
}

/** 'mock' | 'live'. 화면이 이 값을 watch 해서 다시 불러온다. */
export const dataSource = ref(readStored());

export const isMockMode = computed(() => dataSource.value === 'mock');

export const isDevToggleAvailable = IS_DEV;

export function toggleDataSource() {
    if (!IS_DEV) return;
    dataSource.value = dataSource.value === 'mock' ? 'live' : 'mock';
    localStorage.setItem(STORAGE_KEY, dataSource.value);
}

/**
 * 아직 서버에 없는 API 를 `live` 모드에서 부를 때 던진다.
 * 조용히 목데이터로 되돌아가면 연동이 끝난 것처럼 보여서 일부러 실패시킨다.
 */
export function notImplementedYet(what) {
    const error = new Error(`${what} 은(는) 아직 서버에 없습니다. 목데이터 모드로 보세요.`);
    error.code = 'NOT_IMPLEMENTED';
    return error;
}
