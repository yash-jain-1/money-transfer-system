# Swagger UI Access & Usage Guide

## 📍 Where to Find Swagger UI

**URL**: `http://localhost:8080/api/v1/swagger-ui/index.html`

---

## 🚀 Getting Started (5 Steps)

### Step 1: Start the Application
```bash
cd /home/yash/Downloads/money-transfer-system/backend
mvn clean package -DskipTests
java -jar target/money-transfer-system-1.0.0.jar
```

### Step 2: Open Swagger UI
Open in your browser:
```
http://localhost:8080/api/v1/swagger-ui/index.html
```

You should see:
- **Top Bar**: "Money Transfer System API" with version 1.0.0
- **3 Tag Groups**: Authentication, Transfers, Accounts
- **Lock Icon**: 🔒 on all protected endpoints
- **Authorize Button**: Top-right corner (blue button)

### Step 3: Get Authentication Token

In Swagger UI:
1. Click to expand **"Authentication"** section
2. Look for **`POST /auth/login`** endpoint
3. Click **"Try it out"** button
4. Enter request body:
   ```json
   {
     "username": "app-user",
     "password": "app-password"
   }
   ```
5. Click **"Execute"** button
6. Look at **Response body**:
   ```json
   {
     "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhcHAtdXNlciIsImlzcyI6Im1vbmV5LXRyYW5zZmVyLXN5c3RlbSIsImlhdCI6MTc3MDE5NjcxNywiZXhwIjoxNzcwMjAwMzE3fQ.fiG0K9N-KhkAOcgjZ9Fboq6akiYKRm9gco3qIst7ci4",
     "tokenType": "Bearer",
     "expiresIn": 3600
   }
   ```
7. **Copy the token value** (long string starting with `eyJ...`)

### Step 4: Authorize Swagger UI

1. Click the blue **"🔒 Authorize"** button (top-right)
2. In the popup dialog, you'll see a field that says:
   ```
   "Enter JWT token without 'Bearer ' prefix"
   ```
3. **Paste your token value** into this field (just the token, no "Bearer " prefix)
4. Click the **"Authorize"** button (blue button in the dialog)
5. Click **"Close"** button
6. You should now see a small lock icon next to "Authorize" button indicating you're authenticated

### Step 5: Try Protected Endpoints

Now you can test any protected endpoint:

