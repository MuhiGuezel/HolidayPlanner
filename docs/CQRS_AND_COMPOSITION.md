# CQRS and Composition Queries — booking-service

## Section 1: Composition Queries

### What is a Composition Query?

A composition query combines data from multiple services into a single enriched response, so the frontend only needs to make one HTTP call instead of several.

### Candidates Identified

**Candidate 1 — Enriched bookings for a family member**

`GET /api/bookings/family-member/{familyMemberId}/details`

The plain `findByFamilyMemberId` query returned raw booking rows (IDs and status). A parent viewing their bookings needs to see event name, location, dates, and price — all of which live in event-service. Without composition, the frontend would have to make one extra call to event-service per booking. With composition, booking-service makes those calls internally and returns a complete `BookingDetailResponse`.

| Field | Source |
|---|---|
| `bookingId`, `status`, `bookedAt` | booking-service DB |
| `eventName`, `eventLocation`, `termStart`, `termEnd`, `price` | event-service `GET /api/events/terms/{id}` |

**Candidate 2 — Event term summary**

`GET /api/bookings/event-term/{eventTermId}/summary`

Event managers need to see how full a term is: confirmed count, waitlisted count, available spots, and whether it is full. Booking counts come from booking-service; `maxParticipants` and the event name come from event-service. Combining them here removes the need for the consumer to join the two responses themselves.

| Field | Source |
|---|---|
| `confirmedCount`, `waitlistedCount` | booking-service DB |
| `eventName`, `termStart`, `maxParticipants` | event-service `GET /api/events/terms/{id}` |
| `availableSpots`, `isFull` | computed: `max(0, maxParticipants - confirmedCount)` |

### How Failures Are Handled

Both composition queries wrap the call to event-service in a `try/catch`. If event-service is unavailable or returns an error:

- `getBookingsForFamilyMemberEnriched` — event fields are left null in the response for that booking. A `WARN` log records which booking could not be enriched. The rest of the list still returns.
- `getEventTermSummary` — booking counts are still returned correctly. Event-level fields (`eventName`, `termStart`, `maxParticipants`) are left null. `availableSpots` defaults to 0 and `isFull` to false, since we cannot determine capacity without `maxParticipants`.

This is a deliberate graceful-degradation choice: a degraded response is better than a 503 when counts are what matters most.

### Trade-offs

| Pro | Con |
|---|---|
| One HTTP call for the client | Extra latency — one extra HTTP call to event-service per booking (N+1 problem for the enriched list) |
| Cleaner frontend code | booking-service now has a runtime dependency on event-service for read paths |
| Failure is isolated per booking | If event-service is slow, every booking in the list is slow |

---

## Section 2: CQRS

### Why booking-service Benefits from CQRS

Command Query Responsibility Segregation separates write operations (commands) from read operations (queries). In booking-service:

- **Writes** (`createBooking`, `cancelBooking`, `cancelAllBookings`, `promoteFromWaitingList`) need strong consistency: they modify DB state, trigger waitlist promotion, and publish Kafka events. They must be correct and transactional.
- **Reads** (`getBookingsForEventTerm`, `getBookingCount`, `getBookingsForFamilyMember`, composition queries) are called far more often than writes. They need to be fast and should not hold write locks or mix business logic with event publishing.

### Commands vs Queries

| Operation | Type | Notes |
|---|---|---|
| `createBooking` | Command | writes DB, publishes `BookingCreated` |
| `cancelBooking` | Command | writes DB, publishes `BookingCancelled`, triggers promotion |
| `cancelAllBookings` | Command | bulk write, no Kafka event (internal) |
| `promoteFromWaitingList` | Command | writes DB, publishes `WaitlistPromoted` |
| `getBookingsForEventTerm` | Query | read-only |
| `getBookingCount` | Query | read-only |
| `getBookingsForFamilyMember` | Query | read-only |
| `getBookingsForFamilyMemberEnriched` | Query | read-only + composition |
| `getEventTermSummary` | Query | read-only + composition |

### How the Separation Was Implemented

**A) `BookingCommandService`** (`command/BookingCommandService.java`)

