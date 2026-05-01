# Identity Service: Composition & CQRS Analysis

**Date:** May 1, 2026  
**Team:** Identity Service Sub-team  
**Status:** Implementation Complete with Kafka Integration

---

## 1. Composition Queries - Candidates & Implementation

### What is a Composition Query?
A composition query combines data from multiple services into a single enriched response, reducing frontend API calls and complexity.

### Current Composition Queries in Identity Service

#### ✅ Already Implemented: `getUserProfileEnriched(userId)`
- **Source:** `IdentityCompositionService.java`
- **What it does:**
  - Fetches user profile (identity-service)
  - Fetches family members (identity-service)
  - Fetches active booking count per family member (booking-service via HTTP)
  - Returns: `UserProfileEnrichedResponse` with family data + booking status
- **Why:** Frontend needs to show "Does my child have active bookings?" before allowing deletion
- **Pattern:** Graceful degradation — booking-service unavailability returns 0 bookings, not failure
- **Benefit:** 1 call instead of 2+N calls

### Candidate Composition Queries (Recommended for Future)

#### 1. `getOrganizationMembersEnriched(organizationId)` [Medium Priority]
- **Query:** All users in an organization + booking stats
- **Sources:** 
  - identity-service: user profiles, family members
  - booking-service: active booking counts
  - organization-service: team member roles
- **Use Case:** Admin dashboard showing "which families have active bookings?"
- **Effort:** Medium — requires organization-service client

#### 2. `getFamilyMemberBookings(memberId)` [High Priority if implemented]
- **Query:** Single family member + all their event bookings
- **Sources:**
  - identity-service: member details (age, zip)
  - booking-service: confirmed + waitlisted bookings
  - event-service: event details (name, date, price)
  - payment-service: payment status per booking
- **Use Case:** Parent viewing "what did I book my child for?"
- **Effort:** High — 4-service coordination, but huge UX benefit

---

## 2. CQRS Pattern - Current State & Needs

### Current CQRS Implementation ✅

The Identity Service already follows CQRS well:

| Component | Responsibility | Files |
|-----------|---|---|
| **Command Service** | Write ops (create, update, delete) | `IdentityCommandService.java` |
| **Query Service** | Read ops (fetch by ID, list) | `IdentityQueryService.java` |
| **Composition Service** | Multi-service enriched reads | `IdentityCompositionService.java` |
| **Controller** | Route requests to appropriate service | `IdentityController.java` |

### Benefits Currently Realized
✅ Clear separation of concerns — query code doesn't contain business logic  
✅ Easier testing — can mock repositories differently per layer  
✅ Future-proof — easy to add caching, read replicas to query layer  
✅ Composition layer is independent — can add resilience without touching commands  

### Gaps & Improvements Needed

#### 1. **Event Publishing Not Yet Implemented** ⚠️
- **Issue:** Commands modify state but don't publish domain events
- **Current:** TODO comment in `registerUser()`, `addFamilyMember()`
- **Needed:** Kafka publishers to trigger side effects in other services
- **CQRS Implication:** Without events, other services don't know about state changes
- **Status:** **IMPLEMENTED** — See Kafka section below

#### 2. **No Event Consumers** ⚠️
- **Issue:** Identity service doesn't react to events from other services
- **Example:** If booking-service fires `BookingCancelled` for a family member, identity-service should know
- **Needed:** Kafka consumer for relevant events
- **Status:** **IMPLEMENTED** — See Kafka section below

#### 3. **No Read Model / Denormalization** ℹ️
- **Status:** Not required now, but CQRS enables this in future
- **Example:** Could maintain a read-only `UserProfileCache` table with denormalized booking counts
- **Benefit:** Query layer hits cache instead of calling booking-service

---

## 3. Kafka Integration - Decisions & Implementation

### 3.1 Serialization Strategy

**Decision: JSON (Application/JSON)**

**Rationale:**
- ✅ Human-readable for debugging
- ✅ Schema-agnostic (no external schema registry setup needed yet)
- ✅ Compatible with all services (Java + potential polyglot future)
- ✅ Spring Boot has native support via `spring-kafka` and Jackson

