package com.voyanta.support;

import com.voyanta.ai.AiPlanGenerator;
import com.voyanta.ai.dto.request.AiRequest;
import com.voyanta.ai.dto.response.AiResponse;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Replaces the real OpenAI/Gemini client in tests so plan generation never spends API credits.
 */
@Component
@Primary
public class FakeAiPlanGenerator implements AiPlanGenerator {

    private final AtomicBoolean fail = new AtomicBoolean(false);
    private final AtomicReference<AiResponse> nextResponse = new AtomicReference<>(defaultPlan());

    @Override
    public AiResponse generate(AiRequest request) {
        if (fail.get()) {
            throw new IllegalStateException("Forced AI failure for tests");
        }
        return nextResponse.get();
    }

    public void failNextCalls() {
        fail.set(true);
    }

    public void succeedWith(AiResponse response) {
        fail.set(false);
        nextResponse.set(response);
    }

    public void reset() {
        fail.set(false);
        nextResponse.set(defaultPlan());
    }

    public static AiResponse defaultPlan() {
        return new AiResponse(
                "Santorini",
                List.of(
                        day(1, "Səhər gəzintisi"),
                        day(2, "Sahil"),
                        day(3, "Qala ziyarəti")
                ),
                new AiResponse.AiBudgetBreakdown(
                        new BigDecimal("400"),
                        new BigDecimal("150"),
                        new BigDecimal("80"),
                        new BigDecimal("120"),
                        new BigDecimal("750")
                )
        );
    }

    private static AiResponse.AiDayPlan day(int number, String title) {
        return new AiResponse.AiDayPlan(
                number,
                List.of(new AiResponse.AiItineraryItem("09:00", title, title + " təsviri", "activity"))
        );
    }
}
