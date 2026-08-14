package com.kb.tangtang.account.controller;

import com.kb.tangtang.account.dto.FinancialSyncResultDto;
import com.kb.tangtang.account.service.FinancialSyncService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
        when(service.sync(eq(42L))).thenReturn(expected);

        FinancialSyncController controller = new FinancialSyncController(service);

        var response = controller.sync(42L);

        assertEquals("COMPLETED", response.getData().getStatus());
        assertEquals(List.of("BANK", "CARD"), response.getData().getSyncedSources());
        assertEquals(true, response.isSuccess());
    }
}
