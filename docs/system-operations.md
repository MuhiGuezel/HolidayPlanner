# System Operations

This document contains the unified system operations for all services in the Holiday Planner system.

---

## EventManagementService

| Method Signature | Actor | Description | Pre-Conditions | Post-Conditions | Collaborators |
|---|---|---|---|---|---|
| `Event updateEvent(eventId, shortTitle, description, location, meetingPoint, price, paymentMethod, minimalAge, maximalAge, picture)` | Event Owner | Edits the details of an event | Event exists and actor is the owner | Event details are updated | — |
| `EventTerm createEventTerm(eventId, startDate, endDate, minimalParticipants, maximalParticipants)` | Event Owner | Creates an event term | Event exists and actor is the owner | Event term created with status DRAFT | — |
| `EventTerm changeEventTermStatus(eventTermId, newStatus)` | Event Owner | Changes the status of an event term | Event term exists and actor is the owner | Status updated to ACTIVE or CANCELLED. If CANCELLED, all bookings are cancelled and participants/caregivers notified | `BookingService::cancelAllBookings(eventTermId)`, `NotificationService::notifyTermCancelled(...)` |
| `EventTerm updateEventTermCapacity(eventTermId, minimalParticipants, maximalParticipants)` | Event Owner | Changes the min/max participants for an event term | Event term exists and actor is the owner. Max cannot be decreased below confirmed bookings | Capacity updated. If max increased, waiting list entries promoted to confirmed. Parents notified | `BookingService::getBookingCount(eventTermId)`, `BookingService::promoteFromWaitingList(eventTermId, addedCapacity)`, `NotificationService::notifyBookingConfirmed(...)` |
| `Remark createRemark(eventTermId, participantId, description)` | Event Owner | Creates a remark about a participant | Participant and event term exist and actor is the owner | Remark created and visible to all event owners across all event terms booked by the participant | — |
| `Remark[] getRemarks(eventTermId)` | Event Owner | Retrieves remarks about all participants for an event term | Event term exists and actor is the owner | All remarks for participants of the event term are returned | — |
| `EventTerm assignCaregiverToEventTerm(eventTermId, caregiverId)` | Event Owner | Assigns a caregiver to an event term | Event term and caregiver exist and actor is the owner | Caregiver assigned to the event term | — |
| `void sendMessageToParticipants(eventTermId, message)` | Event Owner | Sends emails to participants of an event term | Event exists and actor is the owner | Email sent to all participants | `BookingService::getParticipantContactEmails(eventTermId)`, `NotificationService::sendBulkEmail(...)` |

---

## BookingService

| Method Signature | Actor | Description | Pre-Conditions | Post-Conditions | Collaborators |
|---|---|---|---|---|---|
| `Booking[] getBookingsForEventTerm(eventTermId)` | Event Owner | Retrieves all bookings for a given event term | Event term exists and actor is the owner | List of confirmed bookings and waiting list entries returned | `EventManagementService::verifyEventTerm(eventTermId)` |
| `Booking cancelBooking(bookingId)` | Event Owner | Cancels a booking of a participant | Booking exists and actor is the owner of the associated term | Booking cancelled. Next participant on waiting list moves up. Cancelled participant notified | `BookingService::promoteFromWaitingList(eventTermId, addedCapacity)`, `NotificationService::notifyBookingConfirmed(...)` |
| `Booking createBooking(familyMemberId, eventTermId)` | User | Books an event term for a family member | Event term is ACTIVE and family member matches age requirements | Booking created with status CONFIRMED or WAITLISTED if full | — |
| `void cancelBookingByUser(bookingId)` | User | Cancels a booking (up to 3 days before event) | Booking exists and cancellation deadline not passed | Booking cancelled. Next on waiting list promoted | `NotificationService::notifyBookingConfirmed(...)` |

---

## IdentityService

