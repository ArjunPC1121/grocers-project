# RequestApp

RequestApp is the review queue between employees and administrators for catalogue changes. Employees never change the catalogue directly: they submit a request, and an administrator approves or rejects it. Approval calls ProductsApp to apply the requested change.

## Features

- Employees can request product creation, updates, restocks, and deletion.
- Employees can see only their own requests through the `my` endpoint.
- Administrators can filter every request by status, employee, and action.
- Requests follow a controlled state flow: `PENDING` → `PROCESSING`, `APPROVED`, or `REJECTED`; a processing request can return to `PENDING` or finish as `APPROVED`.
- Rejecting requires a human-readable reason.
- Approval applies the change to ProductsApp only after RequestApp validates the transition.
- New requests publish a `product-request-created` Kafka event. AdminApp consumes it and stores an admin notification.

## API

Reach this service through GatewayApp at `http://localhost:8091/grocers/api`.

| Method | Route | Role | Purpose |
| --- | --- | --- | --- |
| `POST` | `/requests` | Employee | Submit a catalogue request. |
| `GET` | `/requests/my?status=PENDING` | Employee | List the signed-in employee's requests; `status` is optional. |
| `GET` | `/requests/{id}` | Employee/Admin | Get one request. Employees may view only their own. |
| `GET` | `/requests?status=&employeeId=&action=` | Admin | Filter the review queue. All filters are optional. |
| `PATCH` | `/requests/{id}/status` | Admin | Move a request through the valid review flow. |

The gateway provides identity through `X-Authenticated-*` headers. Clients must send a Bearer token to GatewayApp and must never create those headers themselves.

### Create request body

```json
{
  "action": "UPDATE",
  "productId": 42,
  "price": 89.00,
  "quantity": 30,
  "description": "Updated pack details",
  "previousValues": "price: 95.00; quantity: 20"
}
```

Validation rules are action-specific:

- `CREATE` requires name, category, price, quantity, discount, and an image URL.
- `UPDATE` requires a product ID and at least one changed product field.
- `RESTOCK` requires a product ID and a positive quantity.
- `DELETE` requires a product ID and a reason.

### Review body

```json
{ "status": "REJECTED", "rejectionReason": "The supplier price is not confirmed." }
```

`rejectionReason` is required only for `REJECTED`.

## Dependencies and configuration

| Dependency | Why it is used |
| --- | --- |
| Oracle Database | Stores the request record and review decision. |
| ProductsApp | Applies approved create/update/restock/delete changes. |
| Kafka | Publishes `product-request-created` for AdminApp notifications. |
| GatewayApp | Verifies JWTs and injects caller identity headers. |

`services.products-url` and `spring.kafka.bootstrap-servers` are defined in `src/main/resources/application.properties`. Keep `ddl-auto=update` while preserving existing request history.

## Run and test

```cmd
cd backend\requestapp
mvn clean spring-boot:run
```

For an end-to-end approval test, start ProductsApp, RequestApp, AdminApp, Kafka, and GatewayApp. Submit a request as an employee, review it as an admin, then confirm the product change in ProductsApp and the notification in AdminApp.
