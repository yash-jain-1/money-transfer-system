# Money Transfer System - API Endpoints

## Overview

This document describes all available API endpoints for the Money Transfer System. All endpoints (except `/auth/login` and Swagger UI) require authentication via JWT token in the `Authorization: Bearer <token>` header.

---

## Authentication

### Login Endpoint

#### `POST /auth/login`
Login with credentials to receive a JWT token.

**Authentication**: None (public endpoint)

**Request Body**:
```json
{
  "username": "string",
  "password": "string"
}
```

**Response (200 OK)**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

**Error Responses**:
- `401 Unauthorized` - Invalid credentials
- `429 Too Many Requests` - Rate limit exceeded (5 login attempts per minute)

**Rate Limit**: 5 login attempts per minute per username

**Example**:
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password"
  }'
```

---

## User Endpoints

All USER endpoints require authentication with `USER` or `ADMIN` role.

### Account Information

#### `GET /accounts/{accountId}`
Retrieve account details by ID.

**Roles Required**: `USER`, `ADMIN`

**Path Parameters**:
- `accountId` (Long) - The account ID

**Response (200 OK)**:
```json
{
  "id": 1001,
  "accountNumber": "ACC-001",
  "accountHolder": "Alice Johnson",
  "balance": 5000.00,
  "accountType": "CHECKING",
  "status": "ACTIVE",
  "createdAt": "2026-02-01T10:00:00Z",
  "updatedAt": "2026-02-05T15:30:00Z"
}
```

**Error Responses**:
- `401 Unauthorized` - No valid JWT token
- `403 Forbidden` - USER role attempting to access another user's account (future: after ownership checks)
- `404 Not Found` - Account not found

**Rate Limit**: 60 reads per minute per user

**Example**:
```bash
curl -X GET http://localhost:8080/accounts/1001 \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

---

#### `GET /accounts/{accountId}/balance`
Retrieve current account balance.

**Roles Required**: `USER`, `ADMIN`

**Path Parameters**:
- `accountId` (Long) - The account ID

**Response (200 OK)**:
```json
{
  "accountId": 1001,
  "accountNumber": "ACC-001",
  "balance": 5000.00,
  "status": "ACTIVE",
  "updatedAt": "2026-02-05T15:30:00Z"
}
```

**Error Responses**:
- `401 Unauthorized` - No valid JWT token
- `403 Forbidden` - USER role attempting to access another user's account (future: after ownership checks)
- `404 Not Found` - Account not found

**Rate Limit**: 60 reads per minute per user

**Example**:
```bash
curl -X GET http://localhost:8080/accounts/1001/balance \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

---

#### `GET /accounts/{accountId}/transactions`
Retrieve transaction history for an account.

**Roles Required**: `USER`, `ADMIN`

**Path Parameters**:
- `accountId` (Long) - The account ID

**Query Parameters** (optional):
- None currently implemented

**Response (200 OK)**:
```json
[
  {
    "id": 245,
    "fromAccountId": 1001,
    "toAccountId": 1002,
    "amount": 50.00,
    "balanceBefore": 5000.00,
    "balanceAfter": 4950.00,
    "transactionType": "DEBIT",
    "status": "SUCCESS",
    "description": "Transfer to account 1002",
    "idempotencyKey": "550e8400-e29b-41d4-a716-446655440000",
    "createdAt": "2026-02-05T15:30:00Z"
  },
  {
    "id": 246,
    "fromAccountId": 1002,
    "toAccountId": 1001,
    "amount": 25.00,
    "balanceBefore": 10000.00,
    "balanceAfter": 10025.00,
    "transactionType": "CREDIT",
    "status": "SUCCESS",
    "description": "Transfer from account 1001",
    "idempotencyKey": "660f9511-f40c-52e5-b827-557766551111",
    "createdAt": "2026-02-05T14:20:00Z"
  }
]
```

**Error Responses**:
- `401 Unauthorized` - No valid JWT token
- `403 Forbidden` - USER role attempting to access another user's transactions (future: after ownership checks)
- `404 Not Found` - Account not found

**Rate Limit**: 60 reads per minute per user

**Example**:
```bash
curl -X GET http://localhost:8080/accounts/1001/transactions \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

