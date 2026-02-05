# API Quick Reference

## Endpoint Summary

```
┌──────────────────────────── PUBLIC ────────────────────────────┐
│ POST   /auth/login                    - Login (no auth needed)  │
│ GET    /swagger-ui.html               - API Documentation      │
│ GET    /v3/api-docs                   - OpenAPI Spec           │
└────────────────────────────────────────────────────────────────┘

┌──────────────────────── USER ENDPOINTS (USER/ADMIN) ─────────────────┐
│ GET    /accounts/{id}                 - Account details               │
│ GET    /accounts/{id}/balance         - Account balance              │
│ GET    /accounts/{id}/transactions    - Transaction history          │
│ POST   /transfers                     - Initiate transfer            │
│ GET    /transfers/health              - Health check                 │
└────────────────────────────────────────────────────────────────────┘

┌──────────────────────── ADMIN ENDPOINTS (ADMIN ONLY) ──────────────┐
│ GET    /api/v1/admin/accounts/{id}                    - Account   │
│ GET    /api/v1/admin/accounts/{id}/balance           - Balance   │
│ GET    /api/v1/admin/accounts/{id}/transactions      - History   │
│ GET    /api/v1/admin/health                          - Health    │
└─────────────────────────────────────────────────────────────────┘
```

---

## Authentication

### Login
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password"
  }'
```

**Response**:
```json
{
  "token": "eyJhbGc...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

### Using Token
```bash
curl -X GET http://localhost:8080/accounts/1001 \
  -H "Authorization: Bearer {TOKEN}"
```

---

## Test Credentials

| Username | Password | Role |
|----------|----------|------|
| testuser | password | USER |
| admin    | admin123 | ADMIN |

---

## GET Endpoints (User Data)

### Account Details
```
GET /accounts/{accountId}
Authorization: Bearer {TOKEN}
```

### Account Balance
```
GET /accounts/{accountId}/balance
Authorization: Bearer {TOKEN}
```

### Transaction History
```
GET /accounts/{accountId}/transactions
Authorization: Bearer {TOKEN}
```

---

## POST Endpoint (Money Transfer)

### Initiate Transfer
```
POST /transfers
Authorization: Bearer {TOKEN}
Content-Type: application/json

{
  "sourceAccountId": 1001,
  "destinationAccountId": 1002,
  "amount": 100.00,
  "idempotencyKey": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response (201)**:
```json
{
  "transactionId": 245,
  "status": "SUCCESS",
  "sourceAccountId": 1001,
  "destinationAccountId": 1002,
  "amount": 100.00,
  "timestamp": "2026-02-05T15:31:45Z"
}
```

---

## Admin Endpoints

### Get Any Account (ADMIN Only)
```
GET /api/v1/admin/accounts/{accountId}
Authorization: Bearer {ADMIN_TOKEN}
```

### Get Any Account Balance (ADMIN Only)
```
GET /api/v1/admin/accounts/{accountId}/balance
Authorization: Bearer {ADMIN_TOKEN}
```

### Get Any Account Transactions (ADMIN Only)
```
GET /api/v1/admin/accounts/{accountId}/transactions
Authorization: Bearer {ADMIN_TOKEN}
```

---

## Rate Limits

| Operation | Limit | Per |
|-----------|-------|-----|
| Login | 5 | minute |
| Read (accounts, transactions, balance) | 60 | minute |
| Transfer | 10 | minute |

**Response (429)**:
```json
{
  "status": "RATE_LIMITED",
  "description": "Transfer rate limit exceeded"
}
```

---

## Common Status Codes

| Code | Meaning |
|------|---------|
| 200 | Success (GET) |
| 201 | Created (POST transfer) |
| 400 | Invalid input |
| 401 | Unauthorized (no token) |
| 403 | Forbidden (wrong role) |
| 404 | Not found |
| 409 | Conflict (insufficient funds) |
| 429 | Rate limit exceeded |
| 500 | Server error |

---

## Error Example

```json
{
  "status": 400,
  "message": "Validation failed: idempotencyKey - Idempotency key must be a valid UUID",
  "error": "Validation Error",
  "timestamp": "2026-02-05T15:31:45Z",
  "path": "/transfers"
}
```

---

## Roles

### USER
- ✅ View own accounts
- ✅ Initiate transfers
- ❌ Cannot access `/api/v1/admin/**`

### ADMIN
- ✅ View any account
- ✅ View transfers
- ❌ Cannot transfer money

---

## Token Details

```json
{
  "sub": "username",
  "roles": ["USER"],
  "iss": "money-transfer-system",
  "exp": 1675535445
}
```

- Token expires in **1 hour**
- Include in all requests: `Authorization: Bearer {TOKEN}`

---

## Useful Links

- **Full Documentation**: [API_ENDPOINTS.md](API_ENDPOINTS.md)
- **Interactive Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs
- **RBAC Guide**: [RBAC_IMPLEMENTATION.md](RBAC_IMPLEMENTATION.md)

---

*Last Updated: February 5, 2026*
