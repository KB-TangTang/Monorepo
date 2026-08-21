import test from 'node:test';
import assert from 'node:assert/strict';
import { existsSync, readFileSync } from 'node:fs';

function source(path) {
    return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

test('순 절감액 단독 화면은 소스까지 지워져 어떤 경로로도 살아나지 않는다', () => {
    const routerSource = source('src/router/index.js');
    const netSavingsView = new URL(
        '../src/views/challenge/report/ChallengeNetSavingsView.vue',
        import.meta.url,
    );

    /* 「바뀐 내 소비습관」은 챌린지 리포트 안으로 합쳐졌다 - 2026-08-21. #101 에서 진입 함수
       openNetSavings() 를 지우면서 라우트 등록만 남아, URL 을 직접 치면 전월을 기본값으로 조회해
       「확정된 챌린지 리포트를 찾을 수 없습니다」 에러 화면이 뜨는 상태였다. 중복 화면이라
       되살릴 이유가 없으므로 소스까지 지운다. */
    assert.ok(!existsSync(netSavingsView), '단독 화면 소스가 남아 있으면 안 된다');
    assert.ok(!routerSource.includes("path: '/reports/challenge/savings'"));
    assert.ok(!routerSource.includes("name: 'challengeNetSavings'"));
    assert.ok(!routerSource.includes('ChallengeNetSavingsView'));
});

test('순 절감액과 카테고리별 효과는 챌린지 리포트 화면 안에서 보여준다', () => {
    const dropdown = source('src/components/challenge/report/ChallengeConsumptionHabitDropdown.vue');
    const content = source('src/components/challenge/report/ChallengeReportContent.vue');

    /* 위 테스트가 지운 화면의 내용이 여기로 옮겨왔다는 사실을 못박는다. 이 두 검사가 깨지면
       단독 화면을 지운 근거가 사라진 것이므로, 지우기 전에 진입 경로부터 다시 만들어야 한다. */
    assert.ok(dropdown.includes('이번 달 순 절감액'), '순 절감액 요약이 드롭다운에 있어야 한다');
    assert.ok(dropdown.includes('카테고리별 효과'), '카테고리별 효과가 드롭다운에 있어야 한다');
    assert.ok(
        content.includes('<ChallengeConsumptionHabitDropdown'),
        '챌린지 리포트 본문이 드롭다운을 렌더링해야 한다',
    );
});
