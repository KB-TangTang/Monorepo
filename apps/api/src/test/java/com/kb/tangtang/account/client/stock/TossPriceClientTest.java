package com.kb.tangtang.account.client.stock;

import com.kb.tangtang.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TossPriceClientTest {

    private static final String PRICE_URL = "https://openapi.tossinvest.com/api/v1/prices";

    private TossTokenHolder holderWithToken(String token) {
        TossTokenHolder holder = mock(TossTokenHolder.class);
        when(holder.currentToken()).thenReturn(token);
        return holder;
    }

    @Test
    @DisplayName("심볼을 콤마로 묶어 요청하고 Bearer 토큰을 싣는다")
    void fetchesPricesWithBearerToken() {
        /* Set.of() 는 반복 순서가 실행마다 무작위라 쿼리스트링 순서를 단정할 수 없다.
           LinkedHashSet 으로 넘겨 순서를 고정한다(TossPriceClient 도 LinkedHashSet 으로 복사해 순서를 지킨다). */
        Set<String> symbols = new LinkedHashSet<>();
        symbols.add("005930");
        symbols.add("000660");

        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo(PRICE_URL + "?symbols=005930,000660"))
                .andExpect(header("Authorization", "Bearer tok-1"))
                .andRespond(withSuccess(
                        "{\"result\":[" +
                                "{\"symbol\":\"005930\",\"timestamp\":\"2026-03-25T09:30:00.123+09:00\"," +
                                "\"lastPrice\":\"72000\",\"currency\":\"KRW\"}," +
                                "{\"symbol\":\"000660\",\"timestamp\":\"2026-03-25T09:30:00.123+09:00\"," +
                                "\"lastPrice\":\"210000\",\"currency\":\"KRW\"}]}",
                        MediaType.APPLICATION_JSON));

        TossPriceClient client = new TossPriceClient(restTemplate, holderWithToken("tok-1"));
        Map<String, TossPriceDto> prices = client.fetchPrices(symbols);

        assertEquals(new BigDecimal("72000"), prices.get("005930").getLastPrice());
        assertEquals("KRW", prices.get("005930").getCurrency());
        assertEquals(new BigDecimal("210000"), prices.get("000660").getLastPrice());
        server.verify();
    }

    @Test
    @DisplayName("유효 토큰이 없으면 호출 자체를 하지 않고 실패한다")
    void failsFastWithoutCallingServerWhenNoToken() {
        TossTokenHolder holder = holderWithToken(null);
        TossPriceClient client = new TossPriceClient(new RestTemplate(), holder);

        assertThrows(BusinessException.class, () -> client.fetchPrices(Set.of("005930")));
    }

    @Test
    @DisplayName("빈 심볼 집합은 호출 없이 빈 결과를 돌려준다")
    void returnsEmptyForEmptySymbols() {
        TossTokenHolder holder = mock(TossTokenHolder.class);
        TossPriceClient client = new TossPriceClient(new RestTemplate(), holder);

        Map<String, TossPriceDto> prices = client.fetchPrices(Set.of());

        assertTrue(prices.isEmpty());
        verifyNoInteractions(holder);
    }

    @Test
    @DisplayName("종목을 찾지 못하면(404) 예외 대신 빈 결과를 돌려준다")
    void returnsEmptyOnNotFound() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo(PRICE_URL + "?symbols=999999"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"code\":\"NOT_FOUND\",\"message\":\"종목을 찾을 수 없어요\"}}"));

        TossPriceClient client = new TossPriceClient(restTemplate, holderWithToken("tok-1"));
        Map<String, TossPriceDto> prices = client.fetchPrices(Set.of("999999"));

        assertTrue(prices.isEmpty());
    }

    @Test
    @DisplayName("레이트리밋(429)이면 BusinessException으로 바뀐다")
    void wrapsTooManyRequestsAsBusinessException() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo(PRICE_URL + "?symbols=005930"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS));

        TossPriceClient client = new TossPriceClient(restTemplate, holderWithToken("tok-1"));

        assertThrows(BusinessException.class, () -> client.fetchPrices(Set.of("005930")));
    }

    @Test
    @DisplayName("서버 오류(500)면 BusinessException으로 바뀐다")
    void wrapsServerErrorAsBusinessException() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo(PRICE_URL + "?symbols=005930")).andRespond(withServerError());

        TossPriceClient client = new TossPriceClient(restTemplate, holderWithToken("tok-1"));

        assertThrows(BusinessException.class, () -> client.fetchPrices(Set.of("005930")));
    }

    @Test
    @DisplayName("result 배열에 문서에 없는 항목(null 등)이 섞여도 예외 없이 그 항목만 건너뛴다")
    void skipsMalformedResultEntriesInsteadOfThrowing() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        Set<String> symbols = new LinkedHashSet<>();
        symbols.add("005930");
        symbols.add("999999");
        server.expect(requestTo(PRICE_URL + "?symbols=005930,999999"))
                .andRespond(withSuccess(
                        "{\"result\":[" +
                                "{\"symbol\":\"005930\",\"lastPrice\":\"72000\",\"currency\":\"KRW\"}," +
                                "null]}",
                        MediaType.APPLICATION_JSON));

        TossPriceClient client = new TossPriceClient(restTemplate, holderWithToken("tok-1"));
        Map<String, TossPriceDto> prices = client.fetchPrices(symbols);

        assertEquals(new BigDecimal("72000"), prices.get("005930").getLastPrice());
        assertTrue(prices.get("999999") == null);
    }
}
