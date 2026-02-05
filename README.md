# Money Transfer System

A secure REST API for money transfers with JWT authentication, role-based access control, and professional API documentation.

## 🚀 Quick Start

### 1. Start the Application
```bash
cd backend
mvn clean package -DskipTests
java -jar target/money-transfer-system-1.0.0.jar
```

### 2. Access Swagger UI
**Browser**: `http://localhost:8080/swagger-ui.html`

### 3. Get JWT Token
```bash
# Login as regular user
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password"}'

# Or login as admin
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### 4. Call Protected API
```bash
TOKEN="<paste-token-here>"

# Get account balance (USER or ADMIN)
curl -X GET http://localhost:8080/accounts/1001/balance \
  -H "Authorization: Bearer $TOKEN"

# Admin access - view any account (ADMIN only)
curl -X GET http://localhost:8080/api/v1/admin/accounts/1001 \
  -H "Authorization: Bearer $TOKEN"
```

## 📚 Documentation

### API Documentation
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI Spec**: `http://localhost:8080/v3/api-docs`
- **[API_ENDPOINTS.md](docs/API_ENDPOINTS.md)** - Complete endpoint reference with examples
- **[API_QUICK_REFERENCE.md](docs/API_QUICK_REFERENCE.md)** - Quick lookup for all endpoints

### Security & Architecture
- **[RBAC_IMPLEMENTATION.md](docs/RBAC_IMPLEMENTATION.md)** - Role-Based Access Control (USER, ADMIN)
- **[RATE_LIMITING_QUICK_REFERENCE.md](docs/RATE_LIMITING_QUICK_REFERENCE.md)** - Rate limiting details

### Project Documentation
- **[SWAGGER_QUICK_REFERENCE.md](docs/SWAGGER_QUICK_REFERENCE.md)** - Quick reference for Swagger usage
- **[SWAGGER_UI_GUIDE.md](docs/SWAGGER_UI_GUIDE.md)** - Detailed Swagger UI usage guide
- **[SWAGGER_IMPLEMENTATION.md](docs/SWAGGER_IMPLEMENTATION.md)** - Complete implementation details
- **[RATE_LIMITING_IMPLEMENTATION.md](docs/RATE_LIMITING_IMPLEMENTATION.md)** - Rate limiting architecture & guide
- **[RATE_LIMITING_INTEGRATION_TESTS.md](docs/RATE_LIMITING_INTEGRATION_TESTS.md)** - Integration test suite & results
- **[RATE_LIMITING_TEST_REPORT.md](docs/RATE_LIMITING_TEST_REPORT.md)** - Detailed test execution report
- **[CODE_CHANGES.md](docs/CODE_CHANGES.md)** - Code changes summary
- **[FLYWAY_SETUP.md](docs/FLYWAY_SETUP.md)** - Database migration setup

## 🔐 Security & RBAC

### Role-Based Access Control (RBAC)

Two roles with clear separation of concerns:

#### USER Role (Default)
- ✅ Initiate money transfers
- ✅ View own account balance
- ✅ View own transaction history
- ❌ Cannot access admin endpoints
- ❌ Cannot bypass transaction limits

**Test Credentials**:
- Username: `testuser`
- Password: `password`

#### ADMIN Role (Operational)
- ✅ View **any** account details
- ✅ View **any** account balance
- ✅ View **any** transaction history
- ❌ Cannot initiate transfers
- ❌ Cannot bypass transaction rules

**Test Credentials**:
- Username: `admin`
- Password: `admin123`

### Authentication
- **Type**: HTTP Bearer (JWT)
- **Token Expiration**: 1 hour (configurable)
- **Public Endpoints**: `/auth/login`, `/swagger-ui/**`, `/v3/api-docs/**`
- **Protected Endpoints**: All other endpoints require JWT and appropriate role

### Bearer Token Usage
```
Authorization: Bearer <your-jwt-token>
```

### Security Principles
1. **Roles define authority** - What you are allowed to do
2. **Ownership defines access** - Which data you can access (future enhancement)
3. **Defense-in-depth** - Multiple layers of security
4. **Least privilege** - Users get minimum necessary permissions

