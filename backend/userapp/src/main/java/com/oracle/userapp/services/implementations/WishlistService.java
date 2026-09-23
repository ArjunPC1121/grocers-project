package com.oracle.userapp.services.implementations;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.oracle.userapp.dto.WishlistItemResponse;
import com.oracle.userapp.entities.WishlistItem;
import com.oracle.userapp.repositories.UserRepository;
import com.oracle.userapp.repositories.WishlistItemRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final UserRepository userRepository;

    public WishlistItemResponse addItem(
            Integer userId,
            Integer productId
    ) {
        ensureUserExists(userId);

        // Add is idempotent: clicking the wishlist heart twice
        // does not create two rows.
        WishlistItem item = wishlistItemRepository
                .findByUserIdAndProductId(userId, productId)
                .orElseGet(() -> {
                    WishlistItem newItem = new WishlistItem();
                    newItem.setUserId(userId);
                    newItem.setProductId(productId);

                    return wishlistItemRepository.save(newItem);
                });

        return toResponse(item);
    }

    @Transactional(readOnly = true)
    public List<WishlistItemResponse> getItems(Integer userId) {
        ensureUserExists(userId);

        return wishlistItemRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public void removeItem(Integer userId, Integer productId) {
        ensureUserExists(userId);

        long deletedRows = wishlistItemRepository
                .deleteByUserIdAndProductId(userId, productId);

        if (deletedRows == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Wishlist item not found"
            );
        }
    }

    private void ensureUserExists(Integer userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found: " + userId
            );
        }
    }

    private WishlistItemResponse toResponse(WishlistItem item) {
        return new WishlistItemResponse(
                item.getId(),
                item.getUserId(),
                item.getProductId(),
                item.getCreatedAt()
        );
    }
}
