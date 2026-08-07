package com.kb.tangtang.user.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.user.domain.ConsentScope;
import com.kb.tangtang.user.dto.ConsentAgreementDto;
import com.kb.tangtang.user.dto.ConsentRecordDto;
import com.kb.tangtang.user.dto.MyConsentDto;
import com.kb.tangtang.user.dto.MyConsentRowDto;
import com.kb.tangtang.user.domain.ConsentWithdrawnEvent;
import com.kb.tangtang.user.mapper.ConsentMapper;
import org.springframework.context.ApplicationEventPublisher;
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

    /* 금융데이터 철회 시 계좌 연동 해제 이벤트가 나가는지 확인하기 위해 함께 목으로 둔다. */
    @Mock private ApplicationEventPublisher events;

    private ConsentCatalog catalog() {
        return new ConsentCatalog("v1.0", "u/terms", "u/privacy", "u/financial",
                "u/third", "u/ai", "u/marketing");
    }

    private ConsentService service() {
        return new ConsentService(consentMapper, catalog(), events);
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

    @Test
    @DisplayName("TERMS 철회는 막는다")
    void withdrawRejectsTerms() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service().withdraw(1L, "TERMS"));

        assertEquals("CONSENT_NOT_WITHDRAWABLE", ex.getCode());
        verify(consentMapper, never()).withdraw(anyLong(), any(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("PRIVACY 철회도 막는다")
    void withdrawRejectsPrivacy() {
        assertEquals("CONSENT_NOT_WITHDRAWABLE",
                assertThrows(BusinessException.class, () -> service().withdraw(1L, "PRIVACY")).getCode());
    }

    @Test
    @DisplayName("FINANCIAL_DATA 를 철회하면 THIRD_PARTY 도 함께 철회한다")
    void withdrawFinancialDataCascadesToThirdParty() {
        when(consentMapper.withdraw(eq(1L), eq(List.of("FINANCIAL_DATA", "THIRD_PARTY")), any(LocalDateTime.class)))
                .thenReturn(2);
        when(consentMapper.countActive(anyLong(), any(), any(LocalDateTime.class))).thenReturn(2);

        assertTrue(service().withdraw(1L, "FINANCIAL_DATA"), "필수 동의를 철회했으므로 다시 동의가 필요하다");
    }

    @Test
    @DisplayName("FINANCIAL_DATA 철회는 계좌 연동 해제 이벤트를 발행한다 (이슈 #12)")
    void withdrawFinancialDataPublishesEvent() {
        when(consentMapper.withdraw(eq(1L), eq(List.of("FINANCIAL_DATA", "THIRD_PARTY")), any(LocalDateTime.class)))
                .thenReturn(2);
        when(consentMapper.countActive(anyLong(), any(), any(LocalDateTime.class))).thenReturn(2);

        service().withdraw(1L, "FINANCIAL_DATA");

        ArgumentCaptor<ConsentWithdrawnEvent> captor =
                ArgumentCaptor.forClass(ConsentWithdrawnEvent.class);
        verify(events).publishEvent(captor.capture());
        assertEquals(1L, captor.getValue().userId());
        assertEquals("FINANCIAL_DATA", captor.getValue().consentType());
    }

    @Test
    @DisplayName("선택 항목 철회는 연동 해제 이벤트를 발행하지 않는다")
    void withdrawOptionalDoesNotPublishEvent() {
        when(consentMapper.withdraw(eq(1L), eq(List.of("AI_USAGE")), any(LocalDateTime.class))).thenReturn(1);
        when(consentMapper.countActive(anyLong(), any(), any(LocalDateTime.class))).thenReturn(3);

        service().withdraw(1L, "AI_USAGE");

        verify(events, never()).publishEvent(any(ConsentWithdrawnEvent.class));
    }

    @Test
    @DisplayName("AI_USAGE 는 자기 하나만 철회한다")
    void withdrawOptionalOnly() {
        when(consentMapper.withdraw(eq(1L), eq(List.of("AI_USAGE")), any(LocalDateTime.class))).thenReturn(1);
        when(consentMapper.countActive(anyLong(), any(), any(LocalDateTime.class))).thenReturn(3);

        assertFalse(service().withdraw(1L, "AI_USAGE"), "선택 항목 철회는 게이트에 영향이 없다");
    }

    @Test
    @DisplayName("철회할 행이 없으면 NOT_FOUND")
    void withdrawNotFound() {
        when(consentMapper.withdraw(anyLong(), any(), any(LocalDateTime.class))).thenReturn(0);

        assertEquals("NOT_FOUND",
                assertThrows(BusinessException.class, () -> service().withdraw(1L, "MARKETING")).getCode());
    }

    @Test
    @DisplayName("내 동의 현황은 카탈로그 항목 전체를 돌려주고 미동의는 agreed=false 다")
    void myConsentsFillsMissingAsNotAgreed() {
        when(consentMapper.findByUserId(1L)).thenReturn(List.of(
                MyConsentRowDto.builder()
                        .consentType("TERMS").required(true).termsVersion("v1.0")
                        .status(1).withdrawnAt(null).expiresAt(null).createdAt(LocalDateTime.now())
                        .build()));

        List<MyConsentDto> result = service().myConsents(1L);

        assertEquals(6, result.size(), "카탈로그의 SIGNUP 5종 + FINANCIAL 1종");
        MyConsentDto terms = result.stream().filter(d -> "TERMS".equals(d.getType())).findFirst().orElseThrow();
        MyConsentDto marketing = result.stream().filter(d -> "MARKETING".equals(d.getType())).findFirst().orElseThrow();

        assertTrue(terms.isAgreed());
        assertFalse(terms.isWithdrawable());
        assertFalse(marketing.isAgreed(), "행이 없으면 미동의");
        assertTrue(marketing.isWithdrawable());
    }

    @Test
    @DisplayName("내 동의 현황은 항목이 속한 scope 를 함께 알려준다 — 화면의 재동의가 scope 단위 저장을 쓴다")
    void myConsentsCarriesScope() {
        when(consentMapper.findByUserId(1L)).thenReturn(List.of());

        List<MyConsentDto> result = service().myConsents(1L);

        assertEquals("SIGNUP", find(result, "MARKETING").getScope());
        assertEquals("SIGNUP", find(result, "FINANCIAL_DATA").getScope());
        assertEquals("FINANCIAL", find(result, "THIRD_PARTY").getScope(),
                "CODEF 제3자 제공은 FINANCIAL 묶음이다. SIGNUP 으로 저장하면 요청이 거부된다");
    }

    private MyConsentDto find(List<MyConsentDto> items, String type) {
        return items.stream().filter(d -> type.equals(d.getType())).findFirst().orElseThrow();
    }
}
