# Identity Service Team Submission
## Composition Queries, CQRS, and Kafka Event-Driven Architecture

**Date:** May 1, 2026  
**Team:** Identity Service Sub-team  
**Status:** ✅ FULLY IMPLEMENTED

---

## Executive Summary

The Identity Service team has successfully completed three interconnected tasks:

1. **Composition Queries Analysis** — Identified existing and candidate composition queries for future expansion
2. **CQRS Pattern Validation** — Verified clean command/query separation; identified gaps (NOW FILLED)
3. **Kafka Event-Driven Integration** — Fully implemented async event publishing and consumption

All implementation decisions have been made with team consensus and documented for organizational alignment.

---

## Part 1: Composition Queries

### Current Implementation ✅

#### `getUserProfileEnriched(userId)`
- **What:** Combines user profile + family members + active booking counts from booking-service
- **Why:** Single API call instead of 2+N calls; reduces frontend complexity
- **Implementation:** `IdentityCompositionService.java` with HTTP client fallback
- **Benefit:** Graceful degradation if booking-service unavailable

### Future Composition Candidates (Recommended)

1. **`getOrganizationMembersEnriched(orgId)`** [Medium Priority]
   - Combine: users + family members + booking stats
   - Consumers: admin dashboards
   
2. **`getFamilyMemberBookings(memberId)`** [High Priority]
   - Combine: member details + all bookings + event details + payment status
   - Consumers: parent viewing booking history
   - Cross-service: identity + booking + event + payment

### Composition Strategy
- Each composition query is independent and can handle service failures gracefully
- Used for **frontend convenience**, not **critical path logic**
- Perfect for dashboards, enriched reads, and reporting

---

## Part 2: CQRS Pattern

### Current Architecture ✅

```
Controller
    ↓
├─→ CommandService (POST/PATCH/DELETE) → Commands modify state → Events published
├─→ QueryService (GET) → Pure reads, no side effects
└─→ CompositionService (GET enriched) → Multi-service reads, HTTP clients
```

### Benefits Realized
✅ Clear separation of concerns  
✅ Commands and queries can be optimized independently  
✅ Composition layer isolated from business logic  
✅ Easy to test each layer with mocks  
✅ Ready for caching, read replicas, denormalization  

### Gaps Identified & Filled

| Gap | Previous State | Now Implemented |
|-----|---|---|
| Event Publishing | TODO comment | ✅ All commands publish events via `DomainEventPublisher` |
| Event Consumption | None | ✅ `IdentityEventListener` consumes external events |
| Domain Events | Documented but missing | ✅ 4 event types published to Kafka |

---

## Part 3: Kafka Event-Driven Architecture

### 3.1 Serialization Decision ✅

**Selected:** JSON (Application/JSON)

**Rationale:**
- Human-readable for debugging
- No external schema registry needed initially
- Native Spring Boot support
- Compatible with all services

**Configuration:**
```java
Producer: JsonSerializer
Consumer: JsonDeserializer with trusted packages
```

---

### 3.2 Topic Naming Convention ✅

**Format:** `{team}.{entity}.{event-type}`

**Identity Service Topics Produced:**

| Event | Topic | Published By |
|---|---|---|
| User Registration | `identity.user.registered` | `registerUser()` |
| Phone Update | `identity.user.phone_updated` | `updatePhoneNumber()` |
| Family Member Added | `identity.family_member.added` | `addFamilyMember()` |
| Family Member Removed | `identity.family_member.removed` | `removeFamilyMember()` |

**Topics Consumed:**

| Event | Topic | Producer | Status |
|---|---|---|---|
| Booking Cancelled | `booking.booking.cancelled` | booking-service | Listening (handlers ready) |
| Payment Refunded | `payment.payment.refunded` | payment-service | Listening (handlers ready) |

---

### 3.3 Message Envelope ✅

**Standard Envelope (All Events):**

