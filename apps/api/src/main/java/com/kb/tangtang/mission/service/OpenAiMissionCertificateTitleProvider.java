package com.kb.tangtang.mission.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.mission.domain.MissionCertificateTitleInput;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** 인증서 공유에 어울리는 짧은 명예 타이틀 3개만 생성한다. */
@Component
@Log4j2
public class OpenAiMissionCertificateTitleProvider implements MissionCertificateTitleProvider {

    private static final int TITLE_COUNT = 3;
    private static final int MAX_TITLE_LENGTH = 30;
    private static final Pattern TITLE_PATTERN = Pattern.compile("^[가-힣A-Za-z0-9% ]+$");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final String responsesUrl;
    private final int maxOutputTokens;

    public OpenAiMissionCertificateTitleProvider(@Qualifier("openAiRestTemplate") RestTemplate restTemplate,
                                                 ObjectMapper objectMapper,
                                                 @Value("${openai.api-key:}") String apiKey,
                                                 @Value("${openai.model:gpt-5-nano}") String model,
                                                 @Value("${openai.responses-url:https://api.openai.com/v1/responses}")
                                                 String responsesUrl,
                                                 @Value("${openai.max-output-tokens:2048}") int maxOutputTokens) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.responsesUrl = responsesUrl;
        this.maxOutputTokens = maxOutputTokens;
    }

    @Override
    public List<String> generate(MissionCertificateTitleInput input) {
        if (apiKey == null || apiKey.isBlank()) {
            throw unavailable();
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            ResponseEntity<String> response = restTemplate.exchange(
                    responsesUrl, HttpMethod.POST, new HttpEntity<>(buildRequest(input), headers), String.class);
            return parseTitles(extractOutputText(response.getBody()));
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException | IllegalArgumentException exception) {
            log.warn("OpenAI certificate title request failed: {}", exception.getClass().getSimpleName());
            throw unavailable();
        } catch (Exception exception) {
            log.warn("OpenAI certificate title response could not be processed: {}",
                    exception.getClass().getSimpleName());
            throw unavailable();
        }
    }

    private JsonNode buildRequest(MissionCertificateTitleInput input) throws Exception {
        com.fasterxml.jackson.databind.node.ObjectNode request = objectMapper.createObjectNode();
        request.put("model", model);
        request.put("store", false);
        request.put("max_output_tokens", maxOutputTokens);
        request.putObject("reasoning").put("effort", "low");
        request.put("instructions", "You create exactly three Korean honor-certificate titles for a personal "
                + "finance mission app. Use only the supplied aggregate performance data. Each title must be a "
                + "short, boast-worthy accolade suitable for sharing. Use the app's playful court theme when natural. "
                + "Do not shame, insult, fabricate achievements, mention money amounts, recommend financial products, "
                + "or use emojis, punctuation, line breaks, or hashtags. Do not include personal, account, card, merchant, "
                + "or transaction details. Return three distinct titles only.");
        request.put("input", objectMapper.writeValueAsString(input));
        com.fasterxml.jackson.databind.node.ObjectNode schema = request.putObject("text")
                .putObject("format");
        schema.put("type", "json_schema");
        schema.put("name", "mission_certificate_titles");
        schema.put("strict", true);
        com.fasterxml.jackson.databind.node.ObjectNode definition = schema.putObject("schema");
        definition.put("type", "object");
        definition.put("additionalProperties", false);
        com.fasterxml.jackson.databind.node.ObjectNode titles = definition.putObject("properties")
                .putObject("titles");
        titles.put("type", "array");
        titles.put("minItems", TITLE_COUNT);
        titles.put("maxItems", TITLE_COUNT);
        titles.putObject("items")
                .put("type", "string")
                .put("minLength", 1)
                .put("maxLength", MAX_TITLE_LENGTH);
        definition.putArray("required").add("titles");
        return request;
    }

    private String extractOutputText(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        if (root.path("output_text").isTextual() && !root.path("output_text").asText().isBlank()) {
            return root.path("output_text").asText();
        }
        for (JsonNode output : root.path("output")) {
            for (JsonNode content : output.path("content")) {
                if ("output_text".equals(content.path("type").asText())
                        && content.path("text").isTextual()) {
                    return content.path("text").asText();
                }
            }
        }
        log.warn("OpenAI certificate title response has no output text. status={}",
                root.path("status").asText("unknown"));
        throw unavailable();
    }

    private List<String> parseTitles(String outputText) throws Exception {
        JsonNode titlesNode = objectMapper.readTree(outputText).path("titles");
        if (!titlesNode.isArray() || titlesNode.size() != TITLE_COUNT) {
            throw unavailable();
        }
        List<String> titles = new ArrayList<>();
        Set<String> uniqueTitles = new HashSet<>();
        for (JsonNode titleNode : titlesNode) {
            String title = titleNode.isTextual() ? titleNode.asText().trim() : "";
            if (title.isEmpty() || title.length() > MAX_TITLE_LENGTH || !TITLE_PATTERN.matcher(title).matches()
                    || !uniqueTitles.add(title)) {
                throw unavailable();
            }
            titles.add(title);
        }
        return titles;
    }

    private BusinessException unavailable() {
        return new BusinessException("AI_PROVIDER_UNAVAILABLE", "AI 명예 타이틀을 지금 만들 수 없어요.");
    }
}
