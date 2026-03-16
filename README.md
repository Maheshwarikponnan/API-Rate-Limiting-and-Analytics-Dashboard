# API Rate Limiting and Analytics Platform

A full-stack project demonstrating backend engineering concepts including rate limiting, caching, API gateway patterns, authentication, and observability.

## Project Overview

This platform allows developers to register APIs, generate API keys, enforce rate limits, and monitor API usage analytics. It simulates a developer platform where users can manage their APIs and track usage in real-time.

## System Architecture

The system consists of the following components:

1. **Angular Frontend**: Dashboard for developers to manage APIs, keys, and view analytics.
2. **API Gateway (Spring Boot)**: Handles incoming requests, validates API keys, enforces rate limits, and forwards requests.
3. **Developer Service**: Manages developer accounts, API registrations, and API key generation.
4. **Analytics Service**: Collects and stores API usage metrics for visualization.
5. **Redis**: Used for rate limiting counters and caching.
6. **PostgreSQL**: Stores persistent data (developers, APIs, keys, usage records).

## Tech Stack

### Frontend
- Angular
- Angular Material
- RxJS
- HttpClient

### Backend
- Java Spring Boot
- Spring Web
- Spring Security
- Spring Data JPA
- Lombok

### Database & Caching
- PostgreSQL
- Redis

### Other Tools
- Docker
- Maven

## Folder Structure

```
backend/
  gateway-service/
  developer-service/
  analytics-service/

frontend/
  angular-dashboard/

docker/
```

## Database Schema

### Developers
- id (Primary Key)
- name
- email
- password
- created_at

### Apis
- id (Primary Key)
- developer_id (Foreign Key)
- name
- base_url
- rate_limit_per_minute
- created_at

### ApiKeys
- id (Primary Key)
- api_id (Foreign Key)
- key_value
- created_at

### ApiUsage
- id (Primary Key)
- api_id (Foreign Key)
- timestamp
- status_code
- response_time

## Rate Limiting Strategy

The platform uses a **sliding window** algorithm implemented with Redis for rate limiting:

- Each API key has a configurable requests per minute limit
- Counters are stored in Redis with keys like `rate_limit:{apiKey}:{minute}`
- When limit is exceeded, HTTP 429 (Too Many Requests) is returned
- Default limit: 100 requests per minute per API key

## Setup Instructions

### Prerequisites
- Java 17+
- Node.js 18+
- Docker & Docker Compose
- Maven

### Backend Setup

1. **PostgreSQL & Redis**:
   ```bash
   cd docker
   docker-compose up -d
   ```

2. **Build and Run Services**:
   ```bash
   # Developer Service
   cd backend/developer-service
   mvn spring-boot:run

   # Analytics Service
   cd backend/analytics-service
   mvn spring-boot:run

   # Gateway Service
   cd backend/gateway-service
   mvn spring-boot:run
   ```

### Frontend Setup

1. **Install Dependencies**:
   ```bash
   cd frontend/angular-dashboard
   npm install
   ```

2. **Run Development Server**:
   ```bash
   ng serve
   ```

3. **Access the Application**:
   - Frontend: http://localhost:4200
   - Gateway: http://localhost:8080
   - Developer Service: http://localhost:8081
   - Analytics Service: http://localhost:8082

## API Endpoints

### Developer Service (Port 8081)

- `POST /developers/register` - Register new developer
- `POST /developers/login` - Developer login
- `POST /apis` - Register new API
- `GET /apis` - List developer's APIs
- `POST /apikeys` - Generate API key

### Analytics Service (Port 8082)

- `GET /analytics/usage` - Get usage statistics
- `GET /analytics/errors` - Get error rates
- `GET /analytics/latency` - Get response latency metrics

### Gateway (Port 8080)

- `/*` - Proxy requests with rate limiting and API key validation

## Docker Setup

Use the provided `docker-compose.yml` to run all services:

```bash
cd docker
docker-compose up --build
```

## Future Improvements

- JWT authentication implementation
- Role-based access control
- Alert system for rate limit breaches
- API usage heatmaps
- API documentation generation
- Multi-region deployment
- Advanced analytics with machine learning
- API versioning support

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License.