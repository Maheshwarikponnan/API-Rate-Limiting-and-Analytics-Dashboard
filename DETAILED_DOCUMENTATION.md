# API Rate Limiting and Analytics Platform - Complete Documentation

## Executive Summary

This is a full-stack web application that simulates a **developer API management platform** (similar to services like Stripe, Twilio, or AWS). It allows developers to register their own APIs, manage access keys, enforce usage limits, and monitor how their APIs are being used in real-time. Think of it as a traffic cop for APIs—ensuring no single developer consumes too many resources.

---

## 1. What Problem Does This System Solve?

Imagine you run a cloud service and you want to offer APIs to other developers. You need to:
- **Track who is using your APIs** - Know which developers are accessing your services
- **Prevent abuse** - Stop one developer from overwhelming your system by making too many requests (rate limiting)
- **Monitor usage** - See how much each API is being used and how fast it's responding
- **Manage access** - Give developers secure keys to access your APIs
- **Show analytics** - Give developers a dashboard to see their own usage statistics

This system provides all of these capabilities.

---

## 2. System Architecture Overview

The system is built using **microservices architecture**, meaning different parts of the application are separated into independent services that communicate with each other:

```
┌─────────────────────────────────────────────────────────────────────┐
│                        User's Web Browser                           │
│                    (Angular Dashboard Frontend)                     │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ (HTTP Requests)
                               ↓
┌─────────────────────────────────────────────────────────────────────┐
│                      API GATEWAY (Spring Boot)                      │
│  - Validates API keys                                               │
│  - Enforces rate limits using Redis                                 │
│  - Routes requests to backend services                              │
│  - Logs usage analytics                                             │
└──────────────────────────────────────────────────────────────────────┘
            ↓                              ↓
     ┌─────────────────┐          ┌──────────────────┐
     │ DEVELOPER       │          │   ANALYTICS      │
     │ SERVICE         │          │   SERVICE        │
     │                 │          │                  │
     │ - Manages users │          │ - Collects usage │
     │ - Creates APIs  │          │ - Stores metrics │
     │ - Issues keys   │          │ - Computes stats │
     └────────┬────────┘          └────────┬─────────┘
              │                            │
              └──────────────┬─────────────┘
                             │
                             ↓
                    ┌────────────────────┐
                    │   PostgreSQL DB    │
                    │  (Persistent Data) │
                    └────────────────────┘
                    
                    ┌────────────────────┐
                    │   Redis Cache      │
                    │ (Rate Limit Data)  │
                    └────────────────────┘
```

---

## 3. Core Components Explained

### 3.1 Frontend: Angular Dashboard

**What is it?**
A web-based user interface where developers can manage their APIs and view analytics.

**Technology Stack:**
- **Angular 21**: A modern JavaScript framework that builds reactive web interfaces. It provides a structured way to build interactive UIs with real-time updates.
- **Angular Material**: Pre-built UI components (buttons, tables, cards) that provide a professional look and feel
- **RxJS**: A library for handling asynchronous operations (like API calls) in a clean, reactive way
- **TypeScript**: JavaScript with types, making code more reliable and easier to maintain

**What can users do here?**
- Register as a developer
- Create new APIs
- Generate API keys for each API
- View real-time usage analytics and charts
- Monitor rate limit usage
- Manage existing APIs and keys

**Key Features:**
- Dashboard view showing all registered APIs
- API key management (create, view, delete)
- Usage analytics with charts and graphs
- Real-time updates using websockets or polling

---

### 3.2 Backend: API Gateway Service

**What is it?**
Acts as a bouncer or security guard for your API infrastructure. Every request to your API must pass through this gateway first.

**Technology Stack:**
- **Spring Boot 3.2**: Java framework that simplifies building web services
- **Spring WebFlux**: Handles asynchronous, non-blocking requests (can handle thousands of concurrent connections)
- **Spring Data Redis**: Connects to Redis for lightning-fast data access

**What does it do?**

1. **API Key Validation**: When a request comes in, it checks if the API key is valid
   ```
   Request comes in → Check if key exists → If yes, proceed; if no, reject
   ```

