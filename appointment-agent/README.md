# AI Appointment Booking Agent — Phase 1 (Steps 1 & 2)

Implements the DB schema and Spring Boot backend from the Phase 1 plan, with the
hardening fixes noted in review (partial unique index against double-booking,
pessimistic locking, idempotency key support, session state constraints).

## What's here vs. what's next

| Doc's Step | Status |
|---|---|
| 1. PostgreSQL schema | ✅ `src/main/resources/schema.sql` |
| 2. Spring Boot APIs (services/doctors/availability/appointments) | ✅ implemented |
| 3. Test without AI (Postman) | ✅ see below |
| 4. n8n workflow | ⬜ next — outline below |
| 5. Add AI (LLM intent extraction) | ⬜ next |
| 6. Connect WhatsApp | ⬜ next |
| 7. End-to-end test | ⬜ next |

## Run it locally

```bash
createdb appointment_agent
psql -d appointment_agent -f src/main/resources/schema.sql   # includes seed data
export DB_USERNAME=appointment_user DB_PASSWORD=changeme     # or edit application.yml directly
mvn spring-boot:run
```

(This project wasn't compiled in the sandbox that generated it — no Maven Central
access there — so build it locally first and fix anything version-specific to
your Java/Maven setup before wiring up n8n.)

## Step 3 — Postman test sequence (matches doc section 23, Step 3)

1. `GET /api/clinics/1/services` → confirm Dental Consultation (30 min) and Teeth Cleaning appear.
2. `GET /api/clinics/1/doctors` → confirm Dr Sharma, Dr Patel appear.
3. `GET /api/clinics/1/availability?doctorId=1&serviceId=1&date=2026-08-24`
   (pick a Monday–Saturday date) → confirm you get a list of slots from 09:00,
   skipping 13:00–14:00, ending before 19:00.
4. `POST /api/appointments`
   ```json
   {
     "clinicId": 1, "doctorId": 1, "serviceId": 1,
     "patientName": "Rahul Patil", "whatsappNumber": "+919876543210",
     "appointmentDate": "2026-08-24", "startTime": "16:00",
     "idempotencyKey": "test-msg-001"
   }
   ```
   → `201 Created`.
5. **Re-send the exact same request** (same `idempotencyKey`) → should return the
   *same* appointment, not a duplicate or an error — this proves the webhook-retry
   protection works.
6. Change `idempotencyKey` and re-send for the *same* `startTime` → should now get
   `409 Conflict` (`SLOT_UNAVAILABLE`) — proves double-booking prevention works.
7. Re-run step 3's availability call → the booked 16:00 slot should now be absent.
8. **Concurrency test** (do this once step 1–7 pass): fire 10 parallel `POST` requests
   for the same doctor/date/time with different idempotency keys — exactly one
   should succeed with 201, the rest should get 409. This is what actually proves
   the pessimistic lock + unique index are doing their job, not just the happy path.

## Step 4 — n8n workflow outline (for when you get there)

```
Webhook (WhatsApp)
  → Function: extract phone + message text
  → HTTP Request: GET /api/clinics/{clinicId}/services (context for AI)
  → (later) Load conversation_session — Phase 1 can start with n8n's own
    workflow static data / a Postgres node querying conversation_session directly
  → AI Agent node (OpenAI/Anthropic node with function-calling) → structured intent
  → Switch node on session.state:
      COLLECTING_* → respond asking next question, update session
      SHOWING_SLOTS → HTTP Request: GET availability → format + respond
      CONFIRMING → HTTP Request: POST /api/appointments → respond with confirmation
                   OR catch 409 → re-fetch availability → offer alternatives
  → WhatsApp node: send response
```

Two things worth deciding before you build this: (1) where `conversation_session`
reads/writes live — directly via n8n's Postgres node, or via new Spring Boot
endpoints (`GET/PUT /api/sessions`) so business rules stay server-side, consistent
with the doc's "n8n should NOT contain core business rules" principle. I'd lean
toward the latter, same as your Outbox+Kafka preference of keeping orchestration
thin. (2) how you handle the AI mishearing "tomorrow evening" as a specific time —
worth having the AI ask for confirmation whenever it infers a time rather than
having one explicitly stated.

## Step 5 — AI prompt design

The system prompt to the LLM should enforce doc section 19's four safety rules
directly — feed it the *actual* available slots as a tool-call result, never let it
freehand a time, and never let it say "confirmed" until the `POST /appointments`
call actually returns 201.

## Known gaps to close before Phase 1 sign-off

- No auth on the Spring Boot APIs yet — fine for local n8n calling over a private
  network, but add at least a shared-secret header before anything touches a public URL.
- `AvailabilityService` doesn't yet account for a doctor being on leave for a single
  day (that's explicitly Phase 2 per the doc, so this is expected, not a bug).
- Appointment code format (`APT-######-XXXX`) is a placeholder — swap for whatever
  ID scheme the clinic wants on printed slips.