---

### Money Transfers

#### `POST /transfers`
Initiate a money transfer between two accounts with idempotency support.

**Roles Required**: `USER`, `ADMIN`

**Request Body**:
```json
{
  "sourceAccountId": 1001,
  "destinationAccountId": 1002,
  "amount": 100.00,
  "idempotencyKey": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Request Field Details**:
- `sourceAccountId` (Long) - Source account ID (required)
- `destinationAccountId` (Long) - Destination account ID (required)
- `amount` (BigDecimal) - Transfer amount > 0 (required)
- `idempotencyKey` (UUID) - Unique UUID for idempotency (required)

**Response (201 Created)**:
```json
{
  "transactionId": 245,
  "status": "SUCCESS",
  "description": "Transfer completed successfully",
  "sourceAccountId": 1001,
  "destinationAccountId": 1002,
  "amount": 100.00,
  "debitTransactionId": 245,
  "creditTransactionId": 246,
  "timestamp": "2026-02-05T15:31:45Z"
}
```

**Error Responses**:
- `400 Bad Request` - Invalid input (idempotencyKey must be UUID, amount must be > 0, etc.)
- `401 Unauthorized` - No valid JWT token
- `403 Forbidden` - USER role attempting to transfer from account they don't own (future: after ownership checks)
- `404 Not Found` - Source or destination account not found
- `409 Conflict` - Insufficient funds in source account
- `429 Too Many Requests` - Transfer rate limit exceeded (10 transfers per minute)

**Rate Limit**: 10 transfers per minute per user

**Idempotency**: If the same `idempotencyKey` is used within 24 hours, the same response is returned without processing the transfer again.

**Transaction Rules**:
- Minimum amount: 0.01
- Both accounts must exist and be ACTIVE
- Source account must have sufficient balance
- Transfer is atomic (both debit and credit succeed or both fail)

**Example**:
```bash
curl -X POST http://localhost:8080/transfers \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccountId": 1001,
    "destinationAccountId": 1002,
    "amount": 100.00,
    "idempotencyKey": "550e8400-e29b-41d4-a716-446655440000"
  }'
```

---

### Health Check (User)

#### `GET /transfers/health`
Health check endpoint for the transfer service.

**Roles Required**: `USER`, `ADMIN`

**Response (200 OK)**:
```json
"Transfer service is operational"
```

**Example**:
```bash
curl -X GET http://localhost:8080/transfers/health \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

---

## Admin Endpoints

All ADMIN endpoints require authentication with `ADMIN` role and are accessed via `/api/v1/admin/**` prefix.

### Admin Account Information

#### `GET /api/v1/admin/accounts/{accountId}`
Retrieve account details for **any** account (admin access, no ownership restriction).

**Roles Required**: `ADMIN` only

**Path Parameters**:
- `accountId` (Long) - The account ID

**Response (200 OK)**:
```json
{
  "id": 1001,
  "accountNumber": "ACC-001",
  "accountHolder": "Alice Johnson",
  "balance": 5000.00,
  "accountType": "CHECKING",
  "status": "ACTIVE",
  "createdAt": "2026-02-01T10:00:00Z",
  "updatedAt": "2026-02-05T15:30:00Z"
}
```

**Error Responses**:
- `401 Unauthorized` - No valid JWT token
- `403 Forbidden` - USER role attempting to access admin endpoint
- `404 Not Found` - Account not found

**Audit Logging**: Admin access is logged with timestamp, admin username, and account ID.

**Example**:
```bash
curl -X GET http://localhost:8080/api/v1/admin/accounts/1001 \
  -H "Authorization: Bearer {ADMIN_JWT_TOKEN}"
```

---

#### `GET /api/v1/admin/accounts/{accountId}/balance`
Retrieve account balance for **any** account (admin access, no ownership restriction).

**Roles Required**: `ADMIN` only

