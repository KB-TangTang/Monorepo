import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync, readdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join, relative } from 'node:path';

/*
 * 이슈 #326 - UI 문구 표기 규칙.
 *
 * em dash 는 이번이 두 번째다. 8/19 에 판결 카드의 「님 재판 — 유죄」를 고쳤는데
 * 튜토리얼에 같은 패턴이 남아 있었다. 사람 눈으로 훑어 잡는 방식은 또 놓친다.
 * 그래서 개별 문구를 단언하지 않고 **소스 전체를 훑는 스캐너**로 둔다.
 */

const SRC = fileURLToPath(new URL('../src/', import.meta.url));

function walk(dir, extensions) {
    const found = [];
    for (const entry of readdirSync(dir, { withFileTypes: true })) {
        const full = join(dir, entry.name);
        if (entry.isDirectory()) {
            found.push(...walk(full, extensions));
        } else if (extensions.some((extension) => entry.name.endsWith(extension))) {
            found.push(full);
        }
    }
    return found;
}

/** `<template>` 안에서 HTML 주석을 걷어낸 부분만 돌려준다. 주석의 em dash 는 문제가 아니다. */
function templateBlocks(source) {
    return [...source.matchAll(/<template>([\s\S]*?)<\/template>/g)].map((match) =>
        match[1].replace(/<!--[\s\S]*?-->/g, ''),
    );
}

/** 주석을 걷어낸 JS 에서 문자열 리터럴만 돌려준다. */
function stringLiterals(source) {
    const withoutComments = source.replace(/\/\*[\s\S]*?\*\//g, '').replace(/^\s*\/\/.*$/gm, '');
    return [...withoutComments.matchAll(/['"`][^'"`\n]*['"`]/g)].map((match) => match[0]);
}

/*
 * em dash 는 「AI 가 쓴 티가 난다」는 지적을 받는다. UI 문구에도 쓰지 않는다.
 * 구분자가 필요하면 앱이 이미 쓰는 가운뎃점(·)을 쓴다.
 */
test('화면에 보이는 문구에 em dash 를 쓰지 않는다', () => {
    const offenders = [];

    for (const file of walk(SRC, ['.vue'])) {
        const source = readFileSync(file, 'utf8');
        for (const block of templateBlocks(source)) {
            for (const line of block.split('\n')) {
                if (line.includes('—')) {
                    offenders.push(`${relative(SRC, file)}  ${line.trim().slice(0, 80)}`);
                }
            }
        }
    }

    assert.deepEqual(
        offenders,
        [],
        `em dash 를 UI 문구에 쓰지 않는다. 구분자는 가운뎃점(·)으로:\n${offenders.join('\n')}`,
    );
});

test('화면에 나가는 JS 문자열에도 em dash 를 쓰지 않는다', () => {
    const offenders = [];

    for (const file of walk(SRC, ['.js'])) {
        for (const literal of stringLiterals(readFileSync(file, 'utf8'))) {
            if (literal.includes('—')) {
                offenders.push(`${relative(SRC, file)}  ${literal.slice(0, 80)}`);
            }
        }
    }

    assert.deepEqual(offenders, [], offenders.join('\n'));
});

/*
 * 기간 구분자는 `~` 다. personalMissionFlow 한 곳만 en dash 를 써서
 * 요주의 대상 카드에 「7/29 – 8/4」로 찍혔다.
 * 나머지 4곳(groupChallenge.js · GroupEndedCard · GroupChallengeDetailView 2곳)은 전부 `~` 다.
 */
test('기간 구분자는 물결(~)로 통일한다', () => {
    const offenders = [];

    for (const file of walk(SRC, ['.js', '.vue'])) {
        for (const literal of stringLiterals(readFileSync(file, 'utf8'))) {
            if (literal.includes('–')) {
                offenders.push(`${relative(SRC, file)}  ${literal.slice(0, 80)}`);
            }
        }
    }

    assert.deepEqual(offenders, [], `en dash 대신 ~ 를 쓴다:\n${offenders.join('\n')}`);
});
