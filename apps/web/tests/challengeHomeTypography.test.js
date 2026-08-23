import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

/*
 * 지방법원(그룹 챌린지) 홈 타이포 회귀 방지 (2026-08-22).
 *
 * 대법원(개인 미션 홈)이 커밋 5141c59 에서 「작은 글씨를 한 단계씩 올린다」로 정리됐고,
 * 지방법원 홈도 같은 규칙으로 맞췄다. 이 프로젝트에는 컴포넌트 렌더링 하네스가 없어
 * (node:test + 순수 JS) themeColor.test.js 처럼 소스를 읽어 규칙만 검사한다.
 *
 * `GroupChallengeHomeView.vue` 는 대상에서 뺀다 — 의도적으로 남긴 예외가 둘 있다.
 *   · `.gc-dev-fab`        DEV 전용 버튼이라 화면 타이포 규칙 밖이다
 *   · `.gc-group-row__badge` 안 읽은 배지. 목록 화면 `GroupActiveCard` 와 같은 규격(10px)이
 *                          이어야 해서 두 파일을 함께 고칠 때만 바꾼다
 */

const CARD_FILES = [
    'src/components/challenge/group/GroupTrialStatusCard.vue',
    'src/components/challenge/group/GroupPeacefulCard.vue',
    'src/components/challenge/group/GroupTodoDoneCard.vue',
    'src/components/challenge/group/GroupTrialTodoGrid.vue',
    'src/components/challenge/group/GroupTrialListSheet.vue',
    /* 재판 기록 진입도 홈에서 열리므로 같은 타이포 규칙을 받는다 */
    'src/components/challenge/group/GroupTrialRecordCard.vue',
    'src/components/challenge/group/GroupHonorCourtEntry.vue',
];

function source(path) {
    return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

test('지방법원 홈 카드는 11px(--tt-fs-overline)을 쓰지 않는다', () => {
    for (const path of CARD_FILES) {
        assert.doesNotMatch(
            source(path),
            /--tt-fs-overline/,
            `${path} 에 overline(11px)이 남아 있다. 홈에서 퇴출한 크기다`,
        );
    }
});

test('지방법원 홈 카드의 font-size 는 전부 --tt-fs-* 토큰이다', () => {
    for (const path of CARD_FILES) {
        const sizes = source(path).match(/font-size:\s*[^;]+;/g) ?? [];
        assert.ok(sizes.length > 0, `${path} 에서 font-size 를 하나도 못 찾았다`);
        for (const decl of sizes) {
            assert.match(
                decl,
                /var\(--tt-fs-[a-z-]+\)/,
                `${path} 의 "${decl}" 가 토큰이 아니다. px 하드코딩 금지 (apps/web/AGENTS.md)`,
            );
        }
    }
});
