package com.voyanta.destination.dto.response;

import java.util.UUID;

public record DestinationResponse(
        UUID id,
        String name,
        String country,
        String imageUrl,
        String tag
) {}