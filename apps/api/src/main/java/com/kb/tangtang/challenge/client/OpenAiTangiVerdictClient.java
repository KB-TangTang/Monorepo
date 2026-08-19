package com.kb.tangtang.challenge.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kb.tangtang.challenge.domain.IndictmentStatus;
import com.kb.tangtang.challenge.domain.VerdictDecision;
import com.kb.tangtang.challenge.domain.VerdictTallyRow;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

/**
 * 판사 탕이 — OpenAI Responses API 로 동률 재판을 판결한다 (이슈 #172).
 *
 * <p>보내는 것은 <b>사건 개요와 변론뿐</b>이다. 닉네임·사용자 식별자·거래 원문·가맹점명은 넣지
 * 않는다 — 판결에 필요하지 않고, 넣는 순간 외부 모델에 개인 정보가 나간다.
 *
 * <p>표 분포도 보내지 않는다. 동률이라 여기까지 온 것이므로 알려 줄 정보가 없고, 오히려
 * 「2:2 였다」를 알려 주면 모델이 사건이 아니라 표를 근거로 삼는다.
 *
 * <p><b>실패하면 던진다.</b> 여기서 무죄로 삼켜 버리면 호출부가 「탕이가 무죄라고 판단했다」와
 * 「탕이를 못 불렀다」를 구분하지 못한다. 대체 판결을 고르는 것은
 * {@code GroupVerdictBatchService} 의 몫이다.
 */
@Component
@Log4j2
public class OpenAiTangiVerdictClient implements TangiVerdictClient {

    private static final String INSTRUCTIONS =
            "You are 판사 탕이, the judge of a Korean group spending challenge. "
                    + "The jury vote ended in a tie, so you decide the case alone. "
                    + "Return GUILTY when the spending was an avoidable overspend, "
                    + "and INNOCENT when the defense makes it reasonable "
                    + "(for example the member paid for others and will be reimbursed, "
                    + "or the expense was unavoidable such as medical or family duty). "
                    + "When there is no defense, judge on the indictment alone; "
                    + "an unexplained overspend is normally GUILTY. "
                    + "Write reason in Korean, one or two short sentences, at most 120 characters. "
                    + "Speak warmly and plainly to the defendant using polite endings such as '~해요'. "
                    + "State the ground for the verdict; never scold, never give budgeting advice, "
                    + "and never mention voting, a tie, or that you are an AI. "
                    + "Use only the supplied data. Never infer or request identity, "
                    + "account, card, merchant, or transaction details.";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final String responsesUrl;
    private final int maxOutputTokens;
    private final String reasoningEffort;

    public OpenAiTangiVerdictClient(
            @Qualifier("tangiVerdictRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${openai.api-key:}") String apiKey,
            @Value("${challenge.trial.ai-verdict.model:gpt-5-nano}") String model,
            @Value("${openai.responses-url:https://api.openai.com/v1/responses}") String responsesUrl,
            @Value("${challenge.trial.ai-verdict.max-output-tokens:1024}") int maxOutputTokens,
            @Value("${challenge.trial.ai-verdict.reasoning-effort:low}") String reasoningEffort) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.responsesUrl = responsesUrl;
        this.maxOutputTokens = maxOutputTokens;
        this.reasoningEffort = reasoningEffort;
    }

    @Override
    public VerdictDecision judge(VerdictTallyRow row) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("openai.api-key 가 없어 판사 탕이를 부를 수 없습니다");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        ResponseEntity<String> response = restTemplate.exchange(
                responsesUrl,
                HttpMethod.POST,
                new HttpEntity<>(buildRequest(row), headers),
                String.class);
        return parseVerdict(extractOutputText(response.getBody()));
    }

    private JsonNode buildRequest(VerdictTallyRow row) {
        try {
            ObjectNode request = objectMapper.createObjectNode();
            request.put("model", model);
            /* 재판 기록을 OpenAI 쪽에 남기지 않는다 */
            request.put("store", false);
            request.put("max_output_tokens", maxOutputTokens);
            request.putObject("reasoning").put("effort", reasoningEffort);
            request.put("instructions", INSTRUCTIONS);
            request.put("input", objectMapper.writeValueAsString(buildSafeInput(row)));
            request.putObject("text")
                    .putObject("format")
                    .put("type", "json_schema")
                    .put("name", "group_challenge_verdict")
                    .put("strict", true)
                    .set("schema", buildSchema());
            return request;
        } catch (Exception e) {
            throw new IllegalStateException("판사 탕이 요청을 만들지 못했습니다", e);
        }
    }

    private JsonNode buildSafeInput(VerdictTallyRow row) {
        ObjectNode input = objectMapper.createObjectNode();
        input.put("evalType", row.getEvalType());
        input.put("category", row.getCategoryName() == null ? "전체 소비" : row.getCategoryName());
        putAmount(input, "limitAmount", row.getLimitAmount());
        putAmount(input, "spentAmount", row.getCurrentAmount());
        putAmount(input, "exceededAmount", row.getExceededAmount());
        input.put("indictmentMessage", row.getMessage());

        /* 변론 없이 마감돼 투표로 넘어간 재판은 여기가 통째로 비어 있다 */
        if (row.getDefenseContent() == null) {
            input.putNull("defense");
        } else {
            ObjectNode defense = input.putObject("defense");
            defense.put("content", row.getDefenseContent());
            putAmount(defense, "actualBurdenAmount", row.getActualBurdenAmount());
            putAmount(defense, "claimedDeductionAmount", row.getDeductionAmount());
        }
        return input;
    }

    private void putAmount(ObjectNode node, String field, BigDecimal amount) {
        if (amount == null) {
            node.putNull(field);
        } else {
            node.put(field, amount);
        }
    }

    private JsonNode buildSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("verdict")
                .put("type", "string")
                .put("description", "GUILTY when the spending was not justified, INNOCENT otherwise.")
                .putArray("enum").add("GUILTY").add("INNOCENT");
        properties.putObject("reason")
                .put("type", "string")
                .put("minLength", 1)
                .put("maxLength", VerdictDecision.AI_REASON_MAX_LENGTH)
                .put("description", "One or two short Korean sentences explaining the verdict.");
        schema.putArray("required").add("verdict").add("reason");
        return schema;
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
        } catch (Exception e) {
            throw new IllegalStateException("판사 탕이 응답을 읽지 못했습니다", e);
        }
        throw new IllegalStateException("판사 탕이 응답에 판결문이 없습니다");
    }

    private VerdictDecision parseVerdict(String outputText) {
        JsonNode root;
        try {
            root = objectMapper.readTree(outputText);
        } catch (Exception e) {
            throw new IllegalStateException("판사 탕이 판결문이 JSON 이 아닙니다", e);
        }

        JsonNode verdict = root.path("verdict");
        JsonNode reason = root.path("reason");
        if (!verdict.isTextual() || !reason.isTextual() || reason.asText().isBlank()) {
            throw new IllegalStateException("판사 탕이 판결문의 모양이 다릅니다");
        }

        IndictmentStatus status;
        try {
            status = IndictmentStatus.valueOf(verdict.asText());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("판사 탕이가 모르는 판결을 냈습니다: " + verdict.asText(), e);
        }

        /* 사유 절단은 VerdictDecision#byAi 가 한 곳에서 한다 */
        return VerdictDecision.byAi(status, reason.asText().replaceAll("\\s+", " "));
    }
}