#### Example: Get Account Details
1. Expand **"Accounts"** section
2. Click **`GET /accounts/{accountId}`**
3. Click **"Try it out"**
4. Enter `1` in the `accountId` field
5. Click **"Execute"**
6. You should see the response (or 404 if account doesn't exist)

#### Example: Transfer Money
1. Expand **"Transfers"** section
2. Click **`POST /transfers`**
3. Click **"Try it out"**
4. Enter request body:
   ```json
   {
     "sourceAccountId": 1,
     "destinationAccountId": 2,
     "amount": 100.00,
     "idempotencyKey": "550e8400-e29b-41d4-a716-446655440000",
     "description": "Test transfer"
   }
   ```
5. Click **"Execute"**
6. See the response

---

## 🔐 Security Indicators in Swagger UI

### ✅ Endpoints with 🔒 Lock Icon
These endpoints require JWT authentication:
- All endpoints under **"Transfers"** section
- All endpoints under **"Accounts"** section

### ❌ Endpoints WITHOUT Lock Icon
These endpoints don't require authentication:
- **`POST /auth/login`** - Get JWT token
- Swagger UI itself - `http://localhost:8080/api/v1/swagger-ui/**`
- API Docs - `http://localhost:8080/api/v1/v3/api-docs`

### 🔑 How Authorization Works in Swagger UI
1. You authorize **once** with the `/auth/login` token
2. All subsequent requests **automatically include** the JWT token in the Authorization header
3. You don't need to manually add "Authorization: Bearer ..." header

---

## 📊 Swagger UI Layout

```
┌─────────────────────────────────────────────────────────────────┐
│  Money Transfer System API (v1.0.0)                    🔒Auth  │
│  "Secure REST API for money transfers with JWT auth"           │
├─────────────────────────────────────────────────────────────────┤
│ ▼ Authentication                                                │
│   ├─ POST /auth/login                                          │
│   │  "Login with credentials"                                  │
│   │  Authenticate user and receive JWT token                  │
│   └─ Try it out [Execute]                                     │
│                                                                 │
│ ▼ Transfers                                                    │
│   ├─ POST /transfers 🔒                                        │
│   │  "Initiate money transfer"                                 │
│   │  Transfer funds between accounts with idempotency         │
│   │                                                             │
│   └─ GET /transfers/health 🔒                                  │
│      "Health check"                                            │
│      Verify service is running                                 │
│                                                                 │
│ ▼ Accounts                                                     │
│   ├─ GET /accounts/{accountId} 🔒                              │
│   │  "Get account details"                                     │
│   │  Retrieve account information by ID                        │
│   │                                                             │
│   ├─ GET /accounts/{accountId}/balance 🔒                      │
│   │  "Get account balance"                                     │
│   │  Retrieve current balance for an account                   │
│   │                                                             │
│   └─ GET /accounts/{accountId}/transactions 🔒                 │
│      "Get transaction history"                                 │
│      Retrieve all transactions for an account                  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🎯 Common Workflow

### Scenario: Test Money Transfer
```
1. Open Swagger UI
   → http://localhost:8080/api/v1/swagger-ui/index.html

2. Get Token
   → POST /auth/login
   → username: "app-user", password: "app-password"
   → Copy token from response

3. Authorize
   → Click 🔒 Authorize button
   → Paste token (no "Bearer " prefix)
   → Click Authorize → Close

4. Get Source Account Details
   → GET /accounts/1
   → Verify account exists and has balance

5. Get Destination Account Details
   → GET /accounts/2
   → Verify destination account exists

6. Initiate Transfer
   → POST /transfers
   → sourceAccountId: 1
   → destinationAccountId: 2
   → amount: 50.00
   → idempotencyKey: (generate UUID)
   → Click Execute

7. Verify Transfer
   → GET /accounts/1/transactions
   → See transfer deducted from source
   → GET /accounts/2/transactions
   → See transfer added to destination
```

---

## 💡 Pro Tips

### Tip 1: Reuse Token
- Your token is valid for 1 hour
- You only need to authorize **once** per Swagger session
- Token is stored in browser's local storage

### Tip 2: Copy Response Examples
- Click the copy icon 📋 next to any response to copy JSON
- Use in your application or testing

### Tip 3: See Request Details
- Click **"Show/Hide"** to see request headers
- Verify `Authorization: Bearer <token>` is automatically added

### Tip 4: Test Different Status Codes
- Try endpoints with invalid IDs to see 404 responses
- Try transfers with insufficient funds to see 409 responses
- See what actual error messages look like

### Tip 5: Download API Spec
- Click **"Download"** to save OpenAPI spec as JSON
- Use with code generation tools like OpenAPI Generator

---

## 🔧 Troubleshooting

### "401 Unauthorized" Error
**Problem**: Getting 401 on protected endpoints  
**Solution**:
- Click 🔒 Authorize button
- Verify token is pasted (check if field is blue/filled)
- Try logging in again with `/auth/login`
- Check if token has expired (default: 1 hour)

### Can't Find Swagger UI
**Problem**: Getting 404 on `/swagger-ui.html`  
**Solution**:
- Use full URL: `http://localhost:8080/api/v1/swagger-ui/index.html`
- Verify app is running on port 8080
- Check that springdoc-openapi dependency is in pom.xml

### Token Not Working
**Problem**: Correct credentials but still getting 401  
**Solution**:
- Make sure you copied the token **without** quotes
- Remove any "Bearer " prefix before pasting
- The authorization field should show a lock icon 🔒 when authorized

### Endpoints Not Showing Up
**Problem**: Don't see expected endpoints in Swagger UI  
**Solution**:
- Refresh browser (Ctrl+F5 or Cmd+Shift+R)
- Check `/api/v1/v3/api-docs` directly to see raw spec
- Verify endpoints are in your controllers

---

## 📈 Response Codes Reference

| Code | Meaning | Example |
|------|---------|---------|
| **200** | OK | GET account details successful |
| **201** | Created | POST transfer initiated successfully |
| **400** | Bad Request | Invalid request body |
| **401** | Unauthorized | Missing or invalid JWT token |
| **404** | Not Found | Account doesn't exist |
| **409** | Conflict | Insufficient funds for transfer |

---

## 🌐 Related URLs

| Purpose | URL |
|---------|-----|
| **Swagger UI** | `http://localhost:8080/api/v1/swagger-ui/index.html` |
| **API Docs (JSON)** | `http://localhost:8080/api/v1/v3/api-docs` |
| **Swagger Config** | `http://localhost:8080/api/v1/v3/api-docs/swagger-config` |
| **API Base URL** | `http://localhost:8080/api/v1` |
| **Application Home** | `http://localhost:8080/api/v1/transfers/health` |

---

## ✨ Key Features

✅ **Auto-Generated Docs**
- OpenAPI spec generated automatically
- No manual maintenance needed
- Always in sync with code

✅ **Interactive Testing**
- Try endpoints directly in browser
- No curl commands needed
- See real responses

✅ **JWT Support**
- Authorize once, test multiple endpoints
- Token automatically included in headers
- Proper security without complexity

✅ **Professional Documentation**
- Clean, organized interface
- Grouped by resource (Tags)
- Status codes and descriptions

✅ **Mobile Friendly**
- Responsive design
- Works on tablets and phones
- Touch-friendly controls

---

**Version**: 1.0.0  
**Last Updated**: 2026-02-04  
**Status**: ✅ Ready to Use
