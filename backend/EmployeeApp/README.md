# EmployeeApp API

EmployeeApp is an internal service. Use the API gateway (`http://localhost:8091`) for every request.

An administrator creates an employee through `POST /grocers/api/employees` with:

```json
{
  "firstName":"Jane",
  "lastName":"Employee",
  "email":"employee@example.com",
  "defaultPassword":"TemporaryPassword123!"
}
```

The administrator's Bearer token is required. The password is BCrypt-hashed in the `employees` table and `must_change_password` is set to `true`.

Employees authenticate only through AuthApp via the gateway: `POST /grocers/api/auth/login/employee`.
The initial response returns `mustChangePassword: true` and a password-change message. Show the employee a password-change page; its JWT can only call `PUT /grocers/api/employees/{employeeId}/password`:

```json
{
  "currentPassword": "TemporaryPassword123!",
  "newPassword": "A-new-secure-password",
  "confirmPassword": "A-new-secure-password"
}
```

When `newPassword` and `confirmPassword` match, the service hashes the new password, updates the `employees` table, and automatically changes `must_change_password` to `false`.

After a successful password change, sign in again to obtain a normal employee token. That token can call the remaining employee APIs through the gateway.


Set the same `GATEWAY_INTERNAL_SECRET` environment variable for EmployeeApp and GatewayApp in production. If the existing Oracle `employees` table has no `must_change_password` column, run `database-migration.sql` once before deployment.
