# Claude Context — Compose & Messaging Setup

This file summarizes everything done in the "compose + messaging" task session so the next Claude session has full context.

---

## What was the task

- Organize a shared repository with compose file(s) to run all services locally
- Publish service images to a registry (GHCR)
- Decide serialization format as a team
- Define Kafka topic naming convention
- Propose a message envelope format

Teacher template reference: https://github.com/advanced-web-architectures-2026/compose-template

---

## What was done

### 1. docker-compose.yml (repo root)

Merged the teacher's Kafka template with the existing Postgres setup. Currently contains:
- **postgres:15** — single container, port 5432, creates 5 databases via init script
- **kafka** — `public.ecr.aws/bitnami/kafka:4.2.0` with `platform: linux/amd64` (needed for Apple Silicon / Rosetta emulation)
- **kafkaui** — Redpanda Console on port **5001** (5000 was taken by macOS AirPlay)

Important: `kafka_data` volume is declared but Kafka uses a bind mount to `./volumes/kafka/data`.

```bash
docker compose up -d    # start everything
docker compose down -v  # stop + delete volumes (fresh DB)
```

### 2. docker/init-databases.sh

Creates 5 PostgreSQL databases on first start:
- `booking_db`, `event_db`, `identity_db`, `organization_db`, `payment_db`

Note: At some point Docker created this as a directory instead of a file (failed mount). Was fixed by deleting the directory and recreating the file.

### 3. docker/application-*.yml

One config file per service that overrides the DB host from `localhost` to `postgres` (Docker network hostname). These are designed to be volume-mounted and loaded via `SPRING_CONFIG_ADDITIONAL_LOCATION`.

Files:
- `docker/application-event.yml`
- `docker/application-booking.yml` (also sets `event-service.url: http://event-service:8081`)
- `docker/application-identity.yml`
- `docker/application-organization.yml`
- `docker/application-payment.yml`

These are NOT currently used by docker-compose.yml (services section was removed). They are available if needed when adding service definitions to the compose file.

### 4. Dockerfiles — all 7 services fixed

The original Dockerfiles were broken for a multi-module Maven build (they only copied their own `src/` and missed the `shared/` module). All were updated to build from the repo root:

```dockerfile
# Build from root: docker build -f booking-service/Dockerfile .
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY shared/pom.xml shared/pom.xml
COPY shared/src shared/src
COPY booking-service/pom.xml booking-service/pom.xml
COPY booking-service/src booking-service/src
RUN mvn install -pl shared -am -DskipTests && \
    mvn package -pl booking-service -DskipTests
...
```

Note: Some Dockerfiles were reverted to the old (broken) versions by the user. Check each service's Dockerfile before building.

### 5. GHCR image publishing

No GitHub Actions workflow (was created and then removed by user — too much overhead for the task).

Images must be pushed manually:
```bash
echo YOUR_GITHUB_TOKEN | docker login ghcr.io -u MuhiGuezel --password-stdin
docker build -f booking-service/Dockerfile -t ghcr.io/muhiguezel/holidayplanner-booking-service:latest .
docker push ghcr.io/muhiguezel/holidayplanner-booking-service:latest
```

Image naming: `ghcr.io/muhiguezel/holidayplanner-{service-name}:latest`

After pushing: set each package to Public on GitHub (Packages → Package settings → Change visibility).

### 6. Messaging conventions (docs/messaging-conventions.md)

Three decisions documented:

**Serialization:** JSON

**Topic naming:** `{domain}.{entity}.{event}`
- Examples: `booking.booking.created`, `event.eventterm.status-changed`

**Envelope:**
```json
{
  "id": "uuid",
  "type": "booking.booking.created",
  "source": "booking-service",
  "timestamp": "2026-04-25T10:00:00Z",
  "version": "1",
  "data": { ... }
}
```

---

## kcat (Kafka CLI tool)

Located at `.idea/fromTeacher/kcat`. Was modified to work on Apple Silicon Mac:

```bash
docker run --rm --network holidayplanner_default edenhill/kcat:1.7.1 kafkacat -b kafka:29092 "$@"
```

Key fixes made:
- `--network=host` doesn't work on Mac → replaced with `--network holidayplanner_default`
- `localhost:9092` doesn't work from inside Docker on Mac → replaced with `kafka:29092` (internal network)

Usage from `.idea/fromTeacher/`:
```bash
# create topic
./create_topic.sh booking.booking.created

# produce message
echo '{"id":"test-1",...}' | ./kcat -P -t booking.booking.created

# consume messages
./kcat -C -t booking.booking.created -o beginning
```

Kafka UI (browser) at http://localhost:5001 is easier for ad-hoc testing.

---

## Known issues / open items

- `volumes/` folder should be in `.gitignore` — Kafka runtime data gets tracked by git. `.gitignore` entry `volumes/` was added but `git rm --cached volumes/` was not run yet.
- GHCR images have not been pushed yet — still needs to be done manually.
- `docker-compose.yml` does not include the service containers (event-service, booking-service, etc.) — only infrastructure (Postgres, Kafka). Services are run locally via `mvn spring-boot:run`.

---

## Running services locally

```bash
# start infrastructure
docker compose up -d

# start services (one terminal each)
mvn spring-boot:run -pl event-service    # port 8081
mvn spring-boot:run -pl booking-service  # port 8082
mvn spring-boot:run -pl identity-service # port 8083
```

Health checks:
```bash
curl http://localhost:8081/api/events/health    # "EventService is running!"
curl http://localhost:8082/api/bookings/health  # "BookingService is running!"
```

---

## Project structure (relevant files)

```
HolidayPlanner/
├── docker-compose.yml               ← Postgres + Kafka + KafkaUI
├── docker/
│   ├── init-databases.sh            ← creates 5 postgres DBs
│   ├── application-booking.yml      ← docker DB config for booking-service
│   ├── application-event.yml
│   ├── application-identity.yml
│   ├── application-organization.yml
│   └── application-payment.yml
├── docs/
│   └── messaging-conventions.md    ← serialization + topic naming + envelope
├── volumes/
│   └── kafka/data/                  ← Kafka runtime data (should be gitignored)
└── .idea/fromTeacher/
    ├── kcat                         ← Kafka CLI wrapper (modified for Mac)
    └── create_topic.sh
```
