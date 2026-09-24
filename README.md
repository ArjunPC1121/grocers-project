# Grocers

Grocers is a full-stack grocery ordering and fulfilment platform. It provides a customer storefront, JWT authentication and recovery, product and cart management, checkout and order tracking, employee workflows, administrator operations, and a recipe-to-grocery assistant.

> **Documentation status:** This all-in-one README is the project’s source of truth for the implemented SRS and HTTP API. Routes, ports, public access, and contracts were derived from the current Spring controllers and gateway configuration. It documents the code as it exists, not unimplemented features.

## Contents

- [System requirements specification](#system-requirements-specification)
- [Architecture](#architecture)
- [Getting started](#getting-started)
- [API documentation](#api-documentation)
- [Project structure](#project-structure)
- [Development and quality](#development-and-quality)

## System requirements specification

### Purpose and scope

The system lets customers discover grocery products, maintain carts and wishlists, place and manage orders, recover locked accounts, and receive recipe-based purchase recommendations. Employees handle fulfilment work, inventory requests, tickets, and assigned-order status updates. Administrators manage accounts, catalog data, inventory requests, dashboards, and reports.

### Actors and functional requirements

| Actor | Implemented capabilities |
| --- | --- |
| Visitor | Register, browse/search products, log in, and perform locked-account recovery. |
| Customer (`USER`) | Manage profile/password, funds, cart, wishlist, orders, and recipe recommendations. |
| Employee (`EMPLOYEE`) | Change password, manage tickets, submit product requests, view fulfilment orders, and update their statuses. |
| Administrator (`ADMIN`) | Manage admins, customers, employees, products, requests, dashboard data, and reports. |
| Internal services | AuthApp can use narrowly scoped gateway-internal calls for failed-login and recovery operations. |

| ID | Requirement |
| --- | --- |
| FR-01 | Expose catalog listing, lookup, search, inventory adjustment, and image-upload APIs. |
| FR-02 | Allow customers to add, update, increment, decrement, remove, check out, and cancel cart items. |
| FR-03 | Create, retrieve, update, check out, cancel, and track grocery orders. |
| FR-04 | Authenticate users, employees, and administrators separately and issue role-bearing JWTs. |
| FR-05 | Support locked-account ticketing and security-question password recovery. |
| FR-06 | Let employees manage fulfilment, tickets, and product requests; let administrators manage operational data. |
| FR-07 | Produce recipe recommendations by matching requested ingredients against the product catalog and optional budget. |
| FR-08 | Provide a responsive web UI for customer, employee, and administrator workflows. |

### Non-functional requirements and constraints

| Area | Requirement / current design |
| --- | --- |
| Security | Browser requests enter through the gateway. Protected routes require a Bearer JWT; the gateway strips caller-supplied identity headers and injects trusted downstream headers. |
| Authorization | Gateway roles are `USER`, `EMPLOYEE`, and `ADMIN`; services may impose further business/ownership checks. |
| Data | Services use Oracle Database through Spring Data JPA. |
| Integration | OrderApp publishes checkout events and UserApp consumes them through Kafka for customer order summaries. |
| Availability | Services run independently on local ports and are routed by the gateway. |
| Web compatibility | CORS permits `FRONTEND_ORIGIN`, defaulting to `http://localhost:5173`. |
| Validation | Controller bodies use Jakarta validation where annotated; invalid input is a client error. |

### Assumptions and exclusions

- Container orchestration, deployment automation, and an OpenAPI generator are not committed at the repository root.
- The repository contains local development connection settings. Production secrets and database credentials must be externalized.
- The recipe assistant requires a Gemini API key and upstream availability.

## Architecture

```text
React + Vite client (5173)
          | HTTP + Bearer JWT
          v
Spring Cloud Gateway (8091) ── validates JWT / applies CORS / routes requests
          |
          +-- Auth (8090)       +-- User (8080)      +-- Admin (8081)
          +-- Employee (8082)   +-- Products (8083)  +-- Cart (8084)
          +-- Orders (8085)     +-- Requests (8086)  +-- Tickets (8087)
          +-- Funds (8088)      +-- Bank (8089)      +-- Assistant (8092)
                   |                    |                    |
                   +----------- Oracle Database ------------+
                                        |
                                      Kafka
                                (OrderApp -> UserApp)
```

All browser-facing routes use:

```text
http://localhost:8091/grocers/api
```

Do not call downstream service ports from the browser. Several services use a gateway-only guard and depend on identity headers injected by the gateway.

## Getting started

### Prerequisites

- Node.js compatible with Vite 8 and npm.
- JDK 17 (the gateway declares Java 17; use the JDK version in a service’s `pom.xml` if it differs).
- Oracle Database reachable at the JDBC URL configured per service.
- Apache Kafka at `localhost:9092` when testing the order-summary event flow.
- A Gemini API key for the recipe assistant.

### Configuration

Create `frontend/.env.local` locally:

```dotenv
VITE_API_BASE_URL=http://localhost:8091/grocers/api
```

Set backend process environment values before starting relevant services:

```powershell
$env:FRONTEND_ORIGIN = "http://localhost:5173"
$env:GATEWAY_INTERNAL_SECRET = "replace-with-a-long-shared-secret"
$env:KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
$env:GEMINI_API_KEY = "your-key"
$env:GEMINI_MODEL = "gemini-3.5-flash-lite"
```

**Security action before shared deployment:** replace the JWT secret, gateway internal secret, and Oracle credentials currently present in local configuration; load them from environment variables or a secret manager. Rotate them if this repository has been shared.

### Start locally

1. Start Oracle Database and Kafka (if exercising order event flow).
2. In separate terminals, start the gateway and services required for your workflow:

   ```powershell
   cd backend/gatewayapp
   .\mvnw.cmd spring-boot:run
   ```

3. Start the frontend:

   ```powershell
   cd frontend
   npm ci
   npm run dev
   ```

4. Open the Vite URL, normally `http://localhost:5173`.

### Service directory and ports

| Service | Directory | Port | Gateway prefix |
| --- | --- | ---: | --- |
| Gateway | `backend/gatewayapp` | 8091 | — |
| Auth | `backend/authapp` | 8090 | `/auth` |
| User | `backend/userapp` | 8080 | `/users` |
| Admin | `backend/adminapp` | 8081 | `/admin` |
| Employee | `backend/EmployeeApp` | 8082 | `/employees` |
| Products | `backend/productsapp` | 8083 | `/products` |
| Cart | `backend/cartapp` | 8084 | `/carts` |
| Orders | `backend/OrderApp` | 8085 | `/orders` |
| Requests | `backend/requestapp` | 8086 | `/requests` |
| Tickets | `backend/ticketapp` | 8087 | `/tickets` |
| Funds | `backend/FundsApp` | 8088 | `/funds` |
| Bank | `backend/bankapp` | 8089 | `/banks` |
| Assistant | `backend/assistantapp` | 8092 | `/assistant` |

## API documentation

### Conventions

- Prefix every path with `http://localhost:8091/grocers/api`.
- JSON is the default content type. Product image endpoints accept `multipart/form-data` with an `image` file part.
- `Public` means the gateway permits no JWT. `Auth` means any authenticated role unless a role is listed. `Internal` is for AuthApp gateway-internal requests only.
- For protected endpoints send `Authorization: Bearer <accessToken>`. Do not send `X-Authenticated-*` headers; the gateway owns and replaces them.
- Typical success responses are `200`, `201` for creation, and `204` for no-content operations. Authentication failures are `401`; role/business access failures are `403`.

### Authentication and recovery

| Method | Path | Access | Purpose |
| --- | --- | --- | --- |
| POST | `/auth/login/user` | Public | Customer login. |
| POST | `/auth/login/employee` | Public | Employee login. |
| POST | `/auth/login/admin` | Public | Administrator login. |
| POST | `/auth/locked-account/ticket` | Public | Create locked-account support ticket. |
| GET | `/auth/locked-account/status?email=` | Public | Read recovery status. |
| GET | `/auth/locked-account/security-question?email=` | Public | Get recovery question. |
| POST | `/auth/locked-account/verify-security-answer` | Public | Verify answer and receive reset token. |
| POST | `/auth/locked-account/reset-password` | Public | Reset password using recovery token. |

```json
// POST /auth/login/user
{ "email": "customer@example.com", "password": "secret" }

// 200 response
{
  "accessToken": "<jwt>", "tokenType": "Bearer", "expiresInSeconds": 86400,
  "role": "USER", "mustChangePassword": false, "message": "..."
}
```

Recovery payloads: ticket `{ "email", "note" }`; answer `{ "email", "answer" }`; reset `{ "email", "resetToken", "newPassword" }`.

### Users, funds, and wishlist

| Method | Path | Access | Purpose |
| --- | --- | --- | --- |
| POST | `/users` | Public | Register customer. |
| POST | `/users/admin` | ADMIN | Create customer with generated temporary password. |
| GET | `/users` | Auth | List users. |
| GET / PATCH / DELETE | `/users/{id}` | Auth | Retrieve, update, or delete user. |
| POST | `/users/{id}/failed-attempts` | EMPLOYEE, ADMIN, Internal | Record failed login attempt. |
| POST | `/users/{id}/funds` | Auth | Add funds; body `{ "amount": number }`. |
| POST | `/users/{id}/debit` | Auth | Debit funds; body `{ "amount": number }`. |
| POST | `/users/{id}/tickets` | Auth | Raise ticket for a user. |
| POST | `/users/tickets` | Internal | Raise locked-account ticket by email. |
| GET | `/users/{id}/ticket-details` | Auth | Get ticket user details. |
| POST | `/users/{id}/refund` | Auth | Refund order-related amount. |
| POST | `/users/{id}/unlock` | EMPLOYEE, ADMIN | Unlock account. |
| POST | `/users/{id}/secret-answer` | Internal | Validate recovery answer. |
| POST | `/users/{id}/password-reset` | Internal | Set recovery password. |
| GET | `/users/{id}/secret-question` | Internal | Get stored recovery question. |
| POST / DELETE | `/users/{userId}/wishlist/{productId}` | Auth | Add or remove wishlist item. |
| GET | `/users/{userId}/wishlist` | Auth | List wishlist. |
| GET | `/users/{userId}/orders` | Auth | List event-derived order summaries. |
| PATCH | `/users/{id}/password` | USER (own ID) | Change password. |

Registration and update fields are defined in `backend/userapp/src/main/java/com/oracle/userapp/dto`; the server’s validation messages are authoritative for required fields.

### Products

| Method | Path | Access | Purpose |
| --- | --- | --- | --- |
| GET | `/products` | Public | List catalog. |
| GET | `/products/search?q={query}&limit={20}` | Public | Search products. |
| GET | `/products/{id}` | Public | Get product. |
| POST | `/products` | Auth | Create product. |
| PUT / DELETE | `/products/{id}` | Auth | Update or delete product. |
| POST | `/products/{id}/reduce-quantity` | Auth | Reduce inventory; `{ "quantity": positiveInteger }`. |
| POST | `/products/{id}/increase-quantity` | Auth | Increase inventory; `{ "quantity": positiveInteger }`. |
| POST | `/products/{id}/image` | Auth | Upload product image. |
| POST | `/products/images` | Auth | Upload request image. |

```json
{
  "name": "Bananas", "brand": "Farm Fresh", "category": "Fruits",
  "subCategory": "Bananas", "description": "...", "tags": "fruit,healthy",
  "searchAliases": "banana", "unitValue": 1, "unitType": "kg",
  "imageUrl": "https://...", "price": 80.0, "discount": 10,
  "quantity": 25, "active": true
}
```

`name`, `price`, and `quantity` are required. Price/unit values must be positive, quantity cannot be negative, and discount is 0–100.

### Carts and orders

| Method | Path | Access | Purpose |
| --- | --- | --- | --- |
| GET | `/carts` | Auth | List carts. |
| GET | `/carts/{cartId}` | Auth | Get cart. |
| GET | `/carts/users/{userId}/active` | Auth | Get active cart. |
| POST | `/carts/users/{userId}/items` | Auth | Add `{ "productId", "quantity" }`. |
| PATCH | `/carts/{cartId}/items/{productId}` | Auth | Set item quantity. |
| PATCH | `/carts/{cartId}/items/{productId}/increase` | Auth | Increase item by one. |
| PATCH | `/carts/{cartId}/items/{productId}/decrease` | Auth | Decrease item by one. |
| DELETE | `/carts/{cartId}/items/{productId}` | Auth | Remove item. |
| DELETE | `/carts/products/{productId}` | Auth | Remove product from active carts. |
| POST | `/carts/{cartId}/checkout` | Auth | Check out cart. |
| POST | `/carts/{cartId}/cancel` | Auth | Cancel cart. |
| POST | `/orders` | Auth | Create order. |
| GET | `/orders` | Auth | List orders. |
| GET | `/orders/employee-details` | EMPLOYEE, ADMIN | Employee fulfilment view. |
| GET | `/orders/{orderId}` | Auth | Get order. |
| GET | `/orders/customers/{customerId}` | Auth | Get customer orders. |
| PUT | `/orders/{orderId}/address` | Auth | Update delivery address. |
| POST | `/orders/{orderId}/checkout` | Auth | Check out order and publish event. |
| POST | `/orders/{orderId}/cancel` | Auth | Cancel with `{ "reason" }`. |
| POST | `/orders/{orderId}/employee-cancel` | Auth | Cancel with `{ "reason", "employeeId" }`. |
| DELETE | `/orders/{orderId}` | Auth | Delete an eligible order. |
| PATCH | `/orders/{orderId}/status` | Auth | Update order status. |
| GET | `/orders/status/{status}` | Auth | Filter by `OrderStatus`. |

```json
{
  "customerId": 42, "cartId": 7, "deliveryAddress": "10 Main Street",
  "paymentMethod": "<PaymentMethod>",
  "items": [{ "productId": 5, "quantity": 2 }]
}
```

### Employees, requests, and tickets

| Method | Path | Access | Purpose |
| --- | --- | --- | --- |
| GET / POST | `/employees` | EMPLOYEE, ADMIN | List or create employees. |
| GET | `/employees/{employeeId}` | EMPLOYEE, ADMIN | Get employee. |
| PUT | `/employees/{employeeId}/password` | EMPLOYEE, ADMIN | Change password. |
| PATCH | `/employees/{employeeId}/status` | EMPLOYEE, ADMIN | Set status. |
| GET | `/employees/tickets` | EMPLOYEE, ADMIN | List open tickets. |
| GET | `/employees/tickets/history` | EMPLOYEE, ADMIN | Caller’s ticket history. |
| POST | `/employees/tickets/{ticketId}/resolve` | EMPLOYEE, ADMIN | Resolve ticket. |
| POST | `/employees/tickets/{ticketId}/reject` | EMPLOYEE, ADMIN | Reject ticket. |
| POST / GET | `/employees/requests` | EMPLOYEE, ADMIN | Create/list caller inventory requests. |
| GET | `/employees/orders` | EMPLOYEE, ADMIN | List fulfilment orders. |
| PATCH | `/employees/orders/{orderId}/status` | EMPLOYEE, ADMIN | Update fulfilment status. |
| POST | `/requests` | EMPLOYEE | Create product request. |
| GET | `/requests/my?status=` | EMPLOYEE | List caller’s requests. |
| GET | `/requests/{id}` | EMPLOYEE, ADMIN | Get request. |
| GET | `/requests?status=&employeeId=&action=` | ADMIN | Filter all requests. |
| PATCH | `/requests/{id}/status` | ADMIN | Change request status. |
| POST / GET | `/tickets` | Auth | Create or list tickets; legacy `/api/tickets` also works. |
| GET | `/tickets/open` | Auth | List open tickets. |
| GET | `/tickets/user/{userId}/open` | Auth | Get user’s open ticket. |
| GET | `/tickets/employee/{employeeId}/history` | Auth | Employee ticket history. |
| GET | `/tickets/{ticketId}` | Auth | Get ticket. |
| POST | `/tickets/{ticketId}/resolve` | Auth | Resolve with `{ "employeeId": positiveInteger }`. |
| POST | `/tickets/{ticketId}/reject` | Auth | Reject with `{ "employeeId": positiveInteger }`. |

### Administration

All routes require `ADMIN`.

| Method | Path | Purpose |
| --- | --- | --- |
| GET / PATCH | `/admin/me` | Read/update authenticated admin profile. |
| PUT | `/admin/me/password` | Change password. |
| GET | `/admin/dashboard` | Operational dashboard. |
| GET / POST | `/admin/admins` | List/create normal admin accounts. |
| PUT / DELETE | `/admin/admins/{id}` | Update/delete normal admin. |
| GET / POST | `/admin/products` | List/create products. |
| PUT / DELETE | `/admin/products/{id}` | Update/delete product. |
| GET / POST | `/admin/employees` | List/create employees. |
| POST | `/admin/employees/{id}/activate` | Activate employee. |
| POST | `/admin/employees/{id}/deactivate` | Deactivate employee. |
| GET / POST | `/admin/users` | List/create customer. |
| PATCH / DELETE | `/admin/users/{id}` | Update/delete customer. |
| GET | `/admin/requests` | List requests. |
| POST | `/admin/requests/{id}/approve` | Approve request. |
| POST | `/admin/requests/{id}/reject` | Reject with `{ "rejectionReason": "..." }`. |
| GET | `/admin/reports?period=&referenceDate=&productId=&customerId=` | Generate report. |

### Bank, funds, and assistant

| Method | Path | Access | Purpose |
| --- | --- | --- | --- |
| POST | `/banks/add/{userId}` | Auth | Create bank account; body `{ "accountNumber" }`. |
| POST | `/banks/{userId}/deduct` | Auth | Deduct bank funds; body `{ "amount" }`. |
| POST | `/assistant/recommendations` | USER | Create grocery recommendation. |

```json
// assistant request
{ "message": "vegetable pasta", "servings": 4, "budget": 500.0 }

// response, abridged
{
  "dish": "...", "summary": "...", "total": 0, "budget": 500.0,
  "withinBudget": true,
  "recommendedProducts": [{
    "productId": 1, "productName": "...", "ingredient": "...",
    "requiredAmount": 1, "requiredUnit": "kg", "quantity": 1,
    "unitPrice": 0, "lineTotal": 0, "status": "IN_STOCK"
  }]
}
```

The gateway exposes `/funds/**` at port 8088. No HTTP controller was found for FundsApp in the current source tree, so no implemented Funds API contract is documented here.

## Project structure

```text
grocers-user-test/
├── frontend/                  React 19 + TypeScript + Vite application
│   └── src/
│       ├── components/        Shared UI, checkout, delivery and home modules
│       ├── config/            Axios API and product/assistant clients
│       ├── context/           Authentication and cart state
│       ├── pages/             Customer, employee and admin routes
│       └── types/             Shared TypeScript models
├── backend/
│   ├── gatewayapp/            Gateway routing, CORS and JWT filter
│   ├── authapp/               Login and recovery
│   ├── userapp/               Profile, funds, wishlist and order summaries
│   ├── adminapp/              Admin workflows and reporting
│   ├── EmployeeApp/           Employee workflows
│   ├── productsapp/           Catalog, inventory and images
│   ├── cartapp/               Cart lifecycle
│   ├── OrderApp/              Order lifecycle and Kafka producer
│   ├── requestapp/            Employee product requests
│   ├── ticketapp/             Ticket lifecycle
│   ├── bankapp/               Bank operations
│   ├── FundsApp/              Funds module
│   └── assistantapp/          Recipe recommendation service
└── Project-Grocers.pdf         Existing reference artifact
```

Most Spring services follow:

```text
config/ -> controllers/ -> services/(abstractions, implementations)
                         -> entities/ -> repositories/ -> dto/ -> exceptions/
```

## Development and quality

### Frontend

```powershell
cd frontend
npm ci
npm run dev
npm run lint
npm run build
npm run preview
```

### Backend

Run from any individual service directory:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Tests exist for selected services, including assistant and request flows plus application contexts. Run them per service because there is no root Maven reactor.

### Documentation maintenance rules

- Update endpoint tables and request examples whenever controller mappings or DTOs change.
- Update the port table and architecture whenever gateway routes change.
- Keep secrets out of documentation, commits, logs, and examples.
- Prefer gateway URLs in frontend code and user-facing documentation.
- Record role requirements and public/internal status for every new route.

## License

No project license file is currently present. Add a `LICENSE` file before publishing or redistributing the project.
