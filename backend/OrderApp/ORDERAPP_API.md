# OrderApp Insomnia API Test Guide

This guide tests the OrderApp HTTP API and the synchronous REST orchestration it performs with UserApp, EmployeeApp, ProductsApp, CartApp, and FundsApp.

OrderApp runs on port `8085` and owns only order, order-item, checkout-attempt, and cancellation-attempt data. It must not query another service's database.

## Service ports

| Service | Port | Base URL |
| --- | ---: | --- |
| UserApp | 8080 | `http://localhost:8080` |
| AdminApp | 8081 | `http://localhost:8081` |
| EmployeeApp | 8082 | `http://localhost:8082` |
| ProductsApp | 8083 | `http://localhost:8083` |
| CartApp | 8084 | `http://localhost:8084` |
| OrderApp | 8085 | `http://localhost:8085` |
| RequestApp | 8086 | `http://localhost:8086` |
| TicketApp | 8087 | `http://localhost:8087` |
| FundsApp | 8088 | `http://localhost:8088` |
| BankApp | 8089 | `http://localhost:8089` |

AdminApp, RequestApp, TicketApp, and BankApp are not called by the current OrderApp checkout, query, status-update, or cancellation flows.

## Database migration

The order owner is named `userId` throughout OrderApp, and the `orders` table column is `user_id`.

For an existing Oracle database that still contains `customer_id`, back up the schema and run this once before starting the updated OrderApp:

```sql
ALTER TABLE orders RENAME COLUMN customer_id TO user_id;
```

Do not rely on `spring.jpa.hibernate.ddl-auto=update` to interpret a column rename. A fresh schema requires no manual migration.

## Insomnia environment

Create an Insomnia environment with the following JSON. Replace identifiers with records that exist in your running services.

```json
{
  "userBase": "http://localhost:8080",
  "employeeBase": "http://localhost:8082",
  "productsBase": "http://localhost:8083",
  "cartBase": "http://localhost:8084",
  "orderBase": "http://localhost:8085",
  "fundsBase": "http://localhost:8088",
  "userId": 41,
  "employeeId": 7,
  "cartId": 25,
  "productId": 10,
  "orderNumber": "replace-after-checkout",
  "checkoutKey": "checkout-insomnia-001",
  "cancelKey": "cancel-insomnia-001",
  "from": "2026-09-01T00:00:00",
  "to": "2026-10-01T00:00:00"
}
```

Use `{{ _.variableName }}` in Insomnia to reference a value. Generate a new checkout or cancellation key whenever you want a new operation. Retain the same key only when testing an idempotent retry.

## Preconditions

Before checkout:

1. User `{{ _.userId }}` must exist, be valid, and have a nonblank delivery address.
2. Cart `{{ _.cartId }}` must belong to that user, have status `ACTIVE`, and contain at least one item.
3. ProductsApp must have sufficient current stock for every cart item. CartApp only verifies availability when adding an item; OrderApp performs the atomic decrement during checkout.
4. FundsApp must contain sufficient funds for the user.
5. Employee `{{ _.employeeId }}` must be valid before employee operations can be tested.

### Verify the user contract

```http
GET {{ _.userBase }}/grocers/api/users/{{ _.userId }}/verification
```

Expected shape:

```json
{
  "userId": 41,
  "valid": true,
  "email": "user@example.com",
  "deliveryAddress": "12 Market Road"
}
```

### Verify the employee contract

```http
GET {{ _.employeeBase }}/grocers/api/employees/{{ _.employeeId }}/verification
```

Expected shape:

```json
{
  "employeeId": 7,
  "valid": true
}
```

### Inspect the active cart

```http
GET {{ _.cartBase }}/grocers/api/carts/{{ _.cartId }}
```

Expected shape:

```json
{
  "id": 25,
  "userId": 41,
  "status": "ACTIVE",
  "checkedOutOrderNumber": null,
  "items": [
    {
      "productId": 10,
      "quantity": 2
    }
  ]
}
```

## OrderApp requests

### 1. Health check

```http
GET {{ _.orderBase }}/actuator/health
```

Expected status: `200 OK`.

### 2. Checkout a cart

