# Identity Service: Composition & CQRS Analysis


## Composition Queries


### Current Composition Queries in Identity Service

#### Implemented: `getUserProfileEnriched(userId)`
- **Source:** `IdentityCompositionService.java`
- **What it does:**
  - Fetches user profile (identity-service)
  - Fetches family members (identity-service)
  - Fetches active booking count per family member (booking-service via HTTP)
  - Returns: `UserProfileEnrichedResponse` with family data + booking status
- **Why:** Frontend needs to show "Does my child have active bookings?" before allowing deletion
- **Pattern:** Graceful degradation — booking-service unavailability returns 0 bookings, not failure
- **Benefit:** 1 call instead of 2+N calls

### Candidate Composition Queries

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

##  CQRS Pattern

### Current CQRS Implementation

The Identity Service follows CQRS like this:

| Component | Responsibility | Files |
|-----------|---|---|
| **Command Service** | Write ops (create, update, delete) | `IdentityCommandService.java` |
| **Query Service** | Read ops (fetch by ID, list) | `IdentityQueryService.java` |
| **Composition Service** | Multi-service enriched reads | `IdentityCompositionService.java` |
| **Controller** | Route requests to appropriate service | `IdentityController.java` |

 


#### No Read Model / Denormalization
- **Status:** Not required now, but CQRS enables this in future
- **Example:** Could maintain a read-only `UserProfileCache` table with denormalized booking counts
- **Benefit:** Query layer hits cache instead of calling booking-service

---

## Questions & Decisions for Team Sync ideas

### 1. Organization Validation on Register
**Question:** Should `registerUser()` validate that the organization exists?  
**Current:** No validation  
**Implication:** Orphaned users for non-existent orgs  
**Recommendation:** Call organization-service before saving user  
**Status:** Can be added in next sprint

### 2. Booking Guard on Family Member Delete
**Question:** Should `removeFamilyMember()` check booking-service before deletion?  
**Current:** TODO comment exists; not implemented  
**Docs:** Say it should veto if active bookings exist  
**Status:** Should be implemented (SYNC call to booking-service)

### 3. Event Schema Registry
**Question:** Should we use Confluent Schema Registry in future?  
**Current:** Schema embedded in code (DTO classes)  
**Benefit:** Schema evolution, versioning, cross-language compatibility  
**When:** Once we have 5+ event types and strong versioning needs  
**Recommendation:** Post-MVP feature

### 4.*Dead Letter Topics
**Question:** Should failed events go to DLQ?  
**Current:** Not configured  
**Benefit:** Prevents message loss on consumer failures  
**Status:** Should be added in error handling sprint

### 5. Event Ordering Guarantees
**Question:** Do we need events in strict global order or per-entity order?  
**Current:** Per-entity order (partition by entity key)  
**Implication:** `UserRegisteredEvent` may arrive at booking-service before `FamilyMemberAddedEvent` but within same user, order is preserved  
**Status:** Acceptable for now; revisit if causality issues arise


