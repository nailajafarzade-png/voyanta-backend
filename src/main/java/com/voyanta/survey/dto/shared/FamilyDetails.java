package com.voyanta.survey.dto.shared;

// Yalnız companion = FAMILY seçiləndə doldurulur
public record FamilyDetails(
        int adults,
        int children
) {}