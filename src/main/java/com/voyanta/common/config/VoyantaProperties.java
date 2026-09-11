package com.voyanta.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

/**
 * application.yml-dəki "voyanta:" bloku ilə eyni struktur.
 * Hər yerdə @Value("${...}") səpələmək əvəzinə, bunu inject edib
 * properties.getJwt().getSecret() kimi tipli şəkildə istifadə edirik.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "voyanta")
public class VoyantaProperties {

    private Jwt jwt = new Jwt();
    private Ai ai = new Ai();
    private Survey survey = new Survey();
    private RateLimit rateLimit = new RateLimit();
    private Storage storage = new Storage();
    private OAuth oauth = new OAuth();
    private Homepage homepage = new Homepage();
    private Cors cors = new Cors();

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private Duration accessTokenTtl;
        private Duration refreshTokenTtl;
    }

    @Getter
    @Setter
    public static class Ai {
        private String provider;
        private String apiKey;
        private String baseUrl;
        private String model;
        private Duration timeout;
        private int maxRetries;
    }

    @Getter
    @Setter
    public static class Survey {
        private Duration sessionTtl;
    }

    @Getter
    @Setter
    public static class RateLimit {
        private int anonymousPlansPerDay;
        private int authenticatedPlansPerDay;
        private int aiRequestsPerMinuteGlobal;
    }

    @Getter
    @Setter
    public static class Storage {
        private String provider;
        private String bucket;
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String region;
    }

    @Getter
    @Setter
    public static class OAuth {
        private Google google = new Google();
        private Apple apple = new Apple();

        @Getter
        @Setter
        public static class Google {
            private String clientId;
        }

        @Getter
        @Setter
        public static class Apple {
            private String clientId;
        }
    }

    @Getter
    @Setter
    public static class Homepage {
        // Real reytinq (rəy) sistemi qurulana qədər admin-configurable sabit dəyər —
        // application.yml-də dəyişdirilir, kod dəyişmir.
        private BigDecimal avgRating;
        private Duration statsCacheTtl;
    }

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins;
    }
}