```http
POST {{ _.orderBase }}/grocers/api/orders/checkout
X-User-Id: {{ _.userId }}
Idempotency-Key: {{ _.checkoutKey }}
X-Correlation-Id: insomnia-checkout-001
Content-Type: application/json
```

Body:

```json
{
  "cartId": 25
}
```

Use the numeric value from `{{ _.cartId }}` if it differs from the example.

Expected status: `201 Created`.

Expected response shape:

```json
{
  "orderNumber": "ORD-20260916110000-ABC12345",
  "userId": 41,
  "cartId": 25,
  "status": "PLACED",
  "totalAmount": 160.0,
  "deliveryAddress": "12 Market Road",
  "updatedByEmployeeId": null,
  "cancellationReason": null,
  "orderedAt": "2026-09-16T11:00:00",
  "updatedAt": "2026-09-16T11:00:00",
  "items": [
    {
      "productId": 10,
      "productName": "Rice",
      "quantity": 2,
      "unitPrice": 80.0,
      "subtotal": 160.0
    }
  ]
}
```

Copy `orderNumber` into the Insomnia `orderNumber` environment property.

OrderApp performs these calls in order:

1. Verify the user through UserApp.
2. Read and validate the cart through CartApp.
3. Atomically decrement stock through ProductsApp.
4. Debit the user through FundsApp.
5. Save the order locally.
6. Check out the cart through CartApp.

### 3. Retry checkout idempotently

Send request 2 again with the same `Idempotency-Key`, user, and cart.

Expected result: the same order is returned and ProductsApp, FundsApp, and CartApp mutations are not repeated. Reusing the key with a different user or cart returns `409 Conflict` with code `IDEMPOTENCY_KEY_REUSED`.

### 4. Get one user order

```http
GET {{ _.orderBase }}/grocers/api/orders/{{ _.orderNumber }}
X-User-Id: {{ _.userId }}
```

Expected status: `200 OK`. OrderApp verifies the caller through UserApp and returns `403 Forbidden` if the order belongs to another user.

### 5. Get user order history

```http
GET {{ _.orderBase }}/grocers/api/orders/users/{{ _.userId }}
X-User-Id: {{ _.userId }}
```

Expected status: `200 OK`. Results are ordered newest first. The caller cannot request another user's history.

### 6. List orders as an employee

All orders:

```http
GET {{ _.orderBase }}/grocers/api/orders/employee
X-Employee-Id: {{ _.employeeId }}
```

Filter by status:

```http
GET {{ _.orderBase }}/grocers/api/orders/employee?status=PLACED
X-Employee-Id: {{ _.employeeId }}
```

Valid status values are `PLACED`, `SHIPPED`, `OUT_FOR_DELIVERY`, `DELIVERED`, and `CANCELLED`. OrderApp verifies the caller through EmployeeApp.

### 7. Advance order status

The only allowed delivery path is `PLACED` → `SHIPPED` → `OUT_FOR_DELIVERY` → `DELIVERED`. Send these requests sequentially for an order that you do not plan to cancel.

```http
PATCH {{ _.orderBase }}/grocers/api/orders/{{ _.orderNumber }}/status
X-Employee-Id: {{ _.employeeId }}
Content-Type: application/json
```

First body:

```json
{
  "status": "SHIPPED"
}
```

Second body:

```json
{
  "status": "OUT_FOR_DELIVERY"
}
```

Third body:

```json
{
  "status": "DELIVERED"
}
```

Expected status: `200 OK` for each valid next step. Skipping or reversing a step returns `409 Conflict` with code `INVALID_STATUS_TRANSITION`. A delivered order cannot be cancelled.

### 8. Generate an employee report

All orders in a time range:

```http
GET {{ _.orderBase }}/grocers/api/orders/reports?from={{ _.from }}&to={{ _.to }}
X-Employee-Id: {{ _.employeeId }}
```

Filter by user and product:

```http
GET {{ _.orderBase }}/grocers/api/orders/reports?from={{ _.from }}&to={{ _.to }}&userId={{ _.userId }}&productId={{ _.productId }}
X-Employee-Id: {{ _.employeeId }}
```

`from` is inclusive and `to` is exclusive. `userId` and `productId` are optional. Expected response shape:

