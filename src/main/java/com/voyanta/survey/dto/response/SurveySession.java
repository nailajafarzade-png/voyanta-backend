package com.voyanta.survey.dto.response;

import com.voyanta.survey.dto.shared.Budget;
import com.voyanta.survey.dto.shared.FamilyDetails;
import com.voyanta.survey.dto.shared.TravelDates;
import com.voyanta.survey.enums.CompanionType;
import com.voyanta.survey.enums.InterestType;

import java.util.Set;

/**
 * Bu class DB entity DEYİL — Redis-də JSON kimi saxlanılır, TTL ilə (30 dəqiqə).
 * İstifadəçi "Planı hazırla" basana qədər müvəqqəti data daşıyır;
 * plan modulu bunu oxuyub TravelPlan-a çevirəcək.
 */
public record SurveySession(
        String sessionId,
        Set<InterestType> interests,
        CompanionType companion,
        FamilyDetails familyDetails,
        Budget budget,
        TravelDates dates,
        boolean completed
) {

    public static SurveySession empty(String sessionId) {
        return new SurveySession(sessionId, Set.of(), null, null, null, null, false);
    }
}