| Method Signature | Actor | Description | Pre-Conditions | Post-Conditions | Collaborators |
|---|---|---|---|---|---|
| `Caregiver createCaregiver(firstName, lastName, email, phoneNumber)` | Event Owner | Adds a caregiver to the system | Caregiver does not already exist | New caregiver profile created | — |
| `User registerUser(email, password, phoneNumber, organizationId)` | User | Registers a new user for an organization | Email not already in use | New user account created | — |
| `FamilyMember addFamilyMember(userId, firstName, lastName, birthDate, zip)` | User | Adds a family member to a user's profile | User exists | Family member added to profile | — |
| `FamilyMember updateFamilyMember(memberId, firstName, lastName, birthDate, zip)` | User | Updates a family member's details | Family member exists and belongs to user | Family member updated | — |
| `void removeFamilyMember(memberId)` | User | Removes a family member | No active bookings exist for this family member | Family member removed | `BookingService::hasActiveBookings(memberId)` |

---

## OrganizationService

| Method Signature | Actor | Description | Pre-Conditions | Post-Conditions | Collaborators |
|---|---|---|---|---|---|
| `Organization createOrganization(name, bankAccount, bookingStartTime)` | Admin | Creates a new organization | Organization name not already taken | New organization created | — |
| `Organization updateOrganization(organizationId, bankAccount, bookingStartTime)` | Org Team | Updates organization settings | Organization exists | Organization updated | — |
| `TeamMember addTeamMember(organizationId, userId, firstName, lastName, email, role)` | Org Team | Adds a team member to the organization | Organization exists | Team member added | — |
| `void removeTeamMember(teamMemberId)` | Org Team | Removes a team member | Team member exists | Team member removed | — |
| `Sponsor addSponsor(organizationId, name, amount)` | Org Team | Adds a sponsor to the organization | Organization exists | Sponsor added | — |

---

## PaymentService

| Method Signature | Actor | Description | Pre-Conditions | Post-Conditions | Collaborators |
|---|---|---|---|---|---|
| `Payment createPayment(bookingId, organizationId, amount)` | System | Creates a payment record for a booking | Booking exists | Payment created with status PENDING | — |
| `Payment markAsPaid(paymentId, note)` | Accountant | Marks a payment as paid | Payment exists and status is PENDING | Payment status set to PAID | — |
| `Payment refundPayment(paymentId, note)` | Accountant | Refunds a payment for a cancelled event | Payment exists and status is PAID | Payment status set to REFUNDED | `NotificationService::notifyRefund(...)` |
| `BigDecimal calculateBalance(organizationId)` | Accountant | Calculates the total balance for an organization | Organization exists | Balance returned (income - costs) | — |

---

## NotificationService

| Method Signature | Actor | Description | Pre-Conditions | Post-Conditions | Collaborators |
|---|---|---|---|---|---|
| `void sendEmail(to, subject, body)` | System | Sends a single email | Valid email address | Email sent | — |
| `void sendBulkEmail(recipients, subject, body)` | System | Sends email to multiple recipients | Valid list of emails | Emails sent to all recipients | — |
| `void notifyBookingConfirmed(parentEmail, eventName, termDate)` | System | Notifies parent of confirmed booking | — | Confirmation email sent | — |
| `void notifyTermCancelled(parentEmail, eventName, termDate)` | System | Notifies parent of cancelled event term | — | Cancellation email sent | — |
| `void notifyCaregiverWithParticipants(caregiverEmail, eventName, termDate, participants)` | System | Sends participant list PDF to caregiver one day before event | — | Email with participant list sent | `BookletService::generateParticipantListPdf(...)` |

---

## BookletService

| Method Signature | Actor | Description | Pre-Conditions | Post-Conditions | Collaborators |
|---|---|---|---|---|---|
| `byte[] generateBooklet(organizationName, contactInfo, eventSummaries, sponsorNames)` | Org Team | Generates a PDF booklet for the organization | Organization exists with active events | PDF booklet generated and returned | — |
| `byte[] generateParticipantListPdf(eventName, termDate, participantNames)` | System | Generates a participant list PDF for a caregiver | Event term exists with confirmed participants | PDF generated and returned | — |
