import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const rowSource = readFileSync(
    new URL('../src/components/asset/AssetAccountRow.vue', import.meta.url),
    'utf8',
);

test('자산 상세 행은 이니셜 배지 대신 InstitutionLogo 로 금융기관 아이콘을 매칭한다', () => {
    assert.match(rowSource, /import InstitutionLogo from '@\/components\/account\/InstitutionLogo\.vue'/);
    assert.match(rowSource, /<InstitutionLogo[^>]*:code="institutionCode"/);
    assert.match(rowSource, /<InstitutionLogo[^>]*:short-label="badge"/);
});

test('institutionCode 는 필수가 아니다 — 대출처럼 기관코드가 없는 화면도 그대로 동작해야 한다', () => {
    assert.match(rowSource, /institutionCode:\s*{\s*type:\s*String,\s*default:\s*''\s*}/);
});
