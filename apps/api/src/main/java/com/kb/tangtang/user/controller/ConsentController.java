package com.kb.tangtang.user.controller;

import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.user.domain.ConsentScope;
import com.kb.tangtang.user.dto.ConsentCatalogDto;
import com.kb.tangtang.user.dto.ConsentResultDto;
import com.kb.tangtang.user.dto.ConsentSubmitRequestDto;
import com.kb.tangtang.user.dto.MyConsentListDto;
import com.kb.tangtang.user.service.ConsentCatalog;
import com.kb.tangtang.user.service.ConsentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 약관·금융데이터 동의.
 *
 * 사용자 식별은 @LoginUser 로만 한다. 요청으로 userId 를 받으면 남의 데이터에 접근할 수 있다.
 * JwtAuthInterceptor 가 /api/** 를 보호하므로 별도 설정 없이 인증이 걸린다.
 */
@RestController
@RequestMapping("/api/consents")
public class ConsentController {

    private final ConsentService consentService;
    private final ConsentCatalog catalog;

    public ConsentController(ConsentService consentService, ConsentCatalog catalog) {
        this.consentService = consentService;
        this.catalog = catalog;
    }

    @GetMapping("/catalog")
    public ApiResponse<ConsentCatalogDto> catalog(@RequestParam("scope") String scope) {
        ConsentScope parsed = parseScope(scope);
        return ApiResponse.ok(ConsentCatalogDto.builder()
                .scope(parsed.name())
                .termsVersion(catalog.termsVersion())
                .items(catalog.items(parsed))
                .build());
    }

    @PostMapping
    public ApiResponse<ConsentResultDto> submit(@LoginUser Long userId,
                                                @RequestBody ConsentSubmitRequestDto request) {
        boolean needsConsent = consentService.submit(
                userId,
                parseScope(request.getScope()),
                request.getAgreements() == null ? List.of() : request.getAgreements());
        /*
         * 온보딩 게이트를 저장 직후 바로 갱신할 수 있게 두 플래그를 함께 내려준다.
         * 이게 없으면 금융동의를 마쳐도 가드가 계속 동의 화면으로 돌려보낸다.
         * (DECISIONS.md 2026-08-11 (7))
         */
        return ApiResponse.ok(ConsentResultDto.builder()
                .needsConsent(needsConsent)
                .needsFinancialConsent(consentService.needsConsent(userId, ConsentScope.FINANCIAL))
                .build());
    }

    @GetMapping("/me")
    public ApiResponse<MyConsentListDto> myConsents(@LoginUser Long userId) {
        return ApiResponse.ok(MyConsentListDto.builder()
                .items(consentService.myConsents(userId))
                .build());
    }

    @PostMapping("/{type}/withdraw")
    public ApiResponse<ConsentResultDto> withdraw(@LoginUser Long userId,
                                                  @PathVariable("type") String type) {
        /*
         * ⚠ 철회를 먼저 실행하고 그 뒤에 플래그를 읽는다.
         * 빌더 인자 순서대로 평가되므로, THIRD_PARTY 를 철회하는데 플래그를 먼저 읽으면
         * 철회 전 값이 나가 화면이 "아직 동의돼 있음" 으로 잘못 판단한다.
         */
        boolean needsConsent = consentService.withdraw(userId, type);
        return ApiResponse.ok(ConsentResultDto.builder()
                .needsConsent(needsConsent)
                .needsFinancialConsent(consentService.needsConsent(userId, ConsentScope.FINANCIAL))
                .build());
    }

    private ConsentScope parseScope(String raw) {
        try {
            return ConsentScope.valueOf(raw);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new BusinessException("CONSENT_TYPE_INVALID", "알 수 없는 동의 절차입니다: " + raw);
        }
    }
}
