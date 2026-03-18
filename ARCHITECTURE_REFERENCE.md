# System Architecture & Visual Reference

## 1. High-Level System Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                            USERS/DEVELOPERS                             │
│                  (in web browser or mobile app)                         │
└────────────────────────────────┬────────────────────────────────────────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         │                       │                       │
         ▼                       ▼                       ▼
    ┌────────────┐         ┌────────────┐         ┌────────────┐
    │  Angular   │         │  Angular   │         │  Angular   │
    │ Dashboard  │         │   Auth     │         │ Analytics  │
    │  Component │         │   Page     │         │  Charts    │
    └────────────┘         └────────────┘         └────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │ HTTP Requests (JSON)
                                 │
         ┌───────────────────────┴───────────────────────┐
         │                                               │
         ▼                                               ▼
    ┌─────────────────────────────┐          ┌──────────────────────┐
    │   API GATEWAY SERVICE       │          │  LOAD BALANCER       │
    │   (Spring Boot, Port 8080)  │          │  (Optional, for      │
    │                             │          │   horizontal scale)   │
    │  Responsibilities:          │          └──────────────────────┘
    │  • Validate API keys        │
    │  • Check rate limits        │
    │  • Route requests           │
    │  • Log analytics            │
    └─────────────────────────────┘
              │
              │ (Internal communication)
              │
    ┌─────────┴──────────┬──────────────────────┐
    │                    │                      │
    ▼                    ▼                      ▼
┌───────────────┐  ┌──────────────────┐  ┌───────────────┐
│  DEVELOPER    │  │   ANALYTICS      │  │  ACTUAL       │
│  SERVICE      │  │   SERVICE        │  │  API BACKEND  │
│  (Spring Boot,│  │   (Spring Boot,  │  │  (Any server) │
│   Port 8081)  │  │    Port 8082)    │  │               │
│               │  │                  │  │               │
│ Manages:      │  │ Tracks:          │  │ Your business │
│ • Developers  │  │ • Usage metrics  │  │ logic lives   │
│ • APIs        │  │ • Performance    │  │ here!         │
│ • API Keys    │  │ • Error rates    │  │               │
└───────────────┘  └──────────────────┘  └───────────────┘
    │                  │                      │
    └──────────────────┼──────────────────────┘
                       │ (Database queries)
                       │
         ┌─────────────┼─────────────┐
         │             │             │
         ▼             ▼             ▼
    ┌────────────┐  ┌────────────┐ ┌────────────┐
    │PostgreSQL  │  │   Redis    │ │   Redis    │
    │ Database   │  │   Cache    │ │  Cluster   │
    │ (Port 5432)│  │(Port 6379) │ │ (Scaled)   │
    │            │  │            │ └────────────┘
    │ Tables:    │  │ Stores:    │
    │ • developers│  │ • Rate     │
    │ • apis     │  │   limits   │
    │ • api_keys │  │ • Sessions │
    │ • api_usage│  │ • Cache    │
    └────────────┘  └────────────┘
