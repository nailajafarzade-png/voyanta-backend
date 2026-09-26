package com.voyanta.recommendation;

import com.voyanta.destination.dao.entity.Destination;
import com.voyanta.destination.dao.repository.DestinationRepository;
import com.voyanta.destination.dto.response.DestinationResponse;
import com.voyanta.plan.dao.entity.TravelPlan;
import com.voyanta.plan.dao.repository.TravelPlanRepository;
import com.voyanta.recommendation.service.RecommendationService;
import com.voyanta.survey.enums.InterestType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private TravelPlanRepository travelPlanRepository;
    @Mock
    private DestinationRepository destinationRepository;
    @InjectMocks
    private RecommendationService recommendationService;

    @Test
    void fallsBackToFeaturedWhenUserHasNoInterestHistory() {
        UUID userId = UUID.randomUUID();
        when(travelPlanRepository.findByUserId(userId)).thenReturn(List.of());
        Destination featured = Destination.builder()
                .id(UUID.randomUUID())
                .name("Santorini")
                .country("Yunanıstan")
                .imageUrl("http://img")
                .tag("Dəniz")
                .featured(true)
                .build();
        when(destinationRepository.findByFeaturedTrue()).thenReturn(List.of(featured));

        List<DestinationResponse> result = recommendationService.getPersonalized(userId, 3);
        assertThat(result).extracting(DestinationResponse::name).containsExactly("Santorini");
    }

    @Test
    void ranksDestinationsByInterestOverlap() {
        UUID userId = UUID.randomUUID();
        TravelPlan plan = TravelPlan.builder().interests(Set.of(InterestType.SEA)).build();
        when(travelPlanRepository.findByUserId(userId)).thenReturn(List.of(plan));

        Destination egypt = Destination.builder()
                .id(UUID.randomUUID()).name("Misir").country("Misir").imageUrl("e").tag("Tarix")
                .interestTags(Set.of(InterestType.HISTORY_CULTURE)).build();
        Destination maldives = Destination.builder()
                .id(UUID.randomUUID()).name("Maldiv adaları").country("Maldivlər").imageUrl("m").tag("Dəniz")
                .interestTags(Set.of(InterestType.SEA)).build();
        when(destinationRepository.findAll()).thenReturn(List.of(egypt, maldives));

        List<DestinationResponse> result = recommendationService.getPersonalized(userId, 2);
        assertThat(result.get(0).name()).isEqualTo("Maldiv adaları");
        assertThat(result.get(1).name()).isEqualTo("Misir");
    }
}
