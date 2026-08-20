import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

/*
 * 초과 기준 금액 0원(무지출 챌린지) 회귀 방지.
 *
 * 서버는 처음부터 0원을 허용했고(`ChallengeGroupService.validateCreate` 는 음수만 막는다),
 * 목록·상세 카드도 0원을 「무지출」로 표기하고 있었다. 그런데 **만들기 위자드만** 금액을
 * truthy 로 검사해 0을 미입력 취급했다 — 「설정 확인」 버튼이 눌리지 않아 서비스가 지원하는
 * 컨셉을 화면에서 만들 수 없는 상태였다.
 *
 * 렌더링 하네스가 없어(`node:test` + 순수 JS) 소스를 검사한다.
 */
function source(path) {
    return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

const SETTINGS = 'src/components/challenge/group/GroupCreateStepSettings.vue';

test('설정 단계가 금액을 truthy 로 검사하지 않는다 — 0원에서 버튼이 잠기지 않는다', () => {
    const src = source(SETTINGS);
    assert.ok(
        !/props\.limitAmount &&/.test(src),
        'limitAmount 를 truthy 로 검사한다 — 0 이 미입력으로 떨어진다',
    );
    assert.match(
        src,
        /Number\(props\.limitAmount\) >= 0/,
        '0 이상을 허용하는 검사가 없다 — 서버는 음수만 막는다',
    );
});

/* 「비어 있음」과 「0」은 다르다. 이걸 합치면 어느 쪽으로 고쳐도 한쪽이 깨진다. */
test('빈 칸과 0원을 구분한다 — 빈 칸으로는 여전히 넘어가지 못한다', () => {
    const src = source(SETTINGS);
    assert.match(
        src,
        /const isEmpty = computed\(\(\) => props\.limitAmount === null \|\| props\.limitAmount === ''\)/,
        '빈 칸 판정이 따로 없다',
    );
    assert.ok(src.includes('!isEmpty.value'), '검증이 빈 칸을 막지 않는다');
});

/* 입력 처리에서 0을 falsy 로 보면 「0」을 치는 순간 칸이 도로 비워진다. */
test('0을 입력해도 칸이 비워지지 않는다', () => {
    const src = source(SETTINGS);
    assert.match(
        src,
        /const num = raw === '' \? null : Number\(raw\)/,
        '입력값 변환이 0 을 null 로 떨어뜨릴 수 있다',
    );
    assert.match(
        src,
        /e\.target\.value = num === null \? '' : num\.toLocaleString\('ko-KR'\)/,
        '표시값 갱신이 0 을 빈 문자열로 만든다',
    );
});

/* placeholder 가 「0」이면 빈 칸과 실제로 입력한 0원이 화면상 똑같아 보인다. */
test('금액 칸 placeholder 가 0 이 아니다 — 빈 칸과 0원이 구분된다', () => {
    assert.ok(
        !source(SETTINGS).includes('placeholder="0"'),
        'placeholder 가 "0" 이라 미입력과 무지출이 같아 보인다',
    );
});

test('0원을 고르면 무지출이라는 안내가 뜬다', () => {
    const src = source(SETTINGS);
    assert.match(
        src,
        /const isNoSpend = computed\(\(\) => !isEmpty\.value && Number\(props\.limitAmount\) === 0\)/,
        '무지출 판정이 없다',
    );
    assert.ok(src.includes('v-if="isNoSpend"'), '무지출 안내 문구가 템플릿에 없다');
});

/* 만들기 화면만 고치면 최종 확인이 「하루 0원」으로 남아 뜻이 흐려진다. */
test('0원은 어느 화면에서나 「무지출」로 읽힌다', () => {
    const confirm = source('src/components/challenge/group/GroupCreateStepConfirm.vue');
    assert.ok(confirm.includes('무지출'), '최종 확인 화면이 0원을 무지출로 표기하지 않는다');

    const activeCard = source('src/components/challenge/group/GroupActiveCard.vue');
    assert.match(
        activeCard,
        /ch\.limitAmount === 0 \? `\$\{type\} · 무지출`/,
        '진행 중 카드가 0원일 때 금액 칸을 통째로 비운다 — 시작 전 카드와 어긋난다',
    );

    const preStart = source('src/components/challenge/group/GroupPreStartCard.vue');
    assert.ok(preStart.includes('무지출'), '시작 전 카드의 무지출 표기가 사라졌다');
});
