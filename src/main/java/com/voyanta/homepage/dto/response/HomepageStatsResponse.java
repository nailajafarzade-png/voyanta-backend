package com.voyanta.homepage.dto.response;

import java.math.BigDecimal;

public record HomepageStatsResponse(
        long plansCreated,
        BigDecimal avgRating
) {}