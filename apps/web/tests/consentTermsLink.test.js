import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

/*
 * 가입한 뒤 약관 전문을 읽을 수 있는 경로를 지킨다.
 *
 * 가입 동의 화면(ServiceConsentView)·금융 동의 화면(FinancialConsentView)은
 * 라우터 가드가 통과시킨 뒤로는 다시 들어갈 수 없다. 그래서 **가입 후 약관 전문에 닿는 길은
 * 마이페이지 > 동의 관리(ConsentManageView) 하나뿐이다.**
 *
 * 이 화면은 한동안 토글만 그리고 termsUrl 을 버렸다. 서버는 GET /api/consents/me 로
 * 계속 내려주고 있었는데(ConsentService → ConsentCatalog) 화면이 안 쓴 것이고,
 * 심지어 시트 문구는 "약관 전문은 동의 화면에서 확인할 수 있어요" 라며 갈 수 없는 곳을 가리켰다.
 * 렌더링 하네스가 없으므로 소스를 직접 대조한다.
 */

function source(path) {
    return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

/** 약관 링크를 반드시 그려야 하는 화면 → [설명, 파일, 링크에 쓰는 URL 표현] */
const NEEDS_TERMS_LINK = [
    ['동의 관리', 'src/views/my/ConsentManageView.vue', 'item.termsUrl'],
    ['금융 동의', 'src/views/consent/FinancialConsentView.vue', 'item.termsUrl'],
    ['동의 항목 행', 'src/components/consent/ConsentCheckRow.vue', 'termsUrl'],
];

for (const [name, path, urlExpr] of NEEDS_TERMS_LINK) {
    test(`${name} 화면은 termsUrl 을 링크로 그린다`, () => {
        const src = source(path);
        assert.ok(
            src.includes(`:href="${urlExpr}"`),
            `${path} 가 termsUrl 을 href 로 쓰지 않는다 — 약관 전문에 닿을 수 없다`,
        );
    });

    test(`${name} 화면의 약관 링크는 새 탭으로 안전하게 연다`, () => {
        const src = source(path);
        // 새 탭이라야 이 화면의 토글·체크 상태가 유지된다.
        assert.ok(src.includes('target="_blank"'), `${path} 에 target="_blank" 가 없다`);
        // 노션은 외부 도메인이다. opener 를 넘기면 탭내빙(tabnabbing) 통로가 된다.
        assert.ok(
            src.includes('rel="noopener noreferrer"'),
            `${path} 에 rel="noopener noreferrer" 가 없다`,
        );
    });
}

test('동의 관리 화면은 termsUrl 이 없는 항목(CHALLENGE)에 링크를 그리지 않는다', () => {
    const src = source('src/views/my/ConsentManageView.vue');
    // CHALLENGE 만 노션 페이지가 없어 서버가 termsUrl:null 을 내려준다(ConsentCatalog 미등록).
    // v-if 가 빠지면 href 가 undefined 인 링크가 뜬다.
    assert.match(
        src,
        /v-if="item\.termsUrl"[\s\S]{0,200}:href="item\.termsUrl"/,
        'termsUrl 링크에 v-if 가드가 없다',
    );
});

test('동의 관리 화면은 갈 수 없는 "동의 화면"으로 안내하지 않는다', () => {
    const src = source('src/views/my/ConsentManageView.vue');
    assert.ok(
        !src.includes('동의 화면에서 확인'),
        '가입 후에는 동의 화면에 다시 들어갈 수 없다 — 이 화면 안의 전문 보기를 가리켜야 한다',
    );
    assert.ok(src.includes('전문 보기'), '전문 보기 링크 문구가 없다');
});
