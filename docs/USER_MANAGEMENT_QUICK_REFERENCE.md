# User Management Quick Reference

## Quick Start

### 1. Register a New User (Admin Only)
```bash
# First, login as admin to get JWT token
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "password"
  }'
# Save the JWT token from response

# Then register new user with admin token
curl -X POST http://localhost:8080/api/v1/users/register \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN" \
  -d '{
    "username": "alice",
    "password": "pass123",
    "email": "alice@example.com",
    "fullName": "Alice Smith"
  }'
```

### 2. Login
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice",
    "password": "pass123"
  }'
```
Save the JWT token from response.

### 3. Access Your Account
```bash
curl -X GET http://localhost:8080/api/v1/accounts/100/balance \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Default Test Users

| Username   | Password   | Role  | Can Do                           |
|------------|------------|-------|----------------------------------|
| `testuser` | `password` | USER  | Access own accounts only         |
| `admin`    | `password` | ADMIN | Access all accounts (admin APIs) |

## Key Classes

### Domain Layer
- **User**: Entity with username, password, email, role
- **Account**: Entity updated with `owner` (User) relationship
- **UserRole**: Enum (USER, ADMIN)

### Service Layer
- **UserService**: Registration, password reset, user management
- **OwnershipService**: Validates user owns accounts before access
- **CustomUserDetailsService**: Loads users from DB for authentication

### Repository Layer
- **UserRepository**: CRUD + custom queries (findByUsername, findByEmail, etc.)

## Ownership Validation

### How It Works

```java
// Service method example
public AccountResponse getAccountById(Long accountId) {
    // Step 1: Validate ownership
    ownershipService.validateAccountOwnership(accountId);
    
    // Step 2: If validation passes, proceed
    Account account = accountRepository.findById(accountId)
        .orElseThrow(() -> new AccountNotFoundException(accountId));
    
    return toAccountResponse(account);
}
```

### Behavior by Role

**USER Role:**
- ✅ Can access their own accounts
- ❌ Cannot access other users' accounts
- ✅ Can transfer FROM own accounts
- ✅ Can transfer TO any account

**ADMIN Role:**
- ✅ Can access any account
- ✅ Can perform transfers between any accounts
- ✅ Bypasses all ownership checks automatically

## API Endpoints Reference

### User Management

| Method | Endpoint                      | Auth Required | Min Role | Description           |
|--------|-------------------------------|---------------|----------|-----------------------|
| POST   | `/api/v1/users/register`      | Yes           | ADMIN    | Register new user     |
| POST   | `/api/v1/users/forgot-password`| No           | -        | Request password reset|
| POST   | `/api/v1/users/reset-password`| No            | -        | Reset password        |
| GET    | `/api/v1/users/{username}`    | Yes           | USER     | Get user profile      |

### Account Operations (Ownership Protected)

| Method | Endpoint                            | Auth Required | Ownership Check |
|--------|-------------------------------------|---------------|-----------------|
| GET    | `/api/v1/accounts/{id}`             | Yes           | ✅ Yes          |
| GET    | `/api/v1/accounts/{id}/balance`     | Yes           | ✅ Yes          |
| GET    | `/api/v1/accounts/{id}/transactions`| Yes           | ✅ Yes          |

### Transfers (Ownership Protected)

| Method | Endpoint              | Auth Required | Ownership Check           |
|--------|-----------------------|---------------|---------------------------|
| POST   | `/api/v1/transfers`   | Yes           | ✅ Yes (source account)   |

### Admin Endpoints (Admin Only)

| Method | Endpoint                                  | Auth Required | Min Role |
|--------|-------------------------------------------|---------------|----------|
| GET    | `/api/v1/admin/accounts/{id}`             | Yes           | ADMIN    |
| GET    | `/api/v1/admin/accounts/{id}/balance`     | Yes           | ADMIN    |
| GET    | `/api/v1/admin/accounts/{id}/transactions`| Yes           | ADMIN    |