```

---

## 2. Request Flow Diagram

```
┌──────────────────────────────────────────────────────────────────────────┐
│                         Developer/Client                                 │
│                                                                          │
│  GET /api/users 123?api_key=sk_live_a1b2c3d4e5f6g7h8                  │
└────────────────────────────────────┬─────────────────────────────────────┘
                                     │
                                     ▼
                        ┌────────────────────────┐
                        │   API GATEWAY          │
                        │   Receives Request     │
                        └────────┬───────────────┘
                                 │
                    ┌────────────┴─────────────┐
                    │                         │
                    ▼                         ▼
         ┌─────────────────────┐   ┌──────────────────────┐
         │ STEP 1:             │   │ Validate Key         │
         │ Extract API Key     │   │ Query: SELECT * FROM │
         │ sk_live_a1b2c3d4... │   │ api_keys WHERE...    │
         └──────────┬──────────┘   │ Database: PostgreSQL │
                    │              │ ✓ Found              │
                    └──────────────┴────────┬─────────────┘
                                           │
                                 ┌─────────▼──────────┐
                                 │ STEP 2:            │
                                 │ Check Rate Limit   │
                                 │                    │
                                 │ In Redis:          │
                                 │ Key: rate_limit:   │
                                 │ sk_live_...:202603 │
                                 │ 18T1030            │
                                 │ Value: 45/100      │
                                 │                    │
                                 │ ✓ Under limit      │
                                 └────────┬───────────┘
                                          │
                  ┌───────────────────────┼───────────────────────┐
                  │                       │                       │
                  ▼                       ▼                       ▼
         ┌──────────────────┐    ┌──────────────────┐    ┌──────────────┐
         │ STEP 3:          │    │ STEP 4:          │    │ STEP 5:      │
         │ Increment        │    │ Forward Request  │    │ Record Event │
         │ Redis Counter    │    │ to Backend       │    │ in Queue     │
         │                  │    │                  │    │              │
         │ Rate_limit...:45 │    │ Backend Service  │    │ Event:       │
         │        → 46      │    │ Processes logic  │    │ timestamp    │
         │                  │    │ Returns response │    │ api_id       │
         │ TTL: 60 seconds  │    │ Status: 200      │    │ response_time│
         └──────────────────┘    │ Data: {...}     │    │ status_code  │
                  │              └────────┬─────────┘    └──────────────┘
                  │                       │                    │
                  └───────────────────────┼────────────────────┘
                                          │
                                ┌─────────▼─────────┐
                                │ STEP 6:           │
                                │ Return Response   │
                                │ to Client         │
                                │                   │
                                │ HTTP 200 OK       │
                                │ {                 │
                                │  id: 123,         │
                                │  name: "Alice",   │
                                │  email: "..."     │
                                │ }                 │
                                └─────────┬─────────┘
                                          │
                                          ▼
                        ┌──────────────────────────────┐
                        │ Meanwhile...                  │
                        │ Analytics Service processes  │
                        │ the event and stores it in   │
                        │ PostgreSQL for future        │
                        │ analytics queries            │
                        └──────────────────────────────┘
```

---

## 3. Data Model (Entity Relationship Diagram)

```
┌─────────────────────────────┐
│       DEVELOPERS            │
├─────────────────────────────┤
│ id (PK)                     │ ◄────────────────────────┐
│ name                        │                         │
│ email (UNIQUE)              │                         │
│ password (HASHED)           │                         │
│ created_at                  │                         │
└────────────┬────────────────┘                         │
             │                                          │
             │ (One Developer has Many APIs)            │
             │                                          │
             ▼                                          │
┌─────────────────────────────┐                         │
│         APIS                │                         │
├─────────────────────────────┤                         │
│ id (PK)                     │                         │
│ developer_id (FK)           │ ─────────────────────────┘
│ name                        │
│ base_url                    │
│ rate_limit_per_minute       │
│ created_at                  │
└────────────┬────────────────┘
             │
             │ (One API has Many Keys)
             │
             ▼
┌─────────────────────────────┐
│      API_KEYS               │
├─────────────────────────────┤
│ id (PK)                     │
│ api_id (FK)                 │ ◄───────┐
│ key_value (UNIQUE)          │        │
│ created_at                  │        │
└────────────┬────────────────┘        │
             │                         │
             │ (One Key generates      │
             │  Many Usage Records)    │
             │                         │
             ▼                         │
┌─────────────────────────────┐        │
│     API_USAGE               │        │
├─────────────────────────────┤        │
│ id (PK)                     │        │
│ api_id (FK)                 │ ───────┘
│ timestamp                   │
│ status_code                 │
│ response_time_ms            │
│ request_path                │
│ client_ip                   │
└─────────────────────────────┘

