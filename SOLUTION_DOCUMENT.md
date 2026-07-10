# RedBus Clone — Solution Document

**Project:** RedBus Clone REST API  
**Technology Stack:** Java 17 · Spring Boot 3.2.0 · MySQL 8 · Redis (Memurai) · JWT · Maven  
**Date:** June 2026  

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Architecture](#2-architecture)
3. [Project Structure](#3-project-structure)
4. [Technology Stack & Dependencies](#4-technology-stack--dependencies)
5. [Database Design](#5-database-design)
6. [Security — JWT Authentication](#6-security--jwt-authentication)
7. [API Endpoints Reference](#7-api-endpoints-reference)
8. [Business Logic — Core Flows](#8-business-logic--core-flows)
9. [Redis Cache Implementation](#9-redis-cache-implementation)
10. [Validation Layer](#10-validation-layer)
11. [Exception Handling](#11-exception-handling)
12. [Scheduler — Seat Expiry](#12-scheduler--seat-expiry)
13. [Configuration](#13-configuration)
14. [How to Run](#14-how-to-run)
15. [Postman Testing Guide](#15-postman-testing-guide)

---

## 1. Project Overview

RedBus Clone is a production-ready REST API that replicates the core functionality of the RedBus bus ticket booking platform. It allows:

- **Users** to register, sign in, search buses, block seats, book tickets, view history, and cancel tickets.
- **Admins** to manage the bus fleet (add, update, delete buses).
- **System** to automatically release unconfirmed seat blocks after a 10-minute window.
- **Redis** to cache frequently read data and reduce database load.

---

## 2. Architecture

```
┌──────────────┐     HTTP/REST      ┌─────────────────────────────────────────────┐
│   Postman /  │ ─────────────────► │              Spring Boot App                │
│  Frontend    │                    │                                             │
└──────────────┘                    │  ┌───────────┐   ┌──────────┐              │
                                    │  │Controllers│──►│ Services │              │
                                    │  └───────────┘   └────┬─────┘              │
                                    │         ▲             │                    │
                                    │  ┌──────┴──────┐      │ Cache Hit?         │
                                    │  │ JWT Filter  │      ▼                   │
                                    │  │(Auth/Authz) │  ┌───────┐  ┌─────────┐  │
                                    │  └─────────────┘  │ Redis │  │  MySQL  │  │
                                    │                   │ Cache │  │   DB    │  │
                                    │                   └───────┘  └─────────┘  │
                                    └─────────────────────────────────────────────┘
```

### Layered Design

| Layer | Package | Responsibility |
|-------|---------|---------------|
| Controller | `controller/` | HTTP request/response handling |
| Service | `service/serviceImpl/` | Business logic, caching |
| Repository | `repository/` | Database access via Spring Data JPA |
| Entity | `entity/` | JPA-mapped database tables |
| DTO | `dto/request/` `dto/response/` | Request/response payload objects |
| Config | `config/` | Security, JWT, Redis configuration |
| Exception | `exception/` | Custom exceptions + global handler |
| Validation | `validation/` | Centralized input validation rules |
| Scheduler | `scheduler/` | Automated seat expiry job |

---

## 3. Project Structure

```
redbus/
├── pom.xml
├── SOLUTION_DOCUMENT.md
└── src/
    ├── main/
    │   ├── java/com/redbus/
    │   │   ├── RedbusApplication.java          ← App entry point (@EnableCaching, @EnableScheduling)
    │   │   ├── config/
    │   │   │   ├── JwtAuthFilter.java           ← Intercepts every request, validates JWT
    │   │   │   ├── JwtService.java              ← Token generation, validation, blacklist
    │   │   │   ├── SecurityConfig.java          ← Spring Security + public/protected routes
    │   │   │   └── RedisConfig.java             ← Redis CacheManager with per-cache TTL
    │   │   ├── controller/
    │   │   │   ├── UserController.java          ← /signUp, /signIn, /signOut
    │   │   │   ├── AdminController.java         ← /admin, /addBus, /updateBus, /deleteBus
    │   │   │   ├── BusController.java           ← /BusSearch
    │   │   │   └── BookingController.java       ← /seatBlocking, /passengerDetails, /history, /cancelTicket
    │   │   ├── dto/
    │   │   │   ├── request/                     ← Inbound payload classes
    │   │   │   └── response/                    ← Outbound payload classes
    │   │   ├── entity/
    │   │   │   ├── User.java
    │   │   │   ├── Admin.java
    │   │   │   ├── Bus.java
    │   │   │   ├── Passenger.java
    │   │   │   └── SeatBlocking.java
    │   │   ├── exception/                       ← Custom exceptions + GlobalExceptionHandler
    │   │   ├── repository/                      ← JPA repositories
    │   │   ├── scheduler/
    │   │   │   └── SeatExpiryScheduler.java     ← Runs every 60s to expire unconfirmed blocks
    │   │   ├── service/
    │   │   │   ├── serviceInterface/            ← Service contracts (interfaces)
    │   │   │   └── serviceImpl/
    │   │   │       ├── UserService.java
    │   │   │       ├── AdminService.java
    │   │   │       ├── BusService.java          ← @Cacheable, @CacheEvict
    │   │   │       └── BookingService.java      ← @Cacheable, @CacheEvict, @Caching
    │   │   └── validation/
    │   │       └── CentralizedValidator.java    ← All field validation rules in one place
    │   └── resources/
    │       └── application.properties
    └── test/
        └── java/com/redbus/
            ├── config/
            │   └── RedisConfigTest.java
            └── validation/
                └── CentralizedValidatorTest.java
```

---

## 4. Technology Stack & Dependencies

| Dependency | Version | Purpose |
|-----------|---------|---------|
| Spring Boot Parent | 3.2.0 | Framework baseline |
| spring-boot-starter-web | — | REST API |
| spring-boot-starter-security | — | Authentication & Authorization |
| spring-boot-starter-data-jpa | — | ORM / Database access |
| spring-boot-starter-validation | — | Bean validation |
| spring-boot-starter-data-redis | — | Redis cache integration |
| mysql-connector-j | runtime | MySQL JDBC driver |
| jjwt-api / jjwt-impl / jjwt-jackson | 0.12.3 | JWT token creation & parsing |
| lombok | optional | Boilerplate reduction |
| jackson-datatype-jsr310 | — | Java 8 time (LocalDate etc.) in JSON/Redis |
| spring-boot-starter-test | test | Unit & integration testing |
| spring-security-test | test | Security-context test support |

**Runtime:**
- Java 17
- MySQL 8
- Memurai (Redis-compatible, Windows-native) on port 6379

---

## 5. Database Design

### Table: `users`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | Auto-generated |
| name | VARCHAR(100) | Not null |
| mobile_number | VARCHAR(10) | Unique |
| date_of_birth | DATE | Not null |
| email | VARCHAR(150) | Unique |
| address | TEXT | Not null |
| password | VARCHAR(255) | BCrypt-hashed |

### Table: `admin`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | Auto-generated |
| email | VARCHAR(150) | Unique |
| password | VARCHAR(255) | BCrypt-hashed |

### Table: `bus`
| Column | Type | Notes |
|--------|------|-------|
| bus_id | BIGINT PK | Auto-generated |
| travels_name | VARCHAR(100) | Not null |
| total_seats | INT | Not null |
| available_seats_count | INT | Decremented on block, incremented on cancel/expiry |
| from_location | VARCHAR(100) | Not null |
| to_location | VARCHAR(100) | Not null |
| date_of_journey | DATE | Not null |
| price | DECIMAL(10,2) | Not null |

### Table: `seat_blocking`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | Auto-generated |
| bus_id | BIGINT | FK reference |
| seat_number | INT | Unique constraint with bus_id |
| user_id | BIGINT | FK reference |
| blocked_at | DATETIME | When seat was blocked |
| expires_at | DATETIME | blocked_at + 10 minutes |
| is_confirmed | BOOLEAN | false = pending, true = booked |

### Table: `passenger`
| Column | Type | Notes |
|--------|------|-------|
| passenger_id | BIGINT PK | Auto-generated |
| blocking_id | BIGINT | Unique FK to seat_blocking |
| bus_id | BIGINT | Denormalized for query convenience |
| seat_number | INT | Denormalized |
| name | VARCHAR(100) | Validated |
| age | INT | Validated ≥ 1 |
| gender | ENUM(MALE,FEMALE,OTHER) | |
| mobile_number | VARCHAR(10) | Validated |
| email_address | VARCHAR(150) | Validated |
| price | DECIMAL(10,2) | Copied from bus at time of booking |
| booked_at | DATETIME | Timestamp of booking |

---

## 6. Security — JWT Authentication

### Flow

```
User/Admin → POST /signIn → JWT Token returned
                               │
                               ▼
Every protected API call → Authorization: Bearer <token>
                               │
                        JwtAuthFilter (OncePerRequestFilter)
                               │
                    ┌──────────┴──────────┐
                    │  isTokenValid()?     │
                    │  - Not blacklisted   │
                    │  - Not expired       │
                    └──────────┬──────────┘
                               │ YES
                    Extract email, userId, role
                    Set SecurityContext → proceed
```

### Public Endpoints (No token required)

| Endpoint | Method |
|----------|--------|
| `/signUp` | POST |
| `/signIn` | POST |
| `/admin` | POST |
| `/BusSearch` | GET |

### ADMIN-only Endpoints

| Endpoint | Method |
|----------|--------|
| `/addBus` | POST |
| `/updateBus/{busId}` | PUT |
| `/deleteBus/{busId}` | DELETE |

### User Endpoints (JWT required)

| Endpoint | Method |
|----------|--------|
| `/seatBlocking` | POST |
| `/passengerDetails` | POST |
| `/history` | GET |
| `/cancelTicket` | DELETE |
| `/signOut` | POST |

### JWT Token Structure

```
Header:  { "alg": "HS256" }
Payload: { "sub": "email", "userId": 1, "role": "USER", "iat": ..., "exp": ... }
```

- Algorithm: **HS256**
- Expiry: **1 hour** (3,600,000 ms)
- Sign-out: token added to **in-memory blacklist** (`ConcurrentHashSet`)

---

## 7. API Endpoints Reference

### User APIs

#### POST `/signUp`
```json
// Request
{
  "name": "John Doe",
  "mobileNumber": "9876543210",
  "dateOfBirth": "01011995",
  "email": "john@example.com",
  "address": "123 Main St, Chennai",
  "password": "Pass@1234"
}
// Response 201
{ "message": "Successfully registered Please Sign in" }
```

#### POST `/signIn`
```json
// Request
{ "email": "john@example.com", "password": "Pass@1234" }
// Response 200
{ "message": "WELCOME TO THE REDBUS", "token": "eyJ...", "expiresIn": 3600 }
```

#### POST `/signOut`
```
Authorization: Bearer <token>
// Response 200
{ "message": "Signed out successfully" }
```

---

### Admin APIs

#### POST `/admin` (Admin Sign-In)
```json
// Request
{ "email": "admin@redbus.com", "password": "Admin@123" }
// Response 200
{ "message": "WELCOME ADMIN", "token": "eyJ...", "expiresIn": 3600 }
```

#### POST `/addBus`
```
Authorization: Bearer <admin-token>
```
```json
// Request
{
  "travelsName": "VRL Travels",
  "fromLocation": "Chennai",
  "toLocation": "Bangalore",
  "dateOfJourney": "2026-07-15",
  "price": 550.00,
  "totalSeats": 40,
  "availableSeatsCount": 40
}
// Response 200
{ "message": "Bus added successfully", "busId": "1" }
```

#### PUT `/updateBus/{busId}`
```
Authorization: Bearer <admin-token>
```
```json
// Request (only fields to update)
{ "price": 599.00 }
// Response 200
{ "message": "Bus updated successfully" }
```

#### DELETE `/deleteBus/{busId}`
```
Authorization: Bearer <admin-token>
// Response 200
{ "message": "Bus deleted successfully" }
```

---

### Bus Search API

#### GET `/BusSearch`
```
GET /BusSearch?from=Chennai&to=Bangalore&dateOfJourney=2026-07-15
```
```json
// Response 200
[
  {
    "busId": 1,
    "travelsName": "VRL Travels",
    "availableSeats": [3, 7, 12, 18, 25],
    "totalSeats": 40,
    "availableSeatsCount": 35,
    "fromLocation": "Chennai",
    "toLocation": "Bangalore",
    "dateOfJourney": "2026-07-15",
    "price": 550.00
  }
]
```
> **Redis cached** for 30 minutes per `from:to:date` key.

---

### Booking APIs

#### POST `/seatBlocking`
```
Authorization: Bearer <user-token>
```
```json
// Request
{ "busId": 1, "seatNumber": 12 }
// Response 200
{
  "message": "Seat 12 blocked successfully. You have 10 minutes to complete passenger details.",
  "blockingId": 5,
  "expiresAt": "2026-07-10T14:35:00"
}
```

#### POST `/passengerDetails`
```
Authorization: Bearer <user-token>
```
```json
// Request
{
  "blockingId": 5,
  "name": "John Doe",
  "age": 30,
  "gender": "MALE",
  "mobileNumber": "9876543210",
  "emailAddress": "john@example.com"
}
// Response 201
{
  "message": "Booking confirmed!",
  "passengerId": 10,
  "seatNumber": 12,
  "busId": 1,
  "price": 550.00
}
```

#### GET `/history`
```
Authorization: Bearer <user-token>
// Response 200
[
  {
    "passengerId": 10,
    "name": "John Doe",
    "age": 30,
    "gender": "MALE",
    "mobileNumber": "9876543210",
    "emailAddress": "john@example.com",
    "price": 550.00,
    "busId": 1,
    "seatNumber": 12
  }
]
```
> **Redis cached** for 15 minutes per `userId` key.

#### DELETE `/cancelTicket`
```
Authorization: Bearer <user-token>
```
```json
// Request
{ "passengerId": 10 }
// Response 200
{ "message": "Ticket cancelled successfully. Your seat has been released." }
```

---

## 8. Business Logic — Core Flows

### User Registration
1. Validate all fields via `CentralizedValidator`
2. Check for duplicate email / mobile in DB
3. BCrypt-encode password
4. Save to `users` table

### Bus Search + Seat Generation
1. Query `bus` table by `from`, `to`, `dateOfJourney`
2. For each bus, generate a **deterministic pseudo-random** list of seat numbers using `busId` as the Random seed
3. Fetch already-booked seat numbers from `passenger` table
4. Subtract booked seats from generated list → available seats
5. Return `BusSearchResponse` list

> Seat numbers are consistent per bus (same busId always produces same ordering) but appear "random" to users.

### Seat Blocking → Booking (Two-Step Flow)

```
Step 1: POST /seatBlocking
  - Validate seat is in available list
  - Save SeatBlocking record (expires in 10 min, isConfirmed=false)
  - Decrement bus.availableSeatsCount by 1
  - Return blockingId to client

Step 2: POST /passengerDetails  (within 10 minutes)
  - Find SeatBlocking by blockingId
  - Verify it belongs to the requesting user
  - Check it has not expired
  - Validate all passenger fields
  - Save Passenger record
  - Mark SeatBlocking.isConfirmed = true
```

### Ticket Cancellation
1. Find `Passenger` by passengerId
2. Find linked `SeatBlocking` — verify it belongs to requesting user
3. Delete `Passenger` record
4. Delete `SeatBlocking` record
5. Increment `bus.availableSeatsCount` by 1

### Seat Expiry (Automatic)
- Scheduler runs **every 60 seconds**
- Finds all `SeatBlocking` where `isConfirmed=false` AND `expiresAt < now()`
- For each expired block: increments `availableSeatsCount`, deletes the record
- Seat is now available again for other users

---

## 9. Redis Cache Implementation

### Architecture

```
Service Method Called
        │
        ▼
  Cache Key Exists in Redis?
    │               │
   YES              NO
    │               │
    ▼               ▼
Return from     Execute method
Redis cache     → Store result in Redis
(no DB hit)     → Return result
```

### Configuration — `RedisConfig.java`

| Setting | Value |
|---------|-------|
| Serializer | `GenericJackson2JsonRedisSerializer` (JSON) |
| Key serializer | `StringRedisSerializer` |
| Null caching | Disabled |
| Java time support | `JavaTimeModule` registered |

### Cache Catalogue

| Cache Name | Populated By | TTL | Cache Key |
|------------|-------------|-----|-----------|
| `buses` | `searchBuses()` | **30 minutes** | `from:to:dateStr` |
| `availableSeats` | `getAvailableSeatNumbers()` | **5 minutes** | `busId` |
| `bookingHistory` | `getHistory()` | **15 minutes** | `userId` |

### Cache Annotations Applied

**BusService.java**

| Method | Annotation | Effect |
|--------|-----------|--------|
| `searchBuses()` | `@Cacheable("buses")` | Cache search results |
| `getAvailableSeatNumbers()` | `@Cacheable("availableSeats")` | Cache available seats |
| `addBus()` | `@CacheEvict(buses, allEntries=true)` | Clear all bus search caches |
| `updateBus()` | `@Caching` evicts `buses` + `availableSeats::busId` | Clear affected caches |
| `deleteBus()` | `@Caching` evicts `buses` + `availableSeats::busId` | Clear affected caches |

**BookingService.java**

| Method | Annotation | Effect |
|--------|-----------|--------|
| `getHistory()` | `@Cacheable("bookingHistory")` | Cache user history |
| `blockSeat()` | `@CacheEvict(availableSeats, key=busId)` | Evict seat cache on block |
| `savePassengerDetails()` | `@Caching` evicts `availableSeats` + `bookingHistory::userId` | Clear on booking |
| `cancelTicket()` | `@Caching` evicts `availableSeats` + `bookingHistory::userId` | Clear on cancel |

### Redis Connection (`application.properties`)

```properties
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.data.redis.password=${REDIS_PASSWORD:}
spring.data.redis.timeout=2000ms
spring.data.redis.lettuce.pool.max-active=8
spring.data.redis.lettuce.pool.max-idle=8
spring.data.redis.lettuce.pool.min-idle=0
spring.data.redis.lettuce.pool.max-wait=-1ms
```

---

## 10. Validation Layer

All validation is in `CentralizedValidator.java` — a single place to maintain all rules.

| Field | Rules |
|-------|-------|
| **Name** | Not blank · Only letters + single spaces · No 3+ consecutive identical chars |
| **Mobile** | Exactly 10 digits · Must start with 6, 7, 8, or 9 |
| **Date of Birth** | Format: `ddMMYYYY` (8 digits) · Must be ≥ 18 years old · Year ≥ 1901 |
| **Email** | Not blank · No spaces · Starts with letter · Valid format (`x@x.x`) |
| **Address** | Not blank · Minimum length enforced |
| **Password** | Min 8 chars · At least one uppercase · one lowercase · one digit · one special char |
| **Age** (Passenger) | Must be ≥ 1 |

---

## 11. Exception Handling

`GlobalExceptionHandler` (`@RestControllerAdvice`) maps exceptions to HTTP responses:

| Exception | HTTP Status | Trigger |
|-----------|------------|---------|
| `ValidationException` | 400 Bad Request | Any field fails validation |
| `DuplicateUserException` | 409 Conflict | Email or mobile already registered |
| `SeatUnavailableException` | 409 Conflict | Seat already taken |
| `SeatExpiredException` | 410 Gone | 10-minute block window expired |
| `AuthenticationException` | 401 Unauthorized | Wrong credentials |
| `ExpiredJwtException` | 401 Unauthorized | Token expired |
| `JwtException` | 401 Unauthorized | Invalid/tampered token |
| `AccessDeniedException` | 403 Forbidden | User accessing admin route |
| `ResourceNotFoundException` | 404 Not Found | Entity not found by ID |
| `RedBusException` | 500 Internal Server Error | Unexpected application error |

---

## 12. Scheduler — Seat Expiry

**Class:** `SeatExpiryScheduler.java`  
**Trigger:** Every **60 seconds** (`@Scheduled(fixedRate = 60000)`)

```
Every 60s:
  Find SeatBlocking WHERE isConfirmed = false AND expiresAt < NOW()
  For each expired block:
    → INCREMENT bus.availableSeatsCount + 1
    → DELETE SeatBlocking record
    → Log: "Released expired seat X for bus Y"
```

This ensures that seats held but never confirmed (user closed browser, etc.) are automatically freed.

---

## 13. Configuration

### `application.properties` — Complete Reference

```properties
# Server
server.port=8080

# MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/redbus?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
spring.jpa.properties.hibernate.format_sql=true

# JWT
jwt.secret=RedBusSecretKey_MustBe256BitsOrMoreForHS256Algorithm_ChangeInProduction!
jwt.expiration=3600000

# Scheduling
spring.task.scheduling.pool.size=5

# Logging
logging.level.com.redbus=DEBUG
logging.level.org.springframework.security=INFO

# Redis
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.data.redis.password=${REDIS_PASSWORD:}
spring.data.redis.timeout=2000ms
spring.data.redis.lettuce.pool.max-active=8
spring.data.redis.lettuce.pool.max-idle=8
spring.data.redis.lettuce.pool.min-idle=0
spring.data.redis.lettuce.pool.max-wait=-1ms
```

---

## 14. How to Run

### Prerequisites

| Requirement | Version |
|-------------|---------|
| Java | 17+ |
| Maven | 3.8+ |
| MySQL | 8.0 |
| Memurai (Redis) | Latest |

### Step 1 — Start Redis (Memurai)
Memurai runs as a Windows Service automatically after installation.

Verify it is running:
```powershell
Get-Service Memurai
# Status should be: Running

# If not running:
Start-Service Memurai
```

Test connectivity:
```powershell
Test-NetConnection -ComputerName localhost -Port 6379
# TcpTestSucceeded : True
```

### Step 2 — Create MySQL Database
```sql
CREATE DATABASE redbus;
```
> Tables are auto-created by Hibernate (`ddl-auto=update`) on first startup.

### Step 3 — Build & Run
```powershell
cd "redbus-clone - Copy\redbus"
mvn clean install
mvn spring-boot:run
```

The app starts on: `http://localhost:8080`

### Step 4 — Create Admin User (One-time, via MySQL)
```sql
-- Password below is BCrypt hash for "Admin@123"
INSERT INTO admin (email, password)
VALUES ('admin@redbus.com', '$2a$10$N4/WwAv3VBDT.bEr3bCcL.WD4WqU2FvZ5R8O.7gXGDkGWz0kNO2pC');
```

---

## 15. Postman Testing Guide

### Environment Setup in Postman

Create a Postman Environment with these variables:

| Variable | Value |
|----------|-------|
| `base_url` | `http://localhost:8080` |
| `user_token` | *(set after signIn)* |
| `admin_token` | *(set after admin signIn)* |

---

### Complete Test Sequence

#### Phase 1 — User Registration & Login

**1. Register User**
```
POST {{base_url}}/signUp
Body (JSON):
{
  "name": "John Doe",
  "mobileNumber": "9876543210",
  "dateOfBirth": "01011995",
  "email": "john@example.com",
  "address": "123 Main Street, Chennai",
  "password": "Pass@1234"
}
Expected: 201 → { "message": "Successfully registered Please Sign in" }
```

**2. Sign In (User)**
```
POST {{base_url}}/signIn
Body: { "email": "john@example.com", "password": "Pass@1234" }
Expected: 200 → Copy the "token" value → set as {{user_token}}
```

---

#### Phase 2 — Admin Login & Bus Management

**3. Admin Sign In**
```
POST {{base_url}}/admin
Body: { "email": "admin@redbus.com", "password": "Admin@123" }
Expected: 200 → Copy "token" → set as {{admin_token}}
```

**4. Add Bus**
```
POST {{base_url}}/addBus
Header: Authorization: Bearer {{admin_token}}
Body:
{
  "travelsName": "VRL Travels",
  "fromLocation": "Chennai",
  "toLocation": "Bangalore",
  "dateOfJourney": "2026-07-15",
  "price": 550.00,
  "totalSeats": 40,
  "availableSeatsCount": 40
}
Expected: 200 → { "busId": "1", "message": "Bus added successfully" }
```

---

#### Phase 3 — Bus Search (Cache Test)

**5. Search Buses — CALL 1**
```
GET {{base_url}}/BusSearch?from=Chennai&to=Bangalore&dateOfJourney=2026-07-15
Expected: 200 → List of buses with available seat numbers
Console: Hibernate SQL query appears (DB hit)
```

**6. Search Buses — CALL 2 (same request)**
```
GET {{base_url}}/BusSearch?from=Chennai&to=Bangalore&dateOfJourney=2026-07-15
Expected: 200 → Same response, FASTER
Console: NO Hibernate SQL (Redis cache hit ✓)
```

---

#### Phase 4 — Booking Flow

**7. Block a Seat**
```
POST {{base_url}}/seatBlocking
Header: Authorization: Bearer {{user_token}}
Body: { "busId": 1, "seatNumber": 12 }
Expected: 200 → { "blockingId": 1, "expiresAt": "..." }
Save the blockingId!
```

**8. Submit Passenger Details**
```
POST {{base_url}}/passengerDetails
Header: Authorization: Bearer {{user_token}}
Body:
{
  "blockingId": 1,
  "name": "John Doe",
  "age": 30,
  "gender": "MALE",
  "mobileNumber": "9876543210",
  "emailAddress": "john@example.com"
}
Expected: 201 → { "passengerId": 1, "message": "Booking confirmed!" }
```

**9. View Booking History — CALL 1**
```
GET {{base_url}}/history
Header: Authorization: Bearer {{user_token}}
Expected: 200 → List of bookings
Console: Hibernate SQL appears (DB hit)
```

**10. View Booking History — CALL 2**
```
GET {{base_url}}/history
Header: Authorization: Bearer {{user_token}}
Expected: 200 → Same list, FASTER
Console: NO SQL (Redis cache hit ✓)
```

**11. Cancel Ticket**
```
DELETE {{base_url}}/cancelTicket
Header: Authorization: Bearer {{user_token}}
Body: { "passengerId": 1 }
Expected: 200 → { "message": "Ticket cancelled successfully..." }
```

**12. View History Again (after cancel)**
```
GET {{base_url}}/history
Header: Authorization: Bearer {{user_token}}
Console: Hibernate SQL appears again (cache was evicted on cancel ✓)
```

---

#### Phase 5 — Sign Out

**13. Sign Out**
```
POST {{base_url}}/signOut
Header: Authorization: Bearer {{user_token}}
Expected: 200 → { "message": "Signed out successfully" }
```

**14. Try any protected API with the same token**
```
Expected: 401 Unauthorized (token is blacklisted ✓)
```

---

### Redis Cache Verification Summary

| Action | What happens to Redis |
|--------|----------------------|
| `GET /BusSearch` (1st call) | Miss → DB queried → result stored in `buses` cache |
| `GET /BusSearch` (2nd call same params) | Hit → No DB query |
| `POST /addBus` | `buses` cache fully evicted |
| `PUT /updateBus/{id}` | `buses` cache + `availableSeats::busId` evicted |
| `DELETE /deleteBus/{id}` | `buses` cache + `availableSeats::busId` evicted |
| `POST /seatBlocking` | `availableSeats::busId` evicted |
| `POST /passengerDetails` | `availableSeats` (all) + `bookingHistory::userId` evicted |
| `GET /history` (1st call) | Miss → DB queried → stored in `bookingHistory` cache |
| `GET /history` (2nd call) | Hit → No DB query |
| `DELETE /cancelTicket` | `availableSeats` (all) + `bookingHistory::userId` evicted |

---

*Document generated for RedBus Clone v1.0.0*