2. **Rate Limiting (Key Feature!)**: Prevents any single developer from making too many requests
   - Uses a **sliding window algorithm** stored in Redis
   - Tracks how many requests each API key has made in the current minute
   - If limit exceeded, returns HTTP 429 "Too Many Requests"
   
   Example: If limit is 100 requests/minute:
   ```
   Request 1-100: ✓ Allowed
   Request 101: ✗ Blocked (rate limit exceeded)
   Wait for next minute...
   Request 101 (next minute): ✓ Allowed again
   ```

3. **Request Forwarding**: Routes the request to the appropriate backend service

4. **Usage Tracking**: Logs metrics like response time, HTTP status code, timestamp

**Why Redis?** Redis is an in-memory cache that's incredibly fast—essential for checking rate limits on every incoming request (milliseconds matter here).

---

### 3.3 Backend: Developer Service

**What is it?**
Manages everything related to developers, their APIs, and their access keys.

**Technology Stack:**
- **Spring Boot**: Web framework for building REST APIs
- **Spring Data JPA**: Object-Relational Mapping (ORM) that lets us work with databases using Java objects instead of SQL
- **Spring Security**: Handles authentication and authorization
- **PostgreSQL Driver**: Connects to the database

**What does it do?**

1. **Developer Management**
   - Stores developer account information (name, email, password)
   - Handles user registration and login
   - Authenticates developers using security tokens

2. **API Registration**
   - Developers can register their own APIs (create new entries)
   - Store metadata: API name, base URL, rate limit settings
   - Each API gets a unique ID

3. **API Key Generation**
   - Creates random, unique API keys (tokens) for each API
   - Keys are like passwords that grant access to your API
   - Can generate multiple keys per API for different applications

4. **CRUD Operations**
   - Create, Read, Update, Delete operations for all entities
   - Exposes REST endpoints like:
     - `POST /api/developers` - Register new developer
     - `POST /api/apis` - Create new API
     - `POST /api/keys` - Generate new API key
     - `GET /api/apis/{id}` - Get API details

---

### 3.4 Backend: Analytics Service

**What is it?**
Collects and processes usage data about how APIs are being called.

**Technology Stack:**
- **Spring Boot**: Same framework as Developer Service
- **Message Queue Integration**: Can receive events from the gateway
- **Time-series data processing**: Aggregates usage data over time

**What does it do?**

1. **Data Collection**
   - Receives events from the Gateway about each API call
   - Events include: API ID, timestamp, response time, status code, developer ID

2. **Aggregation**
   - Calculates statistics: total requests, average response time, error rates
   - Groups data by time periods (hourly, daily, monthly)

3. **Storage**
   - Persists analytics data to PostgreSQL for historical analysis
   - Makes this data available via REST endpoints

4. **Insights**
   - Calculates metrics developers care about:
     - "How many requests did I make this hour?"
     - "What's my average API response time?"
     - "How many errors did I get?"
     - "Am I approaching my rate limit?"

---

## 4. Database Layer

### PostgreSQL (Persistent Data Storage)

PostgreSQL is a reliable, feature-rich relational database. Think of it as a structured filing system where data relationships are clearly defined.

**Database Schema:**

#### Developers Table
```sql
CREATE TABLE developers (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  name            VARCHAR(255),      -- Developer's name
  email           VARCHAR(255),      -- Developer's email
  password        VARCHAR(255),      -- Encrypted password
  created_at      TIMESTAMP          -- When account was created
);
```

#### Apis Table
```sql
CREATE TABLE apis (
  id                    BIGINT PRIMARY KEY AUTO_INCREMENT,
  developer_id          BIGINT REFERENCES developers(id),  -- Which developer owns this API
  name                  VARCHAR(255),                      -- Name of the API
  base_url              VARCHAR(255),                      -- Where the API actually handles requests
  rate_limit_per_minute INTEGER,                           -- How many requests allowed per minute
  created_at            TIMESTAMP                          -- When API was registered
);
```

#### ApiKeys Table
```sql
CREATE TABLE api_keys (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  api_id     BIGINT REFERENCES apis(id),  -- Which API this key grants access to
  key_value  VARCHAR(255) UNIQUE,         -- The actual key string (like a password)
  created_at TIMESTAMP                    -- When key was issued
);
```

