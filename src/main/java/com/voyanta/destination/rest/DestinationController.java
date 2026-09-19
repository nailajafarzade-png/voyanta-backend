package com.voyanta.destination.rest;

import com.voyanta.common.dto.ApiResponse;
import com.voyanta.destination.dto.response.DestinationResponse;
import com.voyanta.destination.service.DestinationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller that exposes the /api/destinations endpoints. It currently serves the
 * public "featured" list used on the homepage and delegates the work to
 * DestinationService.
 */

@RestController
@RequestMapping("/api/destinations")
@RequiredArgsConstructor
public class DestinationController {

    private final DestinationService destinationService;

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<DestinationResponse>>> featured(
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(ApiResponse.ok(destinationService.getFeatured(limit)));
    }
}

