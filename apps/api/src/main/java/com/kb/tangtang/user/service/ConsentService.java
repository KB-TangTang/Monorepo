package com.kb.tangtang.user.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.user.domain.ConsentScope;
import com.kb.tangtang.user.domain.ConsentType;
import com.kb.tangtang.user.dto.ConsentAgreementDto;
import com.kb.tangtang.user.dto.ConsentRecordDto;
import com.kb.tangtang.user.mapper.ConsentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * scope 단위 묶음 저장. 부분 저장이 생기지 않도록 한 트랜잭션으로 처리한다.
     *
     * 미동의 간주 범위는 요청 scope 의 항목 집합으로 한정한다.
     * 이 경계를 놓치면 계좌 연동(FINANCIAL scope) 한 번에
     * 가입 때 켠 마케팅 동의(SIGNUP scope)가 풀린다.
     *
     * @return 저장 후 needsConsent
     */
    @Transactional
    public boolean submit(Long userId, ConsentScope scope, List<ConsentAgreementDto> agreements) {
        List<ConsentType> scopeTypes = scope.types();
        Map<String, Boolean> requested = new HashMap<>();

        for (ConsentAgreementDto agreement : agreements) {
            ConsentType type = parseType(agreement.getType());
            if (!scopeTypes.contains(type)) {
                throw new BusinessException("CONSENT_TYPE_INVALID",
                        "이 동의 절차에 속하지 않는 항목입니다: " + agreement.getType());
            }
            requested.put(type.name(), agreement.isAgreed());
        }

        for (ConsentType type : scope.requiredTypes()) {
            if (!Boolean.TRUE.equals(requested.get(type.name()))) {
                throw new BusinessException("CONSENT_REQUIRED_MISSING",
                        "필수 항목에 동의해야 합니다: " + type.label());
            }
        }

        LocalDateTime now = LocalDateTime.now();
        for (ConsentType type : scopeTypes) {
            boolean agreed = Boolean.TRUE.equals(requested.get(type.name()));
            consentMapper.upsert(ConsentRecordDto.builder()
                    .userId(userId)
                    .consentType(type.name())
                    .required(type.required())          // 카탈로그 값
                    .termsVersion(catalog.termsVersion())   // 카탈로그 값
                    .status(agreed ? 1 : 0)
                    .withdrawnAt(agreed ? null : now)
                    .expiresAt(agreed && type == ConsentType.FINANCIAL_DATA ? now.plusYears(1) : null)
                    .build());
        }

        return needsConsent(userId);
    }

    private ConsentType parseType(String raw) {
        try {
            return ConsentType.valueOf(raw);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new BusinessException("CONSENT_TYPE_INVALID", "알 수 없는 동의 항목입니다: " + raw);
        }
    }
}
