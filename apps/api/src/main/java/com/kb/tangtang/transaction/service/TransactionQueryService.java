package com.kb.tangtang.transaction.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.common.util.PaymentMethodLabels;
import com.kb.tangtang.transaction.domain.TransactionListRow;
import com.kb.tangtang.transaction.dto.DailySpendingSummaryDto;
import com.kb.tangtang.transaction.dto.LedgerSummaryDto;
import com.kb.tangtang.transaction.dto.TransactionListItemDto;
import com.kb.tangtang.transaction.dto.TransactionListResponseDto;
import com.kb.tangtang.transaction.dto.TransactionMonthDto;
import com.kb.tangtang.transaction.dto.TransactionMonthsDto;
import com.kb.tangtang.transaction.mapper.TransactionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 장부(거래내역) 화면의 월별 조회 두 엔드포인트를 담당한다. 카테고리 수정(쓰기) 경로인
 * TransactionService 와는 관심사가 달라 별도 서비스로 둔다 — report 모듈이
 * MonthlyReportService/MonthlyAiAnalysisService 를 나눈 것과 같은 이유.
 */
@Service
public class TransactionQueryService {

    private static final Pattern YEAR_MONTH_PATTERN = Pattern.compile("\\d{4}-(0[1-9]|1[0-2])");
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final TransactionMapper transactionMapper;
    private final Clock clock;

    /** 테스트용 상속과 기존 직접 생성 경로의 KST 기본값을 유지한다. */
    public TransactionQueryService(TransactionMapper transactionMapper) {
        this(transactionMapper, Clock.system(ZoneId.of("Asia/Seoul")));
    }

    @Autowired
    public TransactionQueryService(TransactionMapper transactionMapper,
                                    @Value("${transaction.ledger.zone:Asia/Seoul}") String zoneId) {
        this(transactionMapper, Clock.system(ZoneId.of(zoneId)));
    }

    TransactionQueryService(TransactionMapper transactionMapper, Clock clock) {
        this.transactionMapper = transactionMapper;
        this.clock = clock;
    }

    /**
     * 데이터가 있는 가장 이른 달(없으면 현재월)~현재월 범위를 오름차순(과거→현재)으로 돌려준다.
     * 프론트 월 이동 버튼이 인덱스 기준이라 순서가 중요하다.
     *
     * 가입월(tbl_user.created_at) 기준이 아니다 — CODEF 동기화는 계좌 연동 시점부터 과거 거래를
     * 끌어오므로 거래 데이터가 가입일보다 앞설 수 있다(로컬 시드 db/seed_local_demo.sql 이 실계정에도
     * 과거 7개월치를 백필하면서 tbl_user.created_at 은 건드리지 않아 실제로 이 어긋남이 재현됐다.
     * 가입월 기준이면 범위가 "이번 달"뿐이라 월 이동 버튼이 항상 비활성화됐다 — 2026-08-15 버그 수정).
     */
    @Transactional(readOnly = true)
    public TransactionMonthsDto getAvailableMonths(long userId) {
        YearMonth currentMonth = YearMonth.now(clock);
        Set<String> dataMonths = new HashSet<>(transactionMapper.findDistinctDataMonths(userId));
        YearMonth startMonth = earliestMonth(dataMonths, currentMonth);

        List<TransactionMonthDto> months = new ArrayList<>();
        for (YearMonth month = startMonth; !month.isAfter(currentMonth); month = month.plusMonths(1)) {
            String value = month.toString();
            months.add(TransactionMonthDto.builder()
                    .value(value)
                    .hasData(dataMonths.contains(value))
                    .build());
        }
        return TransactionMonthsDto.builder().months(months).build();
    }