**Path Parameters**:
- `accountId` (Long) - The account ID

**Response (200 OK)**:
```json
{
  "accountId": 1001,
  "accountNumber": "ACC-001",
  "balance": 5000.00,
  "status": "ACTIVE",
  "updatedAt": "2026-02-05T15:30:00Z"
}
```

**Error Responses**:
- `401 Unauthorized` - No valid JWT token
- `403 Forbidden` - USER role attempting to access admin endpoint
- `404 Not Found` - Account not found

**Audit Logging**: Admin access is logged with timestamp, admin username, and account ID.

**Example**:
```bash
curl -X GET http://localhost:8080/api/v1/admin/accounts/1001/balance \
  -H "Authorization: Bearer {ADMIN_JWT_TOKEN}"
```

---

#### `GET /api/v1/admin/accounts/{accountId}/transactions`
Retrieve transaction history for **any** account (admin access, no ownership restriction).

**Roles Required**: `ADMIN` only

**Path Parameters**:
- `accountId` (Long) - The account ID

**Response (200 OK)**:
```json
[
  {
    "id": 245,
    "fromAccountId": 1001,
    "toAccountId": 1002,
    "amount": 50.00,
    "balanceBefore": 5000.00,
    "balanceAfter": 4950.00,
    "transactionType": "DEBIT",
    "status": "SUCCESS",
    "description": "Transfer to account 1002",
    "idempotencyKey": "550e8400-e29b-41d4-a716-446655440000",
    "createdAt": "2026-02-05T15:30:00Z"
  }
]
```

**Error Responses**:
- `401 Unauthorized` - No valid JWT token
- `403 Forbidden` - USER role attempting to access admin endpoint
- `404 Not Found` - Account not found

**Audit Logging**: Admin access is logged with timestamp, admin username, and account ID.

**Example**:
```bash
curl -X GET http://localhost:8080/api/v1/admin/accounts/1001/transactions \
  -H "Authorization: Bearer {ADMIN_JWT_TOKEN}"
```

---

### Admin Health Check

#### `GET /api/v1/admin/health`
Health check endpoint for admin operations.

**Roles Required**: `ADMIN` only

**Response (200 OK)**:
```
Admin system operational
```

**Error Responses**:
- `401 Unauthorized` - No valid JWT token
- `403 Forbidden` - USER role attempting to access admin endpoint

**Example**:
```bash
curl -X GET http://localhost:8080/api/v1/admin/health \
  -H "Authorization: Bearer {ADMIN_JWT_TOKEN}"
```

---

## Public Endpoints

These endpoints do not require authentication.

### Swagger UI

#### `GET /swagger-ui.html`
Swagger UI for interactive API documentation.

**Authentication**: None required

**Example**:
```bash
curl http://localhost:8080/swagger-ui.html
```

#### `GET /v3/api-docs` or `GET /v3/api-docs.yaml`
OpenAPI 3.0 specification for the API.

**Authentication**: None required

**Example**:
```bash
curl http://localhost:8080/v3/api-docs
```

---

## Rate Limiting

Rate limits are applied per user (based on JWT username) and are enforced independently for different operations:

| Operation | Limit | Window |
|-----------|-------|--------|
| Login attempts | 5 | per minute |
| Account reads | 60 | per minute |
| Transfer initiation | 10 | per minute |

**Rate Limit Response (429 Too Many Requests)**:
```json
{
  "status": "RATE_LIMITED",
  "description": "Operation rate limit exceeded. Maximum 10 transfers per minute."
}
```

---

## Error Responses

### Standard Error Format

All error responses follow this format:

```json
{
  "status": 400,
  "message": "Description of the error",
  "error": "Error Type",
  "timestamp": "2026-02-05T15:31:45Z",
  "path": "/transfers"
}
```

### Common HTTP Status Codes

