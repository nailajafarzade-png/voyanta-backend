package com.voyanta.ai.dto.response;

import java.math.BigDecimal;
import java.util.List;

// Diqqət: bu tip plan.dto.shared-dəki heç nəyi import etmir. ai modulu tam müstəqildir,
// AI-nin cavabını plan-ın öz tiplərinə çevirmək plan.service.PlanGenerationService-in işidir.
public record AiResponse(
        String destination,
        List<AiDayPlan> days,
        AiBudgetBreakdown budgetSummary
) {

    public record AiDayPlan(
            int dayNumber,
            List<AiItineraryItem> items
    ) {}

    public record AiItineraryItem(
            String time,
            String title,
            String description,
            String category
    ) {}

    public record AiBudgetBreakdown(
            BigDecimal accommodation,
            BigDecimal food,
            BigDecimal transport,
            BigDecimal activities,
            BigDecimal total
    ) {}
}