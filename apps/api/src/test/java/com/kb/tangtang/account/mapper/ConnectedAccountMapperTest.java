package com.kb.tangtang.account.mapper;

import com.kb.tangtang.account.domain.ConnectedAccount;
import com.kb.tangtang.user.dto.UserDto;
import com.kb.tangtang.user.mapper.UserMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 진짜 DB 를 쓰므로 테스트 메서드마다 스프링 테스트의 트랜잭션 롤백에 기댄다 — TransactionMapperTest 참고. */
@Disabled("실 DB 연결 필요 — 로컬 수동 검증용")
@SpringJUnitConfig
@ContextConfiguration(classes = com.kb.tangtang.config.RootConfig.class)
@Transactional
class ConnectedAccountMapperTest {

    @Autowired
    private ConnectedAccountMapper connectedAccountMapper;

    @Autowired
    private UserMapper userMapper;

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

    @Test
    @org.junit.jupiter.api.DisplayName("가장 오래 동기화되지 않은 사용자(NULL 포함)부터 limit 만큼 돌려준다")
    void findUserIdsDueForSyncOrdersByOldestLastSyncFirst() {
        // 테스트용 사용자 생성 (transactional rollback 이므로 DB에 영구 기록 안 됨)
        userMapper.insert(UserDto.builder()
                .id(901L)
                .socialProvider("TEST")
                .providerUserId("test-user-901")
                .email("test901@test.com")
                .status("ACTIVE")
                .difficultyId(1L)
                .build());
        userMapper.insert(UserDto.builder()
                .id(902L)
                .socialProvider("TEST")
                .providerUserId("test-user-902")
                .email("test902@test.com")
                .status("ACTIVE")
                .difficultyId(1L)
                .build());
        userMapper.insert(UserDto.builder()
                .id(903L)
                .socialProvider("TEST")
                .providerUserId("test-user-903")
                .email("test903@test.com")
                .status("ACTIVE")
                .difficultyId(1L)
                .build());

        connectedAccountMapper.insert(ConnectedAccount.builder()
                .userId(901L).accountNoEncrypted("MOCK-BANK-T901")
                .accountType("DEMAND_DEPOSIT").syncStatus("NORMAL")
                .lastSyncAt(LocalDateTime.now().minusDays(1))
                .build());
        connectedAccountMapper.insert(ConnectedAccount.builder()
                .userId(902L).accountNoEncrypted("MOCK-BANK-T902")
                .accountType("DEMAND_DEPOSIT").syncStatus("NORMAL")
                .lastSyncAt(null)   // 한 번도 동기화 안 됨 — 가장 먼저 나와야 한다
                .build());
        connectedAccountMapper.insert(ConnectedAccount.builder()
                .userId(903L).accountNoEncrypted("MOCK-BANK-T903")
                .accountType("DEMAND_DEPOSIT").syncStatus("NORMAL")
                .lastSyncAt(LocalDateTime.now().minusDays(5))
                .build());

        List<Long> due = connectedAccountMapper.findUserIdsDueForSync(2);

        assertEquals(List.of(902L, 903L), due);
    }
}
