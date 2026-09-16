package com.oracle.cartapp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.oracle.cartapp.entities.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Integer>{

    Optional<CartItem> findByCartIdAndProductId(Integer cartId, Integer productId);

}
