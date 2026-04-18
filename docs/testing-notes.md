## 1. Was wurde getestet?

Der Hauptanwendungsfall von booking-service: **`createBooking`**

Ein User bucht einen EventTerm für ein FamilyMember. Das System entscheidet ob die Buchung **CONFIRMED** (Platz frei) oder **WAITLISTED** (ausgebucht) wird.

### Testebenen

| Ebene | Klasse | Zweck |
|---|---|---|
| **Unit Test** | `BookingServiceUnitTest` | Reine Business-Logik, alle Abhängigkeiten gemockt |
| **Integration Test** | `BookingServiceIntegrationTest` | Service + echte H2-Datenbank + gemockter IPC |
| **Component Test** | `BookingControllerComponentTest` | Kompletter HTTP-Stack als Black Box |
| **Contract Test (Provider)** | `BookingProviderContractTest` | API-Shape die booking-service nach außen liefert |
| **Contract Test (Consumer)** | `EventServiceConsumerContractTest` | Wie booking-service auf event-service antwortet |

---

## 2. Wie wurden Contracts getestet?

### Das Problem

booking-service kommuniziert mit event-service via HTTP:
- booking-service ruft `GET /api/events/terms/{id}` auf
- event-service antwortet mit `{ "status": "ACTIVE", "maxParticipants": 20, ... }`

Wenn event-service das Feld `maxParticipants` umbenennt (z.B. in `capacity`), bricht booking-service — **ohne Compilerfehler, ohne sofortige Warnung**.

### Lösung: Zwei Seiten des Contracts

#### Provider-Side Contract (`BookingProviderContractTest`)
booking-service ist hier der **Provider** (liefert API an andere Services).  
Der Test verifiziert, dass die Antwort immer exakt die erwarteten Felder enthält:

```java
mockMvc.perform(post("/api/bookings")...)
    .andExpect(jsonPath("$.id").isString())
    .andExpect(jsonPath("$.status").isString())
    .andExpect(jsonPath("$.familyMemberId").exists())
    .andExpect(jsonPath("$.bookedAt").isString());
```

→ Wenn jemand `bookedAt` umbenennt oder entfernt, schlägt dieser Test sofort fehl.

#### Consumer-Side Contract (`EventServiceConsumerContractTest`)
booking-service ist hier der **Consumer** (ruft event-service auf).  
WireMock simuliert event-service mit dem vereinbarten Response-Shape:

```java
wm.stubFor(get("/api/event-terms/" + id)
    .willReturn(okJson("""
        { "status": "ACTIVE", "maxParticipants": 20 }
    """)));

EventTermDetails details = eventServiceClient.getEventTerm(id);
assertThat(details.getMaxParticipants()).isEqualTo(20);
```

→ Wenn event-service `maxParticipants` zu `capacity` umbenennt, parst `EventTermDetails` 0 statt 20 — der Test schlägt fehl.

### Wie werden andere Teams informiert?

**Aktuell (manuell):** Die Contract Tests dokumentieren die erwartete API-Shape.  
Wenn event-service-Team die API ändert, müssen sie `EventServiceConsumerContractTest` in booking-service prüfen.

**Besser (automatisch, nicht implementiert):**  
Mit **Spring Cloud Contract** oder **Pact** würde das so funktionieren:
1. booking-service publiziert seinen Consumer-Contract als Datei
2. event-service lädt diesen Contract und führt ihn als Provider-Test aus
3. Falls event-service die API bricht → CI schlägt beim event-service-Build fehl, **bevor** der Code gemergt wird

---

## 3. Findings / Erkenntnisse

### Security-Problem gefunden
Das ursprüngliche `createBooking` akzeptierte `maxParticipants` als Query-Parameter vom Client:
```
POST /api/bookings?maxParticipants=9999
```
→ Jeder User konnte die Kapazität selbst setzen.  
**Fix:** Parameter entfernt — booking-service holt die Kapazität direkt von event-service.

### @PrePersist wird in Unit Tests nicht ausgeführt
`Booking.bookedAt` wird per `@PrePersist` gesetzt (beim JPA-Persist).  
In Unit Tests mit gemocktem Repository passiert kein echter JPA-Persist → `bookedAt` bleibt null.  
→ `@PrePersist` Verhalten nur in Integration Tests testbar (mit echter DB).

### Partial Response funktioniert
Wenn event-service nur die Pflichtfelder schickt (kein `startDate`, kein `eventId`), crasht booking-service nicht.  
Jackson ignoriert fehlende Felder standardmäßig.  
→ Explizit als Testfall dokumentiert: `createBooking_whenEventServiceReturnsPartialResponse_stillPersists`

### Kein `@Transactional` auf `cancelBooking`
`cancelBooking` macht 2 DB-Writes (Cancel + Promote).  
Wenn der zweite Write fehlschlägt, ist die Buchung gecancelt aber niemand wird promoted → inkonsistenter Zustand.  
→ Offen: `@Transactional` auf `cancelBooking` ergänzen.

### Race Condition bei cancelBooking
Zwei gleichzeitige Cancel-Requests könnten denselben Waitlisted-Booking promoten.  
→ Offen: `@Transactional` + Optimistic Locking oder `SELECT FOR UPDATE`.

---

## 4. Fragen für die Vorlesung

**Q1 — Contract Testing ohne Framework:**  
Unsere Consumer-Contract-Tests mit WireMock prüfen nur dass *wir* die Response korrekt parsen. Aber wenn event-service seine API ändert, laufen unsere Tests trotzdem grün (weil WireMock die alte Response liefert). Ist das noch ein "echter" Contract Test, oder nur ein Parsing-Test?

**Q2 — Wann Choreography vs. Orchestration?**  
Beim `cancelBooking`-Flow: booking-service cancelt, promoted, und müsste eigentlich auch NotificationService und PaymentService informieren. Ist hier Choreography (Events) besser als direkte Calls? Ab wann lohnt sich der Overhead einer Message Queue?

**Q3 — Integration Test mit H2 vs. echter PostgreSQL:**  
H2 und PostgreSQL verhalten sich in manchen Fällen unterschiedlich (z.B. UUID-Generierung, bestimmte SQL-Funktionen). Wie zuverlässig sind unsere Integration Tests mit H2, wenn Produktion auf PostgreSQL läuft? Wann braucht man Testcontainers?

**Q4 — Timeout-Handling:**  
Im Consumer-Contract-Test konfigurieren wir 500ms Timeout manuell auf dem RestClient. Wie sollte das in einem produktiven System konfiguriert werden? Pro Service? Global? Über einen Circuit Breaker (Resilience4j)?

**Q5 — IPC Security:**  
Aktuell gibt es keine Authentifizierung zwischen Services (booking-service ruft event-service ohne Token auf). Wie würde man Service-to-Service Authentication in Spring Boot umsetzen? JWT mit shared secret, mTLS, oder OAuth2 Client Credentials?