| Code | Reason | When |
|------|--------|------|
| `200 OK` | Successful GET/read operation | Account or transaction data retrieved |
| `201 Created` | Successful POST operation | Transfer initiated successfully |
| `400 Bad Request` | Invalid input data | Validation failed (invalid UUID, negative amount, etc.) |
| `401 Unauthorized` | Missing or invalid authentication | No JWT token or invalid token signature |
| `403 Forbidden` | Insufficient permissions | USER trying to access ADMIN endpoint; ownership violation |
| `404 Not Found` | Resource not found | Account ID doesn't exist |
| `409 Conflict` | Business rule violation | Insufficient balance, account inactive |
| `429 Too Many Requests` | Rate limit exceeded | Too many requests within time window |
| `500 Internal Server Error` | Server error | Unexpected error on server side |

---

## Authentication Details

### JWT Token Structure

All JWT tokens include:

```json
{
  "sub": "username",
  "roles": ["USER"],
  "iss": "money-transfer-system",
  "iat": 1675531845,
  "exp": 1675535445
}
```

**Token Fields**:
- `sub` - Username (subject)
- `roles` - Array of roles (e.g., `["USER"]`, `["ADMIN"]`)
- `iss` - Issuer identifier
- `iat` - Issued at timestamp
- `exp` - Expiration timestamp

### Token Expiration

- Default expiration: 1 hour from issuance
- After expiration, re-authenticate by calling `/auth/login`

### Bearer Token Usage

Include the token in the `Authorization` header with `Bearer` prefix:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0dXNlciIsInJvbGVzIjp...
```

---

## Role-Based Access Control (RBAC)

### USER Role

**Capabilities**:
- ✅ Initiate money transfers
- ✅ View own account balance
- ✅ View own transaction history
- ✅ Access health check endpoints

**Restrictions**:
- ❌ Cannot access admin endpoints (`/api/v1/admin/**`)
- ❌ Cannot view other users' accounts (future: after ownership checks)
- ❌ Cannot bypass transaction limits or rules

### ADMIN Role

**Capabilities**:
- ✅ View **any** account details (no ownership restriction)
- ✅ View **any** account balance
- ✅ View **any** account transaction history
- ✅ Access admin health check endpoint
- ✅ Can invoke regular USER endpoints (superset of permissions)

**Restrictions**:
- ❌ Cannot initiate money transfers on behalf of users (no user impersonation)
- ❌ Cannot bypass transaction rules or limits
- ❌ Cannot modify account data

---

## Testing Endpoints

### Quick Test with cURL

#### 1. Login as USER

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password"
  }' | jq .
```

Save the returned `token` value.

#### 2. Get Account Balance (USER)

```bash
USER_TOKEN="<token_from_step_1>"
curl -X GET http://localhost:8080/accounts/1001/balance \
  -H "Authorization: Bearer $USER_TOKEN" | jq .
```

#### 3. Try to Access Admin Endpoint (USER - Should Fail)

```bash
USER_TOKEN="<token_from_step_1>"
curl -X GET http://localhost:8080/api/v1/admin/accounts/1001 \
  -H "Authorization: Bearer $USER_TOKEN"
# Expected: 403 Forbidden
```

#### 4. Login as ADMIN

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }' | jq .
```

Save the returned `token` value.

#### 5. Access Admin Endpoint (ADMIN - Should Succeed)

```bash
ADMIN_TOKEN="<token_from_step_4>"
curl -X GET http://localhost:8080/api/v1/admin/accounts/1001 \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .
```

---

## Additional Resources

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs
- **RBAC Documentation**: See [RBAC_IMPLEMENTATION.md](RBAC_IMPLEMENTATION.md)
- **Rate Limiting Details**: See [RATE_LIMITING_QUICK_REFERENCE.md](RATE_LIMITING_QUICK_REFERENCE.md)

---

## Security Principles

1. **Roles define authority** - What you are allowed to do
2. **Ownership defines access** - Which data you can access
3. **Defense-in-depth** - Multiple layers of security checks
4. **Least privilege** - Users get minimum necessary permissions
5. **Audit logging** - Admin actions are tracked and logged

---

**Last Updated**: February 5, 2026
**API Version**: 1.0.0