    /** yearMonth 가 비어 있으면 기간 제한 없이 전체 거래를 반환한다(검색 화면 전용, summary=null). */
    @Transactional(readOnly = true)
    public TransactionListResponseDto getTransactions(long userId, String rawYearMonth) {
        if (rawYearMonth == null || rawYearMonth.isBlank()) {
            List<TransactionListItemDto> items = mapRows(transactionMapper.findTransactionRows(userId, null, null));
            return TransactionListResponseDto.builder()
                    .period(null)
                    .summary(null)
                    .transactions(items)
                    .build();
        }

        YearMonth yearMonth = parseYearMonth(rawYearMonth);
        YearMonth currentMonth = YearMonth.now(clock);
        Set<String> dataMonths = new HashSet<>(transactionMapper.findDistinctDataMonths(userId));
        YearMonth earliestDataMonth = earliestMonth(dataMonths, currentMonth);
        if (yearMonth.isBefore(earliestDataMonth)) {
            throw new BusinessException("LEDGER_NOT_AVAILABLE", "거래 데이터가 있는 월만 조회할 수 있습니다.");
        }

        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.plusMonths(1).atDay(1);

        List<TransactionListRow> rows = transactionMapper.findTransactionRows(userId, startDate, endDate);
        List<TransactionListItemDto> items = mapRows(rows);

        BigDecimal totalSpent = nonNegative(transactionMapper.sumNetConsumption(userId, startDate, endDate));
        BigDecimal totalDeposit = nonNegative(transactionMapper.sumIncome(userId, startDate, endDate));

        LocalDate prevStart = yearMonth.minusMonths(1).atDay(1);
        BigDecimal previousSpent = nonNegative(transactionMapper.sumNetConsumption(userId, prevStart, startDate));
        BigDecimal monthOverMonthRate = calculateChangeRate(totalSpent, previousSpent);

        List<String> paymentMethods = new ArrayList<>();
        Set<String> seenPaymentMethods = new LinkedHashSet<>();
        for (TransactionListItemDto item : items) {
            if (seenPaymentMethods.add(item.getPaymentMethod())) {
                paymentMethods.add(item.getPaymentMethod());
            }
        }

        LedgerSummaryDto summary = LedgerSummaryDto.builder()
                .period(rawYearMonth)
                .totalSpent(totalSpent)
                .totalDeposit(totalDeposit)
                .monthOverMonthRate(monthOverMonthRate)
                .paymentMethods(paymentMethods)
                .build();

        return TransactionListResponseDto.builder()
                .period(rawYearMonth)
                .summary(summary)
                .transactions(items)
                .build();
    }

    /**
     * 홈 「오늘 쓴 돈」 카드. 오늘·이번 달 순소비와 어제 대비 증감률을 함께 돌려준다.
     *
     * 집계는 장부와 같은 sumNetConsumption 을 그대로 쓴다 — 카드에 「거래내역」 입구가 붙어 있어
     * 사용자가 한 탭 만에 두 숫자를 대조한다. 미션·그룹챌린지 집계는 여기에 direction='OUT' 을 더 걸어
     * 페이머니 소비가 빠지므로 값이 다를 수 있다(기존 시스템의 알려진 불일치. 이번 범위 밖).
     *
     * 「이번 달」은 month-to-date 가 아니라 월 전체다 — 장부 월 요약(getTransactions)이 그렇게 계산하므로
     * 미래 날짜 거래가 한 행이라도 생기면 한 탭 거리의 두 숫자가 어긋난다.
     */
    @Transactional(readOnly = true)
    public DailySpendingSummaryDto getDailySpendingSummary(long userId) {
        LocalDate today = LocalDate.now(clock);
        LocalDate tomorrow = today.plusDays(1);
        LocalDate yesterday = today.minusDays(1);
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate nextMonthStart = monthStart.plusMonths(1);

        BigDecimal todayAmount = nonNegative(transactionMapper.sumNetConsumption(userId, today, tomorrow));
        BigDecimal yesterdayAmount = nonNegative(transactionMapper.sumNetConsumption(userId, yesterday, today));
        BigDecimal monthAmount = nonNegative(transactionMapper.sumNetConsumption(userId, monthStart, nextMonthStart));

        return DailySpendingSummaryDto.builder()
                .date(today.toString())
                .todayAmount(todayAmount)
                .monthAmount(monthAmount)
                .changeRate(calculateDailyChangeRate(todayAmount, yesterdayAmount))
                .build();
    }

