# CQRS and Composition Queries — Identity Service

This document explains the CQRS (Command Query Responsibility Segregation) and Composition Query patterns implemented in the Identity Service, following the pattern established by booking-service.

---

## Overview

The Identity Service has been refactored to separate **write operations (commands)** from **read operations (queries)**. This separation follows the CQRS pattern and enables independent optimization of command and query paths.

### Key Principles

1. **Commands** modify state and are handled by `IdentityCommandService`
2. **Queries** retrieve data and are handled by `IdentityQueryService`
3. **Composition Queries** combine data from multiple services and are handled by `IdentityCompositionService`
4. All routing happens in `IdentityController`, which injects all three services

---

## Section 1: CQRS Implementation

### Why Identity Service Benefits from CQRS

**Characteristics that justify CQRS:**

- **Read-heavy workload**: User profiles, family member lists, and caregiver lookups are called frequently, especially during booking flows and profile views
- **Simple writes**: User registration, adding family members — these are basic CRUD operations without complex business logic
- **Independent optimization**: Queries can be optimized with caching or read replicas without affecting write paths
- **Future event publishing**: When we add Kafka events (e.g., `identity.user.registered`), commands are already isolated and ready

### Commands vs Queries

| Operation | Type | Service | Notes |
|---|---|---|---|
| `registerUser` | Command | `IdentityCommandService` | writes user, future: publishes `identity.user.registered` |
| `updatePhoneNumber` | Command | `IdentityCommandService` | modifies user state |
| `addFamilyMember` | Command | `IdentityCommandService` | creates family member record |
| `updateFamilyMember` | Command | `IdentityCommandService` | modifies family member state |
| `removeFamilyMember` | Command | `IdentityCommandService` | deletes family member record |
| `createCaregiver` | Command | `IdentityCommandService` | creates caregiver record |
| `getUserById` | Query | `IdentityQueryService` | read-only |
| `getFamilyMembers` | Query | `IdentityQueryService` | read-only, returns list |
| `getCaregiverById` | Query | `IdentityQueryService` | read-only |
| `getAllCaregivers` | Query | `IdentityQueryService` | read-only, returns list |
| `getUserProfileEnriched` | Composition | `IdentityCompositionService` | combines user + family members + booking counts |

### How the Separation is Implemented

#### A) `IdentityCommandService` (`command/IdentityCommandService.java`)

Contains all write operations:
- Depends on: `UserRepository`, `FamilyMemberRepository`, `CaregiverRepository`, `PasswordEncoder`
- Methods: `registerUser()`, `updatePhoneNumber()`, `addFamilyMember()`, `updateFamilyMember()`, `removeFamilyMember()`, `createCaregiver()`
- Responsibility: Modify database state only
- Future: Will publish Kafka events to trigger side effects in other services

#### B) `IdentityQueryService` (`query/IdentityQueryService.java`)

Contains all read operations:
- Depends on: `UserRepository`, `FamilyMemberRepository`, `CaregiverRepository`
- Methods: `getUserById()`, `getFamilyMembers()`, `getCaregiverById()`, `getAllCaregivers()`
- Responsibility: Retrieve data only, no state modifications
- Optimization potential: can add caching without affecting commands

#### C) `IdentityController`

Routes requests to appropriate services:
```java
// POST/PATCH/DELETE → Commands
@PostMapping("/users/register")
public ResponseEntity<User> register(...) {
    return ResponseEntity.ok(commandService.registerUser(...));
}

// GET → Queries
@GetMapping("/users/{userId}")
public ResponseEntity<User> getUser(...) {
    return ResponseEntity.ok(queryService.getUserById(...));
}

// GET (enriched) → Composition
@GetMapping("/users/{userId}/profile")
public ResponseEntity<UserProfileEnrichedResponse> getUserProfile(...) {
    return ResponseEntity.ok(compositionService.getUserProfileEnriched(...));
}
```

### Trade-offs

