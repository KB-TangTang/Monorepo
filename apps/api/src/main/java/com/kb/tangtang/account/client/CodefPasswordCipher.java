package com.kb.tangtang.account.client;

import com.kb.tangtang.common.exception.BusinessException;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * CODEF 로 보낼 금융기관 비밀번호를 RSA 로 암호화한다.
 *
 * **CODEF 는 평문 비밀번호를 받지 않는다.** 발급받은 공개키로 암호화한 Base64 문자열만 받는다.
 * 공식 SDK(easycodef)가 대신 해주는 일인데, 우리는 SDK 없이 RestTemplate 으로 붙었으므로
 * 이 계층을 직접 만든다. (2026-08-05 — 이게 빠져 있어 실 CODEF 인증이 실패하는 상태였다)
 *
 * 규격: `RSA/ECB/PKCS1Padding` 으로 암호화한 뒤 Base64 인코딩.
 * 공개키는 헤더(`-----BEGIN...`) 없는 X.509 SubjectPublicKeyInfo Base64 문자열이다.
 *
 * ⚠ 평문 비밀번호는 이 클래스의 인자로만 지나간다. 필드로 보관하지 않고 로그로 남기지 않는다.
 */
public class CodefPasswordCipher {

    private static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding";

    private final PublicKey publicKey;

    /**
     * @param publicKeyBase64 CODEF 마이페이지에서 발급받은 공개키. 비어 있으면 이 구현을 쓸 수 없다.
     */
    public CodefPasswordCipher(String publicKeyBase64) {
        if (publicKeyBase64 == null || publicKeyBase64.isBlank()) {
            throw new IllegalStateException(
                    "financial.codef.public-key 가 없습니다. application-local.properties 를 확인하세요.");
        }
        this.publicKey = parse(publicKeyBase64.trim());
    }

    /** 평문 → RSA 암호화 → Base64. */
    public String encrypt(String plain) {
        if (plain == null || plain.isEmpty()) {
            throw new BusinessException("INVALID_CREDENTIALS", "비밀번호를 입력해 주세요.");
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return Base64.getEncoder()
                    .encodeToString(cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            /* 예외 메시지에 평문이 섞여 나가지 않도록 원인을 감싸서 던진다. */
            throw new IllegalStateException("CODEF 비밀번호 암호화에 실패했습니다.", e);
        }
    }

    /**
     * 공개키 문자열을 키로 만든다.
     *
     * 설정 파일에서 오는 값이라 실수가 섞이기 쉬워 두 가지를 관용적으로 처리한다.
     *   · PEM 헤더(`-----BEGIN...`)
     *   · 줄 끝 주석 — **.properties 는 `키=값 # 설명` 을 지원하지 않아 주석까지 값이 된다.**
     *     실제로 이것 때문에 기동이 실패한 적이 있다(2026-08-05).
     */
    private static PublicKey parse(String base64) {
        String body = base64
                .replaceAll("-----[A-Z ]+-----", "")
                .split("#", 2)[0]
                .replaceAll("\\s", "");
        try {
            byte[] der = Base64.getDecoder().decode(body);
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "CODEF 공개키가 Base64 형식이 아닙니다. application-local.properties 의"
                            + " financial.codef.public-key 를 확인하세요."
                            + " (.properties 는 줄 끝 주석을 지원하지 않습니다 — 설명은 별도 줄에 쓰세요)",
                    e);
        } catch (Exception e) {
            throw new IllegalStateException("CODEF 공개키를 읽지 못했습니다. 값을 확인하세요.", e);
        }
    }
}
