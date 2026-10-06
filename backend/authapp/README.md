# AuthApp

AuthApp is the only application that signs users in and creates JWT access tokens. It does not create a database table and it does not query the database after a successful login.

## Account sources

| Login endpoint | Account table queried | JWT role |
|---|---|---|
| `POST /grocers/api/auth/login/user` | `USERS` | `USER` |
| `POST /grocers/api/auth/login/employee` | `EMPLOYEES` | `EMPLOYEE` |
| `POST /grocers/api/auth/login/admin` | `ADMINS` | `ADMIN` |

All three endpoints receive the same JSON body:

```json
{
  "email": "person@example.com",
  "password": "their-password"
}
```

The response contains one short-lived access token:

```json
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresInSeconds": 86400,
  "role": "USER"
}
```

## User account lock

User login checks `USERS.ACCOUNT_LOCKED` before validating the password. When it is `true`, AuthApp returns `403 Forbidden` and does not issue a token.

Employee and admin tables do not currently contain a lock column, so this check applies only to user login.

## Requirements for account-owning services

When UserApp, EmployeeApp, or AdminApp creates or changes an account:

1. Store a BCrypt hash in its `password` column. Never store a plain-text password.
2. Ensure the email address is valid and unique within that account type.
3. Do not let a client choose or override the JWT role. The login endpoint fixes the role from its own URL.
4. Keep UserApp's `accountLocked` field accurate. A locked user cannot log in until it is set back to `false`.
5. Do not create another login endpoint or issue a second kind of JWT in another service.

## JWT contract for other services

AuthApp signs tokens containing these claims:

| Claim | Meaning |
|---|---|
| `sub` | Account ID from the matching table |
| `email` | Authenticated email address |
| `role` | `USER`, `EMPLOYEE`, or `ADMIN` |
| `iat` / `exp` | Issued-at and expiry timestamps |

The frontend must send the token through GatewayApp:

```http
Authorization: Bearer <access-token>
```

Do not call downstream services directly from the frontend in a deployed environment.

## Run locally

Set the same secret that GatewayApp uses:

```powershell
$env:JWT_SECRET = "a-long-random-secret-with-at-least-32-characters"
.\mvnw.cmd spring-boot:run
```

AuthApp runs on `http://localhost:8090`. The frontend should normally call it through GatewayApp on port `8091`.

## Insomnia test checklist

Start Oracle, then ensure the `USERS`, `EMPLOYEES`, and `ADMINS` tables exist and contain an account with a BCrypt password hash. Start AuthApp and GatewayApp with the same `JWT_SECRET`.

Run requests against GatewayApp (`http://localhost:8091`):

| Test | Request | Expected result |
|---|---|---|
| User login | `POST /grocers/api/auth/login/user` | `200` and a token with `role: USER` |
| Employee login | `POST /grocers/api/auth/login/employee` | `200` and a token with `role: EMPLOYEE` |
| Admin login | `POST /grocers/api/auth/login/admin` | `200` and a token with `role: ADMIN` |
| Wrong password | Any login endpoint with a wrong password | `401` |
| Unknown email | Any login endpoint with an unknown email | `401` |
| Locked user | User endpoint using an account where `accountLocked = true` | `403` |

For every successful login request, set the response token in Insomnia as a Bearer token for gateway-protected requests.

## Locked-account recovery API

These public endpoints are intentionally reachable without an access token because the customer cannot sign in while locked.

| Method | Route | Purpose |
| --- | --- | --- |
| `POST` | `/grocers/api/auth/locked-account/ticket` | Escalate recovery to an employee support ticket. |
| `GET` | `/grocers/api/auth/locked-account/status?email=` | Check whether an account is locked and whether an employee ticket is open. |
| `GET` | `/grocers/api/auth/locked-account/security-question?email=` | Read the recovery question for a locked account. |
| `POST` | `/grocers/api/auth/locked-account/verify-security-answer` | Verify the answer and receive a short-lived reset token. |
| `POST` | `/grocers/api/auth/locked-account/reset-password` | Reset the password with the recovery token. |

Recovery calls from AuthApp to UserApp/TicketApp carry the internal service secret. Do not expose that secret to the browser or replace those calls with user-supplied identity headers.
