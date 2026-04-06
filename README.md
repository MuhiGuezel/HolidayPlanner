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

| Service | Responsibility |
|---|---|
| `IdentityService` | User auth, registration, sessions, caregiver management |
| `EventService` | Events, event terms, status lifecycle, remarks, caregiver assignment |
| `BookingService` | Bookings, waiting list, cancellations, participant management |
| `OrganizationService` | Organizations, team members, bank account, booking start time, sponsors |
| `PaymentService` | Payment tracking, refunds, sponsor payments, balance sheet |
| `NotificationService` | Email notifications (booking confirmed, term cancelled, bulk messaging) |
| `BookletService` | PDF generation of the event booklet per organization |

---

## Repository

**Main Repository:** [https://github.com/MuhiGuezel/HolidayPlanner](https://github.com/MuhiGuezel/HolidayPlanner)

The repository is organized as a Maven multi-module monorepo on `main`. Each service is its own Spring Boot application, and the shared model classes live in the `shared` module.

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
**Maven** — the repository uses a root aggregator/parent `pom.xml` with one module per service plus a shared library module.

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
├── pom.xml
├── README.md
├── docker-compose.yml          ← to be provided by course team
├── docs/
│   ├── domain-model.md         ← domain model description
│   ├── DomainModel.svg         ← domain model UML diagram
│   └── system-operations.md   ← full system operations table
├── shared/
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

### Build the full monorepo
```bash
mvn clean install
```

### Build a single service
```bash
mvn -pl <service-name> -am clean package -DskipTests
```

### Run a single service locally
```bash
cd <service-name>
mvn spring-boot:run
```

### Run tests
```bash
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

Each service is configured via environment variables. Defaults are set for local development in each module's `application.yml`.

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
- [x] GitHub repository created
- [x] Technology stack decided (Java 21 + Spring Boot 3.2.4)
- [x] Services bootstrapped
- [x] Maven multi-module parent/aggregator at the repository root
- [x] Dockerfile added per service (multi-stage build)
- [x] Database configuration per service (PostgreSQL via env variables)
- [x] Domain model documented in `docs/`
- [x] System operations documented in `docs/`

---

## Documentation

- [Domain Model](docs/domain-model.md)
- [System Operations](docs/system-operations.md)