| Pro | Con |
|---|---|
| Command and query logic are easier to reason about independently | More classes to manage (3 services instead of 1) |
| Queries can be optimized (caching, read replicas) without touching write logic | Spring context now has 3 service beans instead of 1 |
| Future event publishing is isolated to commands only | Slightly more complex to understand request routing |
| Cleaner test boundaries: command tests don't need query mocks | The shared `UserRepository` means reads and writes still use the same physical DB |

---

## Section 2: Composition Queries

### What is a Composition Query?

A composition query combines data from **multiple services** into a single enriched response, so the frontend only needs to make **one HTTP call** instead of several.

### Composition Query: Enriched User Profile

**Endpoint:** `GET /api/identity/users/{userId}/profile`

**What It Does:**
Returns a complete user profile including family members and their active booking status.

**Data Sources:**

| Field | Source | Service |
|---|---|---|
| `id`, `email`, `phoneNumber`, `organizationId`, `role` | User table | identity-service DB |
| Family member: `firstName`, `lastName`, `birthDate`, `zip` | FamilyMember table | identity-service DB |
| Family member: `activeBookingCount` | Booking counts per status | booking-service (via HTTP) |

**Response Example:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "parent@example.com",
  "phoneNumber": "+45 12 34 56 78",
  "organizationId": "550e8400-e29b-41d4-a716-446655440001",
  "role": "USER",
  "familyMembers": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440002",
      "firstName": "Alice",
      "lastName": "Johnson",
      "birthDate": "2010-05-15",
      "zip": "2100",
      "activeBookingCount": 2
    },
    {
      "id": "550e8400-e29b-41d4-a716-446655440003",
      "firstName": "Bob",
      "lastName": "Johnson",
      "birthDate": "2012-08-22",
      "zip": "2100",
      "activeBookingCount": 0
    }
  ]
}
```

### Why This Composition Query Is Valuable

**Before Composition (3 API calls):**
1. `GET /api/identity/users/{userId}` → user profile
2. `GET /api/identity/users/{userId}/family-members` → list of family members
3. For each family member: `GET /api/bookings/family-member/{memberId}` → booking list

Frontend code: 
```javascript
// Frontend makes multiple calls and assembles the data
const user = await getUser(userId);
const familyMembers = await getFamilyMembers(userId);
const enrichedMembers = [];
for (const member of familyMembers) {
  const bookings = await getBookings(member.id);
  enrichedMembers.push({...member, bookingCount: bookings.length});
}
```

**With Composition (1 API call):**
```
GET /api/identity/users/{userId}/profile → complete profile with booking counts
```

Frontend code:
```javascript
// Frontend makes one call, gets everything
const profile = await getUserProfile(userId);
// profile.familyMembers already has activeBookingCount
```

**Benefits:**
- ✅ Fewer API calls (1 instead of 2+N)
- ✅ Reduced network latency
- ✅ Simpler frontend logic
- ✅ One place to handle failures from booking-service

### How Failures Are Handled

`IdentityCompositionService.getUserProfileEnriched()` implements **graceful degradation**:

```java
List<FamilyMemberWithBookingsResponse> enriched = familyMembers.stream()
    .map(member -> {
        try {
            long bookingCount = bookingServiceClient.getActiveBookingCount(member.getId());
            return FamilyMemberWithBookingsResponse.from(member, bookingCount);
        } catch (Exception e) {
            log.warn("Could not fetch booking count for family member {}", member.getId(), e);
            // Return with booking count = 0 (graceful degradation)
            return FamilyMemberWithBookingsResponse.from(member, 0L);
        }
    })
    .collect(Collectors.toList());
