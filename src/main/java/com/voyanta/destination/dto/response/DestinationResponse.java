package com.voyanta.destination.dto.response;

public record DestinationResponse(
        String name,
        String country,
        String imageUrl,
        String tag
) {}