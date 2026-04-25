# Holiday Planner — Progress Log

---

## Session 5 — 2026-04-26 — Docker Build Fixes & DockerHub Deployment

**Goal:** Fix broken Docker builds, get all services running end-to-end, and publish images to DockerHub with a production-ready docker-compose.yml.

### What was done

**Fix 1 — spring-boot-maven-plugin executions**
- Added explicit `<executions><execution><goals><goal>repackage</goal></goals></execution></executions>` to `spring-boot-maven-plugin` in all 7 service `pom.xml` files
- Without this, `mvn package` only produced a plain JAR (no `Main-Class` manifest) when not using `spring-boot-starter-parent` — the `repackage` goal does not auto-bind outside that parent
- Resolved: `no main manifest attribute, in app.jar` crash on container start

**Fix 2 — Dockerfile build order**
- Updated all 7 Dockerfiles to install dependencies in the correct order:
  1. `mvn install -N -DskipTests` — installs the root POM into the local Maven repo (non-recursive)
  2. `mvn install -pl shared -DskipTests` — installs the shared module
  3. `mvn package -pl <service> -DskipTests` — builds the service JAR
- Resolved: `Could not find artifact com.holidayplanner:holidayplanner:pom:1.0.0-SNAPSHOT`

**Fix 3 — Docker disk space**
- Removed 4 orphaned anonymous volumes (~34 GB of stale Maven cache) from Docker Desktop's virtual disk
- Resolved: postgres `No space left on device` preventing container startup

**Kafka UI — switched to provectuslabs/kafka-ui**
- Replaced `docker.redpanda.com/redpandadata/console` with `provectuslabs/kafka-ui:latest`
- Corrected internal port mapping from `5001:5001` to `5001:8080`

**DockerHub — published all 7 images**
- Tagged and pushed all service images to `muhiguezel/<service>:latest`:
  - `muhiguezel/booking-service:latest`
  - `muhiguezel/event-service:latest`
  - `muhiguezel/identity-service:latest`
  - `muhiguezel/organization-service:latest`
  - `muhiguezel/payment-service:latest`
  - `muhiguezel/notification-service:latest`
  - `muhiguezel/booklet-service:latest`

**docker-compose.yml — updated to lecturer template format**
- All services now use `image: muhiguezel/<service>:latest` (no local build step required)
- Kafka: Bitnami KRaft `public.ecr.aws/bitnami/kafka:4.2.0`, internal listener `kafka:29092`
- Kafka UI: Redpanda Console on port 5000
- Each service has its own named database (`booking_db`, `event_db`, etc.)
- Added `booklet_db` to `docker/init-databases.sh`
- notification-service internal port corrected to `8086:8086`

---

## Session 4 — 2026-04-25 — Kafka Integration

**Goal:** Add async event communication via Apache Kafka across booking-service, event-service, payment-service, and notification-service.

### What was done

**Task 1 — docker-compose.yml**
- Added all 7 service containers (booking, event, identity, organization, payment, notification, booklet) to the existing `docker-compose.yml`
- Each service wired to the shared PostgreSQL container and Kafka broker (Bitnami KRaft, internal listener `kafka:29092`)
- Mail env vars passed through to notification-service

**Task 2 — Kafka Maven dependency**
- Added `spring-kafka` dependency to `booking-service/pom.xml`, `event-service/pom.xml`, `payment-service/pom.xml`, `notification-service/pom.xml`

**Task 3 — Shared Kafka classes**
- Created `shared/src/main/java/com/holidayplanner/shared/kafka/KafkaEnvelope.java` — generic envelope wrapper
- Created 6 payload classes in `com.holidayplanner.shared.kafka.payload`:
  - `BookingCreatedPayload`
  - `BookingCancelledPayload`
  - `EventTermCancelledPayload`
  - `WaitlistPromotedPayload`
  - `ParticipantListRequestedPayload`
  - `PaymentRefundedPayload`

**Task 4 — Kafka producers**
- `booking-service`: `BookingEventProducer` — publishes `BookingCreated`, `BookingCancelled`, `WaitlistPromoted`
- `event-service`: `EventTermEventProducer` — publishes `EventTermCancelled`, `ParticipantListRequested`
- `payment-service`: `PaymentEventProducer` — publishes `PaymentRefunded`
- All producers wired into the respective service classes (`BookingService`, `EventManagementService`, `PaymentService`)
- Added `cancelAllBookings(UUID eventTermId)` method to `BookingService`

**Task 5 — Kafka consumers**
- `booking-service`: `EventTermCancelledConsumer` — calls `bookingService.cancelAllBookings(eventTermId)`
- `payment-service`: `BookingCreatedConsumer` — creates payment only if status=CONFIRMED; idempotency check via `findByBookingId`
- `notification-service`: 6 consumers — `BookingCreatedConsumer`, `BookingCancelledConsumer`, `EventTermCancelledConsumer`, `WaitlistPromotedConsumer`, `PaymentRefundedConsumer`, `ParticipantListRequestedConsumer`
- Added `notifyRefund(parentEmail, eventName, amount)` to `NotificationService`

**Task 6 — Kafka config in application.yml**
- Added `spring.kafka` block (bootstrap-servers, producer serializers, consumer deserializers, group-id, auto-offset-reset) to all 4 services
- `bootstrap-servers` defaults to `localhost:29092` for local dev, overridden by `SPRING_KAFKA_BOOTSTRAP_SERVERS` env var in Docker

**Task 7 — docs/kafka-decisions.md**
- Created `docs/kafka-decisions.md` covering: serialization rationale, topic naming convention, message envelope structure with example, message keys and why, full producer→consumer mapping table, consumer group table, idempotency approach, error handling strategy, open questions for the lecture

---

## Previous Sessions

*(Sessions 1–3 bootstrapped all 7 services with models, repositories, service classes, controllers, Dockerfiles, DTOs, inter-service HTTP client, unit/integration/contract tests, and system documentation.)*
