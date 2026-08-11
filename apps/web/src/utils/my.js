/**
 * 마이페이지 표시용 순수 함수.
 *
 * 프론트 테스트 하네스가 컴포넌트를 못 띄우므로 판정은 여기에 둔다.
 */

/**
 * 아바타에 쓸 한 글자. 영문은 대문자로 올린다.
 * 넘기는 값은 **표시명**(utils/user.js 의 resolveDisplayName)이다 — 이름과 이니셜이
 * 서로 다른 값에서 나오면 카드 안에서 두 글자가 어긋난다.
 */
export function profileInitial(displayName) {
    if (typeof displayName !== 'string' || displayName.trim() === '') {
        return '?';
    }
    return displayName.trim().charAt(0).toUpperCase();
}

/** 'GOOGLE' → 'google'. 참고화면이 소문자 표기다. */
export function providerLabel(socialProvider) {
    if (typeof socialProvider !== 'string' || socialProvider === '') {
        return '';
    }
    return socialProvider.toLowerCase();
}
