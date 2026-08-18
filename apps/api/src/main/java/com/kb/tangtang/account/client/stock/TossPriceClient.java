package com.kb.tangtang.account.client.stock;

import com.kb.tangtang.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 토스증권 현재가 조회.
 *
 * ⚠ Set 은 구조상 이미 중복이 없지만 반복 순서는 구현체마다 다르고(HashSet 등) Set.of() 는
 *   실행마다 무작위다(JDK Immutable Collections). 요청 URL·로그가 호출마다 들쭉날쭉하지 않도록
 *   LinkedHashSet 으로 한 번 복사해 순서를 고정한다.
 * ⚠ 토스 문서: 한 번에 최대 200개까지만 허용된다. 그보다 많으면 여러 번 나눠 부른다 —
 *   호출부가 몇 개를 넘길지 신경 쓰지 않아도 되게 이 클라이언트가 흡수한다.
 * ⚠ 토큰이 아직 없으면(TossAuthScheduler 첫 갱신 전·갱신 실패로 만료) 호출조차 하지 않고
 *   즉시 실패시킨다 — 호출부가 "시세 조회 잠시 불가"로 다루고 DB 저장값으로 대체할 수 있게 한다.
 */
public class TossPriceClient {

    private static final Logger log = LoggerFactory.getLogger(TossPriceClient.class);
    private static final String PRICE_URL = "https://openapi.tossinvest.com/api/v1/prices";
    private static final int MAX_SYMBOLS_PER_REQUEST = 200;

    private final RestTemplate restTemplate;
    private final TossTokenHolder tokenHolder;

    public TossPriceClient(RestTemplate restTemplate, TossTokenHolder tokenHolder) {
        this.restTemplate = restTemplate;
        this.tokenHolder = tokenHolder;
    }

    /**
     * 요청한 심볼 중 응답에 실제로 담겨 온 것만 돌려준다. 못 찾은 심볼은 그냥 빠진다 —
     * 호출부는 Map 에 키가 없는 것으로 "가격을 못 받았다"를 판정하면 된다.
     */
    public Map<String, TossPriceDto> fetchPrices(Set<String> symbols) {
        Set<String> distinct = new LinkedHashSet<>(symbols);
        if (distinct.isEmpty()) {
            return Map.of();
        }

        String token = tokenHolder.currentToken();
        if (token == null) {
            throw new BusinessException("EXTERNAL_API_UNAVAILABLE", "토스 시세 조회가 잠시 준비 중이에요.");
        }

        Map<String, TossPriceDto> merged = new LinkedHashMap<>();
        for (List<String> chunk : chunk(distinct, MAX_SYMBOLS_PER_REQUEST)) {
            merged.putAll(fetchChunk(chunk, token));
        }
        return merged;
    }

    private Map<String, TossPriceDto> fetchChunk(List<String> symbols, String token) {
        URI uri = UriComponentsBuilder.fromHttpUrl(PRICE_URL)
                .queryParam("symbols", String.join(",", symbols))
                .build()
                .encode()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        Map<?, ?> response;
        try {
            response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), Map.class)
                    .getBody();
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                /* 요청한 심볼을 하나도 못 찾았을 때(상폐 등). 이 묶음만 빈 결과로 넘기고
                   나머지 청크·호출부 흐름은 계속 진행한다 — 예외로 전체를 막을 이유가 없다. */
                log.warn("토스 시세 조회 종목을 찾지 못함 symbols={}", symbols);
                return Map.of();
            }
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.warn("토스 시세 조회 레이트리밋 초과 symbols={}", symbols);
            } else {
                log.error("토스 시세 조회 실패 status={} symbols={}", e.getStatusCode(), symbols);
            }
            throw new BusinessException("EXTERNAL_API_UNAVAILABLE", "토스 시세 조회에 실패했어요.");
        } catch (RestClientException e) {
            throw new BusinessException("EXTERNAL_API_UNAVAILABLE", "토스 시세 서버에 연결하지 못했어요.");
        }

        Map<String, TossPriceDto> result = new LinkedHashMap<>();
        for (Object item : asList(response == null ? null : response.get("result"))) {
            /*
             * item 이 Map 이 아닐 수 있다(문서에 없는 응답 모양·null 항목 등, QA 지적사항) — 캐스트를
             * 그대로 하면 ClassCastException 이 호출부(InvestmentPriceRefresher)의 catch 를 뚫고
             * 나가 요청 전체를 500 으로 만든다. 여기서 걸러 그 항목만 건너뛴다.
             */
            if (!(item instanceof Map)) {
                log.warn("토스 시세 응답에 예상 밖 항목이 있어 건너뛴다: {}", item);
                continue;
            }
            Map<?, ?> row = (Map<?, ?>) item;
            String symbol = TossJsonSupport.string(row.get("symbol"));
            if (symbol == null) {
                continue;
            }
            result.put(symbol, TossPriceDto.builder()
                    .symbol(symbol)
                    .lastPrice(decimal(row.get("lastPrice")))
                    .currency(TossJsonSupport.string(row.get("currency")))
                    .timestamp(TossJsonSupport.string(row.get("timestamp")))
                    .build());
        }
        return result;
    }

    private static List<List<String>> chunk(Collection<String> values, int size) {
        List<List<String>> chunks = new ArrayList<>();
        List<String> current = new ArrayList<>(size);
        for (String value : values) {
            current.add(value);
            if (current.size() == size) {
                chunks.add(current);
                current = new ArrayList<>(size);
            }
        }
        if (!current.isEmpty()) {
            chunks.add(current);
        }
        return chunks;
    }

    private static List<?> asList(Object value) {
        return TossJsonSupport.asList(value);
    }

    /**
     * ⚠ 파싱 실패·값 없음이면 {@code BigDecimal.ZERO} 가 아니라 **null** 을 돌려준다(QA 지적사항으로
     *   명시). CodefFinancialDataClient·MockFinancialSyncClient 의 동명 헬퍼는 ZERO 를 돌려주지만,
     *   여기서 그렇게 하면 "가격을 못 받았다"가 "가격이 0원이다"로 둔갑해 InvestmentPriceRefresher 가
     *   진짜 0원인 것처럼 DB 에 그대로 써버린다 — 실패는 실패로 남겨야 호출부가 저장된 값을 지킨다.
     */
    private static BigDecimal decimal(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