```json
{
  "eventType": "UserRegistered",
  "timestamp": "2026-05-01T10:30:00Z",
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "version": 1,
  "payload": {
    "userId": "uuid",
    "email": "user@example.com",
    "organizationId": "uuid",
    "createdAt": "2026-05-01T10:30:00Z"
  }
}
```

**Components:**
- `eventType` — Semantic name for routing (e.g., "UserRegistered")
- `timestamp` — ISO-8601 for ordering and replay
- `eventId` — Unique UUID for idempotent consumption
- `version` — Schema versioning for evolution
- `payload` — Event-specific data (varies per event type)

**Why This Design:**
- ✅ Enables idempotent processing (via eventId)
- ✅ Supports event sourcing / audit trails
- ✅ Schema-versioning ready
- ✅ Language-agnostic

---

### 3.4 Message Keys ✅

**Strategy:** `{entityType}:{entityId}`

**Examples:**
- User event: `user:550e8400-e29b-41d4-a716-446655440000`
- Family member event: `family_member:550e8400-e29b-41d4-a716-446655440001`

**Why:**
- Kafka partitions by key → same entity always in same partition
- Guarantees ordering per entity (not global ordering)
- Enables stateful processing in consuming services
- Matches system-wide pattern

---

### 3.5 Implementation Files ✅

#### Configuration
- **`KafkaConfig.java`**
  - Producer/consumer factories
  - Topic auto-creation with 3 partitions, replication factor 1
  - JSON serialization setup
  
#### Events
- **`DomainEvent.java`** — Base envelope class
- **`UserRegisteredEvent.java`** — Payload for user registration
- **`UserPhoneUpdatedEvent.java`** — Payload for phone update
- **`FamilyMemberAddedEvent.java`** — Payload for member addition
- **`FamilyMemberRemovedEvent.java`** — Payload for member removal

#### Publishing & Consuming
- **`DomainEventPublisher.java`** — Service to publish events (4 methods)
- **`IdentityEventListener.java`** — Service to consume events (2 topics listening)

#### Configuration
- **`pom.xml`** — Added `spring-kafka` dependency
- **`application.yml`** — Kafka bootstrap servers, producer/consumer config

#### Integration
- **`IdentityCommandService.java`** — Updated with event publishing in all command methods

---

### 3.6 Events Published by Identity Service ✅

#### 1. UserRegisteredEvent
```
Topic: identity.user.registered
Trigger: POST /api/identity/users/register
Key: user:{userId}
Payload: userId, email, organizationId, createdAt
Consumers: notification-service (welcome email), organization-service (member tracking)
```

#### 2. UserPhoneUpdatedEvent
```
Topic: identity.user.phone_updated
Trigger: PATCH /api/identity/users/{userId}/phone
Key: user:{userId}
Payload: userId, phoneNumber, updatedAt
Consumers: notification-service (if urgent contact needed)
```

#### 3. FamilyMemberAddedEvent
```
Topic: identity.family_member.added
Trigger: POST /api/identity/users/{userId}/family-members
Key: family_member:{memberId}
Payload: familyMemberId, userId, firstName, lastName, birthDate, zip, createdAt
Consumers: booking-service (notify of new eligible participant)
```

#### 4. FamilyMemberRemovedEvent
```
Topic: identity.family_member.removed
Trigger: DELETE /api/identity/family-members/{memberId}
Key: family_member:{memberId}
Payload: familyMemberId, userId, firstName, lastName, removedAt
Consumers: booking-service (cascade cleanup of orphaned bookings)
```

---

### 3.7 Events Consumed by Identity Service ✅

#### 1. BookingCancelled
```
Topic: booking.booking.cancelled
Produced by: booking-service
Handler: IdentityEventListener.handleBookingCancelled()
Current: Logs and validates event structure
Future: Could trigger cascade cleanup or notifications
```

#### 2. PaymentRefunded
```
Topic: payment.payment.refunded
Produced by: payment-service
Handler: IdentityEventListener.handlePaymentRefunded()
Current: Logs and validates event structure
Future: Could notify user of refund status
```

---

## Part 4: Integration Summary

### What Changed in Identity Service

