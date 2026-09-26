package com.voyanta.plan.service;

import com.voyanta.plan.dao.entity.ItineraryDay;
import com.voyanta.plan.dao.entity.TravelPlan;
import com.voyanta.plan.dao.repository.TravelPlanRepository;
import com.voyanta.plan.dto.response.PlanResponse;
import com.voyanta.plan.dto.shared.ItineraryItem;
import com.voyanta.plan.enums.PlanStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanQueryServiceTest {

    @Mock
    private TravelPlanRepository planRepository;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @InjectMocks
    private PlanQueryService queryService;

    @Test
    void hidesItemsOnLockedDaysUnlessTheRequesterOwnsThePlan() {
        UUID planId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        TravelPlan plan = TravelPlan.builder()
                .id(planId)
                .userId(ownerId)
                .status(PlanStatus.READY)
                .days(List.of(
                        ItineraryDay.builder()
                                .dayNumber(1)
                                .locked(false)
                                .items(List.of(new ItineraryItem("09:00", "Open", "d", "a")))
                                .build(),
                        ItineraryDay.builder()
                                .dayNumber(2)
                                .locked(true)
                                .items(List.of(new ItineraryItem("10:00", "Secret", "d", "a")))
                                .build()
                ))
                .build();
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));

        PlanResponse hidden = queryService.getPlan(planId, null);
        assertThat(hidden.days().get(0).items()).isNotNull();
        assertThat(hidden.days().get(1).locked()).isTrue();
        assertThat(hidden.days().get(1).items()).isNull();

        PlanResponse visible = queryService.getPlan(planId, ownerId);
        assertThat(visible.days().get(1).locked()).isFalse();
        assertThat(visible.days().get(1).items()).isNotNull();
    }
}