For detailed RBAC information, see [RBAC_IMPLEMENTATION.md](docs/RBAC_IMPLEMENTATION.md)

## 📋 API Endpoints

### Authentication
- `POST /auth/login` - Get JWT token

### User Endpoints (Require USER or ADMIN role)
- `GET /accounts/{accountId}` - Get account details
- `GET /accounts/{accountId}/balance` - Get account balance
- `GET /accounts/{accountId}/transactions` - Get transaction history
- `POST /transfers` - Initiate money transfer
- `GET /transfers/health` - Health check

### Admin Endpoints (ADMIN role only)
- `GET /api/v1/admin/accounts/{accountId}` - View any account
- `GET /api/v1/admin/accounts/{accountId}/balance` - View any account balance
- `GET /api/v1/admin/accounts/{accountId}/transactions` - View any account transactions
- `GET /api/v1/admin/health` - Admin health check

For complete endpoint documentation, see [API_ENDPOINTS.md](docs/API_ENDPOINTS.md) or [API_QUICK_REFERENCE.md](docs/API_QUICK_REFERENCE.md)

## 🛠️ Configuration

### Environment Variables
```bash
# Database
DB_URL=jdbc:mysql://localhost:3306/money_transfer_db
DB_USERNAME=your_username
DB_PASSWORD=your_password

# JWT
JWT_SECRET=your-secret-key-min-32-characters
JWT_EXPIRATION_MS=3600000

# Application Users (Security)
# Regular user (USER role)
APP_USER=testuser
APP_PASSWORD=password

# Admin user (ADMIN role)
ADMIN_USER=admin
ADMIN_PASSWORD=admin123
```

### Application Properties
File: `backend/src/main/resources/application.yml`

## ✅ Features

- ✅ Secure REST API with JWT authentication
- ✅ **Role-Based Access Control (RBAC)** - USER and ADMIN roles with clear separation
- ✅ Auto-generated Swagger/OpenAPI documentation
- ✅ Interactive API testing via Swagger UI
- ✅ **Rate Limiting (Bucket4j)** - Per-user rate limits on all endpoints
- ✅ Idempotent money transfers
- ✅ Comprehensive error handling
- ✅ Database migrations with Flyway
- ✅ Professional code structure

## 📦 Technology Stack

- **Framework**: Spring Boot 3.2.2
- **Language**: Java 17
- **Database**: MySQL 8.0
- **Security**: Spring Security + JWT (JJWT)
- **Authorization**: Role-based (Spring Security @PreAuthorize)
- **Migrations**: Flyway
- **API Documentation**: Springdoc OpenAPI 2.3.0
- **Testing**: JUnit 5 with Testcontainers
- **Build**: Maven

## 🏗️ Project Structure

```
money-transfer-system/
├── backend/
│   ├── src/main/java/com/moneytransfer/
│   │   ├── Application.java
│   │   ├── config/
│   │   │   ├── OpenApiConfig.java      (Swagger configuration)
│   │   │   ├── SecurityConfig.java     (Security & RBAC rules)
│   │   │   ├── JwtProperties.java      (JWT configuration)
│   │   │   └── ...
│   │   ├── controller/
│   │   │   ├── AuthController.java     (Login endpoint)
│   │   │   ├── AdminController.java    (Admin-only endpoints)
│   │   │   ├── TransferController.java (Transfer endpoints)
│   │   │   └── ...
│   │   ├── security/
│   │   │   ├── JwtUtil.java            (JWT generation & parsing)
│   │   │   ├── JwtAuthenticationFilter.java (JWT verification)
│   │   │   ├── Role.java               (Role enum: USER, ADMIN)
│   │   │   └── ...
│   │   ├── service/                    (Business logic)
│   │   ├── repository/                 (Data access)
│   │   └── ...
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/               (Flyway migrations)
│   ├── src/test/java/
│   │   ├── controller/
│   │   ├── integration/
│   │   ├── SecurityRoleIntegrationTest.java (RBAC validation)
│   │   └── service/
│   ├── pom.xml
│   └── target/
├── database/
│   ├── schema.sql
│   └── seed-data.sql
├── docs/
│   ├── API_ENDPOINTS.md                (Complete endpoint reference)
│   ├── API_QUICK_REFERENCE.md          (Quick lookup)
│   ├── RBAC_IMPLEMENTATION.md          (RBAC architecture)
│   ├── RATE_LIMITING_*.md              (Rate limiting docs)
│   ├── SWAGGER_*.md                    (Swagger documentation)
│   └── ...
├── README.md                           (This file)
└── ...
```

