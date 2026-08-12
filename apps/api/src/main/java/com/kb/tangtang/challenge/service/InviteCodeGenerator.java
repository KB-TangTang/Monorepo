package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.mapper.ChallengeGroupMapper;
import com.kb.tangtang.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 초대 코드 채번.
 *
 * 코드는 사용자가 카카오톡 등으로 옮겨 적는 값이라 <b>혼동되는 글자를 알파벳에서 뺐다</b>
 * (0/O, 1/I/L). 대문자만 쓰므로 입력 시 대소문자를 가리지 않아도 된다.
 *
 * {@code uk_cg_invite} 가 유일성을 보장하지만, INSERT 가 깨지면 사용자에게 그대로 500 이
 * 나가므로 먼저 조회해 겹치지 않는 값을 고른다. 조회와 INSERT 사이의 경합은 남아 있으나
 * 6자리 · 32글자 알파벳이면 발생 확률이 무시할 수준이다.
 */
@Component
public class InviteCodeGenerator {

    private static final String ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 6;
    private static final int MAX_ATTEMPTS = 10;

    private final ChallengeGroupMapper challengeGroupMapper;
    private final SecureRandom random = new SecureRandom();

    public InviteCodeGenerator(ChallengeGroupMapper challengeGroupMapper) {
        this.challengeGroupMapper = challengeGroupMapper;
    }

    public String generate() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = randomCode();
            if (challengeGroupMapper.countByInviteCode(candidate) == 0) {
                return candidate;
            }
        }
        throw new BusinessException("GROUP_INVITE_CODE_EXHAUSTED",
                "초대 코드를 만들지 못했습니다. 잠시 후 다시 시도해 주세요.");
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