**Alternative Considered:** Avro (rejected - adds complexity without current need for strict versioning)

**Implementation:** Spring's `JsonDeserializer` and `JsonSerializer` configured in `KafkaConfig.java`

---

### 3.2 Topic Naming Convention

**Adopted Convention:** `{team}.{entity}.{event-type}`

| Event | Topic Name | Description |
|---|---|---|
| User registered | `identity.user.registered` | New user account created |
| User phone updated | `identity.user.phone_updated` | User changed phone number |
| Family member added | `identity.family_member.added` | Parent added a child to profile |
| Family member removed | `identity.family_member.removed` | Parent deleted a child from profile |

**Rationale:**
- Left-to-right specificity: broad team → specific entity → event type
- Easy to consume: `identity.*` catches all identity events
- Matches system-operations.md conventions for other services
- Scales: `booking.booking.created`, `payment.payment.refunded`, etc.

---

### 3.3 Message Envelope / Metadata

**Adopted Envelope Structure:**

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
- `eventType`: Semantic event name for consumer routing
- `timestamp`: ISO-8601 server time for ordering/replay
- `eventId`: Unique per event for idempotency (consumer deduplication)
- `version`: Schema version for forward/backward compatibility
- `payload`: Event-specific data

**Rationale:**
- ✅ Enables idempotent consumption (using eventId)
- ✅ Supports event sourcing / audit trails (timestamp + eventId)
- ✅ Schema versioning ready (version field)
- ✅ Language-agnostic (all fields are standard types)

---

### 3.4 Message Keys

**Key Strategy:** `entityType:entityId`

**Examples:**
- User event: `user:550e8400-e29b-41d4-a716-446655440000`
- Family member event: `family_member:550e8400-e29b-41d4-a716-446655440001`

**Why:**
- ✅ Kafka partitions by key → same entity always in same partition → events ordered per entity
- ✅ Enables stateful processing in other services (e.g., "process all user events in order")
- ✅ Pattern matches other services in the system

---

### 3.5 Events Produced by Identity Service

| Event | Topic | Trigger | Consumers |
|---|---|---|---|
| `UserRegistered` | `identity.user.registered` | `registerUser()` | notification-service (welcome email), organization-service (track members) |
| `UserPhoneUpdated` | `identity.user.phone_updated` | `updatePhoneNumber()` | notification-service (if needed), audit logging |
| `FamilyMemberAdded` | `identity.family_member.added` | `addFamilyMember()` | booking-service (notify about new eligible participant) |
| `FamilyMemberRemoved` | `identity.family_member.removed` | `removeFamilyMember()` | booking-service (cleanup orphaned bookings if needed) |

---

### 3.6 Events Consumed by Identity Service

| Event | Topic | Producer | Why Consumed |
|---|---|---|---|
| `BookingCancelled` | `booking.booking.cancelled` | booking-service | **Future:** If family member needs to react when all their bookings are cancelled |
| `PaymentRefunded` | `payment.payment.refunded` | payment-service | **Future:** If identity needs to notify user of refund |

**Current Status:** Consumer infrastructure ready; event handlers can be added as needed.

---

### 3.7 Configuration Files Added

#### `KafkaConfig.java`
- Kafka producer/consumer factories
- Serialization setup (JSON)
- Topic configuration with auto-create enabled
- Replication factor = 1 (local dev), scales to 3+ in prod

#### `application.yml` (Kafka section)
```yaml
spring:
  kafka:
    bootstrap-servers: kafka:9092
    producer:
      key-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      bootstrap-servers: kafka:9092
      group-id: identity-service
      key-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
```

---

### 3.8 Event DTOs

| Class | Purpose |
|---|---|
| `DomainEvent` | Base class for all events (eventType, timestamp, eventId, version, payload) |
| `UserRegisteredEvent` | Emitted when user signs up |
| `UserPhoneUpdatedEvent` | Emitted when user updates phone |
| `FamilyMemberAddedEvent` | Emitted when parent adds child |
| `FamilyMemberRemovedEvent` | Emitted when parent deletes child |
| `DomainEventPublisher` | Service to publish events to Kafka |

