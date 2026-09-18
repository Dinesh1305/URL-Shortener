# High-Performance URL Shortener Service

A production-ready, high-throughput URL shortener application built with **Java 21**, **Spring Boot 3.4**, **MySQL**, **Redis Cache**, and an interactive **HTML/CSS/JS** frontend.

---

## 🚀 Features

- ⚡ **Collision-Resistant Short Codes**: Base62 random short code generation with automatic retry handling.
- 🏷️ **Custom Aliases**: User-defined custom short URLs with unique database constraint enforcement.
- ⏳ **URL Expiration**: Configurable URL expiration in minutes; automatically returns HTTP 410 Gone when expired.
- ⚡ **High-Performance Redirection (Cache-Aside Pattern)**: Uses Redis as a fast in-memory cache to handle heavy read traffic. Fallbacks directly to MySQL if Redis is unavailable.
- 🛡️ **Atomic Rate Limiting**: Redis-backed atomic sliding window rate limiter per IP address (returns HTTP 429 Too Many Requests).
- 📊 **Click Analytics**: Tracks total redirection clicks atomically per URL without slowing down redirects.
- 🩺 **Health Check Monitoring**: Real-time system status endpoint (`/health`) displaying status (`UP`, `DEGRADED`, `DOWN`) for MySQL and Redis.
- 🎨 **Modern Responsive UI**: Premium glassmorphic web dashboard with one-click copy, live analytics, and URL deletion.

---

## 📐 System Design & Architecture

### Redirection Flow (Cache-Aside Pattern)

```
             ┌───────────────┐
             │    Browser    │
             └───────┬───────┘
                     │
                     ▼
             ┌───────────────┐
             │  Spring Boot  │
             │    REST API   │
             └───────┬───────┘
                     │
             ┌───────┴────────┐
             │                │
             ▼                ▼
        ┌─────────┐      ┌─────────┐
        │  Redis  │      │  MySQL  │
        │  Cache  │      │ Source  │
        └────┬────┘      │ of Truth│
             │           └─────────┘
             ▼
          Redirect
```

1. **Client Request**: `GET /{shortCode}`
2. **Redis Check**: Query Redis key `url:{shortCode}`.
   - **Cache HIT**: Return HTTP 307 Temporary Redirect immediately. Asynchronously / atomically increment MySQL `clickCount`.
   - **Cache MISS**: Query MySQL database.
     - If found & not expired: Store in Redis with TTL matching URL expiration (or default TTL) and return HTTP 307 Redirect.
     - If expired: Evict from Redis and return HTTP 410 Gone.
     - If not found: Return HTTP 404 Not Found.
3. **Redis Fallback**: If Redis service is unreachable or down, the application gracefully degrades by executing queries directly against MySQL without failing requests.

---

## 🗄️ Database Schema & Indexing

### Table: `urls`

| Field | Type | Constraint | Description |
|---|---|---|---|
| `id` | `BIGINT` | `PRIMARY KEY AUTO_INCREMENT` | Unique identifier |
| `original_url` | `TEXT` | `NOT NULL` | Destination URL |
| `short_code` | `VARCHAR(20)` | `UNIQUE NOT NULL` | Short URL identifier |
| `created_at` | `DATETIME` | `NOT NULL` | Creation timestamp |
| `expires_at` | `DATETIME` | `NULL` | Expiration timestamp |
| `click_count` | `BIGINT` | `NOT NULL DEFAULT 0` | Total click count |

### Database Indexes

- `uk_short_code` / `idx_short_code`: **UNIQUE INDEX** on `short_code`.
- **Why Indexing is Critical**: URL redirection is a read-heavy operation ($>95\%$ read requests vs $<5\%$ write requests). Indexing `short_code` converts table scans ($O(N)$) into $O(\log N)$ B-Tree index lookups in MySQL, enabling sub-millisecond retrieval on cache misses.

---

## 🔒 Concurrency & Race Condition Handling

When multiple users attempt to claim the same `customAlias` simultaneously:
1. Application-level check (`existsByShortCode`) performs an initial fast check.
2. The **MySQL UNIQUE constraint** (`uk_short_code`) enforces strict atomic isolation at the database engine level.
3. If a race condition occurs, MySQL throws a `DataIntegrityViolationException`, which is caught by Spring's `@RestControllerAdvice` to safely return an HTTP 409 Conflict response without corrupting state or exposing internal stack traces.

Click counts are incremented using an atomic SQL query:
```sql
UPDATE urls SET click_count = click_count + 1 WHERE short_code = ?
```
This avoids read-modify-write race conditions under concurrent traffic.

---

## 📡 API Endpoints

| Method | Endpoint | Description | Status Codes |
|---|---|---|---|
| `POST` | `/api/urls` | Shorten a long URL | `201 Created`, `400 Bad Request`, `409 Conflict`, `429 Too Many Requests` |
| `GET` | `/{shortCode}` | Redirect to original URL | `307 Temporary Redirect`, `404 Not Found`, `410 Gone` |
| `GET` | `/api/urls/{shortCode}/stats` | Retrieve URL click analytics | `200 OK`, `404 Not Found` |
| `DELETE` | `/api/urls/{shortCode}` | Delete shortened URL and clear cache | `204 No Content`, `404 Not Found` |
| `GET` | `/health` | System health status (MySQL & Redis) | `200 OK` |

---

## 🛠️ Setup & Running

### 1. Prerequisites
- **Java 21** or later
- **MySQL Server** (running on port `3306`)
- **Redis Server** (running on port `6379`)

### 2. Environment Configuration
Copy `.env.example` to `.env` or set environment variables:

```bash
DB_HOST=localhost
DB_PORT=3306
DB_NAME=url_shortener
DB_USERNAME=root
DB_PASSWORD=root

REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

SERVER_PORT=8080
BASE_URL=http://localhost:8080
RATE_LIMIT_PER_MINUTE=10
```

### 3. Build & Run Tests
```bash
./mvnw clean test
```

### 4. Run Application
```bash
./mvnw spring-boot:run
```

Access the Web UI at: [http://localhost:8080](http://localhost:8080)

---

## 📊 Scalability & Production Readiness

To scale this architecture to millions of requests:
1. **Load Balancing**: Deploy multiple stateless instances of the Spring Boot application behind an NGINX / AWS ALB load balancer.
2. **Redis Cluster**: Use Redis Cluster or AWS ElastiCache for horizontal cache partitioning and high availability.
3. **Database Read Replicas**: Configure MySQL Primary for writes and multiple Read Replicas for query distribution.
