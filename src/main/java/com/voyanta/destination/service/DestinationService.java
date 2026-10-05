package com.voyanta.destination.service;

import com.voyanta.destination.dao.entity.Destination;
import com.voyanta.destination.dao.repository.DestinationRepository;
import com.voyanta.destination.dto.response.DestinationResponse;
import com.voyanta.image.DestinationImageQuery;
import com.voyanta.image.ImageService;
import com.voyanta.image.dto.response.ImageCandidateResponse;

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
    private final ImageService imageService;

    @Transactional(readOnly = true)
    public List<DestinationResponse> getFeatured(Integer limit) {
        Stream<DestinationResponse> stream = destinationRepository.findByFeaturedTrue().stream()
                .map(this::toResponse);

        if (limit != null) {
            stream = stream.limit(limit);
        }

        return stream.collect(Collectors.toList());
    }

    private DestinationResponse toResponse(Destination d) {
        // Tag və interestTags real məlumatdır (məs. "Təbiət", "Tarix və mədəniyyət"):
        // şəkil seçimi təbiət üçün dağ, tarix üçün abidə axtarmalıdır.
        List<ImageCandidateResponse> images = imageService.resolveImageCandidates(
                DestinationImageQuery.of(d.getName(), d.getCountry(), d.getTag(), d.getInterestTags()));
        String best = images.isEmpty() ? null : images.get(0).url();
        return new DestinationResponse(
                d.getId(), d.getName(), d.getCountry(),
                best != null ? best : d.getImageUrl(), d.getTag(), images
        );
    }
}

/**
 * Service responsible for reading destinations. It loads the featured destinations and
 * converts them into DestinationResponse DTOs for the card list on the frontend.
 *
 * Images are NOT read from the database here: the image_url column only holds
 * placeholder links, so every card image is resolved dynamically from the destination
 * name via ImageService (Unsplash, cached in Redis). The stored value remains the
 * fallback so the cards still render when no image can be resolved.
 */