```json
{
  "orderCount": 1,
  "totalRevenue": 160.0,
  "orders": [
    {
      "orderNumber": "ORD-20260916110000-ABC12345",
      "userId": 41,
      "status": "PLACED",
      "totalAmount": 160.0,
      "orderedAt": "2026-09-16T11:00:00"
    }
  ]
}
```

An invalid or empty date range returns `400 Bad Request` with code `INVALID_REPORT_RANGE`.

### 9. Cancel an order as an employee

Create a second order and save its number in `orderNumber`; do not advance it to `DELIVERED`.

```http
POST {{ _.orderBase }}/grocers/api/orders/{{ _.orderNumber }}/cancel
X-Employee-Id: {{ _.employeeId }}
Idempotency-Key: {{ _.cancelKey }}
X-Correlation-Id: insomnia-cancel-001
Content-Type: application/json
```

Body:

```json
{
  "reason": "User requested cancellation"
}
```

Expected status: `200 OK` with `status` set to `CANCELLED`, `updatedByEmployeeId` set to the employee, and `cancellationReason` populated.

OrderApp performs these calls in order:

1. Verify the employee through EmployeeApp.
2. Restore the order's stock through ProductsApp.
3. Refund the stored order total to the order's `userId` through FundsApp.
4. Set the local order status to `CANCELLED`.

### 10. Retry cancellation idempotently

Send request 9 again with the same cancellation key, employee, order number, and reason.

Expected result: the same cancelled order is returned. Completed stock restoration and refund operations are not repeated. Reusing the key for another order, employee, or reason returns `409 Conflict` with code `IDEMPOTENCY_KEY_REUSED`.

## Exact downstream REST contracts

These calls are made by OrderApp automatically. Do not submit the mutation calls manually during a normal checkout or cancellation test because doing so can duplicate business operations in downstream services. They are listed for contract verification and troubleshooting.

Every downstream request receives `X-Correlation-Id`. Mutation idempotency is carried in the JSON `operationKey`.

### Checkout orchestration

#### UserApp verification

```http
GET {{ _.userBase }}/grocers/api/users/{{ _.userId }}/verification
```

#### CartApp read

```http
GET {{ _.cartBase }}/grocers/api/carts/{{ _.cartId }}
```

#### ProductsApp atomic decrement

```http
POST {{ _.productsBase }}/grocers/api/products/inventory/decrements
Content-Type: application/json
```

Body generated by OrderApp:

```json
{
  "operationKey": "<orderNumber>:inventory-decrement",
  "orderNumber": "<orderNumber>",
  "items": [
    {
      "productId": 10,
      "quantity": 2
    }
  ]
}
```

ProductsApp must atomically reject the entire request if any item lacks sufficient stock. A successful response uses status `DECREMENTED` and supplies authoritative `Double` values for `unitPrice` and `subtotal`.

#### FundsApp debit

```http
POST {{ _.fundsBase }}/grocers/api/funds/debits
Content-Type: application/json
```

Body generated by OrderApp:

```json
{
  "operationKey": "<orderNumber>:funds-debit",
  "orderNumber": "<orderNumber>",
  "userId": 41,
  "amount": 160.0
}
```

A successful response must contain the same order number, user ID, and amount, plus a finite `remainingBalance` and type `DEBIT`.

#### CartApp checkout

```http
POST {{ _.cartBase }}/grocers/api/carts/{{ _.cartId }}/checkout
Content-Type: application/json
```

Body generated by OrderApp:

```json
{
  "operationKey": "<orderNumber>:cart-checkout",
  "orderNumber": "<orderNumber>",
  "userId": 41
}
```

CartApp must respond with the same cart and user, status `CHECKED_OUT`, and `checkedOutOrderNumber` equal to the order number.

### Checkout compensation

If a later checkout step fails, OrderApp explicitly compensates every downstream mutation that may have been attempted. Depending on where failure occurred, it calls:

#### CartApp restore

```http
POST {{ _.cartBase }}/grocers/api/carts/{{ _.cartId }}/restore
Content-Type: application/json
```