## Database Schema

### Users Table
```sql
id              BIGINT PK AUTO_INCREMENT
username        VARCHAR(50) UNIQUE NOT NULL
password        VARCHAR(255) NOT NULL (BCrypt)
email           VARCHAR(100) UNIQUE NOT NULL
full_name       VARCHAR(100) NOT NULL
role            VARCHAR(20) NOT NULL ('USER' | 'ADMIN')
enabled         BOOLEAN DEFAULT TRUE
created_at      TIMESTAMP
updated_at      TIMESTAMP
last_login      TIMESTAMP
reset_token     VARCHAR(100)
reset_token_expiry TIMESTAMP
```

### Accounts Table (Updated)
```sql
-- Existing columns plus:
user_id         BIGINT FK -> users(id)
```

## Common Errors

### 403 Forbidden - Unauthorized Access
```json
{
  "status": 403,
  "message": "You don't have permission to access account: 100",
  "error": "Unauthorized Access"
}
```
**Cause**: User trying to access account they don't own
**Solution**: Use account IDs that belong to authenticated user

### 400 Bad Request - Username Already Exists
```json
{
  "status": 400,
  "message": "Username already exists: alice",
  "error": "Invalid Request"
}
```
**Cause**: Registration with duplicate username
**Solution**: Choose different username

### 400 Bad Request - Validation Error
```json
{
  "status": 400,
  "message": "Validation failed: password - must be at least 6 characters",
  "error": "Validation Error"
}
```
**Cause**: Input validation failed
**Solution**: Check request data meets validation rules

## Testing

### Unit Tests
```bash
# Test ownership validation
mvn test -Dtest=OwnershipServiceTest
```

### Integration Tests
```bash
# Test user management and ownership
mvn test -Dtest=UserOwnershipIntegrationTest
```

## Code Examples

### Check if User Owns Account (Java)
```java
@Autowired
private OwnershipService ownershipService;

public void someMethod(Long accountId) {
    // Throws UnauthorizedAccessException if not owner
    ownershipService.validateAccountOwnership(accountId);
    
    // Proceed with operation...
}
```

### Get Current User (Java)
```java
@Autowired
private OwnershipService ownershipService;

public void someMethod() {
    User currentUser = ownershipService.getCurrentUser();
    System.out.println("Current user: " + currentUser.getUsername());
    System.out.println("Is admin: " + currentUser.isAdmin());
}
```

### Check if User is Admin (Java)
```java
@Autowired
private OwnershipService ownershipService;

public void restrictedOperation() {
    if (ownershipService.isCurrentUserAdmin()) {
        // Admin-only logic
    } else {
        throw new UnauthorizedAccessException("Admin access required");
    }
}
```

## Migration

### Apply Migration
```bash
cd backend
mvn flyway:migrate
```

### Migration Creates:
- ✅ `users` table
- ✅ `user_id` column in `accounts` table
- ✅ Foreign key relationship
- ✅ Default users (testuser, admin)
- ✅ Links accounts 1 and 2 to testuser

## Swagger Documentation

Access interactive API docs at:
```
http://localhost:8080/swagger-ui.html
```

Look for "User Management" tag for user endpoints.

## Tips & Best Practices

1. **Always validate ownership** before accessing user-specific resources
2. **Use OwnershipService** methods rather than manual checks
3. **Admin methods** should have "Admin" suffix for clarity
4. **JWT tokens** must be included in Authorization header for protected endpoints
5. **Password reset tokens** expire in 24 hours
6. **BCrypt** automatically handles salting and secure hashing

## Need Help?

- Check logs for detailed error messages
- Verify JWT token is valid and not expired
- Ensure user has correct role for the operation
- Confirm account ownership in database
- Review [USER_MANAGEMENT_IMPLEMENTATION.md](USER_MANAGEMENT_IMPLEMENTATION.md) for detailed documentation