#### ApiUsage Table (Analytics)
```sql
CREATE TABLE api_usage (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  api_id        BIGINT REFERENCES apis(id),  -- Which API was called
  timestamp     TIMESTAMP,                    -- When it was called
  status_code   INTEGER,                      -- HTTP response (200=success, 500=error)
  response_time INTEGER                       -- How long it took (milliseconds)
);
```

### Redis (Fast Cache for Rate Limiting)

Redis is an in-memory data store that's millions of times faster than a database. It's used specifically for rate limiting.

**How it works:**
- **Key Format**: `rate_limit:{api_key}:{minute_timestamp}`
- **Value**: Number of requests made in that minute
- **TTL (Time To Live)**: Data expires automatically after 1 minute

Example:
```
Key: rate_limit:sk_123456_abc:202503181030
Value: 42 (42 requests made in that minute)

When request arrives:
- Check if key exists
- If not exists: value = 0
- If value < limit: increment by 1, allow request
- If value >= limit: reject request (HTTP 429)
```

---

## 5. How Data Flows Through The System

### Scenario: A developer calls an API using their API key

```
Step 1: Developer makes HTTP request
Client → GET /api/users?api_key=sk_123456 → Internet

Step 2: API Gateway receives request
Gateway: "Is this API key valid?"
  └─> Check in PostgreSQL: Yes, key exists ✓

Step 3: Gateway checks rate limit
Gateway: "How many requests has this key made this minute?"
  └─> Check in Redis: 42 requests
  └─> Limit is 100
  └─> 42 < 100 ✓ Proceed!

Step 4: Rate limit incremented
Gateway: "Update Redis counter"
  └─> Redis: 42 → 43

Step 5: Request forwarded to actual API
Gateway → Actual API Server (the backend service)
Actual API → Process request → Return response

Step 6: Gateway logs analytics
Gateway → Store in analytics queue:
  {
    api_id: 5,
    timestamp: 2026-03-18 10:30:45,
    status_code: 200,
    response_time: 125 ms
  }

Step 7: Analytics Service processes data
Analytics Service reads the queue event
  └─> Stores in PostgreSQL api_usage table
  └─> Updates aggregate statistics

Step 8: Response returned to client
Gateway → Client: {status: 200, data: {...}}

Step 9: Developer sees updated analytics
Frontend polls Analytics Service
  └─> Shows "42 requests this minute"
  └─> Shows analytics in dashboard
```

---

## 6. Rate Limiting Explained (In Detail)

This is the core feature that prevents abuse.

### The Sliding Window Algorithm

Imagine a 1-minute window moving forward through time:

```
Limit: 100 requests/minute

Time:  |----10:30-----|----10:31-----|
Count:    [95]→96→97→98→99→100│101(BLOCKED)
          
As time moves forward, old requests "fall out" of the window:

Time:  |----10:31-----|----10:32-----|
Count:    [1]→2→3...
```

**Implementation in Redis:**
1. When request arrives, calculate current minute: `minute = floor(timestamp / 60)`
2. Create key: `rate_limit:{api_key}:{minute}`
3. Get current value: `count = redis.get(key) || 0`
4. If `count >= limit`: Return 429 Too Many Requests
5. If `count < limit`: 
   - `redis.increment(key)`
   - `redis.expire(key, 60)` (auto-delete after 60 seconds)
   - Allow request

**Benefits:**
- ✓ Fair: Limits per time window, not globally
- ✓ Fast: Redis lookups are microseconds
- ✓ Memory efficient: Old data auto-expires
- ✓ Configurable: Different limits per API

---

## 7. Technology Stack Summary

### Frontend Technologies
| Technology | Purpose | Why Used |
|-----------|---------|----------|
| **Angular 21** | Web framework | Builds reactive UIs with data binding |
| **TypeScript** | Language | Adds types to JavaScript (catches bugs early) |
| **RxJS** | Async handling | Makes handling multiple API calls clean and elegant |
| **Angular Material** | UI Components | Pre-built professional UI elements (buttons, tables, etc.) |
| **HttpClient** | HTTP Requests | Library to talk to backend APIs |

