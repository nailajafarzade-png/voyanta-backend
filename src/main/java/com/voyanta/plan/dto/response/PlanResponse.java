package com.voyanta.plan.dto.response;

import com.voyanta.plan.dto.shared.BudgetSummary;
import com.voyanta.plan.enums.PlanStatus;
import com.voyanta.survey.enums.CompanionType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PlanResponse(
        UUID id,
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        CompanionType companion,
        BudgetSummary budgetSummary,
        PlanStatus status,
        List<ItineraryDayResponse> days
) {}