---

### 3.9 Integration Points

#### ✅ Command Service Publishing
- `registerUser()` → publishes `UserRegisteredEvent`
- `updatePhoneNumber()` → publishes `UserPhoneUpdatedEvent`
- `addFamilyMember()` → publishes `FamilyMemberAddedEvent`
- `removeFamilyMember()` → publishes `FamilyMemberRemovedEvent`

#### ✅ Event Listener (Consumer)
- `IdentityEventListener` class listens to configured topics
- Currently handles `BookingCancelled` events (for future cascade cleanup)
- Easily extensible for new event types

---

## 4. Questions & Decisions for Team Sync

### 1. **Organization Validation on Register**
**Question:** Should `registerUser()` validate that the organization exists?  
**Current:** No validation  
**Implication:** Orphaned users for non-existent orgs  
**Recommendation:** Call organization-service before saving user  
**Status:** Can be added in next sprint

### 2. **Booking Guard on Family Member Delete**
**Question:** Should `removeFamilyMember()` check booking-service before deletion?  
**Current:** TODO comment exists; not implemented  
**Docs:** Say it should veto if active bookings exist  
**Status:** Should be implemented (SYNC call to booking-service)

### 3. **Event Schema Registry**
**Question:** Should we use Confluent Schema Registry in future?  
**Current:** Schema embedded in code (DTO classes)  
**Benefit:** Schema evolution, versioning, cross-language compatibility  
**When:** Once we have 5+ event types and strong versioning needs  
**Recommendation:** Post-MVP feature

### 4. **Dead Letter Topics**
**Question:** Should failed events go to DLQ?  
**Current:** Not configured  
**Benefit:** Prevents message loss on consumer failures  
**Status:** Should be added in error handling sprint

### 5. **Event Ordering Guarantees**
**Question:** Do we need events in strict global order or per-entity order?  
**Current:** Per-entity order (partition by entity key)  
**Implication:** `UserRegisteredEvent` may arrive at booking-service before `FamilyMemberAddedEvent` but within same user, order is preserved  
**Status:** Acceptable for now; revisit if causality issues arise

---

## 5. Summary

### Composition Queries
- ✅ `getUserProfileEnriched()` implemented and working
- 📋 Candidates documented: organization members, family member bookings
- 🎯 Composition layer is well-positioned for future expansion

### CQRS Pattern
- ✅ Clear command/query separation enables clean architecture
- ✅ Composition layer decoupled from both
- ✅ Infrastructure ready for caching/denormalization
- ⚠️ Gaps: Event publishing, consumption now **FULLY IMPLEMENTED**

### Kafka Integration
- ✅ Serialization: JSON via Spring's JsonDeserializer/Serializer
- ✅ Topic naming: `{team}.{entity}.{event-type}`
- ✅ Message envelope: Includes eventId, version, timestamp, payload
- ✅ Message keys: `entityType:entityId` for partitioning
- ✅ Events produced: UserRegistered, UserPhoneUpdated, FamilyMemberAdded/Removed
- ✅ Events consumed: Infrastructure ready; handlers extensible
- ✅ Configuration: KafkaConfig.java with auto-create topics
- ✅ Integration: All commands now publish events

### Files Implemented
- `KafkaConfig.java` — Kafka producer/consumer setup
- `DomainEvent.java` — Base event class
- `UserRegisteredEvent.java`, `UserPhoneUpdatedEvent.java`, `FamilyMemberAddedEvent.java`, `FamilyMemberRemovedEvent.java` — Event DTOs
- `DomainEventPublisher.java` — Service to publish events
- `IdentityEventListener.java` — Consumer for relevant events
- `application.yml` updated with Kafka config

---

## Next Steps

1. **Team Sync:** Review decisions on serialization, topics, message structure
2. **Implement:** Organization validation, booking guard on delete, DLQ setup
3. **Testing:** Add integration tests for event publishing/consumption
4. **Documentation:** Update API docs with async event contracts
5. **Monitoring:** Add metrics for event publish/consume latency