┌────────────────────────────────────────┐
│    REDIS (NOT persistent)              │
├────────────────────────────────────────┤
│ Key Format:                            │
│ rate_limit:{api_key}:{minute}          │
│                                        │
│ Example:                               │
│ rate_limit:sk_live_abc123:202603      │
│ 181030 → 45                           │
│ (45 requests this minute)              │
│                                        │
│ TTL: 60 seconds (auto expires)         │
└────────────────────────────────────────┘
```

---

## 4. Service Interaction Diagram

```
                            ┌─────────────────┐
                            │  FRONTEND       │
                            │  (Angular)      │
                            └────────┬────────┘
                                     │
                 ┌───────────────────┼───────────────────┐
                 │                   │                   │
                 ▼                   ▼                   ▼
          ┌────────────┐      ┌────────────┐    ┌──────────────┐
          │ GET /apis  │      │ POST /keys │    │ GET /usage   │
          │            │      │            │    │              │
          │ Developer  │      │ Developer  │    │ Analytics    │
          │ Service    │      │ Service    │    │ Service      │
          └─────┬──────┘      └─────┬──────┘    └─────┬────────┘
                │                   │                  │
                └───────┬───────────┴──────────┬───────┘
                        │                      │
                        ▼                      ▼
                ┌────────────────────────────────────────┐
                │        GATEWAY SERVICE                 │
                ├────────────────────────────────────────┤
                │ • Validates every incoming request     │
                │ • Checks rate limits (Redis)           │
                │ • Routes to appropriate service        │
                │ • Logs usage analytics                 │
                └────────────────────────────────────────┘
                        │
         ┌──────────────┼──────────────┐
         │              │              │
         ▼              ▼              ▼
    ┌─────────┐   ┌──────────┐   ┌──────────┐
    │PostgreSQL│   │  Redis   │   │ Message  │
    │ Database │   │  Cache   │   │  Queue   │
    └─────────┘   └──────────┘   └──────────┘
         ▲              ▲              │
         │              │              │
         │ Stores       │ Caches       │ Events
         │ everything   │ rate limits  │
         │              │              ▼
         │              │         ┌─────────────────┐
         │              │         │ Analytics       │
         │              │         │ Service         │
         │              │         │ Processes       │
         │              │         │ & Aggregates    │
         │              │         └────────┬────────┘
         │              │                  │
         └──────────────┴──────────────────┘
```

---

## 5. Rate Limiting Algorithm Visualization

```
Timeline: Each box = 1 minute

Scenario 1: Requests spread throughout minute (ALLOWED)
────────────────────────────────────────────────────
Min 10:30  │  Req 1...10...20...30...40...50... │ (limit: 100)
           │  Rate: 50 requests ✓ UNDER LIMIT    │
────────────────────────────────────────────────────

Scenario 2: All requests at once (ALLOWED until limit)
────────────────────────────────────────────────────
Min 10:30  │  Req 1→100 (rapid fire) │ REQUEST 101 │
           │  ✓ ALLOWED              │ ✗ BLOCKED   │
           │  100/100 used           │ ERROR 429   │
────────────────────────────────────────────────────

Scenario 3: Continues next minute
────────────────────────────────────────────────────
Min 10:30  │  Req 1→100 │ (window closes, counter resets)
Min 10:31  │  Req 1→...  │ New window starts fresh!
           │  ✓ ALLOWED  │
────────────────────────────────────────────────────

Redis Storage Example:
═════════════════════════════════════════════
TimeStamp      │ Redis Key            │ Value │ TTL
───────────────┼──────────────────────┼───────┼─────
10:30:00-10:31 │ rate_limit:key:202603│  95   │ 60s
               │ 181030               │       │
               │                      │       │
10:31:00-10:32 │ rate_limit:key:202603│  42   │ 60s
               │ 181031               │       │
               │                      │       │
               │ (old key expires)    │  -    │ 0s
