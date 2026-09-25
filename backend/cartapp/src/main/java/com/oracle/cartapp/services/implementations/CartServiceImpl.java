package com.oracle.cartapp.services.implementations;

import java.util.ArrayList;
import java.util.List;

import com.oracle.cartapp.clients.UserClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oracle.cartapp.clients.ProductClient;
import com.oracle.cartapp.dtos.CartItemRequest;
import com.oracle.cartapp.dtos.CartItemResponse;
import com.oracle.cartapp.dtos.CartResponse;
import com.oracle.cartapp.dtos.UpdateCartItemRequest;
import com.oracle.cartapp.entities.Cart;
import com.oracle.cartapp.entities.CartItem;
import com.oracle.cartapp.entities.CartStatus;
import com.oracle.cartapp.exceptions.ResourceNotFoundException;
import com.oracle.cartapp.repository.CartItemRepository;
import com.oracle.cartapp.repository.CartRepository;
import com.oracle.cartapp.services.abstractions.CartService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductClient productClient;
    private final UserClient userClient;
    //Get all carts
    @Override
    public List<CartResponse> getAllCarts() {
        return cartRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }
    //Get particular cart
    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(Integer cartId) {
        return toResponse(findCart(cartId));
    }


    // A user may have one active cart at a time; past carts remain available by ID.
    @Override
    public CartResponse getActiveCartByUser(Integer userId) {
        userClient.checkUserExists(userId);

        Cart cart = cartRepository
                .findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active cart not found"));

        return toResponse(cart);
    }
    // Reuse the user's active cart or create one on their first add-to-cart action.
    @Override
    public CartResponse addItem(Integer userId, CartItemRequest request) {
        userClient.checkUserExists(userId);
        Cart cart = cartRepository
                .findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUserId(userId);
                    newCart.setStatus(CartStatus.ACTIVE);
                    newCart.setItems(new ArrayList<>());
                    return cartRepository.save(newCart);
                });

        // Reuse an existing line item so adding the same product increases its quantity.
       CartItem item = cartItemRepository
        .findByCartIdAndProductId(cart.getId(), request.productId())
        .orElseGet(() -> {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProductId(request.productId());
            newItem.setQuantity(0);

            cart.getItems().add(newItem);
            return newItem;
        });

int newQuantity = item.getQuantity() + request.quantity();

// ProductApp is the inventory source of truth and rejects quantities it cannot fulfil.
productClient.checkQuantity(request.productId(), newQuantity);

item.setQuantity(newQuantity);
cartItemRepository.save(item);
        return toResponse(cart);
    }

    @Override
    public CartResponse updateItem(Integer cartId, Integer productId,
                                   UpdateCartItemRequest request) {

        Cart cart = findActiveCart(cartId);

        CartItem item = cartItemRepository
                .findByCartIdAndProductId(cartId, productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found in cart"));

      // Validate the requested final quantity before changing the cart item.
      productClient.checkQuantity(productId, request.quantity());

item.setQuantity(request.quantity());
cartItemRepository.save(item);

        

        return toResponse(cart);
    }

    @Override
    public void removeItem(Integer cartId, Integer productId) {
        findActiveCart(cartId);

        CartItem item = cartItemRepository
                .findByCartIdAndProductId(cartId, productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found in cart"));

        // Deleting the row removes the item from this cart; inventory is checked again
        // when the customer later adds or changes an item.
        cartItemRepository.delete(item);
    }

     @Override
     public CartResponse checkout(Integer cartId) {
         Cart cart = findActiveCart(cartId);

         if (cart.getItems().isEmpty()) {
             throw new IllegalArgumentException("Cannot checkout an empty cart");
         }

//         for (CartItem item : cart.getItems()) {
//             // ProductApp removes it from reservedQuantity permanently.
//             productClient.confirm(item.getProductId(), item.getQuantity());
//         }

         // A checked-out cart is immutable because future item operations require ACTIVE status.
         cart.setStatus(CartStatus.CHECKED_OUT);

         return toResponse(cartRepository.save(cart));
     }

    @Override
    public CartResponse cancel(Integer cartId) {
        Cart cart = findActiveCart(cartId);
       // Cancelling preserves the cart history but makes it immutable.

cart.setStatus(CartStatus.CANCELLED);

return toResponse(cartRepository.save(cart));

    
    }

    private Cart findCart(Integer cartId) {
        return cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart not found: " + cartId));
    }
    private CartItem findCartItem(Integer cartId, Integer productId) {
    return cartItemRepository
            .findByCartIdAndProductId(cartId, productId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Product not found in cart"));
}


    private Cart findActiveCart(Integer cartId) {
        Cart cart = findCart(cartId);

        // Prevent edits to carts that have already been checked out or cancelled.
        if (cart.getStatus() != CartStatus.ACTIVE) {
            throw new IllegalArgumentException("Cart is not active");
        }

        return cart;
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(item -> new CartItemResponse(
                        item.getId(),
                        item.getProductId(),
                        item.getQuantity()))
                .toList();

        return new CartResponse(
                cart.getId(),
                cart.getUserId(),
                cart.getStatus(),
                items);
    }
    public CartResponse increaseItemQuantity(Integer cartId, Integer productId) {
    Cart cart = findActiveCart(cartId);

    CartItem item = findCartItem(cartId, productId);
    int newQuantity = item.getQuantity() + 1;

    productClient.checkQuantity(productId, newQuantity);

    item.setQuantity(newQuantity);
    return toResponse(cartItemRepository.save(item).getCart());
}
public CartResponse decreaseItemQuantity(Integer cartId, Integer productId) {
    Cart cart = findActiveCart(cartId);

    CartItem item = findCartItem(cartId, productId);

    if (item.getQuantity() == 1) {
        cart.getItems().remove(item); // orphanRemoval deletes it
    } else {
        item.setQuantity(item.getQuantity() - 1);
    }

    return toResponse(cart);
}

    @Override
    @Transactional
    public void removeProductFromAllActiveCarts(Integer productId) {
        // Invoked when a product becomes unavailable or is deleted by ProductApp.
        cartItemRepository.deleteProductFromActiveCarts(
                productId,
                CartStatus.ACTIVE
        );
    }
}
