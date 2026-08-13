package com.kb.tangtang.report.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.report.domain.MonthlyAiAnalysisCategory;
import com.kb.tangtang.report.domain.MonthlyAiAnalysisInput;
import com.kb.tangtang.report.dto.MonthlyAiAnalysisDto;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * OpenAI Responses API 호출만 담당한다. 요청에는 월간·카테고리 집계와 절감액만 넣고
 * 사용자 식별자, 계좌·카드 정보, 거래 원문은 절대 넣지 않는다.
 */
@Component
@Log4j2
public class OpenAiMonthlyAnalysisClient implements MonthlyAiAnalysisProvider {

    private static final int MAX_FEEDBACK_COUNT = 3;
    private static final int MAX_FEEDBACK_LENGTH = 100;
    private static final int MAX_SAVINGS_ANALOGY_ITEM_LENGTH = 80;
    private static final int MAX_SAVINGS_ANALOGY_UNIT_LENGTH = 20;
    private static final long MIN_SAVINGS_ANALOGY_QUANTITY = 3;
    private static final long MAX_SAVINGS_ANALOGY_QUANTITY = 30;
    private static final long PREFERRED_MIN_SAVINGS_ANALOGY_QUANTITY = 5;
    private static final long PREFERRED_MAX_SAVINGS_ANALOGY_QUANTITY = 15;
    private static final Pattern KOREAN_ITEM = Pattern.compile("^[가-힣]+(?: [가-힣]+)*$");
    private static final Pattern KOREAN_UNIT = Pattern.compile("^[가-힣]+$");
    private static final Pattern DIRECT_MONETARY_AMOUNT = Pattern.compile(
            "(?:[₩￦$]\\s*\\d|(?:\\d{1,3}(?:,\\d{3})+|\\d+)(?:\\s*(?:KRW|원|만원|만\\s*원|천원|천\\s*원|억\\s*원?|만|천)))",
            Pattern.CASE_INSENSITIVE);
    private static final List<SavingsAnalogyCandidate> SAVINGS_ANALOGY_CANDIDATES = List.of(
            new SavingsAnalogyCandidate("생수", 1_500, "병"),
            new SavingsAnalogyCandidate("컵라면", 2_000, "개"),
            new SavingsAnalogyCandidate("편의점 도시락", 6_000, "개"),
            new SavingsAnalogyCandidate("햄버거 세트", 8_000, "세트"),
            new SavingsAnalogyCandidate("국밥", 12_000, "그릇"),
            new SavingsAnalogyCandidate("영화 관람권", 15_000, "장"),
            new SavingsAnalogyCandidate("치킨", 25_000, "마리"),
            new SavingsAnalogyCandidate("피자", 30_000, "판"),
            new SavingsAnalogyCandidate("대형마트 오만원 상품권", 50_000, "장"),
            new SavingsAnalogyCandidate("대형마트 십만원 상품권", 100_000, "장"),
            new SavingsAnalogyCandidate("대형마트 오십만원 상품권", 500_000, "장")
    );

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final String responsesUrl;
    private final int maxOutputTokens;
    private final String reasoningEffort;

