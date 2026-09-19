package com.voyanta.destination.service;

import com.voyanta.destination.dao.repository.DestinationRepository;
import com.voyanta.destination.dto.response.DestinationResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class DestinationService {

    private final DestinationRepository destinationRepository;

    @Transactional(readOnly = true)
    public List<DestinationResponse> getFeatured(Integer limit) {
        Stream<DestinationResponse> stream = destinationRepository.findByFeaturedTrue().stream()
                .map(d -> new DestinationResponse(d.getId(), d.getName(), d.getCountry(), d.getImageUrl(), d.getTag()));

        if (limit != null) {
            stream = stream.limit(limit);
        }

        return stream.collect(Collectors.toList());
    }
}

/**
 * Service responsible for reading destinations. It loads the featured destinations and
 * converts them into DestinationResponse DTOs for the card list on the frontend.
 */
