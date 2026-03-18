# Quick Reference Guide - API Rate Limiting Platform

## What Is This? (In 30 Seconds)

A web application that lets developers manage their own APIs with automatic rate limiting and usage tracking. Think of it as a service layer that:
- Issues API keys
- Limits requests (prevents abuse)
- Tracks usage statistics
- Shows analytics in a dashboard

---

## Key Components At A Glance

```
Frontend (Angular) 
    ↓ HTTP requests
API Gateway (Java Spring Boot)
    ├─ Validates API keys
    ├─ Enforces rate limits (Redis)
    └─ Logs usage stats
        ↓
    Developer Service + Analytics Service (Java Spring Boot)
        ↓
    Databases (PostgreSQL + Redis)
```

---

## Technology Stack - Simple Breakdown

**Frontend:** Angular + TypeScript (what users see in browser)
**Backend:** Spring Boot Java (the business logic)  
**Databases:** PostgreSQL (permanent storage) + Redis (ultra-fast cache)
**DevOps:** Docker (packaging everything in containers)

---

## The Main Features

1. **Developer Registration** - Sign up, get account
2. **API Registration** - Register your API with platform
3. **API Key Generation** - Get credentials to access APIs
4. **Rate Limiting** - Prevent abuse with configured limits
5. **Analytics Dashboard** - See usage, request counts, errors
6. **Usage Tracking** - Automatic logging of all API calls

---

## How Rate Limiting Works

```
Limit: 100 requests per minute

Your requests:
1-100: ✓ Allowed
101: ✗ BLOCKED (return error 429)

After 1 minute passes:
101: ✓ Now allowed (window refreshed)
```

Uses **Redis** to track counts in real-time (microsecond speed).

---

## Database Tables

| Table | Purpose |
|-------|---------|
| developers | Who's using the platform |
| apis | What APIs are registered |
| api_keys | Access credentials |
| api_usage | Analytics/logging data |
| (Redis cache) | Rate limit counters |

---

## Architecture Pattern

**Microservices** - Multiple independent services that work together:
- Gateway Service (validates, rate limits)
- Developer Service (manages users, APIs, keys)
- Analytics Service (tracks usage)

**Benefits:**
- Each service can scale independently
- Easier to maintain and update
- One service failure doesn't crash everything

---

## Typical Flow

```
1. Developer signs up → stored in PostgreSQL
2. Developer creates API → stored in PostgreSQL  
3. Developer generates key → stored in PostgreSQL
4. Client uses key to call API → Gateway validates key
5. Gateway checks rate limit in Redis ✓
6. Gateway forwards request to actual API
7. Response logged to Redis
8. Analytics service reads logs
9. Dashboard shows usage stats
```

---

## Security Features

- ✓ API keys required (authentication)
- ✓ Rate limiting (prevents DOS abuse)
- ✓ Developers isolated (only see own data)
- ✓ Password encryption
- ✓ Access controls

---

## Performance

- API validation: ~1-2ms (Redis is fast!)
- Rate limit check: ~0.5-1ms
- Full request through gateway: ~10-50ms

Can handle thousands of simultaneous requests.

---

## Tech Justification

| Technology | Why |
|-----------|-----|
| **Spring Boot** | Simplifies Java web services, huge ecosystem |
| **Redis** | Microsecond lookups needed for rate limiting |
| **PostgreSQL** | Reliable, ACID compliant, proven |
| **Angular** | Modern interactive UIs with real-time updates |
| **Docker** | Deploy anywhere, consistent environment |
| **Java** | Typed, compiled, scales well, enterprise-ready |

---

## Deployment Model

Everything runs in Docker containers:
```
PostgreSQL Container 
Redis Container
Gateway Service Container
Developer Service Container
Analytics Service Container
Angular Frontend (separate)
```

Start with: `docker-compose up -d`

---

## Common Scenarios

### Scenario 1: SaaS Company
Offers API to customers. Needs to track usage for billing and prevent one customer from overwhelming system.
**This platform handles it perfectly.**

### Scenario 2: Internal Platform
Multiple teams share API infrastructure. Need to prevent team A from blocking team B.
**This platform handles it.**

### Scenario 3: Public API with Freemium
Free tier: 100 req/day, Pro: 10,000 req/day. Track for billing.
**This platform handles it.**

---

## File Organization

```
backend/
  ├─ gateway-service/       (Rate limiting, validation)
  ├─ developer-service/     (User, API, key management)
  └─ analytics-service/     (Usage tracking, stats)

frontend/
  └─ angular-dashboard/     (User interface)

docker/
  └─ docker-compose.yml     (Run everything)
```

---

## API Endpoints (Examples)

```
Developer Service:
POST   /api/developers              (Register)
POST   /api/apis                    (Create API)
POST   /api/keys                    (Generate key)
GET    /api/developers/{id}/apis    (List APIs)

Analytics Service:
GET    /api/analytics/apis/{id}     (Usage stats)

Gateway Service:
GET/POST/PUT /api/*?api_key=sk_...  (Rate limited)
```

---

## Error Responses

```
401 - Invalid API key
429 - Rate limit exceeded
500 - Server error
404 - Not found
```

---

## Key Metrics

