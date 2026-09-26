package com.voyanta.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.voyanta.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFlowIT extends AbstractIntegrationTest {

    @Test
    void googleLoginIssuesTokensAndRefreshRotatesThem() throws Exception {
        String email = "oauth-" + System.nanoTime() + "@voyanta.test";
        JsonNode login = loginGoogle(email, "Test User");

        assertThat(login.path("accessToken").asText()).isNotBlank();
        assertThat(login.path("refreshToken").asText()).isNotBlank();
        assertThat(login.path("user").path("email").asText()).isEqualTo(email);
        assertThat(login.path("user").path("fullName").asText()).isEqualTo("Test User");

        String firstRefresh = login.path("refreshToken").asText();
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + firstRefresh + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode refreshed = json(refreshResult).path("data");
        assertThat(refreshed.path("accessToken").asText()).isNotBlank();
        assertThat(refreshed.path("refreshToken").asText()).isNotEqualTo(firstRefresh);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + firstRefresh + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(result -> assertThat(json(result).path("errorCode").asText())
                        .isEqualTo("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void googleLoginReusesExistingUserByEmail() throws Exception {
        String email = "reuse-" + System.nanoTime() + "@voyanta.test";
        JsonNode first = loginGoogle(email, "First Name");
        JsonNode second = loginGoogle(email, "Second Name");

        assertThat(second.path("user").path("id").asText())
                .isEqualTo(first.path("user").path("id").asText());
        assertThat(second.path("user").path("fullName").asText()).isEqualTo("First Name");
    }

    @Test
    void invalidGoogleTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"not-a-real-google-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(result -> assertThat(json(result).path("errorCode").asText())
                        .isEqualTo("INVALID_OAUTH_TOKEN"));
    }

    @Test
    void unknownProviderIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/oauth/facebook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"valid:a@b.com:A\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(json(result).path("errorCode").asText())
                        .isEqualTo("UNKNOWN_PROVIDER"));
    }

    @Test
    void blankIdTokenFailsValidation() throws Exception {
        mockMvc.perform(post("/api/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(json(result).path("errorCode").asText())
                        .isEqualTo("VALIDATION_FAILED"));
    }

    @Test
    void logoutRevokesRefreshTokens() throws Exception {
        String email = "logout-" + System.nanoTime() + "@voyanta.test";
        JsonNode login = loginGoogle(email, "Logout User");
        String accessToken = login.path("accessToken").asText();
        String refreshToken = login.path("refreshToken").asText();

        mockMvc.perform(withAuth(post("/api/auth/logout"), accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidRefreshTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"totally-invalid\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(result -> assertThat(json(result).path("errorCode").asText())
                        .isEqualTo("INVALID_REFRESH_TOKEN"));
    }
}