    private List<TransactionListItemDto> mapRows(List<TransactionListRow> rows) {
        List<TransactionListItemDto> items = new ArrayList<>();
        for (TransactionListRow row : rows) {
            items.add(TransactionListItemDto.builder()
                    .id(row.getId())
                    .date(row.getTrDate().toString())
                    .merchant(row.getMerchantName())
                    .category(row.getCategoryName() != null ? row.getCategoryName() : "미분류")
                    .paymentMethod(resolvePaymentMethod(row))
                    .classification(row.getClassification())
                    .amount(resolveSignedAmount(row))
                    .isRefund(row.isRefund())
                    .build());
        }
        return items;
    }

    /**
     * 결제수단 라벨. 규칙은 {@link PaymentMethodLabels} 가 소유한다 —
     * 그룹챌린지 변론 화면(이슈 #170)이 같은 라벨을 써야 해서 {@code common} 으로 옮겼다.
     */
    private String resolvePaymentMethod(TransactionListRow row) {
        return PaymentMethodLabels.resolve(row.getSourceType(), row.getDirection(),
                row.getCardInstitutionName(), row.getAccountBankName());
    }

    /**
     * CONSUMPTION 은 음수, INCOME 은 양수. 환불(is_refund=1)은 지출을 상계하므로 양수로 뒤집는다
     * (refunded_amount 가 없으면 amount 전액 환불로 본다). TRANSFER 는 전체 탭에는 보이되
     * 지출/입금 합계(sumNetConsumption/sumIncome)에는 안 잡히므로 direction 기준 부호만 매긴다.
     */
    private BigDecimal resolveSignedAmount(TransactionListRow row) {
        BigDecimal amount = row.getAmount();
        if ("CONSUMPTION".equals(row.getClassification())) {
            if (row.isRefund()) {
                BigDecimal refunded = row.getRefundedAmount();
                return refunded != null ? refunded : amount;
            }
            return amount.negate();
        }
        if ("INCOME".equals(row.getClassification())) {
            return amount;
        }
        return "OUT".equals(row.getDirection()) ? amount.negate() : amount;
    }

    private YearMonth earliestMonth(Set<String> dataMonths, YearMonth fallback) {
        return dataMonths.stream()
                .map(YearMonth::parse)
                .min(Comparator.naturalOrder())
                .orElse(fallback);
    }

    private YearMonth parseYearMonth(String rawYearMonth) {
        if (!YEAR_MONTH_PATTERN.matcher(rawYearMonth).matches()) {
            throw new BusinessException("INVALID_REQUEST", "yearMonth는 YYYY-MM 형식이어야 합니다.");
        }
        try {
            return YearMonth.parse(rawYearMonth);
        } catch (DateTimeParseException ex) {
            throw new BusinessException("INVALID_REQUEST", "yearMonth는 YYYY-MM 형식이어야 합니다.");
        }
    }

    private BigDecimal nonNegative(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    /** 전달 지출이 0(또는 비교 대상 없음)이면 0. */
    private BigDecimal calculateChangeRate(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.signum() == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return current.subtract(previous)
                .multiply(ONE_HUNDRED)
                .divide(previous, 2, RoundingMode.HALF_UP);
    }

    /**
     * 어제 지출이 0 이하면 비율을 낼 수 없어 null. 프론트가 null 을 「문구 생략」으로 읽는다
     * (utils/home.js getHomeSpendingChange).
     *
     * 음수(어제 환불이 소비보다 큰 날)도 null 이다 — 음수 분모는 부호를 뒤집어
     * 더 썼는데 「적게 쓰는 중」이라고 말하게 된다.
     *
     * 장부의 calculateChangeRate 는 같은 상황에서 0 을 돌려준다. 계약이 달라 일부러 분리했다 —
     * 합치면 어제 0원·오늘 5만원인 사용자에게 「어제와 비슷하게 쓰는 중」이 뜬다.
     */
    private BigDecimal calculateDailyChangeRate(BigDecimal today, BigDecimal yesterday) {
        if (yesterday == null || yesterday.signum() <= 0) {
            return null;
        }
        return today.subtract(yesterday)
                .multiply(ONE_HUNDRED)
                .divide(yesterday, 2, RoundingMode.HALF_UP);
    }
}
