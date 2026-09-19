package com.voyanta.wishlist.dao.repository;

import com.voyanta.wishlist.dao.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WishlistRepository extends JpaRepository<WishlistItem, UUID> {
    boolean existsByUserIdAndDestinationId(UUID userId, UUID destinationId);
    List<WishlistItem> findByUserId(UUID userId);
    void deleteByUserIdAndDestinationId(UUID userId, UUID destinationId);
}