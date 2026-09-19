package com.voyanta.recommendation.rest;

import com.voyanta.common.dto.ApiResponse;
import com.voyanta.destination.dto.response.DestinationResponse;
import com.voyanta.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller that exposes the /api/recommendations endpoints. It serves the
 * personalized destination list for the signed-in user, taking the user id from the
 * security context and delegating the matching to RecommendationService.
 */

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/personalized")
    public ResponseEntity<ApiResponse<List<DestinationResponse>>> personalized(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(ApiResponse.ok(recommendationService.getPersonalized(userId, limit)));
    }
}