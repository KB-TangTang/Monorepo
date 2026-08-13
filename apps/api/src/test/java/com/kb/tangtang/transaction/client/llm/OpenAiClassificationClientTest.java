package com.kb.tangtang.transaction.client.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.transaction.domain.Category;
import com.kb.tangtang.transaction.domain.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAiClassificationClientTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private OpenAiClassificationClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        client = new OpenAiClassificationClient(restTemplate, new ObjectMapper(),
                "sk-test-key", "https://api.openai.com", "gpt-4o-mini");
    }

    private static Transaction tx(long id, String merchantName) {
        return Transaction.builder().id(id).merchantName(merchantName)
                .amount(new BigDecimal("15000")).description1("결제").build();
    }

    @Test
    @DisplayName("Authorization 헤더에 Bearer 토큰을 싣고 chat/completions 에 POST 한다")
    void sendsBearerTokenToCorrectEndpoint() {
        mockServer.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer sk-test-key"))
                .andRespond(withSuccess(
                        "{\"choices\":[{\"message\":{\"content\":\"{\\\"results\\\":[{\\\"transactionId\\\":1,\\\"categoryId\\\":5}]}\"}}]}",
                        MediaType.APPLICATION_JSON));

        List<CategoryAssignmentDto> result = client.classify(
                List.of(tx(1L, "스타벅스")),
                List.of(Category.builder().id(5L).categoryName("카페/간식").parentId(1L).build()));

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getTransactionId());
        assertEquals(5L, result.get(0).getCategoryId());
        mockServer.verify();
    }

    @Test
    @DisplayName("확신이 없으면 categoryId 가 null 인 결과를 그대로 돌려준다")
    void nullCategoryIdMeansUnclassifiable() {
        mockServer.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andRespond(withSuccess(
                        "{\"choices\":[{\"message\":{\"content\":\"{\\\"results\\\":[{\\\"transactionId\\\":2,\\\"categoryId\\\":null}]}\"}}]}",
                        MediaType.APPLICATION_JSON));

        List<CategoryAssignmentDto> result = client.classify(
                List.of(tx(2L, "정체불명상점")),
                List.of(Category.builder().id(5L).categoryName("카페/간식").parentId(1L).build()));

        assertEquals(1, result.size());
        assertNull(result.get(0).getCategoryId());
    }

    @Test
    @DisplayName("요청 본문에 model·거래 id·카테고리 id 가 포함된다")
    void requestBodyContainsModelAndData() throws Exception {
        mockServer.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andExpect(request -> {
                    String body = request.getBody().toString();
                    org.junit.jupiter.api.Assertions.assertTrue(body.contains("\"model\":\"gpt-4o-mini\""));
                    org.junit.jupiter.api.Assertions.assertTrue(body.contains("\"json_schema\""));
                })
                .andRespond(withSuccess(
                        "{\"choices\":[{\"message\":{\"content\":\"{\\\"results\\\":[]}\"}}]}",
                        MediaType.APPLICATION_JSON));

        client.classify(List.of(tx(1L, "스타벅스")),
                List.of(Category.builder().id(5L).categoryName("카페/간식").parentId(1L).build()));

        mockServer.verify();
    }

    @Test
    @DisplayName("모델이 거부(refusal)하면 빈 결과가 아니라 예외를 던진다")
    void throwsWhenModelRefuses() {
        mockServer.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andRespond(withSuccess(
                        "{\"choices\":[{\"message\":{\"refusal\":\"I can't help with that.\"},"
                                + "\"finish_reason\":\"stop\"}]}",
                        MediaType.APPLICATION_JSON));

        /* 그냥 두면 content 가 비어 "0건 분류" 로 읽혀 작업이 COMPLETED 로 마감된다 — 실패로 올려야 한다. */
        BusinessException e = assertThrows(BusinessException.class, () -> client.classify(
                List.of(tx(1L, "스타벅스")),
                List.of(Category.builder().id(5L).categoryName("카페/간식").parentId(1L).build())));

        assertEquals("EXTERNAL_API_ERROR", e.getCode());
        assertTrue(e.getMessage().contains("거부"));
    }

    @Test
    @DisplayName("finish_reason 이 stop 이 아니면(응답 잘림) 예외를 던진다")
    void throwsWhenResponseTruncated() {
        mockServer.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andRespond(withSuccess(
                        "{\"choices\":[{\"message\":{\"content\":\"{\\\"results\\\":[]}\"},"
                                + "\"finish_reason\":\"length\"}]}",
                        MediaType.APPLICATION_JSON));

        BusinessException e = assertThrows(BusinessException.class, () -> client.classify(
                List.of(tx(1L, "스타벅스")),
                List.of(Category.builder().id(5L).categoryName("카페/간식").parentId(1L).build())));

        assertEquals("EXTERNAL_API_ERROR", e.getCode());
        assertTrue(e.getMessage().contains("length"));
    }

    @Test
    @DisplayName("finish_reason 이 stop 이면 정상 파싱한다")
    void parsesNormallyWhenFinishReasonIsStop() {
        mockServer.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andRespond(withSuccess(
                        "{\"choices\":[{\"message\":{\"content\":\"{\\\"results\\\":"
                                + "[{\\\"transactionId\\\":1,\\\"categoryId\\\":5}]}\",\"refusal\":null},"
                                + "\"finish_reason\":\"stop\"}]}",
                        MediaType.APPLICATION_JSON));

        List<CategoryAssignmentDto> result = client.classify(
                List.of(tx(1L, "스타벅스")),
                List.of(Category.builder().id(5L).categoryName("카페/간식").parentId(1L).build()));

        assertEquals(1, result.size());
        assertEquals(5L, result.get(0).getCategoryId());
    }
}