### Backend Technologies
| Technology | Version | Purpose | Why Used |
|-----------|---------|---------|----------|
| **Spring Boot** | 3.2 | Web framework | Simplifies building robust Java web services |
| **Spring Web** | - | HTTP support | Handles HTTP requests and responses |
| **Spring Security** | - | Authentication | Secures endpoints with auth tokens |
| **Spring Data JPA** | - | ORM | Write less SQL, use Java objects |
| **Spring Data Redis** | - | Cache access | Fast access to Redis for rate limiting |
| **Spring WebFlux** | - | Async I/O | Non-blocking requests (handles scale) |
| **Project Lombok** | - | Code generation | Reduces boilerplate (auto getters/setters) |
| **PostgreSQL** | 15 | Database | Reliable relational database |
| **Redis** | 7 | Cache/counting | In-memory store for rate limits |
| **Docker** | - | Containerization | Package services in portable containers |
| **Maven** | - | Build tool | Manages dependencies and building |
| **Java** | 17 | Language | Compiled, typed, enterprise language |

---

## 8. Key Features Explained

### 8.1 Multi-Tenant Support
- Each developer is isolated with their own APIs
- Developers only see their own data
- Rate limits are per API key, not global

### 8.2 Granular Rate Limiting
- Each API can have its own rate limit
- Different API keys for the same API can be used for different purposes
- Rate limits are configurable per API

### 8.3 Real-time Analytics
- See request counts, response times, error rates
- Dashboard updates to show current usage
- Historical data for trend analysis

### 8.4 Security
- API keys are required for all requests
- Developers authenticate with username/password
- Rate limiting prevents DOS attacks
- Encrypted password storage using Spring Security

### 8.5 Scalability
- Stateless services (can run multiple instances)
- Redis handles concurrent rate limit checks
- PostgreSQL can handle thousands of requests
- WebFlux allows async processing of requests

---

## 9. System Deployment Architecture

```
Docker Container 1: PostgreSQL
├─ Port: 5432
├─ Data Volume: /postgres_data
└─ Purpose: Persistent data storage

Docker Container 2: Redis
├─ Port: 6379
├─ Data Volume: /redis_data
└─ Purpose: Fast cache for rate limiting

Docker Container 3: Developer Service
├─ Port: 8081
├─ Connects to: PostgreSQL + Redis
└─ Purpose: API management backend

Docker Container 4: Gateway Service
├─ Port: 8080
├─ Connects to: PostgreSQL + Redis + Developer Service
└─ Purpose: Request validation and rate limiting

Docker Container 5: Analytics Service
├─ Port: 8082
├─ Connects to: PostgreSQL + Gateway
└─ Purpose: Usage tracking and statistics

Local: Angular Frontend
├─ Port: 4200 (development)
├─ Connects to: Gateway Service, Developer Service, Analytics Service
└─ Purpose: User interface
```

All containers are defined in Docker Compose, making it easy to start the entire system with: `docker-compose up`

---

## 10. Request/Response Examples

### Example 1: Developer Registration

**Request:**
```http
POST /api/developers
Content-Type: application/json

{
  "name": "John Developer",
  "email": "john@example.com",
  "password": "secure123"
}
```

**Response:**
```json
{
  "id": 1,
  "name": "John Developer",
  "email": "john@example.com",
  "createdAt": "2026-03-18T10:30:00Z"
}
```

### Example 2: Creating an API

**Request:**
```http
POST /api/apis
Authorization: Bearer {developer_token}
Content-Type: application/json

{
  "name": "User Service API",
  "baseUrl": "https://api.example.com/users",
  "rateLimitPerMinute": 1000
}
```

**Response:**
```json
{
  "id": 1,
  "developerId": 1,
  "name": "User Service API",
  "baseUrl": "https://api.example.com/users",
  "rateLimitPerMinute": 1000,
  "createdAt": "2026-03-18T10:30:00Z"
}
```

### Example 3: Generating API Key

**Request:**
```http
POST /api/keys
Authorization: Bearer {developer_token}

{
  "apiId": 1
}
```

