package com.oracle.requestapp.services.implementations;

import com.oracle.requestapp.dto.CreateProductRequest;
import com.oracle.requestapp.dto.UpdateRequestStatus;
import com.oracle.requestapp.entities.ProductRequest;
import com.oracle.requestapp.entities.RequestAction;
import com.oracle.requestapp.entities.RequestStatus;
import com.oracle.requestapp.events.ProductRequestEventPublisher;
import com.oracle.requestapp.exceptions.InvalidRequestStateException;
import com.oracle.requestapp.repositories.ProductRequestRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductRequestServiceImplTests {
    

    private final ProductRequestRepository repository =
            mock(ProductRequestRepository.class);
    private final ProductRequestEventPublisher eventPublisher =
            mock(ProductRequestEventPublisher.class);

    private final ProductRequestServiceImpl service =
            new ProductRequestServiceImpl(
                    repository,
                    eventPublisher,
                    "http://example.test/products"
            );

    @Test
    void createProductRequestDoesNotRequireProductId() {
        when(repository.save(any(ProductRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(7, new CreateProductRequest(
                RequestAction.CREATE, null, "Rice", "Seed Brand", "Grains", "Rice",
                new BigDecimal("250.00"), 20, 5, "New item", "rice, grain", "basmati rice",
                1.0, "kg", true, "data:image/png;base64,aGVsbG8=", "rice.png",
                "New product request", null));

        assertEquals(RequestStatus.PENDING, response.status());
        assertEquals(7, response.employeeId());
        assertEquals("Rice", response.name());
    }

    @Test
    void rejectionRequiresReason() {
        ProductRequest pending = new ProductRequest(
                7, 4, RequestAction.RESTOCK, null, null, null, null, null, 10, null,
                null, null, null, null, null, null, null, null, null, null);
        when(repository.findById(9)).thenReturn(Optional.of(pending));

        assertThrows(InvalidRequestStateException.class,
                () -> service.updateStatus(9, 1, new UpdateRequestStatus(RequestStatus.REJECTED, " ")));
        verify(repository, never()).save(any());
    }

    @Test
    void approvedRequestCannotBeDecidedAgain() {
        ProductRequest approved = new ProductRequest(
                7, 4, RequestAction.RESTOCK, null, null, null, null, null, 10, null,
                null, null, null, null, null, null, null, null, null, null);
        approved.review(RequestStatus.APPROVED, null, 1);
        when(repository.findById(9)).thenReturn(Optional.of(approved));

        assertThrows(InvalidRequestStateException.class,
                () -> service.updateStatus(9, 1, new UpdateRequestStatus(RequestStatus.REJECTED, "No longer needed")));
    }
}