═════════════════════════════════════════════
```

---

## 6. Authentication & Authorization Flow

```
┌─────────────────────────────────────────────────────┐
│       STEP 1: Developer Tries to Login              │
│                                                     │
│  POST /auth/login                                   │
│  {                                                  │
│    "email": "alice@example.com",                    │
│    "password": "secure123"                          │
│  }                                                  │
└────────────────────────┬────────────────────────────┘
                         │
                         ▼
        ┌──────────────────────────────┐
        │ Spring Security              │
        │ • Hash password              │
        │ • Compare with DB hash       │
        │ • ✓ Match found              │
        └────────────┬─────────────────┘
                     │
                     ▼
        ┌──────────────────────────────┐
        │ Generate JWT Token           │
        │                              │
        │ Header: {alg: "HS256"}        │
        │ Payload: {sub: "alice", id:1}│
        │ Signature: secret_key        │
        │                              │
        │ Token:                       │
        │ eyJhbGciOiJIUzI1NiIs...     │
        └────────────┬─────────────────┘
                     │
                     ▼
        Return to Frontend:
        {
          "token": "eyJhbGciOiJIUzI1NiIs...",
          "expiresIn": 3600
        }

┌─────────────────────────────────────────────────────┐
│  STEP 2: Developer Uses Token For Requests          │
│                                                     │
│  GET /api/apis                                      │
│  Authorization: Bearer eyJhbGciOiJIUzI1NiIs...      │
└────────────────────────┬────────────────────────────┘
                         │
                         ▼
        ┌──────────────────────────────┐
        │ Spring Security              │
        │ • Extract token from header  │
        │ • Verify signature           │
        │ • Check expiration           │
        │ • Extract user ID            │
        │ • ✓ Valid                    │
        └────────────┬─────────────────┘
                     │
                     ▼
        ┌──────────────────────────────┐
        │ API Controller               │
        │ • Load user context          │
        │ • Query only user's APIs     │
        │ • Return response            │
        └────────────┬─────────────────┘
                     │
                     ▼
        Return: [
          {id: 1, name: "API 1", ...},
          {id: 2, name: "API 2", ...}
        ]
```

---

## 7. Deployment Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                    docker-compose.yml                        │
│  (Orchestrates all containers)                               │
└──────────────────────────────────────────────────────────────┘
            │
            ├──────────────┬──────────────┬─────────────────┬───────┐
            │              │              │                 │       │
            ▼              ▼              ▼                 ▼       ▼
    ┌────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────┐ ┌────────────┐
    │ PostgreSQL │  │   Redis     │  │  Gateway    │  │Developer│ │ Analytics  │
    │ Container  │  │  Container  │  │  Container  │  │ Service │ │ Container  │
    │            │  │             │  │             │  │Container│ │            │
    │ Image:     │  │ Image:      │  │ Image:      │  │Image:   │ │ Image:     │
    │ postgres:15│  │ redis:7     │  │ Custom      │  │Custom   │ │ Custom     │
    │            │  │             │  │ (Java)      │  │ (Java)  │ │ (Java)     │
    │ Port: 5432 │  │ Port: 6379  │  │ Port: 8080  │  │Port:8081│ │ Port:8082  │
    │            │  │             │  │             │  │         │ │            │
    │ Volumes:   │  │ Volumes:    │  │ Connects to:│  │Connects │ │ Connects   │
    │ postgres_  │  │ redis_data  │  │ • PostgreSQL│  │to:      │ │ to:        │
    │ data       │  │             │  │ • Redis     │  │• Postgre│ │• PostgreSQL│
    │            │  │             │  │ • Dev Svc   │  │• Redis  │ │• Redis     │
    └────────────┘  └─────────────┘  └─────────────┘  └─────────┘ │            │
         ▲              ▲                  ▲              ▲        └────────────┘
         │              │                  │              │
         └──────────────┴──────────────────┴──────────────┘
                        │
                   Network: compose_network
                (All containers communicate)

┌──────────────────────────────────────────────────────────────┐
│              Local (Not in Docker)                           │
├──────────────────────────────────────────────────────────────┤
│ Angular Frontend                                             │
│ Port: 4200 (http://localhost:4200)                          │
│ Connects to:                                                 │
│ • http://localhost:8080 (Gateway)                           │
│ • http://localhost:8081 (Developer Service)                 │
│ • http://localhost:8082 (Analytics Service)                 │
└──────────────────────────────────────────────────────────────┘
```