**Response:**
```json
{
  "id": 1,
  "apiId": 1,
  "keyValue": "sk_live_a1b2c3d4e5f6g7h8i9j0",
  "createdAt": "2026-03-18T10:30:00Z"
}
```

### Example 4: Making API Request (Rate Limited!)

**Request:**
```http
GET /user/123?api_key=sk_live_a1b2c3d4e5f6g7h8i9j0
```

**Gateway Processing:**
```
1. Validate API key: ✓ Valid
2. Check rate limit: 45/100 requests used this minute ✓
3. Increment counter: 45 → 46
4. Forward to backend
5. Receive response
6. Log metrics
7. Return to client
```

**Response (200 OK):**
```json
{
  "id": 123,
  "name": "Alice",
  "email": "alice@example.com"
}
```

**Response (429 Too Many Requests - when limit exceeded):**
```json
{
  "error": "Rate limit exceeded",
  "limit": 100,
  "remaining": 0,
  "resetTime": "2026-03-18T10:31:00Z"
}
```

### Example 5: Viewing Analytics

**Request:**
```http
GET /api/analytics/apis/1/usage
Authorization: Bearer {developer_token}
```

**Response:**
```json
{
  "apiId": 1,
  "totalRequests": 2543,
  "successfulRequests": 2481,
  "failedRequests": 62,
  "averageResponseTime": 156,    // milliseconds
  "errorRate": 2.4,               // percent
  "requestsThisMinute": 46,
  "rateLimitExceeded": 0,
  "topErrors": [
    {"code": 500, "count": 35},
    {"code": 404, "count": 27}
  ]
}
```

---

## 11. Error Handling

The system handles various error scenarios:

| Error | Status Code | Meaning | Example |
|-------|------------|---------|---------|
| Invalid API Key | 401 | Key doesn't exist or is disabled | `api_key` parameter missing |
| Rate Limited | 429 | Too many requests | 101st request in a minute |
| Server Error | 500 | Something went wrong | Database connection failed |
| Not Found | 404 | Resource doesn't exist | Requesting API ID that doesn't exist |
| Unauthorized | 403 | Access denied | Viewing another dev's analytics |

---

## 12. Why This Architecture?

### Microservices Benefits
- **Scalability**: Scale individual services based on load
- **Maintainability**: Each service has single responsibility
- **Technology Freedom**: Services can use different tech stacks
- **Fault Isolation**: One service failing doesn't crash the whole system

### Why Redis for Rate Limiting?
- **Speed**: In-memory = microsecond response times
- **TTL**: Automatic data expiration
- **Atomic Operations**: Increment is single operation (no race conditions)
- **Distributed**: Can scale across multiple rate limit nodes

### Why PostgreSQL?
- **ACID Compliance**: Data integrity guaranteed
- **Relationships**: Foreign keys enforce data consistency
- **Reliability**: Rock-solid database trusted by enterprises
- **Rich Features**: JSON support, full-text search, etc.

### Why Angular + Spring Boot?
- **Angular**: Industry-standard for enterprise web apps
- **Spring Boot**: Reduces boilerplate, great ecosystem
- **Together**: Java backend + TypeScript frontend = typed end-to-end
- **Ecosystem**: Enormous community, tons of resources

---

## 13. Workflow End-to-End

### Day 1: Developer Sets Up

1. Developer visits dashboard
2. Registers account (name, email, password)
3. System creates developer record in PostgreSQL
4. Developer logs in (credentials validated by Spring Security)
5. Dashboard shows empty APIs list

### Day 2: Developer Creates API

1. Developer clicks "Create New API"
2. Fills form: Name, URL, Rate Limit (e.g., 1000 requests/min)
3. System saves to PostgreSQL `apis` table
4. Dashboard shows new API

### Day 3: Developer Generates Key

1. Developer clicks "Generate API Key" on their API
2. System generates random string: `sk_live_xyz123abc`
3. Saves to PostgreSQL `api_keys` table
4. Shows key to developer (only once!)
5. Developer copies key for use in their application

### Day 4: Client Uses the API

