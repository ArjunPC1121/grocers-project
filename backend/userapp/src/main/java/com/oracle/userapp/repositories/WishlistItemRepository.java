package com.oracle.userapp.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.oracle.userapp.entities.WishlistItem;

@Repository
public interface WishlistItemRepository
        extends JpaRepository<WishlistItem, Integer> {

    List<WishlistItem> findByUserIdOrderByCreatedAtDesc(Integer userId);

    Optional<WishlistItem> findByUserIdAndProductId(
            Integer userId,
            Integer productId
    );

    long deleteByUserIdAndProductId(
            Integer userId,
            Integer productId
    );
}