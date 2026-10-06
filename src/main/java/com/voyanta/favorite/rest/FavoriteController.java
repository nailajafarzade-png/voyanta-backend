package com.voyanta.favorite.rest;

import com.voyanta.common.dto.ApiResponse;
import com.voyanta.favorite.dao.entity.Favorite;
import com.voyanta.favorite.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Favorite>>> list(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(favoriteService.list(userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Favorite>> add(
            @AuthenticationPrincipal UUID userId,
            @RequestBody AddFavoriteRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                favoriteService.add(userId, request.itemType(), request.itemId())
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> remove(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id
    ) {
        favoriteService.remove(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> removeByItem(
            @AuthenticationPrincipal UUID userId,
            @RequestBody RemoveFavoriteRequest request
    ) {
        favoriteService.remove(userId, request.itemType(), request.itemId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}

record AddFavoriteRequest(String itemType, String itemId) {}
record RemoveFavoriteRequest(String itemType, String itemId) {}