1. Client software uses the key: `GET /api/endpoint?api_key=sk_live_xyz123abc`
2. Request hits API Gateway
3. Gateway validates key (checks PostgreSQL)
4. Gateway checks rate limit (checks Redis)
5. If OK: forwards to actual API, receives response
6. Gateway logs event (timestamp, response time, status) to message queue
7. Returns response to client

### Day 5: Analytics Service Processes

1. Analytics Service consumes logged events
2. Stores detailed records in PostgreSQL `api_usage` table
3. Computes aggregates (total requests, average time, error rate)
4. Updates dashboard cache

### Day 6: Developer Views Dashboard

1. Developer refreshes analytics view
2. Frontend requests `/api/analytics/apis/1/usage`
3. Analytics Service queries PostgreSQL
4. Returns summary: 5,432 requests, 98% success rate, avg 145ms
5. Dashboard shows charts and metrics
6. Developer sees: "You're using 45% of your rate limit"

---

## 14. Security Considerations

### What's Protected?
✓ API keys validated on every request
✓ Developer passwords encrypted
✓ Rate limiting prevents abuse
✓ Access controls (developers only see own data)
✓ HTTP headers validated

### What's Not Covered (In Production):
- SSL/TLS encryption (use in production!)
- API key rotation
- Audit logging of who viewed what
- Two-factor authentication
- DDoS protection at network level
- Data encryption at rest

---

## 15. Performance Characteristics

| Operation | Typical Time | Limiting Factor |
|-----------|-------------|-----------------|
| API request validation | 1-2 ms | Redis lookup |
| Rate limit check | 0.5-1 ms | Redis operation |
| Database query | 5-10 ms | PostgreSQL disk I/O |
| Full request through gateway | 10-50 ms | Backend service |
| Analytics page load | 100-200 ms | Network + parsing |

---

## 16. Scaling Considerations

To handle 1 million requests/day:

1. **Multiple Gateway Instances**: Each checks rate limits in Redis (shared)
2. **Database Replication**: Read replicas for analytics queries
3. **Redis Clustering**: Distribute rate limit storage
4. **Caching Layer**: Cache frequently accessed data
5. **Load Balancing**: Distribute traffic across instances

Example scaled setup:
```
Load Balancer
├─ Gateway Instance 1 ─┐
├─ Gateway Instance 2 ─┤─→ PostgreSQL (Primary)
├─ Gateway Instance 3 ─┤   └─ PostgreSQL (Replica for reads)
└─ Gateway Instance 4 ─┘
                └─→ Redis Cluster (3 nodes)
                └─→ Analytics Service Instances
```

---

## 17. Testing Strategy

### Unit Tests
- Test rate limiting logic
- Test API key validation
- Test analytics calculations

### Integration Tests
- Gateway communicates with Redis
- Developer Service connects to PostgreSQL
- Analytics Service processes events

### End-to-End Tests
- Full request through gateway
- Rate limit enforcement actual
- Dashboard displays real data

---

## 18. Common Use Cases

### Use Case 1: SaaS Platform
Provider offers API to customers, needs to:
- Limit customer usage
- Track who uses what
- Charge based on usage
**This system perfectly handles it!**

### Use Case 2: Internal API Management
Company has multiple teams sharing infrastructure:
- Prevent team A from overloading team B
- Track team B's usage for billing
- Alert when approaching limits
**This system handles it!**

### Use Case 3: Public API with Freemium Model
Like GitHub API, Twitter API:
- Free tier: 100 requests/day
- Pro tier: 10,000 requests/day
- Track usage for billing
**This system handles it!**

---

## 19. Future Enhancements

Potential additions:
1. **Tiered Pricing**: Different rate limits for paid tiers
2. **Webhook Notifications**: Alert developer when limit approaching
3. **Custom Rate Patterns**: Hourly, daily, or burst limits
4. **API Versioning**: Support multiple API versions
5. **Request Logging**: Full request/response audit trail
6. **GraphQL Support**: Alternative query language
7. **OAuth2 Integration**: Sign in with GitHub/Google
8. **Metrics Export**: Send to Prometheus/Grafana
9. **Distributed Tracing**: Track request across services
10. **API Documentation**: Auto-generate OpenAPI specs

---

