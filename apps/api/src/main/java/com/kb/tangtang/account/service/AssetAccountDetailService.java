package com.kb.tangtang.account.service;

import com.kb.tangtang.account.dto.AssetAccountDetailDto;
import com.kb.tangtang.account.dto.ConnectedAccountDto;
import com.kb.tangtang.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 자산 상세 목록(입출금·예적금·페이머니) 조회.
 *
 * 이 세 종류는 전부 tbl_connected_account 한 테이블에서 나오므로 새 쿼리를 만들지 않고
 * {@link AccountLinkService#connectedAccounts(long)} (이미 user_id·is_active=1 로 소유 계좌만
 * 반환)를 그대로 재사용해 종류만 걸러낸다. 투자(SECURITIES)·대출(LOAN)은 조인·필드가 완전히 달라
 * 각각 AssetInvestmentDetailService·AssetLoanDetailService 로 분리했다.
 */
@Service
public class AssetAccountDetailService {

    private static final Set<String> SUPPORTED_TYPES = Set.of("DEMAND_DEPOSIT", "SAVINGS", "PAY_MONEY");

    /**
     * tbl_connected_account.account_type 의 실제 저장값은 "PAYMONEY"(밑줄 없음, FinancialSyncServiceImpl
     * 참고)다. AssetSummaryService.DB_ACCOUNT_TYPE_ALIASES 와 같은 정규화를 이 경계에서도 반복한다 —
     * 계약(PAY_MONEY)을 DB 저장값에 맞춰 깨뜨리지 않기 위함이다.
     */
    private static final Map<String, String> DB_ACCOUNT_TYPE_ALIASES = Map.of("PAYMONEY", "PAY_MONEY");

    private final AccountLinkService accountLinkService;

    @Autowired
    public AssetAccountDetailService(AccountLinkService accountLinkService) {
        this.accountLinkService = accountLinkService;
    }

    @Transactional(readOnly = true)
    public AssetAccountDetailDto getByType(long userId, String type) {
        if (!SUPPORTED_TYPES.contains(type)) {
            throw new BusinessException("INVALID_REQUEST",
                    "type 은 DEMAND_DEPOSIT, SAVINGS, PAY_MONEY 중 하나여야 합니다.");
        }

        List<ConnectedAccountDto> matched = accountLinkService.connectedAccounts(userId).getAccounts().stream()
                .filter(account -> normalize(account.getAccountType()).equals(type))
                .toList();

        BigDecimal total = matched.stream()
                .map(ConnectedAccountDto::getBalance)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return AssetAccountDetailDto.builder()
                .total(total)
                .accounts(matched)
                .build();
    }

    private String normalize(String dbAccountType) {
        return DB_ACCOUNT_TYPE_ALIASES.getOrDefault(dbAccountType, dbAccountType);
    }
}
