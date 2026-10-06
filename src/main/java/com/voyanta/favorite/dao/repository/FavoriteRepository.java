package com.voyanta.favorite.dao.repository;

import com.voyanta.favorite.dao.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {
    List<Favorite> findByUserId(UUID userId);
    Optional<Favorite> findByUserIdAndItemTypeAndItemId(UUID userId, String itemType, String itemId);
    boolean existsByUserIdAndItemTypeAndItemId(UUID userId, String itemType, String itemId);
    void deleteByUserIdAndItemTypeAndItemId(UUID userId, String itemType, String itemId);
    void deleteByUserId(UUID userId);
}