## 20. Project Structure Summary

```
API Rate Limiting/
│
├── README.md (Basic overview)
├── DETAILED_DOCUMENTATION.md (This file!)
│
├── backend/
│   ├── gateway-service/
│   │   ├── pom.xml (Maven dependencies)
│   │   └── src/main/java/com/example/gatewayservice/
│   │       ├── config/ (Redis, WebClient configs)
│   │       ├── controller/ (HTTP endpoints)
│   │       └── service/ (Rate limiting logic)
│   │
│   ├── developer-service/
│   │   ├── pom.xml (Maven dependencies)
│   │   └── src/main/java/com/example/developerservice/
│   │       ├── controller/ (REST endpoints)
│   │       ├── service/ (Business logic)
│   │       ├── repository/ (Database access)
│   │       ├── entity/ (JPA entities)
│   │       ├── dto/ (Data transfer objects)
│   │       ├── security/ (Authentication)
│   │       └── exception/ (Custom exceptions)
│   │
│   └── analytics-service/
│       ├── pom.xml
│       └── src/main/java/com/example/analyticsservice/
│
├── frontend/
│   └── angular-dashboard/
│       ├── package.json (npm dependencies)
│       ├── angular.json (Angular config)
│       ├── src/
│       │   ├── app/
│       │   │   ├── api-keys/ (API key management)
│       │   │   ├── api-management/ (Create/edit APIs)
│       │   │   ├── dashboard/ (Main view)
│       │   │   └── usage-analytics/ (Charts/stats)
│       │   └── main.ts (App entry point)
│       └── tsconfig.json
│
└── docker/
    └── docker-compose.yml (Container orchestration)
```

---

## 21. Glossary of Terms

| Term | Meaning |
|------|---------|
| **API** | Application Programming Interface - a way for software to request data/services |
| **Rate Limit** | Maximum number of requests allowed in a time period |
| **API Key** | A secret token that proves you're authorized to use an API |
| **Microservices** | Breaking an app into small, independent services |
| **Gateway** | Entry point that validates/routes requests |
| **Redis** | Super-fast in-memory database |
| **PostgreSQL** | Traditional relational database (reliable, proven) |
| **Spring Boot** | Framework that simplifies Java web app development |
| **Angular** | JavaScript framework for building web UIs |
| **Docker** | Technology to package apps in containers (like shipping containers) |
| **REST** | Standard architectural style for web APIs (GET, POST, PUT, DELETE) |
| **JWT** | Secure token format for authentication |
| **ORM** | Object-Relational Mapping - use objects instead of raw SQL |
| **Async** | Operations that don't block (can do other things while waiting) |
| **Scalability** | How well system handles growing load |

---

## 22. Getting Started (Quick Reference)

### Prerequisites
- Docker & Docker Compose
- Java 17+ (for development)
- Node.js 18+ (for frontend development)

### Quick Start
```bash
# Navigate to docker directory
cd docker/

# Start all services
docker-compose up -d

# Services will be available at:
# Frontend: http://localhost:4200
# Developer Service: http://localhost:8081
# Gateway: http://localhost:8080
# Analytics: http://localhost:8082
```

### Development Setup
```bash
# Backend (Developer Service)
cd backend/developer-service/
mvn spring-boot:run

# Backend (Gateway)
cd backend/gateway-service/
mvn spring-boot:run

# Backend (Analytics)
cd backend/analytics-service/
mvn spring-boot:run

# Frontend
cd frontend/angular-dashboard/
npm install
npm start
```

---

## Conclusion

This API Rate Limiting and Analytics Platform demonstrates enterprise-grade software engineering:

- ✓ **Clean Architecture**: Separated concerns into microservices
- ✓ **Performance**: Using Redis for rate limiting
- ✓ **Scalability**: Stateless services, distributed caching
- ✓ **Security**: API key validation, rate limiting prevents abuse
- ✓ **Observability**: Analytics show what's happening
- ✓ **User Experience**: Dashboard for easy management
- ✓ **Production-Ready**: Docker containers, proper error handling

The system is built to scale from thousands to millions of requests, handle multiple developers with isolation, and provide insight into API usage patterns.
