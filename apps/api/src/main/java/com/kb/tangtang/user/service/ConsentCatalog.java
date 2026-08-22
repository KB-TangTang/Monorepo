package com.kb.tangtang.user.service;

import com.kb.tangtang.user.domain.ConsentScope;
import com.kb.tangtang.user.domain.ConsentType;
import com.kb.tangtang.user.dto.ConsentItemDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 동의 항목 정의의 단일 출처.
 *
 * 약관 본문은 서버가 갖지 않는다. 노션 공개 페이지 URL 만 들고 있다가 프론트에 내려준다.
 * terms_version 은 이 클래스의 값만 쓴다 — 프론트가 보낸 버전 문자열은 신뢰하지 않는다.
 */
@Component
public class ConsentCatalog {

    private final String termsVersion;
    private final Map<ConsentType, String> termsUrls = new EnumMap<>(ConsentType.class);

    public ConsentCatalog(@Value("${consent.terms-version}") String termsVersion,
                          @Value("${consent.url.terms}") String termsUrl,
                          @Value("${consent.url.privacy}") String privacyUrl,
                          @Value("${consent.url.financial-data}") String financialDataUrl,
                          @Value("${consent.url.third-party}") String thirdPartyUrl,
                          @Value("${consent.url.ai-usage}") String aiUsageUrl,
                          @Value("${consent.url.marketing}") String marketingUrl) {
        this.termsVersion = termsVersion;
        termsUrls.put(ConsentType.TERMS, termsUrl);
        termsUrls.put(ConsentType.PRIVACY, privacyUrl);
        termsUrls.put(ConsentType.FINANCIAL_DATA, financialDataUrl);
        termsUrls.put(ConsentType.THIRD_PARTY, thirdPartyUrl);
        termsUrls.put(ConsentType.AI_USAGE, aiUsageUrl);
        termsUrls.put(ConsentType.MARKETING, marketingUrl);
    }

    public String termsVersion() {
        return termsVersion;
    }

    public String termsUrl(ConsentType type) {
        return termsUrls.get(type);
    }

    public List<ConsentItemDto> items(ConsentScope scope) {
        return scope.types().stream()
                .map(type -> ConsentItemDto.builder()
                        .type(type.name())
                        .required(type.required())
                        .label(type.label())
                        .termsUrl(termsUrl(type))
                        .build())
                .toList();
    }
}