```

**If booking-service is unavailable:**
- Family members are still returned ✅
- `activeBookingCount` is set to 0 for affected members
- A WARN log records the issue
- User sees their family members but booking count is incomplete
- Better than returning 503 Service Unavailable

### Trade-offs

| Pro | Con |
|---|---|
| One HTTP call for the client | Adds latency: one HTTP call to booking-service per family member (N+1 problem if N is large) |
| Cleaner frontend code | identity-service now has a runtime dependency on booking-service for read paths |
| Failure is graceful and isolated per family member | If booking-service is slow, the entire profile request is slow |
| Single point of error handling for composition | Adds complexity to identity-service |

---

## Section 3: Implementation Details

### IdentityCompositionService

**File:** `composition/IdentityCompositionService.java`

**Key Method:**
```java
public UserProfileEnrichedResponse getUserProfileEnriched(UUID userId)
```

**Process:**
1. Fetch user by ID (from `IdentityQueryService`)
2. Fetch family members (from `IdentityQueryService`)
3. For each family member:
   - Call `BookingServiceClient.getActiveBookingCount(familyMemberId)`
   - Wrap result in `FamilyMemberWithBookingsResponse`
   - If booking-service fails, return count=0 and log warning
4. Bundle all data in `UserProfileEnrichedResponse` and return

### BookingServiceClient

**File:** `client/BookingServiceClient.java`

**Responsibility:** HTTP communication with booking-service

**Method:**
```java
public long getActiveBookingCount(UUID familyMemberId)
```

**Behavior:**
- Calls `GET /api/bookings/family-member/{familyMemberId}` on booking-service
- Returns the array length (number of bookings)
- If booking-service is unavailable, catches the exception and returns 0
- Logs all failures at WARN level for monitoring

**Configuration:**
```yaml
services:
  booking-service:
    url: http://localhost:8082  # development
    # Docker: http://booking-service:8082
```

### DTOs

**FamilyMemberWithBookingsResponse:**
```java
{
  "id": UUID,
  "firstName": String,
  "lastName": String,
  "birthDate": LocalDate,
  "zip": String,
  "activeBookingCount": long
}
```

**UserProfileEnrichedResponse:**
```java
{
  "id": UUID,
  "email": String,
  "phoneNumber": String,
  "organizationId": UUID,
  "role": UserRole,
  "familyMembers": List<FamilyMemberWithBookingsResponse>
}
```

---

## Section 4: Testing

### Unit Tests for Services

**IdentityCommandService:**
- Test user registration (success, duplicate email failure)
- Test phone number update
- Test family member operations (add, update, delete)
- Test caregiver creation
- Mock repositories

**IdentityQueryService:**
- Test getting user by ID (found, not found)
- Test getting family members (empty, multiple)
- Test getting caregivers (found, all)
- Mock repositories

**IdentityCompositionService:**
- Test `getUserProfileEnriched()` success case
- Test graceful degradation when booking-service fails
- Mock `IdentityQueryService` and `BookingServiceClient`

### Integration Tests for Controller

- Test POST `/users/register` → commandService
- Test GET `/users/{userId}` → queryService
- Test GET `/users/{userId}/profile` → compositionService (with booking count enrichment)
- Test PATCH `/users/{userId}/phone` → commandService
- etc.

---

## Section 5: Configuration

### Local Development

**application.yml:**
```yaml
services:
  booking-service:
    url: http://localhost:8082
```

Both services run locally on their default ports.

### Docker Deployment

**docker/application-identity.yml:**
```yaml
services:
  booking-service:
    url: http://booking-service:8082
