package com.agenticai.expensesagenticai.service;

import com.agenticai.expensesagenticai.dto.ExpenseIntent;
import com.agenticai.expensesagenticai.dto.ExpenseParseResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class GeminiAiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiAiService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final DeterministicExpenseParser fallbackParser;

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-3.5-flash-lite}")
    private String model;

    @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String baseUrl;

    public GeminiAiService(
            DeterministicExpenseParser fallbackParser) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(4).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(6).toMillis());

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
        this.objectMapper = new ObjectMapper();
        this.fallbackParser = fallbackParser;
    }

    public ExpenseParseResult parseMessage(String userMessage) {
        // FAST-PATH: Run instant deterministic parser first (<1ms latency)
        ExpenseParseResult fastResult = fallbackParser.parse(userMessage);
        if (fastResult.intent() == ExpenseIntent.RECORD_EXPENSE
                || fastResult.intent() == ExpenseIntent.QUERY_EXPENSES
                || fastResult.intent() == ExpenseIntent.DELETE_EXPENSE) {
            log.info("Fast-path resolved intent in <1ms: {}", fastResult.intent());
            return fastResult;
        }

        // SLOW-PATH: For ambiguous, conversational, or complex messages, call Gemini AI
        if (apiKey != null && !apiKey.isBlank()) {
            try {
                LocalDate today = LocalDate.now();
                String promptText = String.format(
                        "You are an expense tracker assistant. Today is %s (yesterday was %s).\n" +
                        "Classify intent and extract data for user message: \"%s\"\n" +
                        "Respond ONLY with raw JSON object: " +
                        "{\"intent\": \"RECORD_EXPENSE\"|\"QUERY_EXPENSES\"|\"DELETE_EXPENSE\"|\"AMBIGUOUS\"|\"GENERAL_CONVERSATION\", " +
                        "\"amount\": number, \"category\": \"Food\"|\"Transport\"|\"Entertainment\"|\"Healthcare\"|\"Bills\"|\"Shopping\"|\"Other\", " +
                        "\"description\": string, \"expenseDate\": \"YYYY-MM-DD\", " +
                        "\"queryCategory\": string, \"queryStartDate\": \"YYYY-MM-DD\", \"queryEndDate\": \"YYYY-MM-DD\", " +
                        "\"clarificationPrompt\": string}",
                        today, today.minusDays(1), userMessage
                );

                Map<String, Object> requestBody = Map.of(
                        "contents", List.of(
                                Map.of("parts", List.of(Map.of("text", promptText)))
                        ),
                        "generationConfig", Map.of(
                                "responseMimeType", "application/json",
                                "maxOutputTokens", 150,
                                "temperature", 0.1
                        )
                );

                String url = String.format("%s/models/%s:generateContent?key=%s", baseUrl, model, apiKey);

                String responseBody = restClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(String.class);

                ExpenseParseResult result = parseGeminiResponse(responseBody);
                if (result != null) {
                    log.info("Gemini parsed message successfully with intent: {}", result.intent());
                    return result;
                }
            } catch (Exception e) {
                log.warn("Gemini call timed out or failed: {}. Using deterministic fallback.", e.getMessage());
            }
        }

        return fastResult;
    }

    private ExpenseParseResult parseGeminiResponse(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (textNode.isMissingNode() || textNode.asText().isBlank()) {
                return null;
            }

            String rawText = textNode.asText().trim();
            if (rawText.startsWith("```")) {
                rawText = rawText.replaceAll("(?s)^```(?:json)?\\s*(.*?)\\s*```$", "$1").trim();
            }

            JsonNode json = objectMapper.readTree(rawText);
            String intentStr = json.path("intent").asText("GENERAL_CONVERSATION");
            ExpenseIntent intent;
            try {
                intent = ExpenseIntent.valueOf(intentStr.toUpperCase());
            } catch (Exception e) {
                intent = ExpenseIntent.AMBIGUOUS;
            }

            if (intent == ExpenseIntent.RECORD_EXPENSE) {
                BigDecimal amount = json.has("amount") && !json.get("amount").isNull()
                        ? new BigDecimal(json.get("amount").asText())
                        : null;
                String category = json.path("category").asText("Other");
                String description = json.path("description").asText("Expense");
                String dateStr = json.path("expenseDate").asText();
                LocalDate date = (dateStr != null && !dateStr.isBlank())
                        ? LocalDate.parse(dateStr)
                        : LocalDate.now();

                if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                    return ExpenseParseResult.recordExpense(amount, category, description, date);
                }
            } else if (intent == ExpenseIntent.DELETE_EXPENSE) {
                BigDecimal amount = json.has("amount") && !json.get("amount").isNull()
                        ? new BigDecimal(json.get("amount").asText())
                        : null;
                String category = json.has("category") && !json.get("category").isNull()
                        ? json.get("category").asText()
                        : null;
                String description = json.has("description") && !json.get("description").isNull()
                        ? json.get("description").asText()
                        : null;
                String dateStr = json.path("expenseDate").asText();
                LocalDate date = (dateStr != null && !dateStr.isBlank())
                        ? LocalDate.parse(dateStr)
                        : null;
                return ExpenseParseResult.deleteExpense(amount, category, description, date, date != null);
            } else if (intent == ExpenseIntent.QUERY_EXPENSES) {
                String queryCategory = json.has("queryCategory") && !json.get("queryCategory").isNull()
                        ? json.get("queryCategory").asText()
                        : null;
                String startStr = json.path("queryStartDate").asText();
                String endStr = json.path("queryEndDate").asText();

                LocalDate start = (startStr != null && !startStr.isBlank())
                        ? LocalDate.parse(startStr)
                        : LocalDate.now().withDayOfMonth(1);
                LocalDate end = (endStr != null && !endStr.isBlank())
                        ? LocalDate.parse(endStr)
                        : LocalDate.now();

                return ExpenseParseResult.query(start, end, queryCategory);
            } else if (intent == ExpenseIntent.AMBIGUOUS) {
                String prompt = json.path("clarificationPrompt").asText(
                        "Could you please specify the amount and what you spent it on?"
                );
                return ExpenseParseResult.ambiguous(prompt);
            } else {
                return ExpenseParseResult.general();
            }
        } catch (Exception e) {
            log.warn("Failed to parse Gemini JSON output: {}", e.getMessage());
        }
        return null;
    }
}