---

## 8. Caching Strategy

```
Request Flow with Caching:
═══════════════════════════════════════════════

First Request for API #5 Usage:
  Gateway
    ├─ Check Cache (Redis) → MISS
    ├─ Query PostgreSQL → HIT
    ├─ Store in Cache for 5 minutes
    └─ Return to client
    
Time: 150ms (DB query)

Next 4 Requests (within 5 mins) for API #5 Usage:
  Gateway
    ├─ Check Cache (Redis) → HIT ✓
    └─ Return from cache
    
Time: 1ms (cache hit!)

After 5 minutes Cache Expires:
  Gateway
    ├─ Check Cache (Redis) → EXPIRED
    ├─ Query PostgreSQL (fresh data)
    ├─ Store new cache for 5 mins
    └─ Return to client

Cache Architecture:
═══════════════════════════════════════════════

Redis Cache Hierarchy:

┌─────────────────────────────────────────┐
│ L1: Rate Limit Counters (mandatory)     │
│ TTL: 60 seconds                         │
│ Pattern: rate_limit:{key}:{minute}      │
│ Updates: Every request                  │
└─────────────────────────────────────────┘
         ▼ (if miss)
┌─────────────────────────────────────────┐
│ L2: API Key Info Cache (optional)       │
│ TTL: 24 hours                           │
│ Pattern: api_key:{id}                   │
│ Updates: When key changes               │
└─────────────────────────────────────────┘
         ▼ (if miss)
┌─────────────────────────────────────────┐
│ L3: PostgreSQL Database                 │
│ TTL: None (persistent)                  │
│ Contains: Complete source of truth      │
│ Updates: Application writes             │
└─────────────────────────────────────────┘
```

---

## 9. Error Handling Flow

```
API Request
    │
    ▼
┌─────────────────┐
│ Syntax Error?   │ ──YES──> Return 400 Bad Request
└────────┬────────┘         {errorMessage: "Invalid JSON"}
         │ NO
         ▼
┌─────────────────┐
│ API Key Valid?  │ ──NO──> Return 401 Unauthorized
└────────┬────────┘        {errorMessage: "Invalid API key"}
         │ YES
         ▼
┌─────────────────┐
│ Rate Limit OK?  │ ──NO──> Return 429 Too Many Requests
└────────┬────────┘        {errorMessage: "Rate limit exceeded"}
         │ YES
         ▼
┌─────────────────┐
│ DB Error?       │ ──YES──> Return 500 Internal Server Error
└────────┬────────┘        {errorMessage: "Database connection failed"}
         │ NO
         ▼
┌─────────────────┐
│ Business Logic? │ ──ERROR──> Return 400/422 Invalid Request
└────────┬────────┘          {errorMessage: "Email already exists"}
         │ OK
         ▼
    ✓ Return 200 OK
    {data: {...}}
```

---

## 10. Scaling Paths

