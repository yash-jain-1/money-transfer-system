# Money Transfer System

A secure REST API for money transfers with JWT authentication and professional API documentation.

## 🚀 Quick Start

### 1. Start the Application
```bash
cd backend
mvn clean package -DskipTests
java -jar target/money-transfer-system-1.0.0.jar
```

### 2. Access Swagger UI
**Browser**: `http://localhost:8080/api/v1/swagger-ui/index.html`

### 3. Get JWT Token
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"app-user","password":"app-password"}'
```

### 4. Call Protected API
```bash
TOKEN="<paste-token-here>"
curl -X GET http://localhost:8080/api/v1/accounts/1 \
  -H "Authorization: Bearer $TOKEN"
```

## 📚 Documentation

### API Documentation
- **Swagger UI**: `http://localhost:8080/api/v1/swagger-ui/index.html`
- **OpenAPI Spec**: `http://localhost:8080/api/v1/v3/api-docs`

### Project Documentation
- **[SWAGGER_QUICK_REFERENCE.md](SWAGGER_QUICK_REFERENCE.md)** - Quick reference for Swagger usage
- **[SWAGGER_UI_GUIDE.md](SWAGGER_UI_GUIDE.md)** - Detailed Swagger UI usage guide
- **[SWAGGER_IMPLEMENTATION.md](SWAGGER_IMPLEMENTATION.md)** - Complete implementation details
- **[RATE_LIMITING_IMPLEMENTATION.md](RATE_LIMITING_IMPLEMENTATION.md)** - Rate limiting architecture & guide
- **[RATE_LIMITING_QUICK_REFERENCE.md](RATE_LIMITING_QUICK_REFERENCE.md)** - Quick reference for rate limiting
- **[RATE_LIMITING_INTEGRATION_TESTS.md](RATE_LIMITING_INTEGRATION_TESTS.md)** - Integration test suite & results
- **[RATE_LIMITING_TEST_REPORT.md](RATE_LIMITING_TEST_REPORT.md)** - Detailed test execution report
- **[CODE_CHANGES.md](CODE_CHANGES.md)** - Code changes summary
- **[FLYWAY_SETUP.md](FLYWAY_SETUP.md)** - Database migration setup

## 🔐 Security

### Authentication
- **Type**: HTTP Bearer (JWT)
- **Token Expiration**: 1 hour (configurable)
- **Public Endpoints**: `/auth/login`, `/swagger-ui/**`, `/v3/api-docs/**`
- **Protected Endpoints**: All other `/api/v1/**` endpoints require JWT

### Bearer Token Usage
```
Authorization: Bearer <your-jwt-token>
```

## 📋 API Endpoints

### Authentication
- `POST /auth/login` - Get JWT token

### Transfers
- `POST /transfers` - Initiate money transfer
- `GET /transfers/health` - Health check

### Accounts
- `GET /accounts/{accountId}` - Get account details
- `GET /accounts/{accountId}/balance` - Get account balance
- `GET /accounts/{accountId}/transactions` - Get transaction history

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

# Application User
APP_USER=app-user
APP_PASSWORD=app-password
```

### Application Properties
File: `backend/src/main/resources/application.yml`

## ✅ Features

- ✅ Secure REST API with JWT authentication
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
│   │   │   ├── SecurityConfig.java     (Security rules)
│   │   │   └── ...
│   │   ├── controller/                 (REST endpoints)
│   │   ├── service/                    (Business logic)
│   │   ├── repository/                 (Data access)
│   │   └── ...
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/               (Flyway migrations)
│   ├── pom.xml
│   └── target/
├── database/
│   ├── schema.sql
│   └── seed-data.sql
├── docs/
├── README.md                           (This file)
├── SWAGGER_IMPLEMENTATION.md           (Swagger details)
├── SWAGGER_QUICK_REFERENCE.md          (Quick reference)
├── SWAGGER_UI_GUIDE.md                 (UI guide)
└── CODE_CHANGES.md                     (Code changes)
```

## 🔄 Workflow Example

### Step 1: Login and Get Token
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "app-user",
    "password": "app-password"
  }' | jq '.token'
```

### Step 2: Use Token in Requests
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"app-user","password":"app-password"}' | jq -r '.token')

# Call protected endpoint
curl -X GET http://localhost:8080/api/v1/accounts/1 \
  -H "Authorization: Bearer $TOKEN" | jq
```

### Step 3: View in Swagger UI
1. Open `http://localhost:8080/api/v1/swagger-ui/index.html`
2. Click 🔒 **"Authorize"** button
3. Paste the token (without "Bearer " prefix)
4. Try any endpoint

## 🚦 Rate Limiting

The API implements **per-user rate limiting** using Bucket4j (Token Bucket Algorithm) to protect against abuse:

### Rate Limits
| Endpoint | Limit | Window |
|----------|-------|--------|
| POST `/auth/login` | 5 attempts | Per minute |
| POST `/transfers/initiate` | 10 transfers | Per minute |
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

**For details**: See [RATE_LIMITING_IMPLEMENTATION.md](RATE_LIMITING_IMPLEMENTATION.md) and [RATE_LIMITING_QUICK_REFERENCE.md](RATE_LIMITING_QUICK_REFERENCE.md)

## 🧪 Testing

### Run Tests
```bash
cd backend
mvn test
```

### Run Tests with Coverage
```bash
mvn test -D maven.test.failure.ignore=true
```

## 📖 Additional Resources

- **Swagger/OpenAPI Setup**: See [SWAGGER_IMPLEMENTATION.md](SWAGGER_IMPLEMENTATION.md)
- **Using Swagger UI**: See [SWAGGER_UI_GUIDE.md](SWAGGER_UI_GUIDE.md)
- **Swagger Quick Commands**: See [SWAGGER_QUICK_REFERENCE.md](SWAGGER_QUICK_REFERENCE.md)
- **Rate Limiting Details**: See [RATE_LIMITING_IMPLEMENTATION.md](RATE_LIMITING_IMPLEMENTATION.md)
- **Code Changes**: See [CODE_CHANGES.md](CODE_CHANGES.md)

## 🤝 Contributing

1. Create a feature branch
2. Make your changes
3. Ensure tests pass
4. Submit a pull request

## 📄 License

This project is provided as-is for educational and development purposes.

## 📞 Support

For issues or questions:
1. Check the documentation files
2. Review the Swagger UI at `http://localhost:8080/api/v1/swagger-ui/index.html`
3. Check the API spec at `http://localhost:8080/api/v1/v3/api-docs`
