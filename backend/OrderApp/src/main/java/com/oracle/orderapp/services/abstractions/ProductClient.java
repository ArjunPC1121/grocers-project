package com.oracle.orderapp.services.abstractions;
import com.oracle.orderapp.dtos.clients.*;
public interface ProductClient {
    InventoryResponse decrement(InventoryDecrementRequest request);
    InventoryResponse restore(InventoryRestoreRequest request);
}
