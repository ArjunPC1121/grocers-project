# OrderApp API

OrderApp runs on port `8085` and owns only order, order-item, checkout-attempt, and cancellation-attempt data. User, Employee, Cart, Products, and Funds interactions are synchronous REST calls configured in `application.properties`.

Clients may send `X-Correlation-Id`; OrderApp returns it on the response and forwards it to every downstream REST call. If it is absent, OrderApp uses the idempotency key when available or generates a correlation id.

All examples use PowerShell and assume the service is running locally.

```powershell
$base = 'http://localhost:8085/grocers/api/orders'
```

## Checkout

```powershell
$headers = @{ 'X-User-Id' = '41'; 'Idempotency-Key' = 'checkout-001' }
$body = @{ cartId = 25 } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "$base/checkout" -Headers $headers -ContentType 'application/json' -Body $body
```

The response is `201 Created` with a `PLACED` order. Repeating the same request with the same key returns the same order and does not repeat inventory, funds, or cart mutations. Reusing the key for another user or cart returns `409 IDEMPOTENCY_KEY_REUSED`.

## Customer order detail and history

```powershell
Invoke-RestMethod -Method Get -Uri "$base/ORD-20260916110000-ABC12345" -Headers @{ 'X-User-Id' = '41' }
Invoke-RestMethod -Method Get -Uri "$base/users/41" -Headers @{ 'X-User-Id' = '41' }
```

The caller is verified through UserApp and can read only their own orders.

## Employee order view and status update

```powershell
$employee = @{ 'X-Employee-Id' = '7' }
Invoke-RestMethod -Method Get -Uri "$base/employee?status=PLACED" -Headers $employee

$statusBody = @{ status = 'SHIPPED' } | ConvertTo-Json
Invoke-RestMethod -Method Patch -Uri "$base/ORD-20260916110000-ABC12345/status" -Headers $employee -ContentType 'application/json' -Body $statusBody
```

Allowed transitions are `PLACED -> SHIPPED -> OUT_FOR_DELIVERY -> DELIVERED`.

## Employee cancellation

```powershell
$cancelHeaders = @{ 'X-Employee-Id' = '7'; 'Idempotency-Key' = 'cancel-001' }
$cancelBody = @{ reason = 'Customer requested cancellation' } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "$base/ORD-20260916110000-ABC12345/cancel" -Headers $cancelHeaders -ContentType 'application/json' -Body $cancelBody
```

Cancellation restores Products stock, refunds the stored order total through FundsApp, and then stores `CANCELLED`. A retry resumes from the last durable step and does not repeat completed mutations.

## Reports

```powershell
Invoke-RestMethod -Method Get -Uri "$base/reports?from=2026-09-01T00:00:00&to=2026-10-01T00:00:00&customerId=41&productId=10" -Headers @{ 'X-Employee-Id' = '7' }
```

`from` is inclusive and `to` is exclusive. Filters are optional except for the date range.

## Error contract

Errors include `timestamp`, HTTP `status`, stable `code`, `message`, request `path`, `correlationId`, and validation `fieldErrors`. Dependency timeouts return `503 DOWNSTREAM_UNAVAILABLE`; malformed dependency responses return `502 DOWNSTREAM_CONTRACT_ERROR`; an incomplete checkout reversal returns `503 COMPENSATION_INCOMPLETE`.

## Operations and configuration

`GET /actuator/health` provides a service health check. Database URL, username, password, schema mode, and SQL logging are configurable with `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DDL_AUTO`, and `SHOW_SQL`.
