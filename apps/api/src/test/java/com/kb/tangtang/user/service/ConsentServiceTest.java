package com.kb.tangtang.user.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.user.domain.ConsentScope;
import com.kb.tangtang.user.dto.ConsentAgreementDto;
import com.kb.tangtang.user.dto.ConsentRecordDto;
import com.kb.tangtang.user.mapper.ConsentMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

    private static ConsentAgreementDto agree(String type, boolean agreed) {
        ConsentAgreementDto dto = new ConsentAgreementDto();
        dto.setType(type);
        dto.setAgreed(agreed);
        return dto;
    }

    @Test
    @DisplayName("필수 항목이 하나라도 빠지면 저장을 거부한다")
    void submitRejectsMissingRequired() {
        List<ConsentAgreementDto> agreements = List.of(
                agree("TERMS", true),
                agree("PRIVACY", true),
                agree("FINANCIAL_DATA", false));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service().submit(1L, ConsentScope.SIGNUP, agreements));

        assertEquals("CONSENT_REQUIRED_MISSING", ex.getCode());
        verify(consentMapper, never()).upsert(any());
    }

    @Test
    @DisplayName("카탈로그에 없는 타입이 오면 거부한다")
    void submitRejectsUnknownType() {
        List<ConsentAgreementDto> agreements = List.of(
                agree("TERMS", true), agree("PRIVACY", true),
                agree("FINANCIAL_DATA", true), agree("NOT_A_REAL_TYPE", true));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service().submit(1L, ConsentScope.SIGNUP, agreements));

        assertEquals("CONSENT_TYPE_INVALID", ex.getCode());
    }

    @Test
    @DisplayName("scope 밖의 타입이 오면 거부한다")
    void submitRejectsTypeOutsideScope() {
        List<ConsentAgreementDto> agreements = List.of(
                agree("TERMS", true), agree("PRIVACY", true),
                agree("FINANCIAL_DATA", true), agree("THIRD_PARTY", true));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service().submit(1L, ConsentScope.SIGNUP, agreements));

        assertEquals("CONSENT_TYPE_INVALID", ex.getCode());
    }

    @Test
    @DisplayName("terms_version 과 is_required 는 요청값이 아니라 카탈로그 값을 쓴다")
    void submitUsesCatalogVersionAndRequired() {
        when(consentMapper.countActive(anyLong(), any(), any(LocalDateTime.class))).thenReturn(3);

        service().submit(1L, ConsentScope.SIGNUP, List.of(
                agree("TERMS", true), agree("PRIVACY", true), agree("FINANCIAL_DATA", true)));

        ArgumentCaptor<ConsentRecordDto> captor = ArgumentCaptor.forClass(ConsentRecordDto.class);
        verify(consentMapper, times(5)).upsert(captor.capture());

        ConsentRecordDto terms = captor.getAllValues().stream()
                .filter(r -> "TERMS".equals(r.getConsentType())).findFirst().orElseThrow();
        assertEquals("v1.0", terms.getTermsVersion());
        assertTrue(terms.isRequired());
        assertEquals(1, terms.getStatus());
    }

    @Test
    @DisplayName("요청에 없는 선택 항목은 미동의로 저장된다")
    void submitMarksOmittedOptionalAsWithdrawn() {
        when(consentMapper.countActive(anyLong(), any(), any(LocalDateTime.class))).thenReturn(3);

        service().submit(1L, ConsentScope.SIGNUP, List.of(
                agree("TERMS", true), agree("PRIVACY", true), agree("FINANCIAL_DATA", true)));

        ArgumentCaptor<ConsentRecordDto> captor = ArgumentCaptor.forClass(ConsentRecordDto.class);
        verify(consentMapper, times(5)).upsert(captor.capture());

        ConsentRecordDto marketing = captor.getAllValues().stream()
                .filter(r -> "MARKETING".equals(r.getConsentType())).findFirst().orElseThrow();
        assertEquals(0, marketing.getStatus());
        assertNotNull(marketing.getWithdrawnAt());
    }

    @Test
    @DisplayName("FINANCIAL scope 저장은 SIGNUP 의 선택 항목을 건드리지 않는다")
    void submitFinancialDoesNotTouchSignupItems() {
        when(consentMapper.countActive(anyLong(), any(), any(LocalDateTime.class))).thenReturn(3);

        service().submit(1L, ConsentScope.FINANCIAL, List.of(agree("THIRD_PARTY", true)));

        ArgumentCaptor<ConsentRecordDto> captor = ArgumentCaptor.forClass(ConsentRecordDto.class);
        verify(consentMapper, times(1)).upsert(captor.capture());
        assertEquals("THIRD_PARTY", captor.getValue().getConsentType());
    }

    @Test
    @DisplayName("FINANCIAL_DATA 동의에만 1년 만료가 붙는다")
    void submitSetsExpiryOnlyForFinancialData() {
        when(consentMapper.countActive(anyLong(), any(), any(LocalDateTime.class))).thenReturn(3);

        service().submit(1L, ConsentScope.SIGNUP, List.of(
                agree("TERMS", true), agree("PRIVACY", true),
                agree("FINANCIAL_DATA", true), agree("AI_USAGE", true)));

        ArgumentCaptor<ConsentRecordDto> captor = ArgumentCaptor.forClass(ConsentRecordDto.class);
        verify(consentMapper, times(5)).upsert(captor.capture());

        ConsentRecordDto financial = captor.getAllValues().stream()
                .filter(r -> "FINANCIAL_DATA".equals(r.getConsentType())).findFirst().orElseThrow();
        ConsentRecordDto ai = captor.getAllValues().stream()
                .filter(r -> "AI_USAGE".equals(r.getConsentType())).findFirst().orElseThrow();

        assertNotNull(financial.getExpiresAt(), "금융정보 동의는 1년 만료");
        assertTrue(financial.getExpiresAt().isAfter(LocalDateTime.now().plusDays(364)));
        assertNull(ai.getExpiresAt(), "그 외 항목은 만료 개념이 없다");
    }
}
