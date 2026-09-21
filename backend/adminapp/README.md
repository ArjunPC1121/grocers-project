# AdminApp API

All routes below are exposed by AdminApp on port `8081` and should be reached through GatewayApp under `/grocers/api/admin`.

## Super Admin setup

Insert the first administrator manually with a BCrypt password and set its `role` to `SUPER_ADMIN`. Every administrator created through AdminApp receives the `ADMIN` role. Only the Super Admin may create, edit, or delete normal admin accounts. Passwords and roles are not editable through the admin-management endpoints.

## AdminApp routes

| Method | Route | Purpose |
|---|---|---|
| GET | `/me` | Authenticated admin profile and role |
| GET, POST | `/admins` | List/create normal admins (Super Admin only) |
| PUT, DELETE | `/admins/{id}` | Update/delete a normal admin (Super Admin only) |
| GET | `/dashboard` | Live operational totals and revenue |
| GET, POST | `/products` | View/create products |
| PUT, DELETE | `/products/{id}` | Update/delete a product |
| GET, POST | `/employees` | View/add employees |
| POST | `/employees/{id}/deactivate` | Set employee status to `INACTIVE` |
| GET, POST | `/users` | View/create users through UserApp |
| PATCH, DELETE | `/users/{id}` | Update/delete users through UserApp |
| GET | `/requests` | View inventory requests |
| POST | `/requests/{id}/approve` | Apply and approve a product request |
| POST | `/requests/{id}/reject` | Reject a product request |
| GET | `/reports` | On-screen daily/weekly/monthly order report |

`/reports` accepts `period=DAILY|WEEKLY|MONTHLY`, `referenceDate=YYYY-MM-DD`, and optional `productId` / `customerId`.

## Temporary service contracts

AdminApp calls these endpoints until the owning services publish their final equivalents:

| Service | Expected route |
|---|---|
| ProductsApp | `GET/POST /api/products`, `PUT/DELETE /api/products/{id}`, `POST /api/products/{id}/increase-quantity` |
| CartApp | Its current branch exposes only `DELETE /grocers/api/carts/{cartId}/items/{productId}`. Bulk product cleanup is still pending, so `services.cart-product-cleanup-enabled` remains `false`. |
| EmployeeApp | `GET/POST /grocers/api/employees`, `PATCH /grocers/api/employees/{id}/status` |
| UserApp | `GET/POST /grocers/api/users`, `PATCH/DELETE /grocers/api/users/{id}` |
| RequestApp | `GET /requests`, `GET /requests/{id}`, `PATCH /requests/{id}/status` |
| OrderApp | `GET /grocers/api/orders` |

An employee request must include: `action` (`CREATE`, `UPDATE`, `RESTOCK`, or `DELETE`), the relevant `productId`, and for create/update the product `name`, `price`, `quantity`, and `discount`. A restock request supplies `quantity` as the amount to add.

## Model additions

- `PRODUCT.discount` is an integer percentage between `0` and `100`, defaulting to `0`.
- `EMPLOYEES.status` is `ACTIVE` or `INACTIVE`, defaulting to `ACTIVE`.
