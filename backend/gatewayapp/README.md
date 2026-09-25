# GatewayApp

GatewayApp is the single entry point for frontend API calls. It validates access-token JWTs locally; it does not call AuthApp or Oracle after login.

## Security flow

```text
Login request → GatewayApp → AuthApp → JWT returned
Protected request + JWT → GatewayApp validates signature and expiry → target service
```

GatewayApp uses the JWT role claim for authorization:

| URL prefix | Allowed role |
|---|---|
| `/grocers/api/admin/**` and `/grocers/api/admins/**` | `ADMIN` |
| `/grocers/api/employee/**` and `/grocers/api/employees/**` | `EMPLOYEE` or `ADMIN` |
| Other routed protected paths | `USER`, `EMPLOYEE`, or `ADMIN` |

Login URLs under `/grocers/api/auth/login/**` are intentionally public.

## Requirements for every downstream service

1. Add a route in `src/main/resources/application.yml` when the service exposes a new URL prefix.
2. When testing authentication or roles locally, send the request through GatewayApp (`http://localhost:8091`), not a direct service port.
3. Use the identity headers added by GatewayApp for ownership checks:

   ```text
   X-Authenticated-User-Id
   X-Authenticated-User-Email
   X-Authenticated-Role
   ```

4. Never trust those headers when a request can bypass the gateway. In this local project, direct service ports such as `http://localhost:8085` still work and bypass JWT validation. That is fine for ordinary feature development, but it is not an authentication test. Use GatewayApp on port `8091` to test login, tokens, and role restrictions.
5. Keep role-specific routes under the existing `admin` or `employee` prefixes, or update the gateway rule before exposing the new route.
6. Apply resource ownership checks inside the service. For example, an order service must ensure a `USER` can access only orders belonging to `X-Authenticated-User-Id`; the gateway cannot decide ownership from the URL alone.

## Adding a route

Example for a future review service running on port `8092`:

```yaml
- id: reviewapp
  uri: http://localhost:8092
  predicates:
    - Path=/grocers/api/reviews/**
```

Add it inside `spring.cloud.gateway.server.webflux.routes` in `application.yml`.

## Run locally

AuthApp must be running first. Set the exact same secret used by AuthApp:

```powershell
$env:JWT_SECRET = "a-long-random-secret-with-at-least-32-characters"
.\mvnw.cmd spring-boot:run
```

GatewayApp runs on `http://localhost:8091`.

## Current routed features

`application.yml` is the authoritative route list. In addition to the core account, catalogue, cart, order, request, ticket, funds, bank, and recipe routes, it includes:

| Route prefix | Target service | Purpose |
| --- | --- | --- |
| `/grocers/api/admin/**` | AdminApp | Administrator operations, dashboard, reports, and notifications. |
| `/grocers/api/requests/**` | RequestApp | Employee product requests and administrator review. |
| `/grocers/api/support-assistant/**` | Grocers Help Assistant | Role-aware platform help and narrowly scoped assistant actions. |
| `/grocers/api/chats/**` | ChatApp | Customer-to-employee live support chat. |

For authenticated requests, the gateway removes any caller-supplied `X-Authenticated-*` headers before adding verified values from the JWT. This prevents a browser client from impersonating another user by forging headers.

## Insomnia test checklist

Use this base URL:

```text
http://localhost:8091
```

| Test | Request | Token | Expected result |
|---|---|---|---|
| No token | `GET /requests` | None | `401 Unauthorized` from GatewayApp |
| Invalid token | `GET /requests` | Invalid Bearer token | `401 Unauthorized` from GatewayApp |
| User blocked from admin route | `GET /grocers/api/admin/test` | Valid `USER` token | `403 Forbidden` from GatewayApp |
| Employee blocked from admin route | `GET /grocers/api/admin/test` | Valid `EMPLOYEE` token | `403 Forbidden` from GatewayApp |
| Employee allowed past gateway | `GET /grocers/api/employee/test` | Valid `EMPLOYEE` token | Gateway forwards it; current downstream service may return `404` until that endpoint is built |
| Admin allowed past gateway | `GET /grocers/api/admin/test` | Valid `ADMIN` token | Gateway forwards it; current downstream service may return `404` until that endpoint is built |

`/requests` is an existing RequestApp endpoint and returns `200 OK` when RequestApp is running. The `/grocers/api/admin/test` and `/grocers/api/employee/test` paths are intentional gateway-only role checks; an expected `404` after an allowed role proves GatewayApp accepted the token and forwarded the request, then the downstream application reported that its future endpoint does not exist yet.
