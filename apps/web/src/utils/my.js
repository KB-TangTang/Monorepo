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

/**
 * 아바타 배경색 팔레트.
 *
 * 그룹 fixture(fixtures/groupChallengeDetail.js)가 쓰던 값을 그대로 옮겼다 —
 * 시연 화면 색이 지금과 달라지지 않게 하려는 것이다.
 * ⚠ 이 파일은 node --test 가 별칭 없이 읽으므로 tokens.css 를 참조할 수 없다.
 *   컴포넌트 안의 색이 아니라 **데이터로 계산하는 값**이라 예외로 둔다.
 */
const AVATAR_COLORS = ['#232842', '#3E63D6', '#2E9E6B', '#E7A70C', '#E0664B', '#8B5CF6'];

/**
 * 이름에서 아바타 색을 정한다. **같은 이름은 항상 같은 색**이라야
 * 멤버 목록·랭킹·재판 화면에서 같은 사람이 같은 색으로 보인다.
 *
 * 이름이 비어도 색을 돌려준다 — 투명한 원이 되면 카드가 깨져 보인다.
 */
export function avatarColor(name) {
    const text = typeof name === 'string' ? name.trim() : '';
    let hash = 0;
    for (let i = 0; i < text.length; i += 1) {
        hash = (hash * 31 + text.charCodeAt(i)) % 100000;
    }
    return AVATAR_COLORS[hash % AVATAR_COLORS.length];
}