- **Total requests handled** - Count per API
- **Success rate** - Percentage of 200 responses
- **Error rate** - Percentage of errors
- **Average response time** - In milliseconds
- **Requests this minute** - Current rate
- **Rate limit headroom** - How much until limit

---

## Why Microservices?

Instead of one monolithic app:
- Gateway handles traffic control
- Developer Service handles accounts
- Analytics handles data crunching

Each can:
- Be updated independently
- Scale separately
- Use different technologies
- Fail without taking others down

---

## Why Redis for Rate Limiting?

Not using PostgreSQL because:
- Disk is slow for millions of checks/second
- Redis is in-memory (microseconds)
- Auto-expire features (no cleanup needed)
- Atomic operations (no race conditions)

---

## Data Flow Summary

```
Developer Portal (Browser)
    ↓ (REST API calls)
Gateway
    ├─ Check PostgreSQL for key ✓
    ├─ Check Redis for rate limit ✓
    ├─ Update Redis counter
    └─ Forward to backend service
        ↓
    Developer Service / Business Logic
        ↓
    Return response
        ↓
    Log to queue
        ↓
    Analytics Service processes
        ↓
    Store in PostgreSQL
        ↓
    Dashboard shows stats
```

---

## Real-World Example

**Day 1:** Developer "Alice" signs up
```
POST /api/developers
{name: "Alice", email: "alice@example.com", password: "secure123"}
→ Created in PostgreSQL with ID 1
```

**Day 2:** Alice creates her API
```
POST /api/apis
{name: "Weather API", baseUrl: "https://weather.example.com", rateLimitPerMinute: 1000}
→ Created with ID 42
```

**Day 3:** Alice gets access key
```
POST /api/keys
{apiId: 42}
→ Generated key: sk_live_a1b2c3d4e5f6g7h8
```

**Day 4:** Alice's customer uses the API
```
GET /api/weather/forecast?api_key=sk_live_a1b2c3d4e5f6g7h8
→ Gateway validates key in PostgreSQL ✓
→ Gateway checks Redis: 450/1000 used this minute ✓
→ Gateway increments Redis to 451
→ Request forwarded
→ Response returned
→ Event logged (timestamp, response time, status)
```

**Day 5:** Alice checks dashboard
```
GET /api/analytics/apis/42/usage
→ Analytics Service queries all logged events from PostgreSQL
→ Returns: {totalRequests: 3500, successRate: 99.2%, errorRate: 0.8%, avgResponseTime: 145ms}
→ Dashboard shows charts and stats
```

---

## Performance Optimization Techniques Used

1. **Redis Caching** - Rate limits in ultra-fast memory
2. **Async Processing** - Don't wait for analytics to finish
3. **Connection Pooling** - Reuse DB connections
4. **Stateless Services** - Can run multiple instances
5. **TTL in Redis** - Auto-cleanup old data

---

## Scaling Path

For handling 10x more traffic:
1. Add more Gateway instances (behind load balancer)
2. Add PostgreSQL read replicas
3. Add Redis cluster nodes
4. Add Analytics instances
5. Add caching layer (Memcached, etc.)

All can scale independently!

---

## Testing Approach

- **Unit Tests** - Test individual functions
- **Integration Tests** - Test service communication
- **End-to-End Tests** - Test full request flow

Example: Simulate rate limit being hit, verify 429 response.

---

## Security Best Practices

✓ API keys validated every request
✓ Passwords hashed with Spring Security
✓ Rate limiting (prevents brute force)
✓ Access controls (isolation per developer)
✗ Not yet in this build: SSL/TLS, audit logging, 2FA

---

## Monitoring & Observability

Track:
- Request latency (p50, p95, p99)
- Error rates per endpoint
- Database query performance
- Redis hit/miss rates
- Active connections

Expose via metrics endpoint for monitoring tools (Prometheus, Grafana).

---

## Common Questions

**Q: What happens when I exceed my rate limit?**
A: You get a 429 "Too Many Requests" response. Wait for next minute!

**Q: How many API keys can I create?**
A: Unlimited! One per API is common, or multiple for different clients.

**Q: Can I change rate limits?**
A: Yes, edit the API configuration. Takes effect immediately.

**Q: How long are analytics kept?**
A: Currently stored in PostgreSQL indefinitely. Can add archival policies.

**Q: Can I generate new keys without deleting old ones?**
A: Yes! Create new key, old key still works. Delete old when ready.

**Q: What if Gateway goes down?**
A: With load balancer + multiple instances, traffic automatically reroutes.

---

## Production Checklist

- [ ] Enable SSL/TLS encryption
- [ ] Set strong database passwords
- [ ] Configure backup strategy
- [ ] Set up monitoring/alerting
- [ ] Enable audit logging
- [ ] Configure auto-scaling
- [ ] Set up API rate limit tiers
- [ ] Load test at expected volume
- [ ] Plan disaster recovery
- [ ] Documentation for ops team
- [ ] Security audit
- [ ] Performance baselines

---

## Summary

This is an **enterprise-grade API management platform** that demonstrates:
- Clean microservices architecture
- Performance optimization (Redis)
- Scalability patterns
- Security best practices
- Modern full-stack tech

It can handle real-world use cases like SaaS billing, internal platform quotas, and public API freemium tiers.

For full technical details, see **DETAILED_DOCUMENTATION.md**.
