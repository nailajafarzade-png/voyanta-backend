package com.voyanta.ai.dto.request;

import com.voyanta.survey.dto.shared.Budget;
import com.voyanta.survey.dto.shared.FamilyDetails;
import com.voyanta.survey.dto.shared.TravelDates;
import com.voyanta.survey.enums.CompanionType;
import com.voyanta.survey.enums.InterestType;

import java.util.Set;

public record AiRequest(
        Set<InterestType> interests,
        CompanionType companion,
        FamilyDetails familyDetails,
        Budget budget,
        TravelDates dates
) {}