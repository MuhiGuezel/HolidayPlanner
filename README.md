# 🏖️ Holiday Planner

A web application that provides a platform where municipalities can offer events that can be booked by parents for their children during school holidays.

---

## 👥 Team Members

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

## 🛠️ Services

| Service | Responsibility | Branch |
|---|---|---|
| `IdentityService` | User authentication, registration, sessions, caregiver management | `feature/identity-service` |
| `EventService` | Events, event terms, status lifecycle, remarks, caregiver assignment | `feature/event-service` |
| `BookingService` | Bookings, waiting list, cancellations, participant management | `feature/booking-service` |
| `OrganizationService` | Organizations, team members, bank account, booking start time, sponsors | `feature/organization-service` |
| `PaymentService` | Payment tracking, refunds, sponsor payments, balance sheet, statistics | `feature/payment-service` |
| `NotificationService` | Email notifications (booking confirmed, term cancelled, bulk messaging) | `feature/notification-service` |
| `BookletService` | PDF generation of the event booklet per organization | `feature/booklet-service` |

---

## 🔗 Repository

**Main Repository:** [https://github.com/MuhiGuezel/HolidayPlanner](https://github.com/MuhiGuezel/HolidayPlanner)

Each service is developed on its own branch (see table above). All services will be merged into `main` and shipped as Docker images.

> Repository access has been granted to: `friessnegger` and `frimp73`

---

## 💻 Technology Decisions

### Language & Framework
All services are implemented using **Java** with **Spring Boot**.

**Reasons:**
- Strong ecosystem for REST APIs and microservices
- Built-in support for JPA/Hibernate for database access
- Easy Docker integration
- Familiar to all team members
- Well supported within the allowed languages (Java, JavaScript/TypeScript, C#, Kotlin, Python)

### Database
**PostgreSQL** — one database per service (each service owns its own schema).

### Build Tool
**Maven** — consistent across all services for easier cross-team support.

### Containerization
**Docker** — each service is shipped as a Docker image. A `docker-compose.yml` will be provided by the lecturer for integration.

---

## 📁 Project Structure

```
HolidayPlanner/
├── README.md
├── docker-compose.yml
├── docs/
│   ├── domain-model.png
│   └── system-operations.md
├── identity-service/
├── event-service/
├── booking-service/
├── organization-service/
├── payment-service/
├── notification-service/
└── booklet-service/
```

---

## 🚀 What Has Been Prepared So Far

- [x] Team formed and service responsibilities assigned
- [x] GitHub repository created: [HolidayPlanner](https://github.com/MuhiGuezel/HolidayPlanner)
- [x] Feature branches created for all 7 services
- [x] Technology stack decided (Java + Spring Boot)
- [ ] Spring Boot project bootstrapped per service (hello world REST endpoint)
- [ ] First model classes added per service
- [ ] Dockerfile added per service
- [ ] Database setup per service
- [ ] Documentation on how to build, run tests and deploy

---

## 🏗️ How to Build & Run (per service)

Each service can be built and run independently:

```bash
# Build
cd <service-name>
./mvnw clean package

# Run locally
./mvnw spring-boot:run

# Build Docker image
docker build -t holiday-planner/<service-name> .

# Run with Docker
docker run -p 8080:8080 holiday-planner/<service-name>
```

A `docker-compose.yml` for running all services together will be added once provided by the course team.

---
