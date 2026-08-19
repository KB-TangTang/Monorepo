package com.kb.tangtang.account.mapper;

import com.kb.tangtang.account.domain.ConnectedAccount;
import com.kb.tangtang.account.domain.InvestmentHolding;
import com.kb.tangtang.user.dto.UserDto;
import com.kb.tangtang.user.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 실 DB 를 쓰므로 스프링 테스트의 트랜잭션 롤백에 기댄다 — ConnectedAccountMapperTest 와 같은 패턴.
 *
 * findActiveByUser 는 QA 리뷰에서 "text-scan(InvestmentHoldingMapperColumnOwnershipTest)만 있고
 * 실제 JOIN 을 실행해보는 테스트가 없다"는 지적에 따라 추가한다 — is_active 필터·JOIN 컬럼이
 * 실제로 맞는지는 문자열 검사로는 못 잡는다.
 */
@Disabled("실 DB 연결 필요 — 로컬 수동 검증용")
@SpringJUnitConfig
@ContextConfiguration(classes = com.kb.tangtang.config.RootConfig.class)
@Transactional
class InvestmentHoldingMapperTest {

    @Autowired
    private InvestmentHoldingMapper investmentHoldingMapper;

    @Autowired
    private ConnectedAccountMapper connectedAccountMapper;

    @Autowired
    private UserMapper userMapper;

    private long insertUser(String providerUserId) {
        UserDto user = UserDto.builder()
                .socialProvider("TEST")
                .providerUserId(providerUserId)
                .email(providerUserId + "@test.com")
                .status("ACTIVE")
                .difficultyId(1L)
                .build();
        userMapper.insert(user);
        return user.getId();
    }

    /**
     * ⚠ ConnectedAccountMapper.insert() 는 is_active 를 항상 리터럴 1 로 넣는다 — #{isActive} 를
     *   바인딩하지 않는다(ConnectedAccountMapper.xml 확인). 그래서 여기서 .isActive(false) 를
     *   builder 에 넘겨도 insert 시점엔 무시된다. 비활성 계좌를 만들려면 insert 후
     *   deactivate() 를 따로 불러야 한다 — 운영 코드(FinancialSyncServiceImpl)가 계좌를 항상
     *   활성으로 만든 뒤 별도 deactivate() 로 해제하는 것과 같은 경로다.
     */
    private long insertSecuritiesAccount(long userId, String accountNoEncrypted, boolean isActive) {
        ConnectedAccount account = ConnectedAccount.builder()
                .userId(userId)
                .accountNoEncrypted(accountNoEncrypted)
                .accountType("SECURITIES")
                .syncStatus("NORMAL")
                .build();
        connectedAccountMapper.insert(account);
        if (!isActive) {
            connectedAccountMapper.deactivate(account.getId(), userId);
        }
        return account.getId();
    }

    private void insertHolding(long userId, long accountId, String symbol) {
        investmentHoldingMapper.insert(InvestmentHolding.builder()
                .userId(userId)
                .accountId(accountId)
                .symbol(symbol)
                .name("삼성전자")
                .marketCountry("KR")
                .currency("KRW")
                .quantity(new BigDecimal("10"))
                .averagePurchasePrice(new BigDecimal("70000"))
                .lastPrice(new BigDecimal("72000"))
                .purchaseAmount(new BigDecimal("700000"))
                .marketValue(new BigDecimal("720000"))
                .profitLossAmount(new BigDecimal("20000"))
                .profitLossRate(new BigDecimal("0.0286"))
                .build());
    }

    /**
     * QA 리뷰 지적사항 — findActiveByUser 는 연결 해제(is_active=0)한 계좌의 보유종목을 빼야 한다
     * (AssetSummaryMapper#findAssetGroupsByUser 와 같은 기준). findByUser 는 그대로 전부 돌려줘야 한다.
     */
    @Test
    @DisplayName("findActiveByUser는 연결 해제한 계좌의 보유종목을 뺀다")
    void findActiveByUserExcludesDisconnectedAccountHoldings() {
        long userId = insertUser("test-user-ih-901");
        long activeAccountId = insertSecuritiesAccount(userId, "MOCK-SECURITIES-ACTIVE-901", true);
        long inactiveAccountId = insertSecuritiesAccount(userId, "MOCK-SECURITIES-INACTIVE-901", false);
        insertHolding(userId, activeAccountId, "005930");
        insertHolding(userId, inactiveAccountId, "000660");

        List<InvestmentHolding> active = investmentHoldingMapper.findActiveByUser(userId);
        List<InvestmentHolding> all = investmentHoldingMapper.findByUser(userId);

        assertEquals(1, active.size(), "연결 해제한 계좌의 종목이 섞여 나오면 안 된다");
        assertEquals("005930", active.get(0).getSymbol());
        /* findByUser는 지금 프로덕션 호출부가 없다(InvestmentHoldingMapper.java 참고) — 이 두 줄은
           "필터 없는 전체 조회"라는 findByUser 자체의 계약을 검증한다. */
        assertEquals(2, all.size(), "findByUser는 연결 상태와 무관하게 전부 돌려줘야 한다");
        assertTrue(all.stream().anyMatch(h -> h.getSymbol().equals("000660")),
                "findByUser에서 연결 해제한 계좌의 종목이 빠지면 안 된다");
    }
}
