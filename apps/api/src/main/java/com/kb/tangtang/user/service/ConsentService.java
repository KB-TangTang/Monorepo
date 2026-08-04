package com.kb.tangtang.user.service;

import com.kb.tangtang.user.domain.ConsentScope;
import com.kb.tangtang.user.domain.ConsentType;
import com.kb.tangtang.user.mapper.ConsentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 동의 저장·철회·조회. 트랜잭션 경계는 이 클래스에 둔다.
 *
 * terms_version 과 is_required 는 카탈로그 값만 쓴다.
 * 프론트가 보낸 값을 그대로 저장하면 DevTools 로 위조할 수 있고,
 * 동의 이력은 분쟁 시 증거라 위조 가능한 값을 남기면 의미가 없다.
 */
@Service
public class ConsentService {

    private final ConsentMapper consentMapper;
    private final ConsentCatalog catalog;

    public ConsentService(ConsentMapper consentMapper, ConsentCatalog catalog) {
        this.consentMapper = consentMapper;
        this.catalog = catalog;
    }

    /**
     * 가입 필수 동의를 아직 마치지 않았는지.
     *
     * SIGNUP 필수 항목만 본다. THIRD_PARTY(계좌 연동용)를 포함하면
     * 계좌를 아직 연동하지 않은 사용자가 동의 화면을 영원히 벗어나지 못한다.
     */
    @Transactional(readOnly = true)
    public boolean needsConsent(Long userId) {
        List<ConsentType> required = ConsentScope.SIGNUP.requiredTypes();
        if (required.isEmpty()) {
            return false;   // IN () 은 SQL 문법 오류다. 방어적으로 막는다
        }
        List<String> names = required.stream().map(ConsentType::name).toList();
        return consentMapper.countActive(userId, names, LocalDateTime.now()) != required.size();
    }
}
