# Rate Limiting Quick Reference

## Current Limits

| Feature | Limit | Window |
|---------|-------|--------|
| Login Attempts | 5 | Per minute |
| Transfers | 10 | Per minute |
| Account Reads (all) | 60 | Per minute |

## HTTP Responses

### Rate Limited (HTTP 429)
```bash
# Auth endpoint returns empty 429
HTTP 429 Too Many Requests

# Transfer endpoint returns description
HTTP 429 Too Many Requests
{
  "status": "RATE_LIMITED",
  "description": "Transfer rate limit exceeded. Maximum 10 transfers per minute."
}

# Account endpoint returns empty 429
HTTP 429 Too Many Requests
```

## Testing

### Quick Test: Auth Rate Limiting
```bash
# Run 6 login attempts in quick succession
for i in {1..6}; do
  curl -X POST http://localhost:8080/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"test","password":"test"}' -w "%{http_code}\n"
done
# Expected: 401 401 401 401 401 429
```

### Quick Test: Transfer Rate Limiting
```bash
# Get token first
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"password123"}' | jq -r '.token')

# Make 11 transfer requests
for i in {1..11}; do
  curl -X POST http://localhost:8080/api/v1/transfers/initiate \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"sourceAccountId":1,"destinationAccountId":2,"amount":100}' \
    -w "%{http_code}\n"
done
# 11th request returns 429
```

## Implementation Files

| File | Purpose |
|------|---------|
| `RateLimitConfig.java` | Spring Bean configuration |
| `RateLimitUtil.java` | Core rate limiting logic |
| `AuthController.java` | Login rate limiting |
| `TransferController.java` | Transfer rate limiting |
| `AccountController.java` | Account read rate limiting |

## Key Code Snippets

### Check Rate Limit (Auth)
```java
if (!rateLimitUtil.allowAuth(request.getUsername())) {
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
}
```

### Check Rate Limit (Authenticated)
```java
String userId = SecurityContextHolder.getContext().getAuthentication().getName();
if (!rateLimitUtil.allowTransfer(userId)) {
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
}
```

## Scaling to Multiple Instances

1. Add Redis dependency to pom.xml
2. Update `RateLimitConfig.java` to use Redis backend
3. Configure Redis connection in `application.yml`
4. Deploy - no controller changes needed!

## Logs to Watch

```
# Token consumed successfully
2026-02-04 15:03:56 - auth allowed for john: remaining tokens = 4

# Rate limit exceeded
2026-02-04 15:06:01 - auth rate limit exceeded for test
```

## Status: ✅ COMPLETE

- ✅ Built and tested
- ✅ HTTP 429 responses working
- ✅ Per-user rate limiting verified
- ✅ Swagger documentation added
- ✅ Production-ready architecture
