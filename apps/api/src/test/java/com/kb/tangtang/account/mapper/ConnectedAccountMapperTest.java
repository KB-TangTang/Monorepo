package com.kb.tangtang.account.mapper;

import com.kb.tangtang.account.domain.ConnectedAccount;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled("실 DB 연결 필요 — 로컬 수동 검증용")
@SpringJUnitConfig
@ContextConfiguration(classes = com.kb.tangtang.config.RootConfig.class)
class ConnectedAccountMapperTest {

    @Autowired
    private ConnectedAccountMapper connectedAccountMapper;

    /**
     * 이슈 #147 리뷰에서 발견 — reactivate() 의 SET 절이 bank_code·provider 를 빼먹어서,
     * 최초 insert 이후로는 이 두 컬럼이 재동기화에서 절대 갱신되지 않았다. 계좌를 되살릴 때
     * 새 bankCode/provider 값이 실제로 반영되는지 확인한다.
     */
    @Test
    void reactivateUpdatesBankCodeAndProvider() {
        String accountNoEncrypted = "MOCK-PAYMONEY-TEST-001";
        ConnectedAccount original = ConnectedAccount.builder()
                .userId(1L)
                .bankCode("OLD_CODE")
                .bankName("옛날제공자")
                .accountNoEncrypted(accountNoEncrypted)
                .accountType("PAYMONEY")
                .provider("OLD_PROVIDER")
                .balance(new BigDecimal("10000"))
                .syncStatus("NORMAL")
                .lastSyncAt(LocalDateTime.now())
                .build();
        connectedAccountMapper.insert(original);

        ConnectedAccount reconnected = ConnectedAccount.builder()
                .userId(1L)
                .bankCode("NEW_CODE")
                .bankName("새제공자")
                .accountNoEncrypted(accountNoEncrypted)
                .accountType("PAYMONEY")
                .provider("NEW_PROVIDER")
                .balance(new BigDecimal("20000"))
                .syncStatus("NORMAL")
                .lastSyncAt(LocalDateTime.now())
                .build();
        int updated = connectedAccountMapper.reactivate(reconnected);
        assertEquals(1, updated);

        ConnectedAccount found = connectedAccountMapper.findByIdAndUser(original.getId(), 1L);
        assertEquals("NEW_CODE", found.getBankCode(),
                "reactivate 가 bank_code 를 갱신하지 않으면 최초 insert 값이 영원히 고정된다");
        assertEquals("NEW_PROVIDER", found.getProvider(),
                "reactivate 가 provider 를 갱신하지 않으면 최초 insert 값이 영원히 고정된다");
    }
}