## 🔄 Workflow Example

### Step 1: Login and Get Token
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password"
  }' | jq '.token'
```

### Step 2: Use Token in Requests
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password"}' | jq -r '.token')

# Call protected endpoint (USER can access own resources)
curl -X GET http://localhost:8080/accounts/1001/balance \
  -H "Authorization: Bearer $TOKEN" | jq

# Admin access (ADMIN can view any account)
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')

curl -X GET http://localhost:8080/api/v1/admin/accounts/1001 \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

### Step 3: View in Swagger UI
1. Open `http://localhost:8080/swagger-ui.html`
2. Click 🔒 **"Authorize"** button
3. Paste the token (without "Bearer " prefix)
4. Try any endpoint

## 🚦 Rate Limiting

The API implements **per-user rate limiting** using Bucket4j (Token Bucket Algorithm) to protect against abuse:

### Rate Limits
| Endpoint | Limit | Window |
|----------|-------|--------|
| POST `/auth/login` | 5 attempts | Per minute |
| POST `/transfers` | 10 transfers | Per minute |
| GET `/accounts/**` | 60 reads | Per minute |

### Rate Limited Response
```
HTTP 429 Too Many Requests
```

**Transfer endpoint includes description:**
```json
{
  "status": "RATE_LIMITED",
  "description": "Transfer rate limit exceeded. Maximum 10 transfers per minute."
}
```

### Testing Rate Limits
```bash
# Make 6 rapid login attempts (5th will get 429)
for i in {1..6}; do
  curl -X POST http://localhost:8080/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"test","password":"test"}' -w "HTTP %{http_code}\n"
done
```

**For details**: See [RATE_LIMITING_IMPLEMENTATION.md](docs/RATE_LIMITING_IMPLEMENTATION.md) and [RATE_LIMITING_QUICK_REFERENCE.md](docs/RATE_LIMITING_QUICK_REFERENCE.md)

## 🧪 Testing

### Run All Tests
```bash
cd backend
mvn test
```

### Run RBAC Tests Specifically
```bash
mvn test -Dtest=SecurityRoleIntegrationTest
```

### Run with Coverage
```bash
mvn test -D maven.test.failure.ignore=true
```

## 📖 Additional Resources

- **RBAC Architecture**: See [RBAC_IMPLEMENTATION.md](docs/RBAC_IMPLEMENTATION.md)
- **API Endpoints**: See [API_ENDPOINTS.md](docs/API_ENDPOINTS.md)
- **Swagger/OpenAPI Setup**: See [SWAGGER_IMPLEMENTATION.md](docs/SWAGGER_IMPLEMENTATION.md)
- **Using Swagger UI**: See [SWAGGER_UI_GUIDE.md](docs/SWAGGER_UI_GUIDE.md)
- **Rate Limiting Details**: See [RATE_LIMITING_IMPLEMENTATION.md](docs/RATE_LIMITING_IMPLEMENTATION.md)
- **Code Changes**: See [CODE_CHANGES.md](docs/CODE_CHANGES.md)
- **Database Migrations**: See [FLYWAY_SETUP.md](docs/FLYWAY_SETUP.md)

## 🤝 Contributing

1. Create a feature branch
2. Make your changes
3. Ensure tests pass (`mvn test`)
4. Submit a pull request

## 📄 License

This project is provided as-is for educational and development purposes.

## 📞 Support

For issues or questions:
1. Check the documentation files in `docs/`
2. Review the Swagger UI at `http://localhost:8080/swagger-ui.html`
3. Check the API spec at `http://localhost:8080/v3/api-docs`
4. Run tests to verify functionality: `mvn test`
