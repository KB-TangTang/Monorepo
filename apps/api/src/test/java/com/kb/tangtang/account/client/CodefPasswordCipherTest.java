package com.kb.tangtang.account.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 비밀번호 RSA 암호화 검증.
 *
 * CODEF 서버 없이도 확인할 수 있다 — 테스트에서 키쌍을 만들어 **우리가 암호화한 값을
 * 개인키로 복호화**해 원문과 같은지 본다. 규격(RSA/ECB/PKCS1Padding + Base64)이 맞으면 통과한다.
 */
class CodefPasswordCipherTest {

    private static KeyPair keyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static String publicKeyBase64(KeyPair pair) {
        return Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
    }

    private static String decrypt(KeyPair pair, String encrypted) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, pair.getPrivate());
        return new String(cipher.doFinal(Base64.getDecoder().decode(encrypted)), StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("암호화한 값을 개인키로 풀면 원문이 나온다")
    void encryptsSoItCanBeDecrypted() throws Exception {
        KeyPair pair = keyPair();
        CodefPasswordCipher cipher = new CodefPasswordCipher(publicKeyBase64(pair));

        String encrypted = cipher.encrypt("비밀번호1234!");

        assertEquals("비밀번호1234!", decrypt(pair, encrypted));
    }

    @Test
    @DisplayName("암호문에 평문이 그대로 남지 않는다")
    void doesNotLeakPlainText() throws Exception {
        CodefPasswordCipher cipher = new CodefPasswordCipher(publicKeyBase64(keyPair()));

        String encrypted = cipher.encrypt("myPassword");

        assertFalse(encrypted.contains("myPassword"));
        /* Base64 로 인코딩돼 있어야 한다 — 그대로 JSON 에 실린다. */
        assertDoesNotThrow(() -> Base64.getDecoder().decode(encrypted));
    }

    @Test
    @DisplayName("같은 평문도 매번 다른 암호문이 된다 (PKCS1 패딩)")
    void paddingMakesEachCallDifferent() throws Exception {
        CodefPasswordCipher cipher = new CodefPasswordCipher(publicKeyBase64(keyPair()));

        assertNotEquals(cipher.encrypt("same"), cipher.encrypt("same"));
    }

    @Test
    @DisplayName("PEM 헤더가 붙은 공개키도 받아들인다")
    void acceptsPemWrappedKey() throws Exception {
        KeyPair pair = keyPair();
        String pem = "-----BEGIN PUBLIC KEY-----\n" + publicKeyBase64(pair) + "\n-----END PUBLIC KEY-----";

        CodefPasswordCipher cipher = new CodefPasswordCipher(pem);

        assertEquals("hello", decrypt(pair, cipher.encrypt("hello")));
    }

    @Test
    @DisplayName("공개키가 없으면 기동 시점에 막는다")
    void rejectsMissingKey() {
        assertThrows(IllegalStateException.class, () -> new CodefPasswordCipher(""));
        assertThrows(IllegalStateException.class, () -> new CodefPasswordCipher(null));
    }

    @Test
    @DisplayName("줄 끝 주석이 섞여 들어와도 키를 읽는다 (.properties 인라인 주석 사고 방어)")
    void toleratesTrailingComment() throws Exception {
        KeyPair pair = keyPair();
        /* .properties 는 `키=값 # 설명` 을 지원하지 않아 주석까지 값으로 들어온다. */
        String polluted = publicKeyBase64(pair) + "   # CODEF 마이페이지에서 발급";

        CodefPasswordCipher cipher = new CodefPasswordCipher(polluted);

        assertEquals("hello", decrypt(pair, cipher.encrypt("hello")));
    }

    @Test
    @DisplayName("공개키 형식이 잘못되면 원인을 알려주며 막는다")
    void rejectsBrokenKey() {
        IllegalStateException e =
                assertThrows(IllegalStateException.class, () -> new CodefPasswordCipher("이건키가아니다"));
        assertTrue(e.getMessage().contains("공개키"));
        /* 원인을 짚어줘야 설정 파일을 열어볼 수 있다. */
        assertTrue(e.getMessage().contains("public-key"));
    }
}
