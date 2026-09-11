package com.voyanta.plan.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GeneratePlanRequest(
        @NotBlank
        String surveySessionId
) {}