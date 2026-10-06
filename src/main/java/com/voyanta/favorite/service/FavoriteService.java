package com.voyanta.favorite.service;

import com.voyanta.favorite.dao.entity.Favorite;
import com.voyanta.favorite.dao.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;

    @Transactional(readOnly = true)
    public List<Favorite> list(UUID userId) {
        return favoriteRepository.findByUserId(userId);
    }

    @Transactional
    public Favorite add(UUID userId, String itemType, String itemId) {
        // Idempotent: if it already exists, return the existing one
        return favoriteRepository.findByUserIdAndItemTypeAndItemId(userId, itemType, itemId)
                .orElseGet(() -> {
                    Favorite favorite = Favorite.builder()
                            .userId(userId)
                            .itemType(itemType)
                            .itemId(itemId)
                            .createdAt(Instant.now())
                            .build();
                    return favoriteRepository.save(favorite);
                });
    }

    @Transactional
    public void remove(UUID userId, String itemType, String itemId) {
        favoriteRepository.deleteByUserIdAndItemTypeAndItemId(userId, itemType, itemId);
    }

    @Transactional
    public void remove(UUID userId, UUID favoriteId) {
        favoriteRepository.deleteById(favoriteId);
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(UUID userId, String itemType, String itemId) {
        return favoriteRepository.existsByUserIdAndItemTypeAndItemId(userId, itemType, itemId);
    }
}