```

In Docker Compose, services communicate via internal network using service names.

---

## Section 6: Future Enhancements

### Optimization 1: Caching Booking Counts

**Problem:** Calling booking-service for every family member adds latency.

**Solution:** Cache booking counts with TTL (e.g., 5 minutes):
```java
@Cacheable(value = "bookingCounts", key = "#familyMemberId", cacheManager = "cacheManager")
public long getActiveBookingCount(UUID familyMemberId) { ... }
```

**Risk:** Stale data if bookings change between cache updates. 5-minute TTL is acceptable since user profiles aren't updated in real-time.

### Optimization 2: Kafka Event Publishing

**Future:** When user registers, publish event so other services can react:
```
identity.user.registered → {userId, email, organizationId, timestamp}
```

Services that might subscribe:
- organization-service: confirm user membership
- payment-service: initialize account ledger
- notification-service: send welcome email

**Implementation:** Add `IdentityEventProducer` to `IdentityCommandService`, publish on `registerUser()`.

### Optimization 3: Batch Booking Query

**Problem:** If user has many family members, N calls to booking-service (N+1 problem).

**Solution:** Add batch endpoint to booking-service:
```
POST /api/bookings/family-members/batch
{
  "familyMemberIds": ["id1", "id2", "id3"]
}
```

Returns booking counts for all members in one call.

**Implementation:** Update `BookingServiceClient` to collect distinct family member IDs and call batch endpoint.

### Optimization 4: Separate Read Model

**Full CQRS:** Maintain a denormalized read model (PostgreSQL table or Elasticsearch index) updated via Kafka events:
- `UserProfileCache` table: stores pre-joined user + booking counts
- Updated whenever `identity.user.registered` or booking events are published
- Queries read from cache, no cross-service calls

**Benefit:** Queries are extremely fast, no dependency on booking-service availability.

**Cost:** Added operational complexity, eventual consistency.

---

## Section 7: Open Questions

1. **Should we add a validation check before removing family members?**
   Currently, `removeFamilyMember()` deletes without checking if the family member has active bookings. Safe approach: call booking-service with `hasActiveBookings(memberId)`, reject deletion if true. This prevents orphaned bookings.

2. **Should user registration publish a Kafka event?**
   Today: synchronous, no side effects. Future: `identity.user.registered` event could trigger organization service confirmation, payment account creation, welcome emails.

3. **Should booking counts be cached?**
   If a user has many family members (e.g., 20 children in a large family org), we'd make 20 HTTP calls to booking-service. Cache with 5-minute TTL would reduce to 1 call per distinct event term.

4. **Who owns composition — services or API Gateway?**
   Current: Composition is owned by identity-service. Alternative: Centralize in an API Gateway or BFF (Backend for Frontend) to keep individual services simpler and add one place for batching/caching logic.

5. **Should we batch booking queries instead of per-member?**
   Instead of calling booking-service for each family member, collect all family member IDs and call booking-service once with a batch query. Requires booking-service to implement batch endpoint.

---

## Section 8: Comparison with Booking Service

The identity-service implementation follows the exact pattern established by booking-service:

| Aspect | Booking Service | Identity Service |
|---|---|---|
| Command Service | `BookingCommandService` | `IdentityCommandService` |
| Query Service | `BookingQueryService` | `IdentityQueryService` |
| Composition Service | (queries handle it) | `IdentityCompositionService` |
| External Client | `EventServiceClient` | `BookingServiceClient` |
| Composition Queries | 2 (enriched bookings, term summary) | 1 (enriched profile) |
| Graceful Degradation | Yes (null fields if event-service fails) | Yes (booking count = 0 if booking-service fails) |
| Event Publishing | Yes (Kafka events) | Ready (TODO in code) |

---

## Summary

The Identity Service now implements:

✅ **CQRS Pattern**
- Commands: `IdentityCommandService` (writes, future events)
- Queries: `IdentityQueryService` (reads)
- Composition: `IdentityCompositionService` (enriched reads with cross-service data)

✅ **Composition Query**
- `GET /api/identity/users/{userId}/profile` — returns user + family members + booking counts

✅ **Inter-service Communication**
- `BookingServiceClient` calls booking-service safely with graceful degradation

✅ **Configuration**
- Local dev: `http://localhost:8082`
- Docker: `http://booking-service:8082`

✅ **Foundation for Future Enhancements**
- Caching, Kafka events, batch queries, dedicated read models

---

## References

- [CQRS Pattern (Microsoft Docs)](https://docs.microsoft.com/en-us/azure/architecture/patterns/cqrs)
- Booking Service implementation: `booking-service/src/main/java/com/holidayplanner/bookingservice/`
- Domain model: `docs/domain-model.md`
- System operations: `docs/system-operations.md`