```
CURRENT ARCHITECTURE (Single Instance):
═══════════════════════════════════════

        ┌─────────┐
        │ Browser │
        └────┬────┘
             │
   ┌─────────▼──────────┐
   │  API Gateway (1)   │
   └─────────┬──────────┘
        ┌────┴────────────┐
        │                 │
        ▼                 ▼
    ┌─────────┐      ┌────────┐
    │Postgres │      │ Redis  │
    │   (1)   │      │  (1)   │
    └─────────┘      └────────┘


SCALED ARCHITECTURE (Production):
═════════════════════════════════

    ┌──────┐  ┌──────┐  ┌──────┐
    │ App1 │  │ App2 │  │ App3 │
    └──┬───┘  └──┬───┘  └──┬───┘
       │         │         │
       └────┬────┴────┬────┘
            │         │
       ┌────▼─────────▼────┐
       │  Load Balancer    │
       └────┬──────────────┘
            │
    ┌───────┼─────────┐
    │       │         │
    ▼       ▼         ▼
┌──────┐ ┌──────┐ ┌──────┐
│ GW-1 │ │ GW-2 │ │ GW-3 │
└──┬───┘ └──┬───┘ └──┬───┘
   │        │        │
   └────┬───┴────┬───┘
        │        │
    ┌───▼──┐  ┌──▼────┐
    │ Prim │  │ Repli │
    │  PG  │  │  PG   │
    └──────┘  └───────┘
        ▲
   Read only

    ┌──────────────────┐
    │ Redis Cluster    │
    │ (3+ nodes)       │
    │ Redundancy ✓     │
    │ Failover ✓       │
    └──────────────────┘
```

---

## 11. Data Flow for Analytics

```
API Request Received → Processed → Response Sent
        │
        └────→ Logging Event Created
                │
                ├─ API ID: 5
                ├─ Timestamp: 2026-03-18 10:30:45.123
                ├─ Status Code: 200
                ├─ Response Time: 125ms
                ├─ Client IP: 192.168.1.1
                └─ Path: /users/123

                │
                ▼
        Message Queue (Redis Streams or Kafka)
                │
        ┌───────▼────────┐
        │ Analytics      │
        │ Service        │
        │ Consumer       │
        └───────┬────────┘
                │
        ┌───────▼─────────────────┐
        │ Processing:             │
        │ • Extract event data    │
        │ • Validate              │
        │ • Transform to DB model │
        │ • Batch aggregate stats │
        └───────┬─────────────────┘
                │
                ▼
        PostgreSQL api_usage Table:
        ┌────────────────────────────┐
        │ INSERT INTO api_usage      │
        │ (api_id, timestamp,        │
        │  status_code, response_time)
        │ VALUES (5, '2026-03-18...│
        │  200, 125)                 │
        └────────────┬───────────────┘
                     │
                     ▼
        ┌────────────────────────────────┐
        │ Analytics Computation:         │
        │ SELECT COUNT(*) FROM api_usage │
        │ WHERE api_id = 5 AND           │
        │ timestamp > NOW() - '1 hour'   │
        │                                │
        │ Result: 5432 requests/hour     │
        └────────────┬────────────────────┘
                     │
                     ▼
        Frontend Dashboard:
        ┌────────────────────────────┐
        │ Requests This Hour: 5432   │
        │ Success Rate: 99.2%        │
        │ Avg Response: 145ms        │
        │ Errors: 0.8%               │
        └────────────────────────────┘
```

---

## 12. Technology Selection Rationale Matrix