Contains all write operations. Depends on `BookingRepository`, `EventServiceClient`, and `BookingEventProducer`. Only this class publishes Kafka events.

**B) `BookingQueryService`** (`query/BookingQueryService.java`)

Contains all read operations plus the two composition queries. Depends on `BookingRepository` and `EventServiceClient`. Does not publish Kafka events.

**C) `BookingController`**

Injects both services. Routes `POST` and `DELETE` endpoints to `BookingCommandService`, and all `GET` endpoints to `BookingQueryService`.

**D) `EventTermCancelledConsumer`**

Updated to inject `BookingCommandService` instead of the old `BookingService`. Kafka consumers trigger commands, never queries.

**E) `BookingService` (old)**

Kept in place during this session as a transitional measure. Tests for it still pass. It can be deleted once the team is satisfied that the new split is stable.

**F) `BookingRepository`**

Added a read-optimized query:
```java
@Query("SELECT b FROM Booking b WHERE b.familyMemberId = :familyMemberId AND b.status != 'CANCELLED' ORDER BY b.bookedAt DESC")
List<Booking> findActiveBookingsByFamilyMember(@Param("familyMemberId") UUID familyMemberId);
```
This filters out cancelled bookings at the DB level and orders by most recent, which is what the frontend needs for the "my bookings" view.

### Trade-offs

| Pro | Con |
|---|---|
| Commands and queries are easier to reason about independently | More classes — controller now injects two services |
| Kafka producers are isolated to commands — no risk of accidental event publishing from a read path | `BookingService` still exists alongside the new classes during transition |
| Reads can be optimized (caching, read replicas) without touching write logic | Spring context now has three service beans where there was one |
| Cleaner test boundaries — unit tests for command logic need no query mocks | The shared `BookingRepository` means there is still one physical DB for reads and writes |

---

## Section 3: Open Questions

1. **Should EventTerm details be cached in booking-service?**
   Every call to `getBookingsForFamilyMemberEnriched` makes one HTTP call per booking. If a parent has 10 bookings, that is 10 calls to event-service. A short-lived cache (e.g. 5 minutes via Caffeine or Spring's `@Cacheable`) keyed by `eventTermId` would reduce this to one call per distinct event term. The risk is serving stale data if an event term is updated or cancelled between cache writes. Given that event terms rarely change once active, a 5-minute TTL seems reasonable.

2. **Is separating service classes enough for this exercise, or is a separate read DB expected?**
   In full CQRS the query side reads from a separate, eventually-consistent read model (a denormalized projection, often updated via events). This implementation separates command and query *classes* but both still read from the same PostgreSQL DB. For a university project this is sufficient and avoids the operational overhead of a second database. A full read-model separation would require a projection database updated by consuming `BookingCreated` / `BookingCancelled` Kafka events.

3. **N+1 problem in the enriched family member query — should we batch?**
   `getBookingsForFamilyMemberEnriched` calls event-service once per booking. If a parent has many bookings this causes multiple sequential HTTP calls. Batching would require an endpoint on event-service that accepts a list of event term IDs (e.g. `POST /api/events/terms/batch`) and returns details for all of them in one call. This does not exist yet. Alternative: collect distinct event term IDs from all bookings, call event-service once per distinct term (not once per booking), and join in memory.

4. **Should commands return the updated state or just an acknowledgement?**
   Currently `createBooking` and `cancelBooking` return a `BookingResponse`. In strict CQRS the command side returns nothing (or just an acknowledgement) and the client queries for the updated state separately. For this project returning the state is pragmatic — it removes a follow-up GET call. If write throughput becomes a concern, switching to 202 Accepted with a location header would decouple the write from the read.

5. **Who should own the composition — booking-service or an API Gateway?**
   Composing at the service level means each service that needs enriched data must implement its own client calls and failure handling. Centralizing composition in an API Gateway (or a dedicated BFF — Backend for Frontend) would keep individual services simpler and give one place to optimize (batching, caching, circuit breaking). For a university project where there is no gateway yet, doing it in the service is the right pragmatic choice.