1. **Dependencies Added**
   - `spring-kafka` for Kafka producer/consumer support

2. **Configuration Added**
   - `KafkaConfig.java` — Comprehensive Kafka setup
   - `application.yml` — Kafka bootstrap servers, serialization

3. **Events Package Created** (`com.holidayplanner.identityservice.event`)
   - `DomainEvent.java` — Standard envelope
   - 4 event payload classes
   - `DomainEventPublisher.java` — Publish operations
   - `IdentityEventListener.java` — Consume operations

4. **CommandService Enhanced**
   - `registerUser()` → publishes `UserRegisteredEvent`
   - `updatePhoneNumber()` → publishes `UserPhoneUpdatedEvent`
   - `addFamilyMember()` → publishes `FamilyMemberAddedEvent`
   - `removeFamilyMember()` → publishes `FamilyMemberRemovedEvent`

### Event Flow Example: User Registration

```
1. Client: POST /api/identity/users/register?email=...&password=...
2. Controller: Routes to IdentityCommandService
3. CommandService: 
   a) Validates email uniqueness
   b) Hashes password
   c) Saves user to PostgreSQL
   d) Creates UserRegisteredEvent envelope
   e) Calls DomainEventPublisher.publishUserRegistered()
4. DomainEventPublisher:
   a) Wraps event in DomainEvent with eventId, timestamp, version
   b) Sets message key: "user:{userId}"
   c) Sends to Kafka topic: "identity.user.registered"
5. Other services (notification-service, organization-service):
   a) Listen to "identity.user.registered" topic
   b) React asynchronously (send email, track membership, etc.)
6. Response: User object returned to client (sync response)
```

---

## Part 5: Team Decisions & Questions

### ✅ Decisions Made

| Decision | Value | Rationale |
|----------|-------|-----------|
| Serialization | JSON | Human-readable, no external registry needed |
| Topic Pattern | `{team}.{entity}.{event-type}` | Scales, easy to consume, clear hierarchy |
| Message Envelope | Standard (eventId, timestamp, version, payload) | Idempotency, versioning, ordering |
| Message Key | `{entityType}:{entityId}` | Ordering per entity, stateful processing |
| Partitions | 3 | Balance parallelism vs. complexity for MVP |
| Replication | 1 | Local dev setup (scale to 3+ for production) |

### ⚠️ Questions for Team Sync

1. **Organization Validation on Register**
   - Should `registerUser()` call organization-service to validate org exists?
   - Current: No validation (accepts any orgId)
   - Recommendation: Add SYNC call to organization-service

2. **Booking Guard on Family Member Delete**
   - Should `removeFamilyMember()` check booking-service first?
   - Current: TODO comment; no check
   - Docs say: Should veto if active bookings exist
   - Recommendation: Add SYNC call to booking-service

3. **Dead Letter Queue (DLQ)**
   - Should failed events go to DLQ?
   - Current: Exceptions logged, consumer continues
   - Recommendation: Add for production (separate sprint)

4. **Schema Registry**
   - Should we adopt Confluent Schema Registry?
   - Current: Schema embedded in code (DTO classes)
   - When: After 5+ event types, when versioning needs are clear
   - Recommendation: Post-MVP feature

5. **Event Ordering Guarantees**
   - Do we need strict global ordering or per-entity ordering?
   - Current: Per-entity (partition by key)
   - Implication: UserRegistered may arrive before FamilyMemberAdded but within same user, order preserved
   - Recommendation: Acceptable; revisit if causality issues arise

---

## Part 6: Next Steps

### Immediate (Current Sprint) ✅
- [x] Composition query analysis documented
- [x] CQRS pattern validated and gaps filled
- [x] Kafka infrastructure fully implemented
- [x] All 4 event types producing to topics
- [x] Event listeners configured and ready
- [x] CommandService emitting events on all writes

### Short Term (Next 1-2 Sprints)
- [ ] Implement organization validation on user registration
- [ ] Implement booking guard on family member deletion
- [ ] Add integration tests for event publishing/consumption
- [ ] Document API contracts including async event subscriptions
- [ ] Add monitoring: event publish/consume latency, error rates

