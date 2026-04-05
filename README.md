# Holiday Planner

A web application that provides a platform where municipalities can offer events that can be booked by parents for their children during school holidays.

---

## Team Members

| Name | Service Responsibility |
|---|---|
| Büsra Aydemir | `IdentityService` |
| Denise Müller | `IdentityService` |
| Amir Hodzic | `EventService` |
| Samir Hodzic | `EventService` |
| Muhammed Güzel | `BookingService` |
| Tarik Pasalic | `BookingService` |
| Jan Burtscher | `OrganizationService` + `PaymentService` |
| Aleksander Lukis | `OrganizationService` + `PaymentService` |
| Fabian Türtscher | `NotificationService` + `BookletService` |

---

## Services

| Service | Responsibility | Port | Branch |
|---|---|---|---|
| `IdentityService` | User auth, registration, sessions, caregiver management | 8083 | `feature/identity-service` |
| `EventService` | Events, event terms, status lifecycle, remarks, caregiver assignment | 8081 | `feature/event-service` |
| `BookingService` | Bookings, waiting list, cancellations, participant management | 8082 | `feature/booking-service` |
| `OrganizationService` | Organizations, team members, bank account, booking start time, sponsors | 8084 | `feature/organization-service` |
| `PaymentService` | Payment tracking, refunds, sponsor payments, balance sheet | 8085 | `feature/payment-service` |
| `NotificationService` | Email notifications (booking confirmed, term cancelled, bulk messaging) | 8086 | `feature/notification-service` |
| `BookletService` | PDF generation of the event booklet per organization | 8087 | `feature/booklet-service` |

---

## Repository

**Main Repository:** [https://github.com/MuhiGuezel/HolidayPlanner](https://github.com/MuhiGuezel/HolidayPlanner)

Each service is developed on its own branch. All services are shipped as Docker images.

> Repository access has been granted to: `friessnegger` and `frimp73`

---

## Technology Decisions

### Language & Framework
All services are implemented using **Java 21** with **Spring Boot 3.2.4**.

**Reasons:**
- Strong ecosystem for REST APIs and microservices
- Built-in support for JPA/Hibernate for database access
- Easy Docker integration
- Familiar to all team members
- Well supported within the allowed languages (Java, JavaScript/TypeScript, C#, Kotlin, Python)

### Database
**PostgreSQL** — each service owns its own database schema (no shared DB between services).

### Build Tool
**Maven** — consistent across all services for easier cross-team support.

### PDF Generation
**Apache PDFBox** (BookletService) — open source, no licensing restrictions.

### Email
**Spring Mail** (NotificationService) — built-in Spring Boot support, works with any SMTP server.

### Containerization
**Docker** — each service is shipped as a Docker image using a multi-stage build (Maven build stage + lightweight JRE run stage).

---

## Project Structure

```
HolidayPlanner/
├── README.md
├── docker-compose.yml          ← to be provided by course team
├── docs/
│   ├── domain-model.md         ← domain model description
│   ├── domain-model.png        ← domain model UML diagram
│   └── system-operations.md   ← full system operations table
├── identity-service/
├── event-service/
├── booking-service/
├── organization-service/
├── payment-service/
├── notification-service/
└── booklet-service/
```

---

## How to Build

### Prerequisites
- Java 21
- Maven 3.9+
- Docker
- PostgreSQL (for local development)

### Build a single service
```bash
cd <service-name>
mvn clean package -DskipTests
```

### Run locally
```bash
cd <service-name>
mvn spring-boot:run
```

### Run tests
```bash
cd <service-name>
mvn test
```

---

## 🐳 How to Deploy with Docker

### Build a Docker image
```bash
cd <service-name>
docker build -t holiday-planner/<service-name> .
```

### Run a service with Docker
```bash
docker run -p <port>:<port> \
  -e DB_HOST=localhost \
  -e DB_PORT=5432 \
  -e DB_NAME=<service>_db \
  -e DB_USER=postgres \
  -e DB_PASSWORD=postgres \
  holiday-planner/<service-name>
```

### Run all services (once docker-compose is provided)
```bash
docker-compose up
```

---

## Configuration

Each service is configured via environment variables. Defaults are set for local development in `application.yml`.

### Common variables (all services except NotificationService and BookletService)

| Variable | Default | Description |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `<service>_db` | Database name |
| `DB_USER` | `postgres` | Database user |
| `DB_PASSWORD` | `postgres` | Database password |

### NotificationService additional variables

| Variable | Default | Description |
|---|---|---|
| `MAIL_HOST` | `smtp.gmail.com` | SMTP server host |
| `MAIL_PORT` | `587` | SMTP server port |
| `MAIL_USERNAME` | — | Email address to send from |
| `MAIL_PASSWORD` | — | Email password or app password |

---

## What Has Been Prepared

- [x] Team formed and service responsibilities assigned
- [x] GitHub repository created and all branches set up
- [x] Technology stack decided (Java 21 + Spring Boot 3.2.4)
- [x] All 7 services bootstrapped with hello world REST endpoint
- [x] First model classes added per service
- [x] Basic system operations implemented per service
- [x] Dockerfile added per service (multi-stage build)
- [x] Database configuration per service (PostgreSQL via env variables)
- [x] Domain model documented in `docs/`
- [x] System operations documented in `docs/`

---

## Documentation

- [Domain Model](docs/domain-model.md)
- [System Operations](docs/system-operations.md)
