# Pattern Evaluation for BookletService and NotificationService

## API Composition

I consider BookletService a good fit for API Composition because the booklet is a query-like document assembled from data owned by multiple services. The booklet needs organization contact data, sponsors, events, and event terms. In the current service split, this data belongs to OrganizationService and EventService, while BookletService owns the PDF generation.

With `GET /api/booklets/organizations/{organizationId}`, BookletService becomes the composer. It fetches the needed data from the owning services, combines it, sorts the event terms by date, and returns one PDF.

The request could contain all data needed for the PDF, but then the caller would become the composer. That would push the joining, sorting, and service knowledge into the frontend or another caller. I prefer one service-side composition flow so the booklet is generated consistently.

I do not consider NotificationService the best API Composition example. Notifications need rich data, but the service mainly reacts to events and sends emails. That is an asynchronous side effect, not a current combined view requested by a caller.

For NotificationService, I prefer enriched events. The service that detects the business event should publish the relevant notification facts, such as parent email, event name, term date, meeting point, payment method, and amount. NotificationService can then focus on templating and delivery.

My rule of thumb, that i established for myself while studying this subject, is: I use API Composition when a caller asks for something like a current combined view, and I use enriched events when a service announces that something happened and consumers need the facts of that event.

## CQRS

I do not consider the existing BookletService a strong CQRS candidate. It does not own meaningful mutable business state. It mainly renders a PDF from provided or composed data. Splitting it into commands and queries would mostly be artificial.

I also do not consider NotificationService a strong CQRS candidate. It sends emails and consumes notification events, but it does not currently have a rich read model or nontrivial query workload. Separating command and query models there would not clearly solve a real query problem.

That is why I add a small separate BookletRequestService as an example. It has command-side state for booklet print/distribution requests, emits internal events, and updates an in-memory query projection from those events. Query endpoints read only from that projection.

The example is intentionally small because it should focus on the architectural pattern: dedicated command and query models, event-maintained query views, idempotent projection updates, and the difference between changing state and reading an optimized view.
