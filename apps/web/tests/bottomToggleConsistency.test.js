import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

/*
 * 하단 토글 3종의 생김새를 맞춘다 (이슈 #418).
 *
 * 자산(자산현황·거래내역) · 자료실(월간·재판 보고서) · 재판(대법원·지방법원) 토글은
 * **하단 탭바 바로 위 같은 자리에 번갈아 뜬다.** 탭을 옮길 때마다 모양이 달라지면
 * 같은 종류의 컨트롤로 안 읽힌다.
 *
 * 팀 결정(2026-08-21): 항목은 전부 「아이콘 + 글자」로 하고, 선택 표시 배지는 쓰지 않는다.
 * 배지는 자산·자료실에만 있었고 재판에는 없어서 갈라져 있었다 — 붙이는 대신 걷어냈다.
 *
 * 세 컴포넌트가 각자 CSS 를 들고 있어(공용 컴포넌트가 아니다) 한쪽만 고치면 다시 갈라진다.
 * 렌더링 하네스가 없으므로 세 파일을 직접 대조한다.
 *
 * 📌 토글 CSS 가 세 곳에 복제돼 있다. 발표 뒤 공용 컴포넌트로 올릴 후보다(3의 법칙).
 */

function source(path) {
    return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

/** 토글 이름 → [마크업 파일, 스타일 파일, 최상위 클래스] */
const TOGGLES = [
    [
        '자산',
        'src/components/asset/AssetLedgerToggle.vue',
        'src/components/asset/AssetLedgerToggle.vue',
        'asset-ledger-toggle',
    ],
    [
        '자료실',
        'src/components/challenge/report/ChallengeReportToggle.vue',
        'src/components/challenge/report/ChallengeReportToggle.vue',
        'report-toggle',
    ],
    [
        '재판',
        'src/components/challenge/ChallengeModeTabBar.vue',
        'src/components/challenge/ChallengeModeTabBar.css',
        'challenge-mode',
    ],
];

for (const [label, markupPath, stylePath, rootClass] of TOGGLES) {
    test(`${label} 토글은 항목마다 아이콘과 글자를 함께 그린다`, () => {
        const markup = source(markupPath);
        // 항목이 둘이므로 아이콘도 글자도 둘이다
        assert.equal(
            (markup.match(/<svg viewBox="0 0 24 24" aria-hidden="true">/g) ?? []).length,
            2,
            '두 항목 모두 아이콘을 갖고 있어야 한다',
        );
        assert.equal(
            (markup.match(/<span>/g) ?? []).length,
            2,
            '아이콘 옆 글자를 span 으로 감싼다 — gap 이 아이콘과 글자 사이에만 걸린다',
        );
    });

    test(`${label} 토글 아이콘은 같은 규격을 쓴다`, () => {
        const style = source(stylePath);
        const icon = style.slice(style.indexOf(`.${rootClass} svg`));
        assert.ok(icon, `${rootClass} svg 규칙을 찾지 못했다`);
        assert.match(icon, /width:\s*18px/);
        assert.match(icon, /height:\s*18px/);
        assert.match(icon, /fill:\s*none/);
        assert.match(icon, /stroke:\s*currentColor/);
        assert.match(icon, /stroke-width:\s*1\.9/);
    });

    test(`${label} 토글에 선택 배지가 남아 있지 않다`, () => {
        /*
         * 자산·자료실에만 있던 배지를 걷어낸 상태를 고정한다.
         * 한 곳에 다시 들어오면 셋의 모양이 또 갈라진다.
         */
        assert.doesNotMatch(source(markupPath), /<i v-if=/, '선택 배지 마크업이 남아 있다');
        assert.doesNotMatch(
            source(stylePath),
            /--active i\b|__active i\b/,
            '배지 CSS 가 남아 있다',
        );
    });
}

test('재판 토글 글자가 세로로 접히지 않는다', () => {
    /*
     * 아이콘이 들어오면서 항목 폭이 늘었다. nowrap 이 없으면 좁은 화면에서
     * 「대법원」이 한 글자씩 세로로 쌓인다 — 실제로 한 번 그렇게 렌더됐다.
     */
    assert.match(
        source('src/components/challenge/ChallengeModeTabBar.css'),
        /white-space:\s*nowrap/,
    );
});
