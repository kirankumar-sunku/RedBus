# 🚌 RedBus Clone — Spring Boot REST API

A complete bus booking backend built with **Java 17**, **Spring Boot 3.x**, **MySQL**, and **JWT** security.

---

## Tech Stack

| Layer       | Technology                        |
|-------------|-----------------------------------|
| Language    | Java 17                           |
| Framework   | Spring Boot 3.2                   |
| Security    | Spring Security + JWT (JJWT 0.12) |
| Database    | MySQL 8.x                         |
| ORM         | Spring Data JPA / Hibernate       |
| Build       | Maven                             |
| Utility     | Lombok                            |

---

## Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8.x running locally

---

## Setup

### 1. Create the database
```sql
CREATE DATABASE redbus_db;
```

### 2. Configure credentials
Edit `src/main/resources/application.properties`:
```properties
spring.datasource.username=root
spring.datasource.password=your_password
```

### 3. Seed an admin user
Run this SQL after first startup (tables are auto-created):
```sql
-- Password is: Admin@1234 (BCrypt encoded)
INSERT INTO admin (email, password)
VALUES ('admin@redbus.com', '$2a$12$wRbb.HgW96z0LA5e294E9O0lpxoQRjLA2RSfA71izdQ9AJiCGfuI.');
```

### 4. Run the application
```bash
mvn spring-boot:run
```

---

## API Reference

### Public Endpoints

| Method | Endpoint       | Description                     |
|--------|----------------|---------------------------------|
| POST   | `/signUp`      | Register new user               |
| POST   | `/signIn`      | Login — returns JWT token       |
| GET    | `/BusSearch`   | Search buses by from/to/date    |
| POST   | `/admin`       | Admin login                     |

### Authenticated User Endpoints (JWT required)

| Method | Endpoint            | Description                       |
|--------|---------------------|-----------------------------------|
| POST   | `/signOut`          | Invalidate token                  |
| POST   | `/seatBlocking`     | Block a seat for 10 minutes       |
| POST   | `/passengerDetails` | Confirm booking with passenger info |
| GET    | `/history`          | View all bookings                 |
| DELETE | `/cancelTicket`     | Cancel a booking                  |

### Admin Endpoints (Admin JWT required)

| Method | Endpoint             | Description       |
|--------|----------------------|-------------------|
| POST   | `/addBus`            | Add a bus         |
| PUT    | `/updateBus/{id}`    | Update a bus      |
| DELETE | `/deleteBus/{id}`    | Delete a bus      |
| POST   | `/addRoutes`         | Add a route       |
| PUT    | `/updateRoutes/{id}` | Update a route    |
| DELETE | `/deleteRoutes/{id}` | Delete a route    |

---

## Sample Requests

### Sign Up
```json
POST /signUp
{
  "name": "Ramesh Kumar",
  "mobileNumber": "9876543210",
  "dateOfBirth": "15061995",
  "email": "ramesh@gmail.com",
  "address": "123 MG Road, Bengaluru",
  "password": "Ram@1234"
}
```

### Sign In
```json
POST /signIn
{
  "email": "ramesh@gmail.com",
  "password": "Ram@1234"
}
```

### Search Buses
```
GET /BusSearch?from=Chennai&to=Bengaluru&dateOfJourney=2025-12-25
```

### Block a Seat
```json
POST /seatBlocking
Authorization: Bearer <token>
{
  "busId": 1,
  "seatNumber": 13
}
```

### Passenger Details
```json
POST /passengerDetails
Authorization: Bearer <token>
{
  "blockingId": 1,
  "name": "Ramesh Kumar",
  "age": 28,
  "gender": "MALE",
  "mobileNumber": "9876543210",
  "emailAddress": "ramesh@gmail.com"
}
```

---

## Validation Rules (CentralizedValidator)

All validation is handled in a single `CentralizedValidator` class.

| Field          | Rules                                                                 |
|----------------|-----------------------------------------------------------------------|
| Name           | Letters and spaces only. No 3+ consecutive identical chars.          |
| Mobile Number  | Exactly 10 digits. First digit: 6, 7, 8, or 9.                      |
| Date of Birth  | DDMMYYYY format. Valid calendar date. Year ≥ 1901. Age ≥ 18.        |
| Email          | First char letter. Must have `@` and domain. No spaces.              |
| Address        | No 5+ consecutive identical letters.                                 |
| Password       | Min 8 chars. Uppercase, lowercase, digit, special char required.     |

---

## Project Structure

```
src/main/java/com/redbus/
├── RedbusApplication.java
├── config/
│   ├── JwtAuthFilter.java
│   └── SecurityConfig.java
├── controller/
│   ├── UserController.java
│   ├── BusController.java
│   ├── BookingController.java
│   └── AdminController.java
├── dto/
│   ├── request/   (SignUpRequest, SignInRequest, SeatBlockingRequest, ...)
│   └── response/  (JwtResponse, BusSearchResponse, PassengerResponse, ...)
├── entity/
│   ├── User.java
│   ├── Admin.java
│   ├── Bus.java
│   ├── SeatBlocking.java
│   └── Passenger.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   └── (ValidationException, DuplicateUserException, ...)
├── repository/
│   ├── UserRepository.java
│   ├── BusRepository.java
│   ├── SeatBlockingRepository.java
│   └── PassengerRepository.java
├── scheduler/
│   └── SeatExpiryScheduler.java
├── service/
│   ├── UserService.java
│   ├── BusService.java
│   ├── BookingService.java
│   ├── AdminService.java
│   └── JwtService.java
└── validation/
    └── CentralizedValidator.java
```

---

## Key Business Rules

- **Seat blocking**: A seat is blocked for exactly **10 minutes**. If passenger details are not submitted within this window, the seat is automatically released.
- **Token expiry**: JWT tokens expire after **3600 seconds**. Even if a token expires mid-booking, the seat remains blocked until the 10-minute window ends.
- **Scheduler**: A background job runs every **60 seconds** to release expired seat blocks.
- **Cancellation**: Cancelling a ticket atomically deletes the passenger record, the seat block, and increments available seats — all in a single `@Transactional` operation.

---

## Running Tests

```bash
mvn test
```
