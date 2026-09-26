package com.voyanta.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voyanta.ai.dto.request.AiRequest;
import com.voyanta.ai.dto.response.AiResponse;
import com.voyanta.common.config.VoyantaProperties;
import com.voyanta.common.exception.ApiException;
import com.voyanta.survey.dto.shared.Budget;
import com.voyanta.survey.dto.shared.TravelDates;
import com.voyanta.survey.enums.BudgetTier;
import com.voyanta.survey.enums.CompanionType;
import com.voyanta.survey.enums.HotelType;
import com.voyanta.survey.enums.InterestType;
import com.voyanta.survey.enums.MealPreference;
import com.voyanta.survey.enums.TripPurpose;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiPlanGeneratorTest {

    private MockWebServer server;
    private OpenAiPlanGenerator generator;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        VoyantaProperties properties = new VoyantaProperties();
        properties.getAi().setApiKey("sk-test");
        properties.getAi().setBaseUrl(server.url("/").toString().replaceAll("/$", ""));
        properties.getAi().setModel("gpt-4o");
        properties.getAi().setTimeout(Duration.ofSeconds(3));
        properties.getAi().setMaxRetries(0);

        generator = new OpenAiPlanGenerator(
                WebClient.builder(),
                properties,
                new PromptBuilder(new ObjectMapper()),
                new ObjectMapper()
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void parsesStructuredChatCompletionJsonWithoutCallingTheRealProvider() throws Exception {
        String itineraryJson = """
                {"destination":"Santorini","days":[{"dayNumber":1,"items":[{"time":"09:00","title":"Walk","description":"Old town","category":"activity"}]}],"budgetSummary":{"accommodation":100,"food":40,"transport":20,"activities":30,"total":190}}
                """;
        String escaped = itineraryJson.replace("\"", "\\\"").replace("\n", "");
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"choices":[{"message":{"content":"%s"}}]}
                        """.formatted(escaped)));

        AiResponse response = generator.generate(sampleRequest());
        assertThat(response.destination()).isEqualTo("Santorini");
        assertThat(response.days()).hasSize(1);
        assertThat(response.budgetSummary().total()).isNotNull();

        RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getPath()).isEqualTo("/v1/chat/completions");
        assertThat(recorded.getHeader("Authorization")).isEqualTo("Bearer sk-test");
        assertThat(recorded.getBody().readUtf8()).contains("gpt-4o").contains("generate_itinerary");
    }

    @Test
    void emptyContentIsMappedToApiException() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"choices\":[{\"message\":{\"content\":\"\"}}]}"));

        assertThatThrownBy(() -> generator.generate(sampleRequest()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getErrorCode()).isEqualTo("AI_EMPTY_RESPONSE");
                    assertThat(api.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                });
    }

    @Test
    void unparsableContentIsMappedToApiException() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"choices\":[{\"message\":{\"content\":\"not-json\"}}]}"));

        assertThatThrownBy(() -> generator.generate(sampleRequest()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo("AI_PARSE_ERROR"));
    }

    private static AiRequest sampleRequest() {
        return new AiRequest(
                Set.of(InterestType.SEA),
                CompanionType.SOLO,
                null,
                new Budget(BudgetTier.ECONOMY, null),
                new TravelDates(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 3), null),
                HotelType.THREE_STAR,
                MealPreference.NO_MEALS,
                Set.of(TripPurpose.ADVENTURE)
        );
    }
}
