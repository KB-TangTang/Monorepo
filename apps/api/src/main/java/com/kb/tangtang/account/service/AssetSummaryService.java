package com.kb.tangtang.account.service;

import com.kb.tangtang.account.domain.AssetGroupRow;
import com.kb.tangtang.account.domain.AssetSnapshotRow;
import com.kb.tangtang.account.dto.AssetCompositionItemDto;
import com.kb.tangtang.account.dto.AssetGroupItemDto;
import com.kb.tangtang.account.dto.AssetSummaryDto;
import com.kb.tangtang.account.dto.AssetTrendItemDto;
import com.kb.tangtang.account.mapper.AssetSummaryMapper;
import com.kb.tangtang.account.mapper.LoanMapper;
import com.kb.tangtang.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 자산 현황(GET /api/assets/summary) 집계.
 *
 * 순자산 = 입출금 + 예적금 + 투자(SECURITIES 는 tbl_investment_holding.market_value 합계, report 모듈의
 * sumActiveTotalAssets 와 동일 규칙) + 페이머니 - 대출잔액. 연결 해제(is_active=0) 계좌는 제외한다.
 *
 * 전월 비교·6개월 추이는 tbl_asset_snapshot(현재는 report 모듈의 월간 AI 분석 생성 시에만 채워짐)을
 * 조회해 net_worth 가 있으면 쓰고, 없으면 해당 월은 null 로 반환한다 — 배치로 매월 자동 스냅샷을
 * 쌓는 별도 기능이 아직 없어 "스냅샷 없음"이 정상 상태이기 때문이다. 요청 기준월(baseDate 가 속한 달)만은
 * 스냅샷 유무와 무관하게 방금 계산한 라이브 netWorth 를 그대로 채운다 — asOf 카드와 추이 그래프의
 * 마지막 점이 항상 같은 값이도록 하기 위함이다.
 */
@Service
public class AssetSummaryService {

    private static final int TREND_MONTH_COUNT = 6;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private static final List<String> ASSET_ACCOUNT_TYPES =
            List.of("DEMAND_DEPOSIT", "SAVINGS", "SECURITIES", "PAY_MONEY");
    private static final String LOAN_TYPE = "LOAN";

    /**
     * tbl_connected_account.account_type 의 실제 저장값은 "PAYMONEY"(밑줄 없음, FinancialSyncServiceImpl
     * 참고)다. DEMAND_DEPOSIT·SAVINGS·SECURITIES 와 달리 유일하게 이 API 계약의 밑줄 표기(PAY_MONEY)와
     * 어긋난다 — 계약을 DB 저장값에 맞춰 깨뜨리는 대신, 이 경계에서만 정규화한다.
     */
    private static final Map<String, String> DB_ACCOUNT_TYPE_ALIASES = Map.of("PAYMONEY", "PAY_MONEY");

    private static final Map<String, String> COMPOSITION_LABELS = Map.of(
            "DEMAND_DEPOSIT", "입출금",
            "SAVINGS", "예적금",
            "SECURITIES", "투자",
            "PAY_MONEY", "페이머니",
            "LOAN", "대출"
    );

    private static final Map<String, String> GROUP_LABELS = Map.of(
            "DEMAND_DEPOSIT", "입출금 계좌",
            "SAVINGS", "예금·적금",
            "SECURITIES", "투자·증권",
            "PAY_MONEY", "페이머니",
            "LOAN", "대출"
    );

    private final AssetSummaryMapper assetSummaryMapper;
    private final LoanMapper loanMapper;
    private final Clock clock;

    @Autowired
    public AssetSummaryService(AssetSummaryMapper assetSummaryMapper, LoanMapper loanMapper) {
        this(assetSummaryMapper, loanMapper, Clock.system(ZoneId.of("Asia/Seoul")));
    }

