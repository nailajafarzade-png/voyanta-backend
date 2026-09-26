package com.voyanta.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.profiles.active=test"
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES;
    static final GenericContainer<?> REDIS;

    static {
        TestcontainersDockerSupport.install();
        POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("voyanta")
                .withUsername("voyanta")
                .withPassword("voyanta")
                .withStartupTimeout(Duration.ofMinutes(2));
        REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)
                .waitingFor(Wait.forListeningPort())
                .withStartupTimeout(Duration.ofMinutes(2));
        POSTGRES.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");
        registry.add("sentry.enabled", () -> "false");
        registry.add("voyanta.oauth.google.client-id", () -> "");
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected FakeAiPlanGenerator fakeAiPlanGenerator;

    @BeforeEach
    void resetAiStub() {
        fakeAiPlanGenerator.reset();
    }

    protected JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder builder, String accessToken) {
        if (accessToken != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }
        return builder;
    }

    protected JsonNode createSurveySession() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/survey/sessions"))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).path("data");
    }

    protected String completeSurvey(String sessionId) throws Exception {
        String body = """
                {
                  "interests": ["SEA", "NATURE"],
                  "companion": "COUPLE",
                  "budget": { "tier": "MID_RANGE" },
                  "dates": { "startDate": "2026-10-01", "endDate": "2026-10-05" },
                  "hotelType": "FOUR_STAR",
                  "mealPreference": "BREAKFAST_INCLUDED",
                  "tripPurpose": ["RELAXATION"]
                }
                """;
        mockMvc.perform(patch("/api/survey/sessions/" + sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
        return sessionId;
    }

    protected String completeSurveyWithInterests(String sessionId, String interestsJsonArray) throws Exception {
        String body = """
                {
                  "interests": %s,
                  "companion": "SOLO",
                  "budget": { "tier": "ECONOMY" },
                  "dates": { "startDate": "2026-11-01", "endDate": "2026-11-04" },
                  "hotelType": "THREE_STAR",
                  "mealPreference": "NO_MEALS",
                  "tripPurpose": ["ADVENTURE"]
                }
                """.formatted(interestsJsonArray);
        mockMvc.perform(patch("/api/survey/sessions/" + sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
        return sessionId;
    }

    protected UUID generatePlan(String surveySessionId, String accessToken, String forwardedFor) throws Exception {
        MockHttpServletRequestBuilder request = post("/api/plans/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"surveySessionId\":\"" + surveySessionId + "\"}");
        if (forwardedFor != null) {
            request.header("X-Forwarded-For", forwardedFor);
        }
        MvcResult result = mockMvc.perform(withAuth(request, accessToken))
                .andExpect(status().isAccepted())
                .andReturn();
        return UUID.fromString(json(result).path("data").asText());
    }

    protected JsonNode loginGoogle(String email, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"valid:" + email + ":" + name + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).path("data");
    }

    protected String uniqueIp() {
        return UUID.randomUUID().toString();
    }
}
