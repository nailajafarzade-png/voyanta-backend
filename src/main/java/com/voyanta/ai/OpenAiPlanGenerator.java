package com.voyanta.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voyanta.ai.dto.request.AiRequest;
import com.voyanta.ai.dto.response.AiResponse;
import com.voyanta.common.config.VoyantaProperties;
import com.voyanta.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Diqqət: package-private — plan modulu yalnız AiPlanGenerator interfeysini görür.
 * OpenAI-nin "Structured Outputs" (response_format: json_schema, strict: true) rejimi
 * istifadə olunur — Anthropic-in tool_choice-una konseptual alternativ, eyni məqsədlə:
 * modelin sərbəst mətn yox, bizim schema-mıza tam uyğun JSON qaytarması.
 */
@Component
class OpenAiPlanGenerator implements AiPlanGenerator {

    private final WebClient webClient;
    private final VoyantaProperties properties;
    private final PromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    OpenAiPlanGenerator(
            WebClient.Builder webClientBuilder,
            VoyantaProperties properties,
            PromptBuilder promptBuilder,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder
                .baseUrl(properties.getAi().getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getAi().getApiKey())
                .build();
    }

    @Override
    public AiResponse generate(AiRequest request) {
        Map<String, Object> requestBody = Map.of(
                "model", properties.getAi().getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", PromptBuilder.SYSTEM_PROMPT),
                        Map.of("role", "user", "content", promptBuilder.buildUserMessage(request))
                ),
                "response_format", Map.of(
                        "type", "json_schema",
                        "json_schema", Map.of(
                                "name", "generate_itinerary",
                                "strict", true,
                                "schema", promptBuilder.outputSchema()
                        )
                )
        );

        JsonNode response = webClient.post()
                .uri("/v1/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(properties.getAi().getTimeout())
                .retryWhen(Retry.backoff(properties.getAi().getMaxRetries(), Duration.ofSeconds(1)))
                .block();

        return extractContent(response);
    }

    private AiResponse extractContent(JsonNode response) {
        if (response == null) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI_EMPTY_RESPONSE", "AI-dan cavab alınmadı");
        }

        String content = response.path("choices").path(0).path("message").path("content").asText(null);
        if (content == null || content.isBlank()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI_EMPTY_RESPONSE", "AI-dan boş cavab gəldi");
        }

        try {
            // Diqqət: OpenAI-də content hazır JSON obyekt DEYİL, JSON STRING-dir —
            // Anthropic-in tool_use.input-undan fərqli olaraq, əlavə parse addımı lazımdır.
            return objectMapper.readValue(content, AiResponse.class);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI_PARSE_ERROR", "AI cavabı parse olunmadı");
        }
    }
}