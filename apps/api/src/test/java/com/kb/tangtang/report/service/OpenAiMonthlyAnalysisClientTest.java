package com.kb.tangtang.report.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.report.domain.MonthlyAiAnalysisCategory;
import com.kb.tangtang.report.domain.MonthlyAiAnalysisInput;
import com.kb.tangtang.report.dto.MonthlyAiAnalysisDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;

class OpenAiMonthlyAnalysisClientTest {

    private static final String RESPONSES_URL = "https://api.openai.test/v1/responses";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockRestServiceServer server;
    private OpenAiMonthlyAnalysisClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        client = new OpenAiMonthlyAnalysisClient(restTemplate, objectMapper, "test-key", "gpt-5-nano",
                RESPONSES_URL, 2048, "low");
    }

    @Test
    @DisplayName("Responses API에 비식별 집계와 분리된 절약 비유 스키마를 보내고 문장을 조합한다")
    void sendsResponsesRequestAndComposesSavingsAnalogy() throws Exception {
        String output = objectMapper.writeValueAsString(analogyOutput(
                List.of("식비 지출을 한 번 점검해 보세요."), "피자", 30000, 4, "판"));
        String response = "{\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":"
                + objectMapper.writeValueAsString(output) + "}]}]}";

        server.expect(once(), requestTo(RESPONSES_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"model\":\"gpt-5-nano\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"store\":false")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"max_output_tokens\":2048")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"reasoning\":{\"effort\":\"low\"}")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"savingsAnalogy\":{\"anyOf\":[")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"savingsAnalogyItem\":{\"type\":\"string\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"savingsAnalogyQuantity\":{\"type\":\"integer\",\"minimum\":3,\"maximum\":30")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"savingsAnalogyReferenceUnitPrice\":{\"type\":\"integer\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"savingsAnalogyUnit\":{\"type\":\"string\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Do not use a fixed or default item")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("피자 | 30000 | 판")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Never use products whose price differs widely by brand, model, or option")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("prefer 5 through 15 whenever possible")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\\\"savingsAmount\\\":128000")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("userId"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("merchantName"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("accountNumber"))))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        MonthlyAiAnalysisDto result = client.generate(inputWithSavings());

        assertEquals(List.of("식비 지출을 한 번 점검해 보세요."), result.getFeedbacks());
        assertEquals("이번달 아낀 128,000원은 피자 4판", result.getSavingsAnalogy());
        server.verify();
    }

    @Test
    @DisplayName("첫 리포트는 savingsAnalogy가 null일 때만 응답을 수용한다")
    void acceptsNullSavingsAnalogyForFirstReport() throws Exception {
        Map<String, Object> outputPayload = new LinkedHashMap<>();
        outputPayload.put("feedbacks", List.of("Verify this month's spending categories."));
        outputPayload.put("savingsAnalogy", null);
        String output = objectMapper.writeValueAsString(outputPayload);
        String response = "{\"output_text\":" + objectMapper.writeValueAsString(output) + "}";

        server.expect(requestTo(RESPONSES_URL))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"savingsAnalogy\":{\"type\":\"null\"}")))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        MonthlyAiAnalysisDto result = client.generate(MonthlyAiAnalysisInput.builder()
                .yearMonth("2026-03")
                .currentMonthSpent(new BigDecimal("50000"))
                .previousMonthSpent(null)
                .hasPreviousComparison(false)
                .savingsAmount(BigDecimal.ZERO)
                .categories(List.of())
                .build());

        assertEquals(List.of("Verify this month's spending categories."), result.getFeedbacks());
        assertNull(result.getSavingsAnalogy());
        server.verify();
    }

    @Test
    void rejectsSavingsAnalogyWithZeroQuantity() throws Exception {
        String output = objectMapper.writeValueAsString(analogyOutput(
                List.of("Review this month's spending."), "피자", 32000, 0, "판"));
        String response = "{\"output_text\":" + objectMapper.writeValueAsString(output) + "}";

        server.expect(requestTo(RESPONSES_URL))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        AiProviderException exception = assertThrows(AiProviderException.class,
                () -> client.generate(inputWithSavings()));

        assertEquals("AI_PROVIDER_UNAVAILABLE", exception.getCode());
        server.verify();
    }

    @Test
    @DisplayName("피드백이 없거나 네 개 이상이거나 빈 문자열이면 응답을 거부한다")
    void rejectsInvalidFeedbackCountsAndBlankMessage() throws Exception {
        assertInvalidAnalysisOutput(List.of(""));
        assertInvalidAnalysisOutput(List.of());
        assertInvalidAnalysisOutput(List.of("하나", "둘", "셋", "넷"));
    }

    @Test
    @DisplayName("필수 필드가 없거나 JSON이 아니면 응답을 거부한다")
    void rejectsMissingOrMalformedStructuredOutput() throws Exception {
        String missingField = objectMapper.writeValueAsString(Map.of(
                "feedbacks", List.of("이번 달 소비를 점검해 보세요."),
                "savingsAnalogy", "이번달 아낀 128,000원은 카페라떼 26잔"));
        server.expect(requestTo(RESPONSES_URL))
                .andRespond(withSuccess("{\"output_text\":" + objectMapper.writeValueAsString(missingField) + "}",
                        MediaType.APPLICATION_JSON));

        AiProviderException missingFieldException = assertThrows(AiProviderException.class,
                () -> client.generate(inputWithSavings()));
        assertEquals("AI_PROVIDER_UNAVAILABLE", missingFieldException.getCode());
        server.verify();

        server.reset();
        server.expect(requestTo(RESPONSES_URL))
                .andRespond(withSuccess("{\"output_text\":\"not-json\"}", MediaType.APPLICATION_JSON));

        AiProviderException malformedException = assertThrows(AiProviderException.class,
                () -> client.generate(inputWithSavings()));
        assertEquals("AI_PROVIDER_UNAVAILABLE", malformedException.getCode());
        server.verify();
    }

    @Test
    @DisplayName("절약 비유의 품목ㆍ수량ㆍ단위가 유효하지 않으면 응답을 거부한다")
    void rejectsInvalidSavingsAnalogyParts() throws Exception {
        assertInvalidSavingsAnalogyParts("피자", 30000, 4, "조각");
        assertInvalidSavingsAnalogyParts("", 30000, 4, "판");
        assertInvalidSavingsAnalogyParts("피자 네판", 30000, 4, "판");
        assertInvalidSavingsAnalogyParts("피자", 30000, 31, "판");
        assertInvalidSavingsAnalogyParts("피자", 30000, 5, "판");
        assertInvalidSavingsAnalogyParts("피자", 0, 4, "판");
        assertInvalidSavingsAnalogyParts("카페라떼", 4500, 28, "잔");
        assertInvalidSavingsAnalogyParts("백팩", 32000, 4, "개");
    }

    @Test
    @DisplayName("외부 호출 timeout은 503으로 변환하며 자동 재시도하지 않는다")
    void convertsTimeoutToUnavailableWithoutAutomaticRetry() {
        server.expect(once(), requestTo(RESPONSES_URL))
                .andRespond(withException(new java.net.SocketTimeoutException("read timed out")));

        AiProviderException exception = assertThrows(AiProviderException.class,
                () -> client.generate(inputWithSavings()));

        assertEquals("AI_PROVIDER_UNAVAILABLE", exception.getCode());
        server.verify();
    }

    private void assertInvalidAnalysisOutput(List<String> feedbacks) throws Exception {
        String output = objectMapper.writeValueAsString(analogyOutput(feedbacks, "피자", 30000, 4, "판"));
        server.expect(requestTo(RESPONSES_URL))
                .andRespond(withSuccess("{\"output_text\":" + objectMapper.writeValueAsString(output) + "}",
                        MediaType.APPLICATION_JSON));

        AiProviderException exception = assertThrows(AiProviderException.class,
                () -> client.generate(inputWithSavings()));

        assertEquals("AI_PROVIDER_UNAVAILABLE", exception.getCode());
        server.verify();
        server.reset();
    }

    private void assertInvalidSavingsAnalogyParts(String item, Number referenceUnitPrice,
                                                   Number quantity, String unit) throws Exception {
        String output = objectMapper.writeValueAsString(analogyOutput(
                List.of("이번 달 소비를 점검해 보세요."), item, referenceUnitPrice, quantity, unit));
        server.expect(requestTo(RESPONSES_URL))
                .andRespond(withSuccess("{\"output_text\":" + objectMapper.writeValueAsString(output) + "}",
                        MediaType.APPLICATION_JSON));

        AiProviderException exception = assertThrows(AiProviderException.class,
                () -> client.generate(inputWithSavings()));

        assertEquals("AI_PROVIDER_UNAVAILABLE", exception.getCode());
        server.verify();
        server.reset();
    }

    private Map<String, Object> analogyOutput(List<String> feedbacks, String item, Number referenceUnitPrice,
                                              Number quantity, String unit) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("feedbacks", feedbacks);
        Map<String, Object> savingsAnalogy = new LinkedHashMap<>();
        savingsAnalogy.put("savingsAnalogyItem", item);
        savingsAnalogy.put("savingsAnalogyQuantity", quantity);
        savingsAnalogy.put("savingsAnalogyReferenceUnitPrice", referenceUnitPrice);
        savingsAnalogy.put("savingsAnalogyUnit", unit);
        output.put("savingsAnalogy", savingsAnalogy);
        return output;
    }

    private MonthlyAiAnalysisInput inputWithSavings() {
        return MonthlyAiAnalysisInput.builder()
                .yearMonth("2026-07")
                .currentMonthSpent(new BigDecimal("1284000"))
                .previousMonthSpent(new BigDecimal("1412000"))
                .hasPreviousComparison(true)
                .savingsAmount(new BigDecimal("128000"))
                .categories(List.of(MonthlyAiAnalysisCategory.builder()
                        .parentCategoryName("식비")
                        .categoryName("카페/간식")
                        .amount(new BigDecimal("125000"))
                        .build()))
                .build();
    }
}
