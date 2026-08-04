package com.kb.tangtang.user.service;

import com.kb.tangtang.user.domain.ConsentScope;
import com.kb.tangtang.user.domain.ConsentType;
import com.kb.tangtang.user.dto.ConsentItemDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsentCatalogTest {

    private static ConsentCatalog catalog() {
        return new ConsentCatalog(
                "v1.0",
                "https://notion/terms",
                "https://notion/privacy",
                "https://notion/financial-data",
                "https://notion/third-party",
                "https://notion/ai-usage",
                "https://notion/marketing");
    }

    @Test
    @DisplayName("SIGNUP 은 5개 항목, 그중 필수는 3개다")
    void signupScope() {
        List<ConsentItemDto> items = catalog().items(ConsentScope.SIGNUP);

        assertEquals(5, items.size());
        assertEquals(
                List.of("TERMS", "PRIVACY", "FINANCIAL_DATA", "AI_USAGE", "MARKETING"),
                items.stream().map(ConsentItemDto::getType).toList());
        assertEquals(3, items.stream().filter(ConsentItemDto::isRequired).count());
    }

    @Test
    @DisplayName("FINANCIAL 은 THIRD_PARTY 하나뿐이고 필수다")
    void financialScope() {
        List<ConsentItemDto> items = catalog().items(ConsentScope.FINANCIAL);

        assertEquals(1, items.size());
        assertEquals("THIRD_PARTY", items.get(0).getType());
        assertTrue(items.get(0).isRequired());
    }

    @Test
    @DisplayName("SIGNUP 필수 타입은 TERMS·PRIVACY·FINANCIAL_DATA 다")
    void signupRequiredTypes() {
        assertEquals(
                List.of(ConsentType.TERMS, ConsentType.PRIVACY, ConsentType.FINANCIAL_DATA),
                ConsentScope.SIGNUP.requiredTypes());
    }

    @Test
    @DisplayName("TERMS·PRIVACY 는 철회할 수 없고 나머지는 철회할 수 있다")
    void withdrawable() {
        assertFalse(ConsentType.TERMS.withdrawable());
        assertFalse(ConsentType.PRIVACY.withdrawable());
        assertTrue(ConsentType.FINANCIAL_DATA.withdrawable());
        assertTrue(ConsentType.THIRD_PARTY.withdrawable());
        assertTrue(ConsentType.AI_USAGE.withdrawable());
        assertTrue(ConsentType.MARKETING.withdrawable());
    }

    @Test
    @DisplayName("항목마다 설정된 약관 URL 이 실린다")
    void termsUrls() {
        Map<String, String> byType = catalog().items(ConsentScope.SIGNUP).stream()
                .collect(java.util.stream.Collectors.toMap(
                        ConsentItemDto::getType, ConsentItemDto::getTermsUrl));

        assertEquals("https://notion/terms", byType.get("TERMS"));
        assertEquals("https://notion/marketing", byType.get("MARKETING"));
        assertEquals("v1.0", catalog().termsVersion());
    }
}
