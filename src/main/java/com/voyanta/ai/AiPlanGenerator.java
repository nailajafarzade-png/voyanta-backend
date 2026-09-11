package com.voyanta.ai;

import com.voyanta.ai.dto.request.AiRequest;
import com.voyanta.ai.dto.response.AiResponse;

public interface AiPlanGenerator {
    AiResponse generate(AiRequest request);
}