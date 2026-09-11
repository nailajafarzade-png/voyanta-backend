package com.voyanta.survey.dto.shared;

import com.voyanta.survey.enums.BudgetTier;

public record Budget(
        BudgetTier tier,
        Integer exactAmount   // yalnız tier = CUSTOM olanda dolu olur
) {}