package com.voyanta.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voyanta.ai.dto.request.AiRequest;
import com.voyanta.survey.dto.shared.Budget;
import com.voyanta.survey.dto.shared.FamilyDetails;
import com.voyanta.survey.dto.shared.TravelDates;
import com.voyanta.survey.enums.BudgetTier;
import com.voyanta.survey.enums.CompanionType;
import com.voyanta.survey.enums.HotelType;
import com.voyanta.survey.enums.InterestType;
import com.voyanta.survey.enums.MealPreference;
import com.voyanta.survey.enums.TripPurpose;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTest {

    private final PromptBuilder promptBuilder = new PromptBuilder(new ObjectMapper());

    @Test
    void userMessageIncludesSurveyFields() {
        AiRequest request = new AiRequest(
                Set.of(InterestType.SEA, InterestType.NATURE),
                CompanionType.FAMILY,
                new FamilyDetails(2, 1),
                new Budget(BudgetTier.MID_RANGE, null),
                new TravelDates(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5), null),
                HotelType.FOUR_STAR,
                MealPreference.BREAKFAST_INCLUDED,
                Set.of(TripPurpose.RELAXATION)
        );

        String message = promptBuilder.buildUserMessage(request);
        assertThat(message).contains("SEA").contains("NATURE");
        assertThat(message).contains("FAMILY").contains("2 böyük").contains("1 uşaq");
        assertThat(message).contains("MID_RANGE").contains("2026-10-01");
        assertThat(message).contains("FOUR_STAR").contains("BREAKFAST_INCLUDED").contains("RELAXATION");
    }

    @Test
    void outputSchemaIsValidJsonObject() {
        Map<String, Object> schema = promptBuilder.outputSchema();
        assertThat(schema.get("type")).isEqualTo("object");
        assertThat(schema).containsKey("properties");
        assertThat(schema).containsKey("required");
    }
}
