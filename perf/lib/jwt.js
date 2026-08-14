import crypto from 'k6/crypto';
import encoding from 'k6/encoding';

/*
 * 백엔드(JwtProvider)와 같은 모양의 액세스 토큰을 만든다.
 *
 * 구글 OAuth 는 자동화할 수 없지만 우회할 필요도 없다 —
 * JwtProvider 가 만드는 토큰은 클레임이 sub·iat·exp 뿐이고 HS256 대칭키라
 * 서명키만 있으면 여기서 그대로 재현된다.
 *
 * ⚠ 서명키를 이 파일이나 시나리오 파일에 적지 말 것. 환경변수(-e JWT_SECRET=...)로 받는다.
 */

/*
 * jjwt 의 Keys.hmacShaKeyFor 는 키 길이로 알고리즘을 정한다 —
 * 32~47B=HS256 / 48~63B=HS384 / 64B~=HS512.
 * 우리 쪽에서 알고리즘을 고정해 버리면 팀원이 더 긴 secret 을 쓸 때 401 이 나고,
 * 그 401 은 「부하테스트가 실패했다」로 잘못 읽힌다.
 */
function algorithmFor(secret) {
    const bytes = secret.length; // 시크릿은 ASCII 전제. 한글을 넣으면 이 계산이 틀어진다
    if (bytes >= 64) return { jwt: 'HS512', hmac: 'sha512' };
    if (bytes >= 48) return { jwt: 'HS384', hmac: 'sha384' };
    return { jwt: 'HS256', hmac: 'sha256' };
}

/**
 * @param {number|string} userId 토큰 주체(tbl_user.id)
 * @param {string} secret jwt.secret 과 같은 값
 * @param {number} validitySeconds 만료까지 남길 시간
 * @returns {string} 서명된 JWT
 */
export function createAccessToken(userId, secret, validitySeconds = 3600) {
    if (!secret) {
        throw new Error('JWT_SECRET 이 비어 있다. k6 run -e JWT_SECRET=... 로 넘겨야 한다.');
    }

    const alg = algorithmFor(secret);
    const now = Math.floor(Date.now() / 1000);

    // JWT 는 패딩 없는 base64url 이다. 'rawurl' 이 아니면 서명 검증이 실패한다.
    const header = encoding.b64encode(JSON.stringify({ alg: alg.jwt, typ: 'JWT' }), 'rawurl');
    const payload = encoding.b64encode(
        JSON.stringify({ sub: String(userId), iat: now, exp: now + validitySeconds }),
        'rawurl',
    );

    const signature = crypto.hmac(alg.hmac, secret, `${header}.${payload}`, 'base64rawurl');
    return `${header}.${payload}.${signature}`;
}
