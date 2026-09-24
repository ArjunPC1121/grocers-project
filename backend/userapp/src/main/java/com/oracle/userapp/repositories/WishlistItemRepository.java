package com.oracle.userapp.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.oracle.userapp.entities.WishlistItem;

@Repository
// Provides database operations for products saved in user wishlists.
public interface WishlistItemRepository
        extends JpaRepository<WishlistItem, Integer> {

    // Returns saved products from newest to oldest.
    List<WishlistItem> findByUserIdOrderByCreatedAtDesc(Integer userId);

    // Checks whether a user has already saved a product.
    Optional<WishlistItem> findByUserIdAndProductId(
            Integer userId,
            Integer productId
    );

    // Removes one saved product and returns the number of records removed.
    long deleteByUserIdAndProductId(
            Integer userId,
            Integer productId
    );
}
