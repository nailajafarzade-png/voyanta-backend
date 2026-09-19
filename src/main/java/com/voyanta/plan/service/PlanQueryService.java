package com.voyanta.plan.service;

import com.voyanta.common.exception.ResourceNotFoundException;
import com.voyanta.plan.dao.entity.ItineraryDay;
import com.voyanta.plan.dao.entity.TravelPlan;
import com.voyanta.plan.dao.repository.TravelPlanRepository;
import com.voyanta.plan.dto.response.ItineraryDayResponse;
import com.voyanta.plan.dto.response.PlanResponse;
import com.voyanta.plan.dto.response.PlanStatusResponse;

import com.voyanta.plan.enums.GenerationStage;
import com.voyanta.plan.enums.PlanStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service responsible for reading plans. It builds the polling status with a progress
 * message, and returns the full plan while hiding the activities of locked days from
 * anyone who does not own the plan.
 */
@Service
@RequiredArgsConstructor
public class PlanQueryService {

    private static final String PROGRESS_KEY_PREFIX = "plan:progress:";

    private final TravelPlanRepository planRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public PlanStatusResponse getStatus(UUID planId) {
        TravelPlan plan = findPlan(planId);

        if (plan.getStatus() != PlanStatus.GENERATING) {
            return new PlanStatusResponse(plan.getStatus(), null, messageFor(plan.getStatus(), null));
        }

        GenerationStage stage = (GenerationStage) redisTemplate.opsForValue().get(PROGRESS_KEY_PREFIX + planId);
        return new PlanStatusResponse(plan.getStatus(), stage, messageFor(plan.getStatus(), stage));
    }

    @Transactional(readOnly = true)
    public PlanResponse getPlan(UUID planId, UUID requestingUserId) {
        TravelPlan plan = findPlan(planId);

        // Defense in depth: DB-dəki locked bayrağına etibar etməklə yanaşı, məhz burada
        // da bir daha yoxlayırıq ki, sahiblik olmadan heç kim items görməsin.
        boolean canSeeAll = requestingUserId != null && requestingUserId.equals(plan.getUserId());

        List<ItineraryDayResponse> days = plan.getDays().stream()
                .map(day -> toDayResponse(day, canSeeAll))
                .collect(Collectors.toList());

        return new PlanResponse(
                plan.getId(), plan.getDestination(), plan.getStartDate(), plan.getEndDate(),
                plan.getCompanion(), plan.getBudgetSummary(), plan.getStatus(), days
        );
    }

    private ItineraryDayResponse toDayResponse(ItineraryDay day, boolean canSeeAll) {
        boolean visible = canSeeAll || !day.isLocked();
        return new ItineraryDayResponse(day.getDayNumber(), day.isLocked(), visible ? day.getItems() : null);
    }

    private TravelPlan findPlan(UUID planId) {
        return planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan tapılmadı"));
    }

    private String messageFor(PlanStatus status, GenerationStage stage) {
        if (status == PlanStatus.FAILED) return "Plan hazırlanarkən xəta baş verdi";
        if (status == PlanStatus.READY) return "Planın hazırdır";
        if (stage == null) return "Plan hazırlanır...";

        return switch (stage) {
            case ANALYZING_INTERESTS -> "Maraq dairən təhlil olundu";
            case SELECTING_PLACES -> "Uyğun məkanlar seçildi";
            case BUILDING_ITINERARY -> "Gündəlik marşrut qurulur...";
            case DONE -> "Planın hazırdır";
        };
    }
}