```
╔════════════════════╦═════════════╦═════════════════════════════════════╗
║ Layer              ║ Technology  ║ Why This Choice                     ║
╠════════════════════╬═════════════╬═════════════════════════════════════╣
║ Frontend           ║ Angular 21  ║ • Modern, reactive, real-time       ║
║                    ║             ║ • Component-based                   ║
║                    ║             ║ • Rich ecosystem                    ║
║                    ║             ║ • TypeScript support (type safety)  ║
╠════════════════════╬═════════════╬═════════════════════════════════════╣
║ Web Framework      ║ Spring Boot ║ • Simplifies Java development       ║
║                    ║             ║ • Auto-configuration                ║
║                    ║             ║ • Embedded server                   ║
║                    ║             ║ • Huge ecosystem                    ║
╠════════════════════╬═════════════╬═════════════════════════════════════╣
║ Language           ║ Java 17     ║ • Compiled (fast, type-safe)        ║
║                    ║             ║ • Enterprise standard               ║
║                    ║             ║ • GC (automatic memory mgmt)        ║
║                    ║             ║ • Mature ecosystem                  ║
╠════════════════════╬═════════════╬═════════════════════════════════════╣
║ Datastore          ║ PostgreSQL  ║ • ACID compliance (data integrity)  ║
║ (Persistent)       ║             ║ • Relationships (foreign keys)      ║
║                    ║             ║ • Reliable, proven                  ║
║                    ║             ║ • Free, open-source                 ║
╠════════════════════╬═════════════╬═════════════════════════════════════╣
║ Cache/Rate Limit   ║ Redis       ║ • In-memory (microsecond speed)     ║
║                    ║             ║ • TTL (auto-expiration)             ║
║                    ║             ║ • Atomic operations                 ║
║                    ║             ║ • Blazingly fast                    ║
╠════════════════════╬═════════════╬═════════════════════════════════════╣
║ Container Runtime  ║ Docker      ║ • Portable (ship anywhere)          ║
║                    ║             ║ • Consistent environments           ║
║                    ║             ║ • Easy deployment                   ║
║                    ║             ║ • Industry standard                 ║
╠════════════════════╬═════════════╬═════════════════════════════════════╣
║ Orchestration      ║ Docker      ║ • All services at once              ║
║                    ║ Compose     ║ • Simple, beginner-friendly         ║
║                    ║             ║ • Easy networking                   ║
║                    ║             ║ • Volume management                 ║
╚════════════════════╩═════════════╩═════════════════════════════════════╝
```

---

## 13. Development vs. Production Differences

```
DEVELOPMENT ENVIRONMENT:
════════════════════════════════════════════

Frontend:
  ├─ ng serve --poll (auto-reload on changes)
  ├─ Hot Module Replacement (HMR)
  └─ Source maps (easy debugging)

Backend:
  ├─ Spring Boot DevTools (auto-restart)
  ├─ H2 or PostgreSQL local
  ├─ Debug mode enabled
  └─ Logging: DEBUG level

Database:
  ├─ PostgreSQL in Docker (local)
  ├─ Redis in Docker (local)
  ├─ Small test database
  └─ Can reset anytime

Secrets:
  ├─ Hardcoded in application.properties (NOT SECURE!)
  ├─ Default passwords
  └─ No encryption

PRODUCTION ENVIRONMENT:
════════════════════════════════════════════

Frontend:
  ├─ ng build --prod (optimized minified)
  ├─ AOT compilation
  ├─ Source maps disabled
  └─ Served by nginx/CDN

Backend:
  ├─ JAR/Docker image
  ├─ JVM tuning (heap, GC)
  ├─ Debug mode DISABLED
  ├─ Logging: INFO/WARN level
  └─ Error handling (never expose stack traces)

Database:
  ├─ Managed PostgreSQL (AWS RDS, Azure, etc.)
  ├─ Replicated for redundancy
  ├─ Automated backups
  ├─ Large capacity
  ├─ Encryption at rest
  └─ Regular monitoring

Secrets:
  ├─ Environment variables
  ├─ Secrets management (HashiCorp Vault)
  ├─ Strong passwords
  ├─ JWT secret rotation
  └─ API key encryption

Security:
  ├─ SSL/TLS certificates
  ├─ WAF (Web Application Firewall)
  ├─ DDoS protection
  ├─ Rate limiting at gateway level
  ├─ Audit logging
  └─ Penetration testing

Infrastructure:
  ├─ Load balancer (high availability)
  ├─ Multiple instances (redundancy)
  ├─ Auto-scaling policies
  ├─ Health checks (auto-restart)
  ├─ Monitoring & alerting
  └─ Log aggregation (ELK, Splunk, etc.)
```

---

This visual reference guide complements the detailed documentation and provides architects/developers with clear system diagrams!
