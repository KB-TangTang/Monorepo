package com.kb.tangtang.account.service;

import com.kb.tangtang.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 계좌번호 취급 규칙. **원본 계좌번호를 서버에 남기지 않는다.**
 *
 * 두 값을 만든다.
 *   · 해시   → account_no_encrypted. HMAC-SHA256(결정적·단방향). 중복 연결 검사 전용
 *   · 마스킹 → account_no_masked.    화면 표시 전용
 *
 * 왜 복호화 가능한 암호화가 아닌가:
 *   UNIQUE KEY (user_id, account_no_encrypted) 가 중복을 막는 열쇠라 **같은 입력에 항상 같은 결과**여야 한다.
 *   랜덤 IV 를 쓰는 AES-GCM 은 매번 달라져 중복 검사가 무력화되고, 그렇다고 결정적 모드를 쓰면
 *   보안이 한 단계 약해진다. 우리는 원본을 되돌릴 일이 없다(화면에는 마스킹 값만 쓴다).
 *   그래서 단방향 해시가 요구사항에 정확히 맞는다. 복호화 키를 관리할 필요도 사라진다.
 *
 * ⚠ 실 CODEF 는 계좌번호를 마스킹해 주지 않는다(2026-08-05 실계좌 확인).
 *   마스킹 책임은 전적으로 우리에게 있다.
 */
@Component
public class AccountNumberPolicy {

    /**
     * 해시 키. 없으면 기동을 막지 않고 JWT 시크릿을 재사용한다 —
     * 팀원 개인 설정 파일에 키를 하나 더 요구하면 세팅이 또 깨진다(2026-08-03 사고 참고).
     */
    private final byte[] key;

    public AccountNumberPolicy(@Value("${account.hash-secret:${jwt.secret}}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "account.hash-secret 또는 jwt.secret 이 필요합니다. application-local.properties 를 확인하세요.");
        }
        this.key = secret.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 중복 검사용 해시. 같은 기관의 같은 계좌번호는 언제나 같은 값이 된다.
     *
     * **기관코드를 함께 넣는 이유가 있다.** 목서버는 이미 마스킹된 번호(`110-***-****23`)만 주고
     * 우리는 그 문자열 위에 해시를 만든다 — 유일성이 사실상 "앞 3자리 + 뒤 2자리" 로 줄어든다.
     * 기관코드가 빠지면 **서로 다른 은행의 계좌가 같은 해시를 갖는 일**이 생기고,
     * `reactivate` 가 `user_id + account_no_encrypted` 로 매칭하므로 다른 은행 행을 덮어쓴다
     * (bank_code 는 SET 대상이 아니라 `bank_code=0004` 인데 `bank_name=신한은행` 인 행이 남는다).
     */
    public String hash(String bankCode, String accountNo) {
        String normalized = normalize(accountNo);
        if (normalized.isEmpty()) {
            throw new BusinessException("EXTERNAL_API_ERROR", "계좌번호를 확인할 수 없어요.");
        }
        /* 구분자를 둬 ("0004","1234") 와 ("00041","234") 가 같은 입력이 되지 않게 한다. */
        String material = (bankCode == null ? "" : bankCode) + "|" + normalized;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(material.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("계좌번호 해시를 만들지 못했습니다.", e);
        }
    }

    /**
     * 표시용 마스킹. 참고화면의 `110-***-****23` 형식을 따른다 —
     * 앞 3자리와 뒤 2자리만 남기고 가운데를 가린다.
     *
     * 이미 마스킹된 값(목서버 응답)이 들어오면 그대로 돌려준다.
     */
    public String mask(String accountNo) {
        if (accountNo == null || accountNo.isBlank()) {
            return null;
        }
        if (accountNo.contains("*")) {
            return accountNo;
        }
        String digits = normalize(accountNo);
        if (digits.length() <= 5) {
            return "*".repeat(digits.length());
        }
        String head = digits.substring(0, 3);
        String tail = digits.substring(digits.length() - 2);
        return head + "-***-****" + tail;
    }

    /** 하이픈·공백을 뺀 값. 같은 계좌가 표기만 달라도 같은 해시가 나와야 한다. */
    private static String normalize(String accountNo) {
        return accountNo == null ? "" : accountNo.replaceAll("[^0-9A-Za-z*]", "");
    }
}
