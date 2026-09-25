package com.oracle.cartapp.repository;

import java.util.Optional;

import com.oracle.cartapp.entities.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import com.oracle.cartapp.entities.CartItem;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartItemRepository extends JpaRepository<CartItem, Integer>{

    // Derived Spring Data query used to ensure a product has at most one line item per cart.
    Optional<CartItem> findByCartIdAndProductId(Integer cartId, Integer productId);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    // Bulk JPQL delete: removes a removed product only from carts still being edited.
    // The persistence context is flushed first and cleared afterward to avoid stale items.
    @Query("""
    DELETE FROM CartItem item
    WHERE item.productId = :productId
      AND item.cart.id IN (
          SELECT cart.id
          FROM Cart cart
          WHERE cart.status = :status
      )
    """)
    int deleteProductFromActiveCarts(
            @Param("productId") Integer productId,
            @Param("status") CartStatus status
    );

}
