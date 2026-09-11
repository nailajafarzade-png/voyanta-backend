package com.voyanta.plan.service;

import com.voyanta.ai.AiPlanGenerator;
import com.voyanta.ai.dto.request.AiRequest;
import com.voyanta.ai.dto.response.AiResponse;
import com.voyanta.common.config.VoyantaProperties;
import com.voyanta.common.exception.ApiException;
import com.voyanta.common.exception.RateLimitExceededException;
import com.voyanta.common.ratelimit.RateLimiter;
import com.voyanta.plan.dao.entity.ItineraryDay;
import com.voyanta.plan.dao.entity.TravelPlan;
import com.voyanta.plan.dao.repository.TravelPlanRepository;
import com.voyanta.plan.dto.shared.BudgetSummary;
import com.voyanta.plan.dto.shared.ItineraryItem;
import com.voyanta.plan.enums.GenerationStage;
import com.voyanta.plan.enums.PlanStatus;
import com.voyanta.survey.dto.response.SurveySession;
import com.voyanta.survey.service.SurveySessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlanGenerationService {

    private static final String PROGRESS_KEY_PREFIX = "plan:progress:";
    private static final Duration PROGRESS_TTL = Duration.ofMinutes(5);

    private final SurveySessionService surveySessionService;
    private final AiPlanGenerator aiPlanGenerator;
    private final TravelPlanRepository planRepository;
    private final PlanLockingService lockingService;
    private final RateLimiter rateLimiter;
    private final RedisTemplate<String, Object> redisTemplate;
    private final VoyantaProperties properties;

    public TravelPlan startGeneration(String surveySessionId, String rateLimitKey, boolean isAuthenticated) {
        // 1) Idempotency ƏVVƏL yoxlanılır — təkrar sorğu rate-limit büdcəsini yeməsin
        TravelPlan existing = planRepository.findByAnonymousSessionId(surveySessionId).orElse(null);
        if (existing != null) {
            return existing;
        }

        SurveySession survey = surveySessionService.get(surveySessionId);
        validateComplete(survey);

        int dailyLimit = isAuthenticated
                ? properties.getRateLimit().getAuthenticatedPlansPerDay()
                : properties.getRateLimit().getAnonymousPlansPerDay();

        if (!rateLimiter.tryConsume(rateLimitKey, dailyLimit, Duration.ofDays(1))) {
            throw new RateLimitExceededException("Gündəlik plan limitinə çatmısan");
        }

        surveySessionService.markCompleted(surveySessionId);

        TravelPlan plan = TravelPlan.builder()
                .anonymousSessionId(surveySessionId)
                .companion(survey.companion())
                .interests(survey.interests())
                .startDate(survey.dates().startDate())
                .endDate(survey.dates().endDate())
                .status(PlanStatus.GENERATING)
                .build();
        planRepository.save(plan);

        generateAsync(plan.getId(), survey, isAuthenticated);
        return plan;
    }

    @Async
    public void generateAsync(UUID planId, SurveySession survey, boolean isAuthenticated) {
        try {
            updateStage(planId, GenerationStage.ANALYZING_INTERESTS);

            AiRequest request = new AiRequest(
                    survey.interests(), survey.companion(), survey.familyDetails(),
                    survey.budget(), survey.dates()
            );

            updateStage(planId, GenerationStage.SELECTING_PLACES);
            AiResponse response = aiPlanGenerator.generate(request);

            updateStage(planId, GenerationStage.BUILDING_ITINERARY);
            applyAiResponse(planId, response, isAuthenticated);

            updateStage(planId, GenerationStage.DONE);
        } catch (Exception e) {
            markFailed(planId);
        }
    }

    private void applyAiResponse(UUID planId, AiResponse response, boolean isAuthenticated) {
        TravelPlan plan = planRepository.findById(planId).orElseThrow();

        plan.setDestination(response.destination());
        plan.setBudgetSummary(toBudgetSummary(response.budgetSummary()));

        List<ItineraryDay> days = response.days().stream()
                .map(d -> ItineraryDay.builder()
                        .plan(plan)
                        .dayNumber(d.dayNumber())
                        .items(toItineraryItems(d.items()))
                        .build())
                .collect(Collectors.toList());

        plan.setDays(days);
        lockingService.applyLocking(plan, isAuthenticated);
        plan.setStatus(PlanStatus.READY);

        planRepository.save(plan);
    }

    private void markFailed(UUID planId) {
        planRepository.findById(planId).ifPresent(plan -> {
            plan.setStatus(PlanStatus.FAILED);
            planRepository.save(plan);
        });
    }

    private void updateStage(UUID planId, GenerationStage stage) {
        redisTemplate.opsForValue().set(PROGRESS_KEY_PREFIX + planId, stage, PROGRESS_TTL);
    }

    private void validateComplete(SurveySession survey) {
        if (survey.interests() == null || survey.interests().isEmpty()
                || survey.companion() == null || survey.budget() == null || survey.dates() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SURVEY_INCOMPLETE", "Sorğu tam doldurulmayıb");
        }
    }

    // ai.dto.response tiplərini plan-ın öz DTO-larına çevirir — bu mapping bilərəkdən
    // burada, plan tərəfindədir ki, ai modulu plan-dan asılı olmasın.
    private BudgetSummary toBudgetSummary(AiResponse.AiBudgetBreakdown b) {
        return new BudgetSummary(b.accommodation(), b.food(), b.transport(), b.activities(), b.total());
    }

    private List<ItineraryItem> toItineraryItems(List<AiResponse.AiItineraryItem> items) {
        return items.stream()
                .map(i -> new ItineraryItem(i.time(), i.title(), i.description(), i.category()))
                .collect(Collectors.toList());
    }
}