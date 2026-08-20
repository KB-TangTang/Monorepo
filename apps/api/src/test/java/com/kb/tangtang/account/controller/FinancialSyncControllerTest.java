package com.kb.tangtang.account.controller;

import com.kb.tangtang.account.dto.FinancialSyncRequestDto;
import com.kb.tangtang.account.dto.FinancialSyncResultDto;
import com.kb.tangtang.account.service.FinancialSyncService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FinancialSyncControllerTest {

    @Test
    void syncDelegatesToServiceWithLoginUserId() {
        FinancialSyncService service = mock(FinancialSyncService.class);
        FinancialSyncResultDto expected = FinancialSyncResultDto.builder()
                .status("COMPLETED")
                .syncedSources(List.of("BANK", "CARD"))
                .syncedAt("2026-08-12T10:00:00+09:00")
                .build();
        when(service.sync(eq(42L), eq(Set.of()))).thenReturn(expected);

        FinancialSyncController controller = new FinancialSyncController(service);

        /* 요청 본문 없이 부르는 평소 동기화 — LinkDoneView 최초 동기화가 아닌 나머지 전부. */
        var response = controller.sync(42L, null);

        assertEquals("COMPLETED", response.getData().getStatus());
        assertEquals(List.of("BANK", "CARD"), response.getData().getSyncedSources());
        assertEquals(true, response.isSuccess());
    }

    @Test
    void syncPassesInstitutionCodesFromRequestBodyAsExtraScope() {
        FinancialSyncService service = mock(FinancialSyncService.class);
        when(service.sync(eq(42L), eq(Set.of("PAY_KB", "CP_KB"))))
                .thenReturn(FinancialSyncResultDto.builder().status("COMPLETED")
                        .syncedSources(List.of()).syncedAt("2026-08-20T10:00:00+09:00").build());

        FinancialSyncController controller = new FinancialSyncController(service);
        FinancialSyncRequestDto request = new FinancialSyncRequestDto();
        request.setInstitutionCodes(List.of("PAY_KB", "CP_KB"));

        controller.sync(42L, request);

        /* #334: 계좌 연동 직후 최초 동기화가 대출·페이머니 기관코드를 넘기면 그대로 전달돼야 한다. */
        verify(service).sync(42L, Set.of("PAY_KB", "CP_KB"));
    }
}
