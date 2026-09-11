package com.voyanta.plan.dto.response;

import com.voyanta.plan.enums.GenerationStage;
import com.voyanta.plan.enums.PlanStatus;

public record PlanStatusResponse(
        PlanStatus status,
        GenerationStage stage,   // yalnız status = GENERATING olanda dolu
        String message           // frontend-in birbaşa göstərdiyi hazır mətn
) {}