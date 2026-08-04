package com.kb.tangtang.user.service;

import com.kb.tangtang.user.mapper.ConsentMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsentServiceTest {

    @Mock private ConsentMapper consentMapper;

    private ConsentCatalog catalog() {
        return new ConsentCatalog("v1.0", "u/terms", "u/privacy", "u/financial",
                "u/third", "u/ai", "u/marketing");
    }

    private ConsentService service() {
        return new ConsentService(consentMapper, catalog());
    }

    @Test
    @DisplayName("SIGNUP 필수 3종이 모두 활성이면 동의가 더 필요하지 않다")
    void needsConsentFalseWhenAllRequiredActive() {
        when(consentMapper.countActive(eq(1L), eq(List.of("TERMS", "PRIVACY", "FINANCIAL_DATA")), any(LocalDateTime.class)))
                .thenReturn(3);

        assertFalse(service().needsConsent(1L));
    }

    @Test
    @DisplayName("필수 3종 중 2건만 활성이면 여전히 동의가 필요하다")
    void needsConsentTrueWhenPartial() {
        when(consentMapper.countActive(eq(1L), any(), any(LocalDateTime.class))).thenReturn(2);

        assertTrue(service().needsConsent(1L), "부분 동의는 통과시키지 않는다");
    }

    @Test
    @DisplayName("동의 이력이 없으면 동의가 필요하다")
    void needsConsentTrueWhenNone() {
        when(consentMapper.countActive(eq(1L), any(), any(LocalDateTime.class))).thenReturn(0);

        assertTrue(service().needsConsent(1L));
    }
}
