package com.kb.tangtang.account.service;

import com.kb.tangtang.account.dto.AssetAccountDetailDto;
import com.kb.tangtang.account.dto.ConnectedAccountDto;
import com.kb.tangtang.account.dto.ConnectedAccountListDto;
import com.kb.tangtang.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetAccountDetailServiceTest {

    private static final long USER_ID = 1L;

    @Mock
    private AccountLinkService accountLinkService;

    private AssetAccountDetailService service() {
        return new AssetAccountDetailService(accountLinkService);
    }

    private ConnectedAccountDto account(long id, String accountType, long balance) {
        return ConnectedAccountDto.builder()
                .accountId(id)
                .bankCode("0004")
                .bankName("KB국민은행")
                .shortLabel("KB")
                .accountName("입출금통장")
                .accountNoMasked("110-***-****23")
                .accountType(accountType)
                .balance(BigDecimal.valueOf(balance))
                .syncStatus("NORMAL")
                .lastSyncAt("2026-08-15T09:00:00")
                .build();
    }

    @Test
    @DisplayName("요청한 종류의 계좌만 걸러 합계를 계산한다 — 페이머니는 DB 저장값(PAYMONEY)을 정규화해 매칭한다")
    void filtersByTypeAndNormalizesPaymoney() {
        when(accountLinkService.connectedAccounts(USER_ID)).thenReturn(
                ConnectedAccountListDto.builder()
                        .accounts(List.of(
                                account(1L, "DEMAND_DEPOSIT", 100_000),
                                account(2L, "PAYMONEY", 50_000),
                                account(3L, "PAYMONEY", 30_000),
                                account(4L, "SAVINGS", 1_000_000)
                        ))
                        .build());

        AssetAccountDetailDto result = service().getByType(USER_ID, "PAY_MONEY");

        assertEquals(new BigDecimal("80000"), result.getTotal());
        assertEquals(2, result.getAccounts().size());
        assertTrue(result.getAccounts().stream().allMatch(a -> "PAYMONEY".equals(a.getAccountType())));
    }

    @Test
    @DisplayName("연동된 계좌가 해당 종류에 없으면 total 0 과 빈 목록을 돌려준다")
    void returnsZeroWhenNoMatch() {
        when(accountLinkService.connectedAccounts(USER_ID)).thenReturn(
                ConnectedAccountListDto.builder()
                        .accounts(List.of(account(1L, "DEMAND_DEPOSIT", 100_000)))
                        .build());

        AssetAccountDetailDto result = service().getByType(USER_ID, "SAVINGS");

        assertEquals(BigDecimal.ZERO, result.getTotal());
        assertTrue(result.getAccounts().isEmpty());
    }

    @Test
    @DisplayName("지원하지 않는 type 이면 INVALID_REQUEST 예외를 던진다")
    void throwsOnUnsupportedType() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service().getByType(USER_ID, "SECURITIES"));

        assertEquals("INVALID_REQUEST", ex.getCode());
    }
}
