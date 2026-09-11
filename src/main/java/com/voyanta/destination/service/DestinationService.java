package com.voyanta.destination.service;

import com.voyanta.destination.dao.repository.DestinationRepository;
import com.voyanta.destination.dto.response.DestinationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DestinationService {

    private final DestinationRepository destinationRepository;

    @Transactional(readOnly = true)
    public List<DestinationResponse> getFeatured() {
        return destinationRepository.findByFeaturedTrue().stream()
                .map(d -> new DestinationResponse(d.getName(), d.getCountry(), d.getImageUrl(), d.getTag()))
                .collect(Collectors.toList());
    }
}