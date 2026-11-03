
# Meeting Room Booking API

A REST API built using Java 17, Spring Boot 3, Spring Data JPA, and H2 Database.

## Features

### Q1 — Room Booking API
- Create, retrieve, list, and cancel bookings
- Validate booking times, room capacity, and attendee information
- Prevent overlapping bookings
- Return appropriate HTTP error responses

### Q2 — Free Slots & Safe Booking
- Calculate common free slots for multiple attendees
- Suggest available rooms based on capacity and availability
- Use database row locking to prevent concurrent double bookings
- Support idempotency keys to prevent duplicate bookings on retries
- Exclude cancelled bookings from availability calculations

## Technology
- Java 17+
- Spring Boot 3.x
- Maven
- Spring Data JPA
- H2 Database
- JUnit 5

## Run the Application

```powershell
.\mvnw.cmd spring-boot:run
```

The application starts at http://localhost:8080.

## Run Tests

```powershell
.\mvnw.cmd clean test
```

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | /api/rooms | List rooms |
| POST | /api/bookings | Create a booking |
| GET | /api/bookings/{id} | Retrieve a booking |
| DELETE | /api/bookings/{id} | Cancel a booking |
| GET | /api/rooms/{id}/bookings | List room bookings |
| GET | /api/free-slots | Find common available slots |

## Testing

Automated tests cover:
- Booking overlaps and back-to-back meetings
- Idempotent booking retries
- Concurrent booking requests
- Free-slot calculations and cancellation handling

## Limitations

The default in-memory H2 database is intended for local development. Multi-instance concurrency safety requires deployment against a shared transactional database and separate verification.

## Pull Requests

- Q1: https://github.com/GitGoodTM/be-hands-on-set3/pull/1
- Q2: To be added after creating the second pull request.