### Medium Term (Next 3-4 Sprints)
- [ ] Implement Dead Letter Queue for failed events
- [ ] Set up event replay capabilities for disaster recovery
- [ ] Consider read model denormalization in query service
- [ ] Evaluate schema registry adoption

### Long Term (Post-MVP)
- [ ] Multi-region Kafka deployment
- [ ] Event versioning and schema evolution
- [ ] Distributed tracing (correlation IDs in envelopes)
- [ ] Real-time analytics using event streams

---

## Part 7: Cross-Team Communication

### Information for Other Teams

**Notification Service:**
- Consume `identity.user.registered` to send welcome emails
- Consume `identity.user.phone_updated` if urgent contact needed
- Consume `identity.family_member.added` for member tracking notifications

**Organization Service:**
- Consume `identity.user.registered` to track organization members
- Produce `organization.members.updated` (if needed)

**Booking Service:**
- Consume `identity.family_member.added` to enable new bookings
- Consume `identity.family_member.removed` to cascade-cleanup bookings
- Should produce `booking.booking.cancelled` for identity-service consumption

**Payment Service:**
- Should produce `payment.payment.refunded` for identity-service consumption

---

## Appendix A: File Structure

```
identity-service/
├── COMPOSITION_CQRS_ANALYSIS.md              (Team analysis document)
├── IDENTITY_SERVICE_SUBMISSION.md            (This document)
├── pom.xml                                   (Updated: spring-kafka dependency)
├── src/main/resources/
│   └── application.yml                       (Updated: Kafka config)
└── src/main/java/com/holidayplanner/identityservice/
    ├── config/
    │   ├── SecurityConfig.java               (Existing)
    │   └── KafkaConfig.java                  (NEW: Kafka setup)
    ├── event/                                (NEW PACKAGE)
    │   ├── DomainEvent.java
    │   ├── UserRegisteredEvent.java
    │   ├── UserPhoneUpdatedEvent.java
    │   ├── FamilyMemberAddedEvent.java
    │   ├── FamilyMemberRemovedEvent.java
    │   ├── DomainEventPublisher.java
    │   └── IdentityEventListener.java
    ├── command/
    │   └── IdentityCommandService.java       (Updated: Event publishing)
    ├── query/
    │   └── IdentityQueryService.java         (Unchanged)
    ├── composition/
    │   └── IdentityCompositionService.java   (Unchanged)
    └── ...
```

---

## Appendix B: Testing Verification

To verify Kafka integration locally:

```bash
# 1. Ensure Kafka is running (docker compose up -d)
docker compose up -d kafka

# 2. Start identity-service
mvn spring-boot:run -pl identity-service

# 3. Register a user (triggers UserRegisteredEvent)
curl -X POST "http://localhost:8083/api/identity/users/register?email=test@example.com&password=pass123&phoneNumber=+43664123456&organizationId=<uuid>"

# 4. Listen to Kafka topic
kafka-console-consumer --bootstrap-server kafka:9092 --topic identity.user.registered --from-beginning --property print.key=true

# 5. Verify message received with:
# - eventType: "UserRegistered"
# - eventId: unique UUID
# - timestamp: ISO-8601
# - payload: contains userId, email, organizationId
```

---

## Conclusion

The Identity Service team has successfully implemented a **complete event-driven architecture** with:

- ✅ **Composition Queries** → Documented and implemented
- ✅ **CQRS Pattern** → Clean separation with event publishing
- ✅ **Kafka Integration** → Full producer/consumer infrastructure
- ✅ **Serialization** → JSON with standard envelope
- ✅ **Topic Convention** → System-wide naming pattern
- ✅ **Event Publishing** → All commands emit events
- ✅ **Event Consumption** → Ready for cross-service flows

**Status:** Ready for deployment to staging. Monitoring and additional guards (booking-service veto, org validation) recommended for follow-up sprints.

