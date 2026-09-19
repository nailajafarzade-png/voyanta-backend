package com.voyanta.recommendation.service;

import com.voyanta.destination.dao.entity.Destination;
import com.voyanta.destination.dao.repository.DestinationRepository;
import com.voyanta.destination.dto.response.DestinationResponse;
import com.voyanta.plan.dao.entity.TravelPlan;
import com.voyanta.plan.dao.repository.TravelPlanRepository;
import com.voyanta.survey.enums.InterestType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/*
 * v1 — sadə rule-based overlap matching (Jira tələbinə uyğun).
 * Destinasiya sayı böyüyəndə (yüzlərlə/minlərlə) bu, DB-səviyyəli
 * hesablamaya (JSONB containment sorğusu və ya join cədvəli) keçirilə bilər.
 */

/**
 * Service responsible for personalized destination recommendations. It builds an interest
 * profile from the user's past travel plans, ranks destinations by how many interest tags
 * they share, and returns the top few. New users without history get the featured list.
 */
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final int MAX_RESULTS = 6;

    private final TravelPlanRepository travelPlanRepository;
    private final DestinationRepository destinationRepository;

    @Transactional(readOnly = true)
    public List<DestinationResponse> getPersonalized(UUID userId, Integer limit) {
        int effectiveLimit = limit != null ? limit : MAX_RESULTS;
        Set<InterestType> userInterests = aggregateInterests(userId);

        // Tarixçəsi olmayan (yeni) istifadəçi üçün boş/mənasız cavab qaytarmaqdansa
        // featured siyahısına keçirik.
        if (userInterests.isEmpty()) {
            return destinationRepository.findByFeaturedTrue().stream()
                    .limit(effectiveLimit)
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }

        return destinationRepository.findAll().stream()
                .sorted(Comparator.comparingInt((Destination d) -> overlapScore(d, userInterests)).reversed())
                .limit(effectiveLimit)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private Set<InterestType> aggregateInterests(UUID userId) {
        Set<InterestType> result = EnumSet.noneOf(InterestType.class);
        for (TravelPlan plan : travelPlanRepository.findByUserId(userId)) {
            if (plan.getInterests() != null) {
                result.addAll(plan.getInterests());
            }
        }
        return result;
    }

    private int overlapScore(Destination destination, Set<InterestType> userInterests) {
        if (destination.getInterestTags() == null) {
            return 0;
        }
        return (int) destination.getInterestTags().stream()
                .filter(userInterests::contains)
                .count();
    }

    private DestinationResponse toResponse(Destination d) {
        return new DestinationResponse(d.getId(), d.getName(), d.getCountry(), d.getImageUrl(), d.getTag());
    }
}


