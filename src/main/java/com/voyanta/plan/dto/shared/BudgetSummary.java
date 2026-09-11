package com.voyanta.plan.dto.shared;

import java.math.BigDecimal;

// BigDecimal — pul məbləği üçün double/int yox, dəyirmilləşmə xətalarının qarşısını almaq üçün
public record BudgetSummary(
        BigDecimal accommodation,
        BigDecimal food,
        BigDecimal transport,
        BigDecimal activities,
        BigDecimal total
) {}