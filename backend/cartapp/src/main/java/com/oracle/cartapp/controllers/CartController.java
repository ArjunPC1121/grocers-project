package com.oracle.cartapp.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.oracle.cartapp.dtos.CartItemRequest;
import com.oracle.cartapp.dtos.CartResponse;
import com.oracle.cartapp.dtos.UpdateCartItemRequest;
import com.oracle.cartapp.services.abstractions.CartService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/{cartId}")
    public ResponseEntity<CartResponse> getCart(
            @PathVariable Integer cartId) {

        return ResponseEntity.ok(cartService.getCart(cartId));
    }

    @GetMapping("/users/{userId}/active")
    public ResponseEntity<CartResponse> getActiveCart(
            @PathVariable Integer userId) {

        return ResponseEntity.ok(cartService.getActiveCartByUser(userId));
    }

    @PostMapping("/users/{userId}/items")
    public ResponseEntity<CartResponse> addItem(
            @PathVariable Integer userId,
            @Valid @RequestBody CartItemRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cartService.addItem(userId, request));
    }

    @PatchMapping("/{cartId}/items/{productId}")
    public ResponseEntity<CartResponse> updateItem(
            @PathVariable Integer cartId,
            @PathVariable Integer productId,
            @Valid @RequestBody UpdateCartItemRequest request) {

        return ResponseEntity.ok(
                cartService.updateItem(cartId, productId, request));
    }

    @DeleteMapping("/{cartId}/items/{productId}")
    public ResponseEntity<Void> removeItem(
            @PathVariable Integer cartId,
            @PathVariable Integer productId) {

        cartService.removeItem(cartId, productId);
        return ResponseEntity.noContent().build();
    }

    // @PostMapping("/{cartId}/checkout")
    // public ResponseEntity<CartResponse> checkout(
    //         @PathVariable Integer cartId) {

    //     return ResponseEntity.ok(cartService.checkout(cartId));
    // }

    @PostMapping("/{cartId}/cancel")
    public ResponseEntity<CartResponse> cancel(
            @PathVariable Integer cartId) {

        return ResponseEntity.ok(cartService.cancel(cartId));
    }
    @PatchMapping("/{cartId}/items/{productId}/increase")
public ResponseEntity<CartResponse> increase(
        @PathVariable Integer cartId,
        @PathVariable Integer productId) {

    return ResponseEntity.ok(
            cartService.increaseItemQuantity(cartId, productId));
}
@PatchMapping("/{cartId}/items/{productId}/decrease")
public ResponseEntity<CartResponse> decrease(
        @PathVariable Integer cartId,
        @PathVariable Integer productId) {

    return ResponseEntity.ok(
            cartService.decreaseItemQuantity(cartId, productId));
}
}