    AssetSummaryService(AssetSummaryMapper assetSummaryMapper, LoanMapper loanMapper, Clock clock) {
        this.assetSummaryMapper = assetSummaryMapper;
        this.loanMapper = loanMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AssetSummaryDto getSummary(long userId, String rawBaseDate) {
        LocalDateTime asOf = LocalDateTime.now(clock);
        YearMonth baseMonth = YearMonth.from(parseBaseDate(rawBaseDate, asOf.toLocalDate()));

        Map<String, AssetGroupRow> groupsByType = new LinkedHashMap<>();
        for (AssetGroupRow row : assetSummaryMapper.findAssetGroupsByUser(userId)) {
            String normalizedType = DB_ACCOUNT_TYPE_ALIASES.getOrDefault(row.getAccountType(), row.getAccountType());
            groupsByType.put(normalizedType, row);
        }
        BigDecimal loanBalance = zeroIfNull(loanMapper.sumBalanceByUser(userId));
        int loanCount = loanMapper.countByUser(userId);

        List<AssetCompositionItemDto> composition = new ArrayList<>();
        List<AssetGroupItemDto> assetGroups = new ArrayList<>();
        BigDecimal totalAsset = BigDecimal.ZERO;

        for (String type : ASSET_ACCOUNT_TYPES) {
            AssetGroupRow row = groupsByType.get(type);
            BigDecimal amount = row == null ? BigDecimal.ZERO : zeroIfNull(row.getAmount());
            int count = row == null ? 0 : row.getCount();
            totalAsset = totalAsset.add(amount);
            composition.add(AssetCompositionItemDto.builder()
                    .type(type).label(COMPOSITION_LABELS.get(type)).amount(amount).build());
            assetGroups.add(AssetGroupItemDto.builder()
                    .type(type).label(GROUP_LABELS.get(type)).count(count).amount(amount).build());
        }

        BigDecimal negativeLoanBalance = loanBalance.negate();
        composition.add(AssetCompositionItemDto.builder()
                .type(LOAN_TYPE).label(COMPOSITION_LABELS.get(LOAN_TYPE)).amount(negativeLoanBalance).build());
        assetGroups.add(AssetGroupItemDto.builder()
                .type(LOAN_TYPE).label(GROUP_LABELS.get(LOAN_TYPE)).count(loanCount).amount(negativeLoanBalance).build());

        BigDecimal netWorth = totalAsset.subtract(loanBalance);

        List<YearMonth> priorMonths = new ArrayList<>();
        for (int i = TREND_MONTH_COUNT - 1; i >= 1; i--) {
            priorMonths.add(baseMonth.minusMonths(i));
        }
        List<String> priorMonthKeys = new ArrayList<>();
        for (YearMonth month : priorMonths) {
            priorMonthKeys.add(month.toString());
        }
        Map<String, AssetSnapshotRow> snapshotByMonth = new LinkedHashMap<>();
        for (AssetSnapshotRow row : assetSummaryMapper.findNetWorthSnapshots(userId, priorMonthKeys)) {
            snapshotByMonth.put(row.getYearMonth(), row);
        }

        List<AssetTrendItemDto> trend = new ArrayList<>();
        for (YearMonth month : priorMonths) {
            AssetSnapshotRow row = snapshotByMonth.get(month.toString());
            trend.add(AssetTrendItemDto.builder()
                    .yearMonth(month.toString())
                    .netWorth(row == null ? null : row.getNetWorth())
                    .totalDebt(row == null ? null : row.getTotalDebt())
                    .build());
        }
        trend.add(AssetTrendItemDto.builder()
                .yearMonth(baseMonth.toString())
                .netWorth(netWorth)
                .totalDebt(loanBalance)
                .build());

        AssetSnapshotRow previousMonthSnapshot = snapshotByMonth.get(baseMonth.minusMonths(1).toString());
        BigDecimal previousMonthNetWorth = previousMonthSnapshot == null ? null : previousMonthSnapshot.getNetWorth();
        BigDecimal changeAmount = previousMonthNetWorth == null
                ? null : netWorth.subtract(previousMonthNetWorth);
        BigDecimal changeRate = previousMonthNetWorth == null
                ? null : calculateChangeRate(netWorth, previousMonthNetWorth);

        return AssetSummaryDto.builder()
                .asOf(asOf)
                .netWorth(netWorth)
                .previousMonthNetWorth(previousMonthNetWorth)
                .changeAmount(changeAmount)
                .changeRate(changeRate)
                .trend(trend)
                .composition(composition)
                .assetGroups(assetGroups)
                .build();
    }

    private LocalDate parseBaseDate(String rawBaseDate, LocalDate defaultDate) {
        if (rawBaseDate == null || rawBaseDate.isBlank()) {
            return defaultDate;
        }
        try {
            return LocalDate.parse(rawBaseDate, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ex) {
            throw new BusinessException("INVALID_REQUEST", "baseDate는 YYYY-MM-DD 형식이어야 합니다.");
        }
    }

    private BigDecimal calculateChangeRate(BigDecimal current, BigDecimal previous) {
        if (previous.signum() == 0) {
            return current.signum() == 0
                    ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    : null;
        }
        return current.subtract(previous)
                .multiply(ONE_HUNDRED)
                .divide(previous, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal zeroIfNull(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
