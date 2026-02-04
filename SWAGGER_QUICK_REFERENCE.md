# Swagger/OpenAPI Quick Reference

## 🔗 Access URLs

| Resource | URL | Auth Required |
|----------|-----|---------------|
| **Swagger UI** | `http://localhost:8080/api/v1/swagger-ui/index.html` | ❌ No |
| **API Docs (JSON)** | `http://localhost:8080/api/v1/v3/api-docs` | ❌ No |
| **Login** | `POST http://localhost:8080/api/v1/auth/login` | ❌ No |
| **Any Business Endpoint** | `http://localhost:8080/api/v1/**` | ✅ JWT Required |

---

## 🔐 How to Use Swagger UI with JWT

### 1️⃣ Open Swagger UI
```
http://localhost:8080/api/v1/swagger-ui/index.html
```

### 2️⃣ Get JWT Token
- Expand **"Authentication"** section
- Click **"Try it out"** on `/auth/login` endpoint
- Enter credentials in request body:
  ```json
  {
    "username": "app-user",
    "password": "app-password"
  }
  ```
- Click **"Execute"**
- Copy the `token` value from response

### 3️⃣ Authorize for Protected Endpoints
- Click the 🔒 **"Authorize"** button (top-right)
- Paste token value (without "Bearer " prefix)
- Click **"Authorize"** → **"Close"**

### 4️⃣ Call Protected Endpoints
- All endpoints under **"Transfers"** and **"Accounts"** now work
- Token is automatically included in Authorization header
- Try **"Try it out"** → **"Execute"**

---

## 💻 Command Line Usage

### Get Token
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username":"app-user",
    "password":"app-password"
  }' | jq '.token'
```

### Use Token (Example: Get Account)
```bash
TOKEN="<paste-token-here>"

curl -X GET http://localhost:8080/api/v1/accounts/1 \
  -H "Authorization: Bearer $TOKEN" | jq
```

### Check API Docs
```bash
curl http://localhost:8080/api/v1/v3/api-docs | jq
```

---

## 🏷️ Endpoint Categories

### 🔑 Authentication (Public)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/login` | Get JWT token |

### 💸 Transfers (Protected)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/transfers` | Initiate money transfer |
| GET | `/transfers/health` | Health check |

### 💰 Accounts (Protected)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/accounts/{accountId}` | Get account details |
| GET | `/accounts/{accountId}/balance` | Get current balance |
| GET | `/accounts/{accountId}/transactions` | Get transaction history |

---

## 🔒 Security Details

**Authentication Type**: HTTP Bearer (JWT)

**Token Format**: 
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Token Expiration**: 1 hour (configurable via `JWT_EXPIRATION_MS`)

**Error Response** (without valid token):
```
HTTP 401 Unauthorized
```

---

## 🛠️ Configuration

### Environment Variables
```bash
JWT_SECRET=your-secret-key-min-32-chars
JWT_EXPIRATION_MS=3600000  # 1 hour
APP_USER=app-user
APP_PASSWORD=app-password
```

### Application Properties
File: `backend/src/main/resources/application.yml`
```yaml
server.servlet.context-path: /api/v1
app.jwt.expiration-ms: 3600000
app.security.user.username: app-user
app.security.user.password: app-password
```

---

## ✅ What's Included

- ✅ **Springdoc OpenAPI 2.3.0** - Auto-generates OpenAPI spec
- ✅ **Swagger UI** - Interactive API documentation
- ✅ **Bearer Token Support** - JWT authentication in UI
- ✅ **Response Codes** - HTTP status codes documented
- ✅ **Tag Groups** - Organized by resource (Transfers, Accounts, Auth)
- ✅ **Security Scheme** - HTTP Bearer scheme configured

---

## 📋 API Response Examples

### 200 OK - Account Details
```json
{
  "id": 1,
  "accountNumber": "ACC001",
  "accountHolder": "John Doe",
  "balance": 10000.00,
  "accountType": "SAVINGS",
  "status": "ACTIVE",
  "createdAt": "2024-02-04T10:30:00",
  "updatedAt": "2024-02-04T14:30:00"
}
```

### 200 OK - Login Success
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

### 401 Unauthorized
```json
{
  "status": 401,
  "message": "Unauthorized access",
  "error": "Unauthorized",
  "timestamp": "2024-02-04T14:30:00",
  "path": "/api/v1/accounts/1"
}
```

---

## 🚀 Quick Start Commands

```bash
# 1. Build project
cd backend
mvn clean package -DskipTests

# 2. Start application
java -jar target/money-transfer-system-1.0.0.jar

# 3. Open Swagger UI in browser
# http://localhost:8080/api/v1/swagger-ui/index.html

# 4. Or use curl to get token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"app-user","password":"app-password"}' | jq -r '.token')

# 5. Call protected endpoint
curl -X GET "http://localhost:8080/api/v1/accounts/1" \
  -H "Authorization: Bearer $TOKEN"
```

---

## ❓ Common Issues

### "401 Unauthorized" on Protected Endpoint
- ✅ Ensure token is in `Authorization: Bearer <token>` format
- ✅ Check token hasn't expired (default: 1 hour)
- ✅ Verify credentials are correct

### Swagger UI shows endpoints but no descriptions
- ✅ Refresh browser (Ctrl+F5 or Cmd+Shift+R)
- ✅ Check API docs endpoint: `/api/v1/v3/api-docs`

### Can't paste token in Swagger UI
- ✅ Click the 🔒 **"Authorize"** button
- ✅ Paste token **WITHOUT** "Bearer " prefix
- ✅ Click **"Authorize"** button in dialog

---

## 📚 Files Reference

| File | Purpose |
|------|---------|
| `pom.xml` | Maven dependencies (springdoc-openapi added) |
| `config/OpenApiConfig.java` | OpenAPI configuration and schema |
| `config/SecurityConfig.java` | Security rules (Swagger URLs are public) |
| `controller/*.java` | API endpoints with OpenAPI annotations |

---

## 🎯 Key Takeaways

1. **No Security Weakened** - Swagger is public, but all endpoints still require JWT
2. **Easy to Use** - Get token once, use for all requests
3. **Auto-Documented** - OpenAPI spec generated automatically
4. **Professional** - Swagger UI provides polished API documentation
5. **Standard** - Uses industry-standard OpenAPI 3.0.1 spec

---

**Version**: 1.0.0  
**Last Updated**: 2026-02-04  
**Status**: ✅ Production Ready
