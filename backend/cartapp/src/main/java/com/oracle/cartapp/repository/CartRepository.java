package com.oracle.cartapp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.oracle.cartapp.entities.Cart;
import com.oracle.cartapp.entities.CartStatus;

public interface CartRepository extends JpaRepository<Cart, Integer>{
    Optional<Cart> findByUserIdAndStatus(Integer userId, CartStatus status);

}