    public OpenAiMonthlyAnalysisClient(@Qualifier("openAiRestTemplate") RestTemplate restTemplate,
                                       ObjectMapper objectMapper,
                                       @Value("${openai.api-key:}") String apiKey,
                                       @Value("${openai.model:gpt-5-nano}") String model,
                                       @Value("${openai.responses-url:https://api.openai.com/v1/responses}")
                                       String responsesUrl,
                                       @Value("${openai.max-output-tokens:2048}") int maxOutputTokens,
                                       @Value("${openai.reasoning-effort:low}") String reasoningEffort) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.responsesUrl = responsesUrl;
        this.maxOutputTokens = maxOutputTokens;
        this.reasoningEffort = reasoningEffort;
    }

    @Override
    public MonthlyAiAnalysisDto generate(MonthlyAiAnalysisInput input) {
        if (apiKey == null || apiKey.isBlank()) {
            throw unavailable();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            ResponseEntity<String> response = restTemplate.exchange(
                    responsesUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(buildRequest(input), headers),
                    String.class);
            return parseAnalysis(extractOutputText(response.getBody()), input);
        } catch (AiProviderException ex) {
            throw ex;
        } catch (HttpStatusCodeException ex) {
            if (HttpStatus.TOO_MANY_REQUESTS.equals(ex.getStatusCode())) {
                log.warn("OpenAI monthly analysis request was rate limited");
                throw new AiProviderException("TOO_MANY_REQUESTS",
                        "AI 요청이 많아요. 잠시 후 다시 시도해 주세요.", HttpStatus.TOO_MANY_REQUESTS);
            }
            log.warn("OpenAI monthly analysis request failed with HTTP {}", ex.getStatusCode().value());
            throw unavailable();
        } catch (RestClientException ex) {
            log.warn("OpenAI monthly analysis request failed: {}", ex.getClass().getSimpleName());
            throw unavailable();
        } catch (Exception ex) {
            log.warn("OpenAI monthly analysis response could not be processed: {}",
                    ex.getClass().getSimpleName());
            throw unavailable();
        }
    }

    private JsonNode buildRequest(MonthlyAiAnalysisInput input) {
        try {
            JsonNode schema = buildSchema(input);
            com.fasterxml.jackson.databind.node.ObjectNode request = objectMapper.createObjectNode();
            request.put("model", model);
            request.put("store", false);
            request.put("max_output_tokens", maxOutputTokens);
            request.putObject("reasoning").put("effort", reasoningEffort);
            request.put("instructions", buildInstructions(input));
            request.put("input", objectMapper.writeValueAsString(buildSafeInput(input)));
            request.putObject("text")
                    .putObject("format")
                    .put("type", "json_schema")
                    .put("name", "monthly_report_ai_analysis")
                    .put("strict", true)
                    .set("schema", schema);
            return request;
        } catch (Exception ex) {
            throw unavailable();
        }
    }

    private JsonNode buildSafeInput(MonthlyAiAnalysisInput input) {
        com.fasterxml.jackson.databind.node.ObjectNode safeInput = objectMapper.createObjectNode();
        safeInput.put("yearMonth", input.getYearMonth());
        safeInput.put("currentMonthSpent", input.getCurrentMonthSpent());
        if (input.isHasPreviousComparison()) {
            safeInput.put("previousMonthSpent", input.getPreviousMonthSpent());
        } else {
            safeInput.putNull("previousMonthSpent");
        }
        safeInput.put("hasPreviousComparison", input.isHasPreviousComparison());
        safeInput.put("savingsAmount", input.getSavingsAmount());
        com.fasterxml.jackson.databind.node.ArrayNode categories = safeInput.putArray("categories");
        for (MonthlyAiAnalysisCategory category : input.getCategories()) {
            categories.addObject()
                    .put("parentCategoryName", category.getParentCategoryName())
                    .put("categoryName", category.getCategoryName())
                    .put("amount", category.getAmount());
        }
        return safeInput;
    }

    private JsonNode buildSchema(MonthlyAiAnalysisInput input) {
        com.fasterxml.jackson.databind.node.ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        com.fasterxml.jackson.databind.node.ObjectNode properties = schema.putObject("properties");
        com.fasterxml.jackson.databind.node.ObjectNode feedbacks = properties.putObject("feedbacks");
        feedbacks.put("type", "array");
        feedbacks.put("minItems", 1);
        feedbacks.put("maxItems", MAX_FEEDBACK_COUNT);
        feedbacks.putObject("items")
                .put("type", "string")
                .put("minLength", 1)
                .put("maxLength", MAX_FEEDBACK_LENGTH);

        if (isSavingsEligible(input)) {
            List<SavingsAnalogyCandidate> eligibleCandidates = eligibleSavingsAnalogyCandidates(input.getSavingsAmount());
            com.fasterxml.jackson.databind.node.ObjectNode savingsAnalogy = properties.putObject("savingsAnalogy");
            com.fasterxml.jackson.databind.node.ArrayNode choices = savingsAnalogy.putArray("anyOf");
            eligibleCandidates.forEach(candidate -> choices.add(buildSavingsAnalogyChoice(candidate)));
            schema.putArray("required").add("feedbacks").add("savingsAnalogy");
        } else {
            properties.putObject("savingsAnalogy").put("type", "null");
            schema.putArray("required").add("feedbacks").add("savingsAnalogy");
        }
        return schema;
    }

    private String buildInstructions(MonthlyAiAnalysisInput input) {
        StringBuilder instructions = new StringBuilder();
        instructions.append("You write Korean monthly spending feedback. ")
                .append("Use only the supplied aggregate data. ")
                .append("Never infer or request identity, account, card, merchant, or transaction details. ")
                .append("feedbacks must contain one to three standalone Korean messages for the user, ")
                .append("not titles, labels, categories, or objects. ")
                .append("Use a warm, calm, practical tone that is neither stern nor overly casual. ")
                .append("Write in friendly Korean banmal, using natural endings such as '~해봐', '~해보자', or '~좋아'. ")
                .append("Do not use formal or polite Korean endings such as '~요', '~세요', or '~습니다'. ")
                .append("Give concise asset-management guidance about spending behavior, not a numerical recap. ")
                .append("Never include exact monetary amounts, total spending, category subtotals, currency symbols, or amount abbreviations in feedbacks, even when they appear in the supplied data. ")
                .append("You may name a category only when it makes the next action clearer; never state that category's amount. ")
                .append("If supplied data supports a positive behavior, acknowledge it naturally in one feedback; otherwise do not invent praise. ")
                .append("Each feedback must be one or two short sentences, no more than 100 characters, and never a long paragraph. ")
                .append("When a comparison is supplied, it is only with the immediately preceding month; never describe it as a year-over-year or annual comparison. ");
        if (isSavingsEligible(input)) {
            instructions.append("Return savingsAnalogy as one object with savingsAnalogyItem, savingsAnalogyQuantity, ")
                    .append("savingsAnalogyReferenceUnitPrice, and savingsAnalogyUnit. ")
                    .append("Choose exactly one item from this familiar Korean everyday-expense catalog (item | representative unit price in Korean won | unit): ")
                    .append(savingsAnalogyCandidateGuide(input.getSavingsAmount()))
                    .append(". Do not use a fixed or default item: select the candidate that best fits this savings amount. ")
                    .append("Favor everyday food and living-expense items. Use a face-value mart gift certificate only when it gives a more natural quantity for a large savings amount. ")
                    .append("Never use products whose price differs widely by brand, model, or option, such as bags, dolls, coffee, electronics, or apparel. ")
                    .append("Copy the selected item's name, representative unit price, and unit exactly; never invent a brand, a new item, or a placeholder. ")
                    .append("Calculate ")
                    .append("savingsAnalogyQuantity as floor(savingsAmount / savingsAnalogyReferenceUnitPrice). ")
                    .append("Choose the item and representative unit price so savingsAnalogyQuantity is a whole number from ")
                    .append(MIN_SAVINGS_ANALOGY_QUANTITY)
                    .append(" through ")
                    .append(MAX_SAVINGS_ANALOGY_QUANTITY)
                    .append(" inclusive; prefer ")
                    .append(PREFERRED_MIN_SAVINGS_ANALOGY_QUANTITY)
                    .append(" through ")
                    .append(PREFERRED_MAX_SAVINGS_ANALOGY_QUANTITY)
                    .append(" whenever possible. ")
                    .append("Before returning, verify that the selected catalog values and floor calculation are exact. ")
                    .append("The server will compose the user-facing sentence as '")
                    .append(savingsPrefix(input.getSavingsAmount()))
                    .append("{item} {quantity}{unit}'.");
        } else {
            instructions.append("savingsAnalogy must be null because a positive comparable saving is unavailable.");
        }
        return instructions.toString();
    }

    private String extractOutputText(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode directOutputText = root.path("output_text");
            if (directOutputText.isTextual() && !directOutputText.asText().isBlank()) {
                return directOutputText.asText();
            }
            for (JsonNode output : root.path("output")) {
                for (JsonNode content : output.path("content")) {
                    if ("output_text".equals(content.path("type").asText())
                            && content.path("text").isTextual()
                            && !content.path("text").asText().isBlank()) {
                        return content.path("text").asText();
                    }
                }
            }
        } catch (Exception ex) {
            throw unavailable();
        }
        throw unavailable();
    }

    private MonthlyAiAnalysisDto parseAnalysis(String outputText, MonthlyAiAnalysisInput input) {
        try {
            JsonNode root = objectMapper.readTree(outputText);
            if (!root.isObject() || !root.has("feedbacks")) {
                throw unavailable();
            }

            JsonNode feedbacksNode = root.path("feedbacks");
            if (!feedbacksNode.isArray() || feedbacksNode.size() < 1 || feedbacksNode.size() > MAX_FEEDBACK_COUNT) {
                throw unavailable();
            }
            List<String> feedbacks = new ArrayList<>();
            for (JsonNode feedback : feedbacksNode) {
                if (!feedback.isTextual()) {
                    throw unavailable();
                }
                String message = feedback.asText().trim();
                if (message.isEmpty() || message.length() > MAX_FEEDBACK_LENGTH
                        || message.contains("\n") || message.contains("\r")
                        || DIRECT_MONETARY_AMOUNT.matcher(message).find()) {
                    throw unavailable();
                }
                feedbacks.add(message);
            }

            String savingsAnalogy = null;
            if (isSavingsEligible(input)) {
                JsonNode savingsNode = root.path("savingsAnalogy");
                if (root.size() != 2 || !savingsNode.isObject()) {
                    throw unavailable();
                }
                savingsAnalogy = composeSavingsAnalogy(savingsNode, input.getSavingsAmount());
            } else {
                JsonNode savingsNode = root.path("savingsAnalogy");
                if (root.size() != 2 || !savingsNode.isNull()) {
                    throw unavailable();
                }
            }

            return MonthlyAiAnalysisDto.builder()
                    .yearMonth(input.getYearMonth())
                    .feedbacks(feedbacks)
                    .savingsAnalogy(savingsAnalogy)
                    .build();
        } catch (AiProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            throw unavailable();
        }
    }

    private boolean isSavingsEligible(MonthlyAiAnalysisInput input) {
        return input.isHasPreviousComparison() && input.getSavingsAmount().signum() > 0;
    }

    private String composeSavingsAnalogy(JsonNode savingsNode, BigDecimal savingsAmount) {
        if (savingsNode.size() != 4) {
            throw unavailable();
        }
        JsonNode itemNode = savingsNode.path("savingsAnalogyItem");
        JsonNode quantityNode = savingsNode.path("savingsAnalogyQuantity");
        JsonNode referenceUnitPriceNode = savingsNode.path("savingsAnalogyReferenceUnitPrice");
        JsonNode unitNode = savingsNode.path("savingsAnalogyUnit");
        if (!itemNode.isTextual() || !quantityNode.isIntegralNumber()
                || !referenceUnitPriceNode.isIntegralNumber() || !unitNode.isTextual()) {
            throw unavailable();
        }

        String item = itemNode.asText().trim();
        String unit = unitNode.asText().trim();
        BigDecimal quantity = quantityNode.decimalValue();
        BigDecimal referenceUnitPrice = referenceUnitPriceNode.decimalValue();
        SavingsAnalogyCandidate candidate = findSavingsAnalogyCandidate(item);
        if (referenceUnitPrice.signum() <= 0) {
            throw unavailable();
        }
        BigDecimal expectedQuantity = savingsAmount.setScale(0, RoundingMode.DOWN)
                .divide(referenceUnitPrice, 0, RoundingMode.DOWN);
        if (item.isEmpty() || item.length() > MAX_SAVINGS_ANALOGY_ITEM_LENGTH
                || item.contains("\n") || item.contains("\r")
                || !KOREAN_ITEM.matcher(item).matches()
                || candidate == null
                || unit.isEmpty() || unit.length() > MAX_SAVINGS_ANALOGY_UNIT_LENGTH
                || !KOREAN_UNIT.matcher(unit).matches()
                || !candidate.unit().equals(unit)
                || referenceUnitPrice.compareTo(BigDecimal.valueOf(candidate.referenceUnitPrice())) != 0
                || quantity.compareTo(BigDecimal.valueOf(MIN_SAVINGS_ANALOGY_QUANTITY)) < 0
                || quantity.compareTo(BigDecimal.valueOf(MAX_SAVINGS_ANALOGY_QUANTITY)) > 0
                || quantity.compareTo(expectedQuantity) != 0) {
            throw unavailable();
        }
        return savingsPrefix(savingsAmount) + item + " " + quantity.stripTrailingZeros().toPlainString() + unit;
    }

    private SavingsAnalogyCandidate findSavingsAnalogyCandidate(String item) {
        return SAVINGS_ANALOGY_CANDIDATES.stream()
                .filter(candidate -> candidate.item().equals(item))
                .findFirst()
                .orElse(null);
    }

    private JsonNode buildSavingsAnalogyChoice(SavingsAnalogyCandidate candidate) {
        com.fasterxml.jackson.databind.node.ObjectNode choice = objectMapper.createObjectNode();
        choice.put("type", "object");
        choice.put("additionalProperties", false);
        com.fasterxml.jackson.databind.node.ObjectNode properties = choice.putObject("properties");
        properties.putObject("savingsAnalogyItem")
                .put("type", "string")
                .put("description", "The selected familiar Korean everyday-expense reference item.")
                .putArray("enum").add(candidate.item());
        properties.putObject("savingsAnalogyQuantity")
                .put("type", "integer")
                .put("minimum", MIN_SAVINGS_ANALOGY_QUANTITY)
                .put("maximum", MAX_SAVINGS_ANALOGY_QUANTITY)
                .put("description", "A whole-number quantity equal to floor(savingsAmount / savingsAnalogyReferenceUnitPrice).");
        properties.putObject("savingsAnalogyReferenceUnitPrice")
                .put("type", "integer")
                .put("description", "The selected item's representative unit price in Korean won.")
                .putArray("enum").add(candidate.referenceUnitPrice());
        properties.putObject("savingsAnalogyUnit")
                .put("type", "string")
                .put("description", "The selected item's Korean counting unit.")
                .putArray("enum").add(candidate.unit());
        choice.putArray("required")
                .add("savingsAnalogyItem")
                .add("savingsAnalogyQuantity")
                .add("savingsAnalogyReferenceUnitPrice")
                .add("savingsAnalogyUnit");
        return choice;
    }

    private List<SavingsAnalogyCandidate> eligibleSavingsAnalogyCandidates(BigDecimal savingsAmount) {
        List<SavingsAnalogyCandidate> eligibleCandidates = new ArrayList<>();
        for (SavingsAnalogyCandidate candidate : SAVINGS_ANALOGY_CANDIDATES) {
            BigDecimal quantity = savingsAmount.setScale(0, RoundingMode.DOWN)
                    .divide(BigDecimal.valueOf(candidate.referenceUnitPrice()), 0, RoundingMode.DOWN);
            if (quantity.compareTo(BigDecimal.valueOf(MIN_SAVINGS_ANALOGY_QUANTITY)) >= 0
                    && quantity.compareTo(BigDecimal.valueOf(MAX_SAVINGS_ANALOGY_QUANTITY)) <= 0) {
                eligibleCandidates.add(candidate);
            }
        }
        if (eligibleCandidates.isEmpty()) {
            throw unavailable();
        }
        return eligibleCandidates;
    }

    private String savingsAnalogyCandidateGuide(BigDecimal savingsAmount) {
        StringBuilder guide = new StringBuilder();
        for (SavingsAnalogyCandidate candidate : eligibleSavingsAnalogyCandidates(savingsAmount)) {
            if (guide.length() > 0) {
                guide.append("; ");
            }
            guide.append(candidate.item())
                    .append(" | ")
                    .append(candidate.referenceUnitPrice())
                    .append(" | ")
                    .append(candidate.unit());
        }
        return guide.toString();
    }

    private String savingsPrefix(BigDecimal savingsAmount) {
        NumberFormat numberFormat = NumberFormat.getIntegerInstance(Locale.KOREA);
        return "이번달 아낀 "
                + numberFormat.format(savingsAmount.setScale(0, RoundingMode.HALF_UP))
                + "원은 ";
    }

    private AiProviderException unavailable() {
        return new AiProviderException("AI_PROVIDER_UNAVAILABLE",
                "AI 분석을 지금 만들 수 없어요. 잠시 후 다시 시도해 주세요.",
                HttpStatus.SERVICE_UNAVAILABLE);
    }

    private record SavingsAnalogyCandidate(String item, long referenceUnitPrice, String unit) {
    }
}
