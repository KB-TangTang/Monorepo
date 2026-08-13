package com.kb.tangtang.transaction.client.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.transaction.domain.Category;
import com.kb.tangtang.transaction.domain.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * 이슈 #147 후속 — OpenAI Chat Completions API 로 거래를 표준 카테고리로 분류한다.
 *
 * Structured Outputs(response_format.type=json_schema, strict=true)로 응답 모양을
 * {"results":[{"transactionId":.., "categoryId":..}, ...]} 로 강제한다 — 모델이 자유 텍스트를
 * 섞어 보내 파싱이 깨지는 사고를 막는다. categoryId 값 자체가 실제 존재하는 카테고리인지는
 * 이 클래스의 책임이 아니다 — 호출부(LlmCategorizationProcessingServiceImpl)가 한 번 더 검증한다.
 */
@Component
public class OpenAiClassificationClient implements LlmClassificationClient {

    private static final String CHAT_COMPLETIONS_PATH = "/v1/chat/completions";

    private static final String SYSTEM_PROMPT =
            "당신은 한국 개인 금융 거래를 표준 소비 카테고리로 분류하는 어시스턴트입니다. "
            + "반드시 사용자가 제공한 카테고리 목록의 id 중 하나만 선택하세요. "
            + "목록에 없는 id를 만들어내지 마세요. 확신이 서지 않으면 categoryId를 null로 두세요.";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    @Autowired
    public OpenAiClassificationClient(RestTemplate restTemplate,
                                      ObjectMapper objectMapper,
                                      @Value("${openai.api.key}") String apiKey,
                                      @Value("${openai.api.base-url}") String baseUrl,
                                      @Value("${openai.api.model}") String model) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
    }

    @Override
    public List<CategoryAssignmentDto> classify(List<Transaction> transactions, List<Category> categories) {
        ObjectNode requestBody = buildRequestBody(transactions, categories);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);
        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(baseUrl + CHAT_COMPLETIONS_PATH, HttpMethod.POST, entity, String.class);
        } catch (Exception e) {
            throw new BusinessException("EXTERNAL_API_ERROR", "OpenAI 호출 실패: " + e.getMessage());
        }

        return parseResults(response.getBody());
    }

    private ObjectNode buildRequestBody(List<Transaction> transactions, List<Category> categories) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);

        ArrayNode messages = root.putArray("messages");
        ObjectNode systemMessage = messages.addObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", SYSTEM_PROMPT);

        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", buildUserContent(transactions, categories));

        root.set("response_format", buildResponseFormat());
        return root;
    }

    private String buildUserContent(List<Transaction> transactions, List<Category> categories) {
        ObjectNode payload = objectMapper.createObjectNode();

        ArrayNode categoryArray = payload.putArray("categories");
        for (Category category : categories) {
            ObjectNode node = categoryArray.addObject();
            node.put("id", category.getId());
            node.put("name", category.getCategoryName());
        }

        ArrayNode transactionArray = payload.putArray("transactions");
        for (Transaction tx : transactions) {
            ObjectNode node = transactionArray.addObject();
            node.put("transactionId", tx.getId());
            node.put("merchantName", tx.getMerchantName() == null ? "" : tx.getMerchantName());
            node.put("amount", tx.getAmount() == null ? "" : tx.getAmount().toPlainString());
            node.put("description", tx.getDescription1() == null ? "" : tx.getDescription1());
        }

        return payload.toString();
    }

    /** OpenAI Structured Outputs 스키마. results 배열 하나만 강제한다. */
    private ObjectNode buildResponseFormat() {
        ObjectNode responseFormat = objectMapper.createObjectNode();
        responseFormat.put("type", "json_schema");

        ObjectNode jsonSchema = responseFormat.putObject("json_schema");
        jsonSchema.put("name", "transaction_categorization");
        jsonSchema.put("strict", true);

        ObjectNode schema = jsonSchema.putObject("schema");
        schema.put("type", "object");
        schema.putArray("required").add("results");
        schema.put("additionalProperties", false);

        ObjectNode properties = schema.putObject("properties");
        ObjectNode resultsProperty = properties.putObject("results");
        resultsProperty.put("type", "array");

        ObjectNode item = resultsProperty.putObject("items");
        item.put("type", "object");
        item.put("additionalProperties", false);
        item.putArray("required").add("transactionId").add("categoryId");

        ObjectNode itemProperties = item.putObject("properties");
        itemProperties.putObject("transactionId").put("type", "integer");
        ObjectNode categoryIdProperty = itemProperties.putObject("categoryId");
        categoryIdProperty.putArray("type").add("integer").add("null");

        return responseFormat;
    }

    private List<CategoryAssignmentDto> parseResults(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("choices").path(0).path("message").path("content").asText();
            JsonNode parsed = objectMapper.readTree(content);

            List<CategoryAssignmentDto> results = new ArrayList<>();
            for (JsonNode item : parsed.path("results")) {
                Long transactionId = item.path("transactionId").asLong();
                Long categoryId = item.path("categoryId").isNull() || item.path("categoryId").isMissingNode()
                        ? null : item.path("categoryId").asLong();
                results.add(CategoryAssignmentDto.builder()
                        .transactionId(transactionId)
                        .categoryId(categoryId)
                        .build());
            }
            return results;
        } catch (Exception e) {
            throw new BusinessException("EXTERNAL_API_ERROR", "OpenAI 응답 파싱 실패: " + e.getMessage());
        }
    }
}
