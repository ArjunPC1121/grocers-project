package com.oracle.userapp.controllers;

import com.oracle.userapp.dto.*;
import com.oracle.userapp.entities.SecretQuestion;
import com.oracle.userapp.services.implementations.UserService;
import com.oracle.userapp.services.implementations.WishlistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.oracle.userapp.dto.CustomerOrderSummaryResponse;
import com.oracle.userapp.repositories.CustomerOrderSummaryRepository;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/grocers/api/users")
// Defines the HTTP endpoints used to manage users, wallets, orders, and wishlists.
public class UserAppController {

    /*
    Required dependency injections.
    userService for accessing user CRUD operations and other utilities
    wishlistService to enable wishlist feature for users
    customerOrderSummaryRepository to track orders using kafka
     */
    private final UserService userService;
    private final WishlistService wishlistService;
    private final CustomerOrderSummaryRepository customerOrderSummaryRepository;

    public UserAppController(
            UserService userService,
            WishlistService wishlistService,
            CustomerOrderSummaryRepository customerOrderSummaryRepository
    ) {
        this.userService = userService;
        this.wishlistService = wishlistService;
        this.customerOrderSummaryRepository = customerOrderSummaryRepository;
    }

    //Used during registration - made public in gateway
    @PostMapping
    public ResponseEntity<UserResponse> add(@Valid @RequestBody UserRequest request) {
        UserResponse response = userService.add(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Admin creation does not accept a caller-supplied password; it returns a generated one once. */
    @PostMapping("/admin")
    public ResponseEntity<AdminCreatedUserResponse> addByAdmin(@Valid @RequestBody AdminUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.addByAdmin(request));
    }

    @GetMapping
    // Returns the public details of every user.
    public ResponseEntity<Collection<UserResponse>> getAll() {
        return ResponseEntity.ok(userService.getAll());
    }

    @GetMapping("/{id}")
    // Returns one user's public details by ID.
    public ResponseEntity<UserResponse> get(@PathVariable Integer id) {
        return ResponseEntity.ok(userService.get(id));
    }

    @PatchMapping("/{id}")
    // Changes only the user details supplied in the request.
    public ResponseEntity<UserResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    // Deletes a user and returns the details that were removed.
    public ResponseEntity<UserResponse> delete(@PathVariable Integer id) {
        return ResponseEntity.ok(userService.delete(id));
    }

    /*
    Called by Auth app in AuthService -> userLogin()
     */
    @PostMapping("/{id}/failed-attempts")
    public ResponseEntity<Integer> incrementFailedAttempts(@PathVariable Integer id) {
        return ResponseEntity.ok(userService.incFailedAttempts(id));
    }

    @PostMapping("/{id}/funds")
    // Moves money from the user's bank account into their app wallet.
    public ResponseEntity<Double> addFunds(
            @PathVariable Integer id,
            @RequestBody AddFundsRequest request) {
        return ResponseEntity.ok(userService.addFunds(id, request.amount()));
    }

    /*
    Called by OrderApp upon checkout in UserService-> checkout() -> userClient.debit()
     */

    @PostMapping("/{id}/debit")
    // Removes money from the user's wallet when an order is paid for.
    public ResponseEntity<Double> deductFunds(
            @PathVariable Integer id,
            @RequestBody AddFundsRequest request) {
        return ResponseEntity.ok(userService.deductFunds(id, request.amount()));
    }

    @PostMapping("/{id}/tickets")
    public ResponseEntity<TicketResponse> raiseTicket(@PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.raiseTicket(id));
    }

    /** Called internally by AuthApp after a locked customer fails self-service recovery. */
    @PostMapping("/tickets")
    public ResponseEntity<TicketResponse> raiseLockedAccountTicket(@Valid @RequestBody PublicTicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.raiseTicketByEmail(request));
    }

    /*
    Called by Employee app in EmplpoyeeService -> withCustomerDetails()
     */
    @GetMapping("/{id}/ticket-details")
    public ResponseEntity<TicketUserDetails> ticketDetails(@PathVariable Integer id) {
        return ResponseEntity.ok(userService.ticketDetails(id));
    }

    /*
    Called by Order app in service -> cancelOrder()
     */
    @PostMapping("/{id}/refund")
    public ResponseEntity<String> refund(@PathVariable Integer id, @RequestBody OrderCancelRequest request)
    {
        return ResponseEntity.ok("Order "+request.reference()+" cancelled :(\nNew fund balance : "+userService.refund(id, request.amount()));
    }

    /*
    Called by Ticket app -> unlockUser() function
     */
    @PostMapping("/{id}/unlock")
    public ResponseEntity<String> unlockUser(@PathVariable Integer id)
    {
        return ResponseEntity.ok("User "+userService.unlock(id)+" unlocked!");
    }

    /*
    Called by auth app
     */
    @PostMapping("/{id}/secret-answer")
    public ResponseEntity<String> verifySecretAnswer(
            @PathVariable Integer id,
            @Valid @RequestBody SecretAnswerRequest request) {

        return ResponseEntity.ok(
                userService.verifySecretAnswer(id, request)
        );
    }

    @PostMapping("/{id}/password-reset")
    public ResponseEntity<Void> resetPassword(
            @PathVariable Integer id,
            @Valid @RequestBody ResetPasswordRequest request) {

        userService.resetPassword(id, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/secret-question")
    public ResponseEntity<SecretQuestion> getSecretQuestion(
            @PathVariable Integer id) {

        return ResponseEntity.ok(userService.getSecretQuestion(id));
    }
    @PostMapping("/{userId}/wishlist/{productId}")
    public ResponseEntity<WishlistItemResponse> addWishlistItem(
            @PathVariable Integer userId,
            @PathVariable Integer productId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(wishlistService.addItem(userId, productId));
    }

    @GetMapping("/{userId}/wishlist")
    public ResponseEntity<List<WishlistItemResponse>> getWishlist(
            @PathVariable Integer userId
    ) {
        return ResponseEntity.ok(
                wishlistService.getItems(userId)
        );
    }

    @DeleteMapping("/{userId}/wishlist/{productId}")
    public ResponseEntity<Void> removeWishlistItem(
            @PathVariable Integer userId,
            @PathVariable Integer productId
    ) {
        wishlistService.removeItem(userId, productId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/orders")
    // Returns this user's saved order history, newest order first.
    public ResponseEntity<List<CustomerOrderSummaryResponse>> getOrders(
            @PathVariable Integer userId
    ) {
        List<CustomerOrderSummaryResponse> orders =
                customerOrderSummaryRepository
                        .findByCustomerIdOrderByCheckedOutAtDesc(userId)
                        .stream()
                        .map(CustomerOrderSummaryResponse::from)
                        .toList();

        return ResponseEntity.ok(orders);
    }
    @PatchMapping("/{id}/password")
    // Lets an authenticated user change only their own password.
    public ResponseEntity<Void> changePassword(
            @PathVariable Integer id,
            @RequestHeader("X-Authenticated-User-Id") Integer authenticatedUserId,
            @Valid @RequestBody ChangePasswordRequest request) {

        if (!id.equals(authenticatedUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only change your own password"
            );
        }

        userService.changePassword(id, request);
        return ResponseEntity.noContent().build();
    }


}