```json
{
  "operationKey": "<orderNumber>:cart-restore",
  "orderNumber": "<orderNumber>",
  "userId": 41
}
```

#### FundsApp refund after failed checkout

```http
POST {{ _.fundsBase }}/grocers/api/funds/refunds
Content-Type: application/json
```

```json
{
  "operationKey": "<orderNumber>:funds-refund",
  "orderNumber": "<orderNumber>",
  "userId": 41,
  "amount": 160.0
}
```

#### ProductsApp restore after failed checkout

```http
POST {{ _.productsBase }}/grocers/api/products/inventory/restores
Content-Type: application/json
```

```json
{
  "operationKey": "<orderNumber>:inventory-restore",
  "orderNumber": "<orderNumber>"
}
```

If every compensation succeeds, the original checkout error is returned and the attempt becomes terminally compensated. If any compensation fails, OrderApp returns `503 Service Unavailable` with code `COMPENSATION_INCOMPLETE`; retrying the original checkout key resumes recovery.

### Employee cancellation orchestration

#### EmployeeApp verification

```http
GET {{ _.employeeBase }}/grocers/api/employees/{{ _.employeeId }}/verification
```

#### ProductsApp cancellation restore

```http
POST {{ _.productsBase }}/grocers/api/products/inventory/restores
Content-Type: application/json
```

```json
{
  "operationKey": "<orderNumber>:cancel-inventory",
  "orderNumber": "<orderNumber>"
}
```

#### FundsApp cancellation refund

```http
POST {{ _.fundsBase }}/grocers/api/funds/refunds
Content-Type: application/json
```

```json
{
  "operationKey": "<orderNumber>:cancel-refund",
  "orderNumber": "<orderNumber>",
  "userId": 41,
  "amount": 160.0
}
```

Cancellation retries resume from the last stored step. OrderApp does not repeat a completed inventory restore or funds refund.

## Failure and validation requests

### Missing authenticated user identity

Send checkout without `X-User-Id`.

Expected status: `400 Bad Request`, code `MISSING_HEADER`.

### Empty or invalid cart ID

```http
POST {{ _.orderBase }}/grocers/api/orders/checkout
X-User-Id: {{ _.userId }}
Idempotency-Key: checkout-invalid-cart
Content-Type: application/json
```

```json
{
  "cartId": 0
}
```

Expected status: `400 Bad Request`, code `VALIDATION_FAILED`.

### Missing idempotency key

Send checkout or cancellation without `Idempotency-Key`.

Expected status: `400 Bad Request`, code `MISSING_HEADER` when the header is absent. A present but blank key is rejected with `IDEMPOTENCY_KEY_REQUIRED`.

### Invalid employee identity

Use a nonexistent or invalid `X-Employee-Id` for an employee endpoint.

Expected status: `403 Forbidden`, code `INVALID_EMPLOYEE`.

### Dependency failure mapping

If a downstream service is unavailable or times out, OrderApp returns `503 Service Unavailable` with code `DOWNSTREAM_UNAVAILABLE`. A malformed or contradictory downstream response returns `502 Bad Gateway` with code `DOWNSTREAM_CONTRACT_ERROR`.

Error responses contain:

```json
{
  "timestamp": "2026-09-16T11:00:00",
  "status": 400,
  "code": "VALIDATION_FAILED",
  "message": "Request validation failed",
  "path": "/grocers/api/orders/checkout",
  "correlationId": "insomnia-checkout-001",
  "fieldErrors": []
}
```

## Suggested end-to-end run order

1. Start UserApp, EmployeeApp, ProductsApp, CartApp, FundsApp, and OrderApp.
2. Run the three prerequisite verification requests.
3. Checkout an active cart and save the returned order number.
4. Retry checkout with the same key and confirm the same order is returned.
5. Test order detail, user history, employee list, and reports.
6. Use one order to test every delivery status transition.
7. Create another active cart/order and use it to test cancellation and cancellation retry.
8. Inspect ProductsApp, FundsApp, and CartApp through their public GET APIs or service logs to confirm stock, balance, and cart state changes. Do not verify cross-service behavior by querying their databases from OrderApp.
9. In a controlled test environment, inject a downstream failure after a mutation and verify the compensation calls described above.
